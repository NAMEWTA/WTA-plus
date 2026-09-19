package org.namewta.test.contracts;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.mybatis.aspect.DataPermissionPointcutAdvisor;
import org.namewta.common.mybatis.interceptor.PlusDataPermissionInterceptor;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.demo.domain.bo.TestTreeBo;
import org.namewta.demo.mapper.TestTreeMapper;
import org.namewta.demo.service.impl.TestTreeServiceImpl;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/** Real classic Service/Mapper/MySQL regressions; the runner owns the complete isolated schema. */
@Tag("dev")
@EnabledIfSystemProperty(named = "demo.tree.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoTreeMySqlIntegrationTest {
    private static final long ROOT = 9270001, A = 9270010, B = 9270011;
    private DynamicRoutingDataSource routing;
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate db;
    private TestTreeServiceImpl service;
    private Object previousFactory, previousContext;

    @BeforeAll
    void open() throws Exception {
        String url = System.getProperty("demo.tree.mysql.integration.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_tree_test_");
        previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        var pool = new HikariDataSource(); pool.setJdbcUrl(url); pool.setUsername("root");
        pool.setPassword("owned-tree-test-only"); pool.setMaximumPoolSize(6);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DynamicRoutingDataSource.class, () -> routing, bean -> bean.setDestroyMethodName(""));
        context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter()); context.refresh();
        var config = new MybatisConfiguration(new Environment("owned-t27", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new PlusDataPermissionInterceptor()); config.addInterceptor(interceptor);
        config.addMapper(TestTreeMapper.class);
        String xml = "/mapper/demo/TestTreeMapper.xml";
        try (var input = getClass().getResourceAsStream(xml)) {
            assertThat(input).isNotNull(); new XMLMapperBuilder(input, config, xml, config.getSqlFragments()).parse();
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var mapperProxy = new ProxyFactory(sessions.getMapper(TestTreeMapper.class)); mapperProxy.addAdvisor(new DataPermissionPointcutAdvisor());
        var proxy = new ProxyFactory(new TestTreeServiceImpl((TestTreeMapper) mapperProxy.getProxy())); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        service = (TestTreeServiceImpl) proxy.getProxy();
    }

    @BeforeEach
    void seed() {
        clean();
        for (var edge : List.of(new long[]{ROOT, 0}, new long[]{A, ROOT}, new long[]{B, A})) {
            db.update("insert into test_tree(id,parent_id,dept_id,user_id,tree_name) values (?,?,100,1,'same-name')", edge[0], edge[1]);
        }
    }

    @AfterEach
    void clean() {
        if (db != null) {
            db.execute("drop trigger if exists owned_t27_delete_failure");
            db.update("delete from test_tree where id between 9270000 and 9270999");
        }
    }

    @AfterAll
    void close() throws Exception {
        if (routing != null) routing.destroy(); if (context != null) context.close();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
    }

    @Test
    void nonexistentParentCannotCreateAnOrphan() {
        var before = rows();
        try (var login = mockStatic(LoginHelper.class);
             var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            assertThatThrownBy(() -> service.insertByBo(node(9270020L, 9270999L))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void aDescendantCannotBecomeTheParent() {
        var before = rows();
        try (var login = mockStatic(LoginHelper.class);
             var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            assertThatThrownBy(() -> service.updateByBo(node(A, B))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void childrenPreventDeletionEvenWhenLegacyValidationFlagIsFalse() {
        var before = rows();
        try (var login = mockStatic(LoginHelper.class);
             var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            assertThatThrownBy(() -> service.deleteWithValidByIds(List.of(A), false)).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void rootChildrenCrossTreeMovesAndDuplicateNamesAreValid() {
        asAdmin(() -> service.insertByBo(node(9270020L, 0L)));
        asAdmin(() -> service.insertByBo(node(9270021L, 9270020L)));
        asAdmin(() -> service.updateByBo(node(A, 9270021L)));
        assertInvariant();
        asAdmin(() -> service.updateByBo(node(A, 0L)));
        assertInvariant();
        assertThat(db.queryForObject("select count(*) from test_tree where tree_name='same-name' and id between 9270000 and 9270999", Integer.class)).isEqualTo(5);
        assertThat(asAdmin(() -> service.queryById(B))).isNotNull();
    }

    @Test
    void nullNegativeSelfAndDeletedParentsFailWithoutPartialChanges() {
        db.update("insert into test_tree(id,parent_id,dept_id,user_id,tree_name,del_flag) values (9270020,0,100,1,'deleted',1)");
        var before = rows();
        for (Long parent : java.util.Arrays.asList(null, -1L, A, 9270020L)) {
            assertThatThrownBy(() -> asAdmin(() -> service.updateByBo(node(A, parent)))).isInstanceOf(ServiceException.class);
        }
        assertThatThrownBy(() -> asAdmin(() -> service.insertByBo(node(9270030L, 9270030L)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void existingCycleOrOrphanCannotBeExtended() {
        db.update("update test_tree set parent_id=? where id=?", B, A);
        var before = rows();
        assertThatThrownBy(() -> asAdmin(() -> service.insertByBo(node(9270020L, B)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
        db.update("update test_tree set parent_id=9270999 where id=?", A);
        before = rows();
        assertThatThrownBy(() -> asAdmin(() -> service.updateByBo(node(B, ROOT)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void bulkDeleteRejectsAnyParentOrMissingRowBeforeWriting() {
        var before = rows();
        assertThatThrownBy(() -> asAdmin(() -> service.deleteWithValidByIds(List.of(B, A), true))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> asAdmin(() -> service.deleteWithValidByIds(List.of(B, 9270999L), false))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
        assertThat(asAdmin(() -> service.deleteWithValidByIds(List.of(B, B), false))).isTrue();
        assertThat(asAdmin(() -> service.deleteWithValidByIds(List.of(A), true))).isTrue();
        assertThat(asAdmin(() -> service.deleteWithValidByIds(List.of(ROOT), true))).isTrue();
        assertInvariant();
    }

    @Test
    void databaseFailureRollsBackAllLeafDeletes() {
        asAdmin(() -> service.insertByBo(node(9270020L, ROOT)));
        var before = rows();
        db.execute("create trigger owned_t27_delete_failure before update on test_tree for each row begin if old.id=9270020 and new.del_flag=1 then signal sqlstate '45000' set message_text='owned deletion failure'; end if; end");
        assertThatThrownBy(() -> asAdmin(() -> service.deleteWithValidByIds(List.of(B, 9270020L), true))).isInstanceOf(RuntimeException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void hiddenChildrenStillPreventDeletingAnOtherwiseVisibleParent() {
        db.update("update test_tree set dept_id=200 where id=?", B);
        var before = rows();
        assertThatThrownBy(() -> asScoped(() -> service.deleteWithValidByIds(List.of(A), false))).isInstanceOf(ServiceException.class).hasMessage("存在子节点，不允许删除");
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void dataScopeDeniesHiddenCurrentNodeAndNewParentButAllowsUnchangedHiddenParent() {
        db.update("update test_tree set dept_id=200 where id=?", ROOT);
        db.update("insert into test_tree(id,parent_id,dept_id,user_id,tree_name) values (9270020,0,200,1,'hidden')");
        var before = rows();
        assertThat(asScoped(() -> service.updateByBo(node(A, ROOT)))).isTrue();
        assertThatThrownBy(() -> asScoped(() -> service.updateByBo(node(ROOT, 0L)))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> asScoped(() -> service.updateByBo(node(A, 9270020L)))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> asScoped(() -> service.insertByBo(node(9270030L, 9270020L)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void mixedVisibleAndHiddenLeavesCannotPartiallyDelete() {
        db.update("insert into test_tree(id,parent_id,dept_id,user_id,tree_name) values (9270020,0,200,1,'hidden')");
        var before = rows();
        assertThatThrownBy(() -> asScoped(() -> service.deleteWithValidByIds(List.of(B, 9270020L), true))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void simultaneousOppositeMovesCannotIntroduceACycle() throws Exception {
        asAdmin(() -> service.insertByBo(node(9270020L, ROOT)));
        var results = race(() -> service.updateByBo(node(A, 9270020L)), () -> service.updateByBo(node(9270020L, A)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1); assertInvariant();
    }

    @Test
    void simultaneousOppositeRootMovesUseAnOrderedLockDomain() throws Exception {
        asAdmin(() -> service.insertByBo(node(9270020L, 0L)));
        var results = race(() -> service.updateByBo(node(ROOT, 9270020L)), () -> service.updateByBo(node(9270020L, ROOT)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1); assertInvariant();
    }

    @Test
    void concurrentLeafDeletionAndChildCreationCannotLeaveAnOrphan() throws Exception {
        var results = race(() -> service.deleteWithValidByIds(List.of(B), true), () -> service.insertByBo(node(9270020L, B)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1); assertInvariant();
    }

    @Test
    void concurrentSubtreeMoveAndDescendantInsertionUseRevalidatedParents() throws Exception {
        asAdmin(() -> service.insertByBo(node(9270020L, 0L)));
        var results = race(() -> service.updateByBo(node(A, 9270020L)), () -> service.insertByBo(node(9270030L, B)));
        assertThat(results).contains(true); assertInvariant();
    }

    private static <T> T asAdmin(Supplier<T> work) {
        try (var login = mockStatic(LoginHelper.class); var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            return work.get();
        }
    }

    private static <T> T asScoped(Supplier<T> work) {
        try (var login = mockStatic(LoginHelper.class); var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            var role = new org.namewta.system.api.domain.RoleDTO(); role.setRoleId(9270500L); role.setRoleKey("owned-tree-scope"); role.setDataScope("3");
            var user = new org.namewta.system.api.model.LoginUser(); user.setUserId(9270600L); user.setDeptId(100L); user.setRoles(List.of(role));
            user.setDataScopeRoleMap(Map.of("demo:tree:edit", List.of(role.getRoleId())));
            login.when(LoginHelper::getLoginUser).thenReturn(user);
            org.namewta.common.mybatis.helper.DataPermissionHelper.setAccess(new org.namewta.common.mybatis.core.domain.DataPermissionAccess(Set.of("demo:tree:edit"), Set.of()));
            return work.get();
        } finally { org.namewta.common.mybatis.helper.DataPermissionHelper.removePermission(); }
    }

    private List<Boolean> race(Supplier<Boolean> one, Supplier<Boolean> two) throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(one, two).stream().map(work -> executor.submit((Callable<Boolean>) () -> {
                ready.countDown(); assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                try { return asAdmin(work); } catch (ServiceException expectedConflict) { return false; }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(futures.getFirst().get(15, TimeUnit.SECONDS), futures.getLast().get(15, TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }

    private void assertInvariant() {
        var active = rows().stream().filter(row -> ((Number) row.get("del_flag")).intValue() == 0)
            .collect(java.util.stream.Collectors.toMap(row -> ((Number) row.get("id")).longValue(), row -> ((Number) row.get("parent_id")).longValue()));
        for (long id : active.keySet()) {
            var seen = new java.util.HashSet<Long>();
            while (id != 0) {
                assertThat(seen.add(id)).as("acyclic path").isTrue(); assertThat(active).containsKey(id); id = active.get(id);
            }
        }
    }

    private List<java.util.Map<String, Object>> rows() {
        return db.queryForList("select id,parent_id,tree_name,del_flag from test_tree where id between 9270000 and 9270999 order by id");
    }

    private static TestTreeBo node(Long id, Long parent) {
        var bo = new TestTreeBo(); bo.setId(id); bo.setParentId(parent); bo.setDeptId(100L); bo.setUserId(1L); bo.setTreeName("same-name"); return bo;
    }
}
