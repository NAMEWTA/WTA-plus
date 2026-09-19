package org.namewta.test.workflow;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.DefaultLockFailureStrategy;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.liteflow.component.NoopComponent;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.liteflow.complete.*;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.config.WarmFlow;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.FrameworkType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.invoker.FrameInvoker;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.mapper.*;
import org.dromara.warm.plugin.modes.sb.config.BeanConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.satoken.core.service.SaPermissionImpl;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.workflow.domain.bo.FlowNextNodeBo;
import org.namewta.workflow.handler.WorkflowPermissionHandler;
import org.namewta.workflow.mapper.*;
import org.namewta.workflow.oss.WorkflowHistoryOssOwner;
import org.namewta.workflow.service.*;
import org.namewta.workflow.service.impl.FlwTaskServiceImpl;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Real Warm-Flow/MyBatis/MySQL permissions and task transition regression; owns only runner-provided data. */
@Tag("dev")
@EnabledIfSystemProperty(named = "workflow.engine.integration", matches = "true")
class WorkflowTaskEngineIntegrationTest {
    @Test
    void readsRespectTaskRelationshipsAndCompletedTaskCannotAdvanceTwice() throws Exception {
        String url = System.getProperty("workflow.mysql.integration.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_workflow_test_");
        var previousConfig = SaManager.getConfig(); var previousContext = SaManager.getSaTokenContext();
        var previousPermissions = SaManager.getStpInterface(); var previousLogic = StpUtil.getStpLogic();
        var previousFrame = FrameInvoker.frameInvoker; var previousFlow = FlowEngine.getFlowConfig();
        var previousJson = FlowEngine.jsonConvert;
        var engineState = new HashMap<java.lang.reflect.Field, Object>();
        for (var field : FlowEngine.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) { field.setAccessible(true); engineState.put(field, field.get(null)); }
        }
        var previousRequest = RequestContextHolder.getRequestAttributes();
        var previousDao = SaManager.getSaTokenDao(); var ownedDao = new cn.dev33.satoken.dao.SaTokenDaoDefaultImpl(); SaManager.setSaTokenDao(ownedDao);
        Object previousLiteConfig = ReflectionTestUtils.getField(com.yomahub.liteflow.property.LiteflowConfigGetter.class, "liteflowConfig");
        var previousLiteContext = com.yomahub.liteflow.spi.spring.SpringAware.getApplicationContext();
        var liteflowTouched = new boolean[]{false};
        Object previousSpringContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        Object previousBeanFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        var routing = new DynamicRoutingDataSource(List.of());
        var redisConfig = new Config(); redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:" + System.getProperty("profile.redis.integration.port")).setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        RedissonClient redis = Redisson.create(redisConfig);
        try (var pooled = new HikariDataSource(); var context = new GenericApplicationContext()) {
            pooled.setJdbcUrl(url); pooled.setUsername("root"); pooled.setPassword("owned-profile-test-only"); pooled.setMaximumPoolSize(4);
            routing.setPrimary("master"); routing.setStrict(true); routing.addDataSource("master", pooled);
            var db = new JdbcTemplate(routing);
            var configuration = new MybatisConfiguration(new Environment("workflow-owned", new SpringManagedTransactionFactory(), routing));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
            for (Class<?> mapper : List.of(FlowDefinitionMapper.class, FlowNodeMapper.class, FlowSkipMapper.class,
                FlowInstanceMapper.class, FlowTaskMapper.class, FlowHisTaskMapper.class, FlowUserMapper.class, FlowFormMapper.class)) configuration.addMapper(mapper);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) context.getBeanFactory().registerSingleton(mapper.getSimpleName(), sessions.getMapper(mapper));
            var beans = new BeanConfig(); beans.setNewEntity();
            context.getBeanFactory().registerSingleton("engineChart", beans.chartService());
            context.getBeanFactory().registerSingleton("engineForm", beans.flowFormService(beans.formDao()));
            context.getBeanFactory().registerSingleton("engineDefinitions", beans.definitionService(beans.definitionDao()));
            context.getBeanFactory().registerSingleton("engineNodes", beans.nodeService(beans.nodeDao()));
            context.getBeanFactory().registerSingleton("engineSkips", beans.skipService(beans.skipDao()));
            context.getBeanFactory().registerSingleton("engineInstances", beans.instanceService(beans.instanceDao()));
            context.getBeanFactory().registerSingleton("engineTasks", beans.taskService(beans.taskDao()));
            context.getBeanFactory().registerSingleton("engineHistory", beans.hisTaskService(beans.hisTaskDao()));
            context.getBeanFactory().registerSingleton("engineUsers", beans.flowUserService(beans.flowUserDao()));
            IFlwTaskAssigneeService assignees = mock(IFlwTaskAssigneeService.class);
            when(assignees.fetchUsersByStorageIds(anyString())).thenAnswer(invocation -> Arrays.stream(invocation.getArgument(0, String.class).split(","))
                .map(id -> { var user = new UserDTO(); user.setUserId(Long.valueOf(id)); user.setNickName("owned-" + id); return user; }).toList());
            context.getBeanFactory().registerSingleton("workflowPermissions", new WorkflowPermissionHandler(assignees));
            context.registerBean(SpringUtils.class);
            context.registerBean(com.yomahub.liteflow.spi.spring.SpringAware.class);
            context.refresh();
            FrameInvoker.frameInvoker = new FrameInvoker<>();
            FrameInvoker.setBeanFunction(type -> context.getBeanProvider(type).getIfAvailable());
            FrameInvoker.setCfgFunction(key -> null);
            var flow = new WarmFlow(); flow.setFramework(FrameworkType.SPRING_BOOT); flow.setBanner(false); flow.setDataSourceType("mysql");
            FlowEngine.setFlowConfig(flow); flow.init();
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
                .setJwtSecretKey("owned-workflow-fixture-secret-at-least-32-characters"));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setStpInterface(new SaPermissionImpl()); StpUtil.setStpLogic(new StpLogicJwtForSimple());
            var tasks = new FlwTaskServiceImpl(FlowEngine.taskService(), FlowEngine.insService(), FlowEngine.defService(), FlowEngine.hisTaskService(),
                FlowEngine.nodeService(), sessions.getMapper(FlowTaskMapper.class), sessions.getMapper(FlowHisTaskMapper.class),
                mock(org.namewta.system.api.UserService.class), mock(FlwTaskMapper.class), mock(FlwHisTaskMapper.class), mock(FlwCategoryMapper.class),
                sessions.getMapper(FlowNodeMapper.class), assignees, mock(IFlwCommonService.class), mock(IFlwNodeExtService.class), mock(WorkflowHistoryOssOwner.class));
            var completing = completionProxy(tasks, context, sessions, redis, liteflowTouched);
            seedDefinition(db);
            login(2001L);
            var instance = FlowEngine.insService().start("owned-task-16", FlowParams.build().flowCode("owned-task-integrity").flowStatus("waiting").variable(new HashMap<>()));
            Task apply = onlyTask(db, instance.getId());
            FlowEngine.taskService().skip(apply.getId(), FlowParams.build().skipType(SkipType.PASS.getKey()).flowStatus("waiting").hisStatus("pass"));
            Task approval = onlyTask(db, instance.getId());
            assertThat(approval.getNodeCode()).isEqualTo("review");
            var query = new FlowNextNodeBo(); query.setTaskId(approval.getId()); query.setVariables(new HashMap<>());
            for (int relation=2; relation<=4; relation++) db.update("insert into flow_user (id,type,processed_by,associated) values (?,?,?,?)", 916020+relation, String.valueOf(relation), String.valueOf(2010+relation), approval.getId());
            for (long userId : List.of(2001L, 2002L, 2012L, 2013L, 2014L)) {
                login(userId);
                assertThat(tasks.getNextNodeList(query)).extracting("nodeCode").containsExactly("final");
                assertThat(tasks.getBackTaskNode(approval.getId(), "review")).isNotEmpty();
            }
            for (String permission : List.of("workflow:task:list", "workflow:task:edit")) {
                login(2003L, permission);
                assertThat(tasks.getNextNodeList(query)).extracting("nodeCode").containsExactly("final");
                assertThat(tasks.getBackTaskNode(approval.getId(), "review")).isNotEmpty();
            }
            login(org.namewta.common.core.constant.SystemConstants.SUPER_ADMIN_USER_ID);
            assertThat(tasks.getNextNodeList(query)).isNotEmpty();
            assertThat(tasks.getBackTaskNode(approval.getId(), "review")).isNotEmpty();
            var instanceReads = new org.namewta.workflow.service.impl.FlwInstanceServiceImpl(FlowEngine.insService(), FlowEngine.defService(), FlowEngine.taskService(),
                sessions.getMapper(FlowHisTaskMapper.class), sessions.getMapper(FlowInstanceMapper.class), tasks, mock(FlwInstanceMapper.class), mock(FlwCategoryMapper.class));
            var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new org.namewta.workflow.controller.FlwInstanceController(FlowEngine.insService(), instanceReads),
                new org.namewta.workflow.controller.FlwTaskController(tasks))
                .addInterceptors(new cn.dev33.satoken.interceptor.SaInterceptor())
                .setControllerAdvice(new org.namewta.common.satoken.handler.SaTokenExceptionHandler(),
                    new org.namewta.common.web.handler.GlobalExceptionHandler()).build();
            login(2003L);
            String strangerToken = StpUtil.getTokenValue();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/workflow/task/getNextNodeList")
                .param("taskId", approval.getId().toString()).param("variables", "{\"amount\":12.5,\"approved\":true}")
                .header("Authorization", "Bearer " + strangerToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/workflow/instance/instanceVariable/" + instance.getId())
                .header("Authorization", "Bearer " + strangerToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
            login(2003L, "workflow:instance:variableQuery");
            String monitorToken = StpUtil.getTokenValue();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/workflow/instance/instanceVariable/" + instance.getId())
                .header("Authorization", "Bearer " + monitorToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
            login(2002L);
            String assigneeToken = StpUtil.getTokenValue();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/workflow/task/getNextNodeList")
                .param("taskId", approval.getId().toString()).param("variables", "{\"amount\":12.5,\"approved\":true}")
                .header("Authorization", "Bearer " + assigneeToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].nodeCode").value("final"));
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/workflow/task/getNextNodeList")
                .contentType("application/json").content("{\"taskId\":" + approval.getId() + "}")
                .header("Authorization", "Bearer " + assigneeToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(405));
            login(2003L);
            Throwable nextDenied = catchThrowable(() -> tasks.getNextNodeList(query));
            Throwable backDenied = catchThrowable(() -> tasks.getBackTaskNode(approval.getId(), "review"));
            assertThatThrownBy(() -> FlowEngine.taskService().skip(approval.getId(), FlowParams.build().skipType(SkipType.PASS.getKey()).flowStatus("waiting").hisStatus("pass"))).isInstanceOf(RuntimeException.class);
            assertThat(onlyTask(db, instance.getId()).getId()).isEqualTo(approval.getId());
            login(2002L);
            var command = new CompleteTaskBo(); command.setTaskId(approval.getId()); command.setMessage("owned approval"); command.setMessageType(List.of());
            String lockKey = "owned-t16:org.namewta.workflow.service.impl.FlwTaskServiceImplcompleteTask#" + approval.getId();
            var occupied = redis.getLock(lockKey);
            assertThat(occupied.tryLockAsync(0, 30, TimeUnit.SECONDS, 916L).get()).isTrue();
            try {
                assertThatThrownBy(() -> completing.completeTask(command)).isInstanceOf(com.baomidou.lock.exception.LockFailureException.class);
                assertThat(onlyTask(db, instance.getId()).getId()).isEqualTo(approval.getId());
            } finally { occupied.unlockAsync(916L).get(); }
            assertThat(completing.completeTask(command)).isTrue();
            assertThat(occupied.isLocked()).isFalse();
            long history = db.queryForObject("select count(*) from flow_his_task where instance_id=?", Long.class, instance.getId());
            assertThat(onlyTask(db, instance.getId()).getNodeCode()).isEqualTo("final");
            assertThatThrownBy(() -> completing.completeTask(command)).isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
            assertThat(occupied.isLocked()).isFalse();
            assertThat(db.queryForObject("select count(*) from flow_his_task where instance_id=?", Long.class, instance.getId())).isEqualTo(history);
            System.out.println("T-16: real engine rejected stranger processing and duplicate task transition without history growth");
            org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(nextDenied).as("stranger next nodes").isInstanceOf(NotPermissionException.class),
                () -> assertThat(backDenied).as("stranger back nodes").isInstanceOf(NotPermissionException.class));
        } finally {
            RequestContextHolder.setRequestAttributes(previousRequest);
            redis.shutdown(); routing.destroy(); ownedDao.destroy(); SaManager.setSaTokenDao(previousDao);
            if (liteflowTouched[0]) {
                FlowBus.removeChain("completeTaskChain");
                for (String name : List.of("completePrepare", "completeExecute", "completeNeedAutoPass", "completeAutoPass", "noop")) FlowBus.removeNode(name);
            }
            ReflectionTestUtils.setField(com.yomahub.liteflow.property.LiteflowConfigGetter.class, "liteflowConfig", previousLiteConfig);
            new com.yomahub.liteflow.spi.spring.SpringAware().setApplicationContext(previousLiteContext);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousSpringContext);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousBeanFactory);
            for (var entry : engineState.entrySet()) entry.getKey().set(null, entry.getValue());
            SaManager.setConfig(previousConfig); SaManager.setSaTokenContext(previousContext); SaManager.setStpInterface(previousPermissions); StpUtil.setStpLogic(previousLogic);
            FrameInvoker.frameInvoker = previousFrame; FlowEngine.setFlowConfig(previousFlow); FlowEngine.jsonConvert = previousJson;
        }
    }
    private static FlwTaskServiceImpl completionProxy(FlwTaskServiceImpl target, GenericApplicationContext context, SqlSessionTemplate sessions, RedissonClient redis, boolean[] touched) throws Exception {
        var config = new LiteflowConfig(); config.setPrintBanner(false); config.setPrintExecutionLog(false);
        com.yomahub.liteflow.property.LiteflowConfigGetter.setLiteflowConfig(config);
        assertThat(FlowBus.containChain("completeTaskChain")).isFalse(); touched[0] = true;
        var executor = new FlowExecutor(config);
        context.getBeanFactory().registerSingleton("ownedFlowExecutor", executor);
        var history = new WorkflowHistoryOssOwner(sessions.getMapper(FlowHisTaskMapper.class), mock(org.namewta.system.api.OssService.class));
        FlowBus.addManagedNode("completePrepare", new CompletePrepareComponent(sessions.getMapper(FlowTaskMapper.class), FlowEngine.insService()));
        FlowBus.addManagedNode("completeExecute", new CompleteExecuteComponent(FlowEngine.taskService(), history));
        FlowBus.addManagedNode("completeNeedAutoPass", new CompleteNeedAutoPassComponent());
        FlowBus.addManagedNode("completeAutoPass", new CompleteAutoPassComponent(FlowEngine.taskService(), sessions.getMapper(FlowTaskMapper.class)));
        FlowBus.addManagedNode("noop", new NoopComponent());
        try (var input = WorkflowTaskEngineIntegrationTest.class.getResourceAsStream("/liteflow/task-chain.el.xml")) {
            var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance(); factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var chains = factory.newDocumentBuilder().parse(input).getElementsByTagName("chain");
            for (int i=0; i<chains.getLength(); i++) {
                var chain = (org.w3c.dom.Element) chains.item(i);
                if (chain.getAttribute("name").equals("completeTaskChain")) LiteFlowChainELBuilder.createChain().setChainId("completeTaskChain").setEL(chain.getTextContent()).build();
            }
        }
        assertThat(FlowBus.containChain("completeTaskChain")).isTrue();
        var properties = new Lock4jProperties(); properties.setLockKeyPrefix("owned-t16"); properties.setAcquireTimeout(100L); properties.setExpire(10000L);
        var template = new LockTemplate(); template.setProperties(properties); template.setExecutors(List.of(new RedissonLockExecutor(redis))); template.afterPropertiesSet();
        var interceptor = new LockInterceptor(template, List.of(new DefaultLockKeyBuilder(context.getBeanFactory())), List.of(new DefaultLockFailureStrategy()), properties); interceptor.afterPropertiesSet();
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new LockAnnotationAdvisor(interceptor, 0));
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (FlwTaskServiceImpl) proxy.getProxy();
    }
    private static void login(long id, String... permissions) {
        var request = new MockHttpServletRequest(); request.addHeader("User-Agent", "Mozilla/5.0 OwnedWorkflowFixture/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        var user = new LoginUser(); user.setUserId(id); user.setUsername("owned-" + id); user.setUserType("sys_user");
        user.setClientPk(1L); user.setClientKey("admin-web"); user.setDeptId(1L); user.setIpaddr("127.0.0.1"); user.setLoginLocation("owned fixture");
        user.setMenuPermission(Set.of(permissions)); user.setRolePermission(Set.of("owned"));
        LoginHelper.login(user, new SaLoginParameter().setTimeout(300).setExtra(LoginHelper.CLIENT_PK_KEY, 1L).setExtra(LoginHelper.CLIENT_KEY, "admin-web"));
    }
    private static Task onlyTask(JdbcTemplate db, long instance) {
        List<Long> ids = db.queryForList("select id from flow_task where instance_id=? and del_flag='0'", Long.class, instance);
        assertThat(ids).hasSize(1); return FlowEngine.taskService().getById(ids.getFirst());
    }
    private static void seedDefinition(JdbcTemplate db) {
        db.update("insert into flow_definition (id,flow_code,flow_name,version,is_publish) values (916000,'owned-task-integrity','Owned integrity','1',1)");
        String[] names = {"start", "apply", "review", "final", "end"};
        for (int i=0;i<names.length;i++) {
            int type = i==0 ? 0 : i==4 ? 2 : 1;
            String user = i==1 ? "2001" : i==2 ? "2002" : "2004";
            db.update("insert into flow_node (id,node_type,definition_id,node_code,node_name,permission_flag,node_ratio,version) values (?,?,916000,?,?,?,'0','1')",916001+i,type,names[i],names[i],user);
            if (i>0) db.update("insert into flow_skip (id,definition_id,now_node_code,now_node_type,next_node_code,next_node_type,skip_type) values (?,916000,?,?,?,?, 'PASS')",916010+i,names[i-1],i==1?0:1,names[i],type);
        }
    }
}
