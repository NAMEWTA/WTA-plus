package org.namewta.test.contracts;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.mybatis.aspect.DataPermissionPointcutAdvisor;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.mybatis.interceptor.PlusDataPermissionInterceptor;
import org.namewta.common.redis.aspectj.RepeatSubmitAspect;
import org.namewta.common.satoken.core.service.SaPermissionImpl;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.security.handler.AllUrlHandler;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.demo.controller.TestDemoController;
import org.namewta.demo.controller.TestTreeController;
import org.namewta.demo.mapper.TestDemoMapper;
import org.namewta.demo.mapper.TestTreeMapper;
import org.namewta.demo.service.impl.TestDemoServiceImpl;
import org.namewta.demo.service.impl.TestTreeServiceImpl;
import org.namewta.system.controller.system.SysPostController;
import org.namewta.system.mapper.SysDeptMapper;
import org.namewta.system.mapper.SysPostMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.mapper.SysUserPostMapper;
import org.namewta.system.service.impl.SysDeptServiceImpl;
import org.namewta.system.service.impl.SysPostServiceImpl;
import org.namewta.workflow.controller.FlwSpelController;
import org.namewta.workflow.mapper.FlwSpelMapper;
import org.namewta.workflow.service.impl.FlwSpelServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.namewta.test.contracts.CrudHttpDispatchIntegrationTest.login;
import static org.namewta.test.contracts.CrudHttpDispatchIntegrationTest.send;

/** Real HTTP, security, validation, repeat-submit, Controller/Service/Mapper and owned MySQL/Redis. */
@Tag("dev")
@EnabledIfSystemProperty(named = "crud.mysql.integration", matches = "true")
class CrudMySqlHttpIntegrationTest {
    record Resource(String path, String table, String idColumn, String idProperty, String valueColumn,
                    String valueProperty, Map<String, Object> fields) { }

    @Test
    void systemWorkflowAndDemoCrudPersistOnlyAuthorizedValidatedPostRequests() throws Exception {
        String url = System.getProperty("crud.mysql.integration.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_crud_test_");
        int redisPort = Integer.parseInt(System.getProperty("crud.redis.integration.port"));
        assertThat(redisPort).isBetween(1, 65535);
        var previousConfig = SaManager.getConfig(); var previousContext = SaManager.getSaTokenContext();
        var previousPermissions = SaManager.getStpInterface(); var previousLogic = StpUtil.getStpLogic();
        var previousDao = SaManager.getSaTokenDao(); var previousRequest = RequestContextHolder.getRequestAttributes();
        var previousMatcher = cn.dev33.satoken.strategy.SaStrategy.instance.routeMatcher;
        Object previousSpring = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        Object previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        var ownedDao = new SaTokenDaoDefaultImpl();
        var routing = new DynamicRoutingDataSource(List.of());
        var redisConfig = new Config(); redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:" + redisPort)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        var redis = Redisson.create(redisConfig);
        var server = new Server(new QueuedThreadPool(16, 2));
        var connector = new ServerConnector(server); connector.setHost("127.0.0.1"); connector.setPort(0); server.addConnector(connector);
        connector.getConnectionFactory(org.eclipse.jetty.server.HttpConnectionFactory.class).getHttpConfiguration().setPersistentConnectionsEnabled(false);
        var handler = new ServletContextHandler(); handler.setContextPath("/"); server.setHandler(handler);
        var results = new ArrayList<Map<String, Object>>();
        try (var context = new GenericWebApplicationContext(); var pool = new HikariDataSource();
             var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            pool.setJdbcUrl(url); pool.setUsername("root"); pool.setPassword("owned-crud-test-only"); pool.setMaximumPoolSize(4);
            routing.setPrimary("master"); routing.addDataSource("master", pool);
            new SpringUtils().postProcessBeanFactory(context.getDefaultListableBeanFactory());
            var db = new JdbcTemplate(routing);
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
                .setJwtSecretKey("owned-crud-http-fixture-secret-at-least-32-characters"));
            SaManager.setSaTokenDao(ownedDao); SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            new cn.dev33.satoken.spring.SaTokenContextRegister();
            SaManager.setStpInterface(new SaPermissionImpl()); StpUtil.setStpLogic(new StpLogicJwtForSimple());
            var configuration = new MybatisConfiguration(new Environment("owned-t26", new SpringManagedTransactionFactory(), routing));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults().setMetaObjectHandler(new InjectionMetaObjectHandler()));
            var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new PlusDataPermissionInterceptor());
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor()); interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            configuration.addInterceptor(interceptor);
            for (var mapper : List.of(SysPostMapper.class, SysDeptMapper.class, SysRoleMapper.class, SysUserMapper.class,
                SysUserPostMapper.class, FlwSpelMapper.class, TestDemoMapper.class, TestTreeMapper.class)) configuration.addMapper(mapper);
            for (String xml : List.of("/mapper/system/SysPostMapper.xml", "/mapper/system/SysDeptMapper.xml", "/mapper/demo/TestDemoMapper.xml", "/mapper/demo/TestTreeMapper.xml")) {
                try (var input = getClass().getResourceAsStream(xml)) {
                    assertThat(input).as(xml).isNotNull(); new XMLMapperBuilder(input, configuration, xml, configuration.getSqlFragments()).parse();
                }
            }
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            var deptMapperProxy = new ProxyFactory(sessions.getMapper(SysDeptMapper.class)); deptMapperProxy.addAdvisor(new DataPermissionPointcutAdvisor());
            var deptMapper = (SysDeptMapper) deptMapperProxy.getProxy();
            var deptProxy = new ProxyFactory(new SysDeptServiceImpl(deptMapper, sessions.getMapper(SysRoleMapper.class), sessions.getMapper(SysUserMapper.class)));
            deptProxy.setProxyTargetClass(true);
            deptProxy.addAdvisor(new com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor(
                new com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor(true), com.baomidou.dynamic.datasource.annotation.DSTransactional.class));
            var departments = (SysDeptServiceImpl) deptProxy.getProxy();
            var posts = new SysPostServiceImpl(sessions.getMapper(SysPostMapper.class), sessions.getMapper(SysDeptMapper.class), sessions.getMapper(SysUserPostMapper.class));
            var demoProxy = new ProxyFactory(sessions.getMapper(TestDemoMapper.class)); demoProxy.addAdvisor(new DataPermissionPointcutAdvisor());
            var demo = new TestDemoServiceImpl((TestDemoMapper) demoProxy.getProxy());
            var treeMapperProxy = new ProxyFactory(sessions.getMapper(TestTreeMapper.class)); treeMapperProxy.addAdvisor(new DataPermissionPointcutAdvisor());
            var treeProxy = new ProxyFactory(new TestTreeServiceImpl((TestTreeMapper) treeMapperProxy.getProxy())); treeProxy.setProxyTargetClass(true);
            treeProxy.addAdvisor(new com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor(
                new com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor(true), com.baomidou.dynamic.datasource.annotation.DSTransactional.class));
            var trees = (TestTreeServiceImpl) treeProxy.getProxy();
            context.setServletContext(handler.getServletContext());
            new AnnotatedBeanDefinitionReader(context).register(CrudHttpDispatchIntegrationTest.Mvc.class);
            context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter());
            context.registerBean(RedissonClient.class, () -> redis, bean -> bean.setDestroyMethodName(""));
            context.registerBean(DynamicRoutingDataSource.class, () -> routing, bean -> bean.setDestroyMethodName(""));
            context.registerBean(org.springframework.cache.concurrent.ConcurrentMapCacheManager.class);
            context.registerBean(SaTokenExceptionHandler.class); context.registerBean(GlobalExceptionHandler.class);
            for (Object target : List.of(new SysPostController(posts, departments), new org.namewta.system.controller.system.SysDeptController(departments, posts), new FlwSpelController(new FlwSpelServiceImpl(sessions.getMapper(FlwSpelMapper.class))),
                new TestDemoController(demo), new TestTreeController(trees))) {
                var proxy = new AspectJProxyFactory(target); proxy.setProxyTargetClass(true);
                proxy.addAspect(new LogAspect()); proxy.addAspect(new RepeatSubmitAspect());
                register(context, target.getClass(), proxy.getProxy());
            }
            context.refresh();
            var allUrls = new AllUrlHandler(); allUrls.setUrls(List.of("/**")); context.getBeanFactory().registerSingleton("allUrlHandler", allUrls);
            handler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/"); server.start();
            String allowed = login(SystemConstants.SUPER_ADMIN_USER_ID, Set.of("*:*:*"), Set.of(SystemConstants.SUPER_ADMIN_ROLE_KEY));
            String denied = login(26002L, Set.of(), Set.of()); RequestContextHolder.resetRequestAttributes();
            String origin = "http://127.0.0.1:" + connector.getLocalPort();
            List<Resource> resources = List.of(
                new Resource("/system/post", "sys_post", "post_id", "postId", "post_name", "postName", Map.of("deptId", 100, "postCode", "owned-t26", "postSort", 0, "status", "0")),
                new Resource("/workflow/spel", "flow_spel", "id", "id", "view_spel", "viewSpel", Map.of("status", "0", "componentName", "owned")),
                new Resource("/demo/demo", "test_demo", "id", "id", "value", "value", Map.of("deptId", 100, "userId", 1, "orderNum", 0, "testKey", "owned-t26", "version", 0)),
                new Resource("/demo/tree", "test_tree", "id", "id", "tree_name", "treeName", Map.of("deptId", 100, "userId", 1, "parentId", 0)));
            long nextId = 9260100;
            for (Resource resource : resources) {
                long first = nextId++, second = nextId++; var firstBody = new LinkedHashMap<String, Object>();
                for (long id : List.of(first, second)) {
                    var body = new LinkedHashMap<>(resource.fields()); body.put(resource.idProperty(), id); body.put(resource.valueProperty(), "owned-t26-" + id);
                    if (body.containsKey("postCode")) body.put("postCode", "owned-t26-" + id);
                    assertCode(client, origin, "POST", resource.path(), allowed, body, 200);
                    if (id == first) firstBody.putAll(body);
                }
                assertThat(active(db, resource, first, second)).isEqualTo(2);
                var read = send(client, origin, "GET", resource.path() + "/" + first, allowed, "owned-crud", null);
                assertThat(JsonUtils.parseMap(read.body()).getObj("data")).isNotNull();
                assertThat(read.body()).contains("owned-t26-" + first);
                firstBody.put(resource.valueProperty(), "owned-t26-updated-" + first);
                assertCode(client, origin, "PUT", resource.path(), allowed, firstBody, 405);
                assertThat(value(db, resource, first)).isEqualTo("owned-t26-" + first);
                assertCode(client, origin, "POST", resource.path() + "/update", denied, firstBody, 403);
                assertThat(value(db, resource, first)).isEqualTo("owned-t26-" + first);
                assertCode(client, origin, "POST", resource.path() + "/update", allowed, Map.of(), 500);
                assertThat(value(db, resource, first)).isEqualTo("owned-t26-" + first);
                assertCode(client, origin, "POST", resource.path() + "/update", allowed, firstBody, 200);
                assertThat(value(db, resource, first)).isEqualTo("owned-t26-updated-" + first);
                String bulk = resource.path() + "/" + first + "," + second;
                assertCode(client, origin, "DELETE", bulk, allowed, null, 405);
                assertCode(client, origin, "POST", bulk, denied, null, 403);
                assertThat(active(db, resource, first, second)).isEqualTo(2);
                assertCode(client, origin, "POST", bulk, allowed, null, 200);
                assertThat(active(db, resource, first, second)).isZero();
                results.add(Map.of("resource", resource.path(), "table", resource.table(), "created", 2,
                    "read", true, "updated", 1, "bulk_deleted", 2, "old_methods_rejected", true, "denied_and_invalid_unchanged", true));
            }
            long treeRoot = 9260300, treeChild = 9260301;
            assertCode(client, origin, "POST", "/demo/tree", allowed, treeNode(treeRoot, 0), 200);
            assertCode(client, origin, "POST", "/demo/tree", allowed, treeNode(treeChild, treeRoot), 200);
            var beforeDemoTree = db.queryForList("select id,parent_id,del_flag from test_tree where id in (?,?) order by id", treeRoot, treeChild);
            assertCode(client, origin, "POST", "/demo/tree", allowed, treeNode(9260302, 9260399), 500);
            assertCode(client, origin, "POST", "/demo/tree/update", allowed, treeNode(treeRoot, treeChild), 500);
            assertCode(client, origin, "POST", "/demo/tree/" + treeRoot, allowed, null, 500);
            assertCode(client, origin, "POST", "/demo/tree/update", denied, treeNode(treeChild, 0), 403);
            assertThat(db.queryForList("select id,parent_id,del_flag from test_tree where id in (?,?) order by id", treeRoot, treeChild)).isEqualTo(beforeDemoTree);
            assertCode(client, origin, "POST", "/demo/tree/update", allowed, treeNode(treeChild, 0), 200);
            assertCode(client, origin, "POST", "/demo/tree/" + treeRoot + "," + treeChild, allowed, null, 200);
            results.add(Map.of("resource", "/demo/tree", "tree_invariants_over_http", true, "missing_parent_cycle_parent_delete_denied", true));
            long root = 9260200, child = 9260201, grandchild = 9260202;
            for (long id : List.of(root, child, grandchild)) {
                assertCode(client, origin, "POST", "/system/dept", allowed, department(id, id == root ? 0 : id - 1), 200);
            }
            var beforeTree = db.queryForList("select dept_id,parent_id,ancestors from sys_dept where dept_id between ? and ? order by dept_id", root, grandchild);
            var readTree = send(client, origin, "GET", "/system/dept/" + child, allowed, "owned-crud", null);
            assertThat(readTree.body()).contains("owned-t26-" + child);
            assertCode(client, origin, "PUT", "/system/dept", allowed, department(child, 0), 405);
            assertCode(client, origin, "POST", "/system/dept/update", denied, department(child, 0), 403);
            assertCode(client, origin, "POST", "/system/dept/update", allowed, department(root, grandchild), 500);
            assertThat(db.queryForList("select dept_id,parent_id,ancestors from sys_dept where dept_id between ? and ? order by dept_id", root, grandchild)).isEqualTo(beforeTree);
            assertCode(client, origin, "POST", "/system/dept/update", allowed, department(child, 0), 200);
            assertThat(db.queryForObject("select ancestors from sys_dept where dept_id=?", String.class, grandchild)).isEqualTo("0," + child);
            assertCode(client, origin, "DELETE", "/system/dept/" + grandchild, allowed, null, 405);
            assertCode(client, origin, "POST", "/system/dept/" + grandchild, denied, null, 403);
            for (long id : List.of(grandchild, child, root)) assertCode(client, origin, "POST", "/system/dept/" + id, allowed, null, 200);
            assertThat(db.queryForObject("select count(*) from sys_dept where dept_id between ? and ? and del_flag='0'", Integer.class, root, grandchild)).isZero();
            results.add(Map.of("resource", "/system/dept", "table", "sys_dept", "created", 3, "read", true,
                "subtree_updated", true, "single_deleted", 3, "cycle_and_denied_unchanged", true));
            String output = System.getProperty("crud.mysql.output");
            if (output != null) Files.writeString(Path.of(output), JsonUtils.toJsonString(Map.of("resources", results,
                "scope", "real Jetty/security/validation/repeat-submit/log/Controller/Service/MyBatis/MySQL; owned Redis; no mocked business service or mapper")));
        } finally {
            if (server.isStarted() || server.isStarting()) server.stop(); server.destroy();
            routing.destroy(); redis.shutdown(); ownedDao.destroy(); SaManager.setSaTokenDao(previousDao); SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext); SaManager.setStpInterface(previousPermissions); StpUtil.setStpLogic(previousLogic);
            cn.dev33.satoken.strategy.SaStrategy.instance.routeMatcher = previousMatcher;
            RequestContextHolder.setRequestAttributes(previousRequest);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousSpring);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        }
    }

    private static <T> void register(GenericWebApplicationContext context, Class<T> type, Object bean) {
        context.registerBean(type.getName(), type, () -> type.cast(bean));
    }

    private static void assertCode(HttpClient client, String origin, String method, String path, String token, Object body, int code) throws Exception {
        var response = send(client, origin, method, path, token, "owned-crud", body == null ? null : JsonUtils.toJsonString(body));
        assertThat(JsonUtils.parseMap(response.body()).getInt("code")).as("%s %s: %s", method, path, response.body()).isEqualTo(code);
    }

    private static int active(JdbcTemplate db, Resource resource, long first, long second) {
        return db.queryForObject("select count(*) from " + resource.table() + " where " + resource.idColumn() + " in (?,?) and del_flag='0'", Integer.class, first, second);
    }

    private static Map<String, Object> department(long id, long parent) {
        return Map.of("deptId", id, "parentId", parent, "deptName", "owned-t26-" + id, "orderNum", 0, "status", "0");
    }

    private static Map<String, Object> treeNode(long id, long parent) {
        return Map.of("id", id, "parentId", parent, "treeName", "owned-tree-" + id, "deptId", 100, "userId", 1);
    }

    private static String value(JdbcTemplate db, Resource resource, long id) {
        return db.queryForObject("select " + resource.valueColumn() + " from " + resource.table() + " where " + resource.idColumn() + "=?", String.class, id);
    }
}
