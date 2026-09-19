package org.namewta.test.department;

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
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.mybatis.aspect.DataPermissionPointcutAdvisor;
import org.namewta.common.mybatis.interceptor.PlusDataPermissionInterceptor;
import org.namewta.common.mybatis.helper.DataPermissionHelper;
import org.namewta.common.mybatis.core.domain.DataPermissionAccess;
import org.namewta.system.api.domain.RoleDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.bo.SysDeptBo;
import org.namewta.system.mapper.SysDeptMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.impl.SysDeptServiceImpl;
import org.namewta.system.service.impl.SysDataScopeServiceImpl;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/** Owned MySQL schema and the actual classic Service/Mapper/transaction chain. */
@Tag("dev")
@EnabledIfSystemProperty(named = "department.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepartmentTreeMySqlIntegrationTest {
    private static final long ROOT = 9250001, OTHER_ROOT = 9250002, A = 9250010, B = 9250011, C = 9250012, D = 9250020, E = 9250030;
    private DynamicRoutingDataSource routing;
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate db;
    private SysDeptServiceImpl service;
    private RollbackBoundary boundary;
    private ConcurrentMapCacheManager caches;
    private Object previousFactory, previousContext;

    @BeforeAll
    void open() throws Exception {
        String url = System.getProperty("department.mysql.integration.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_dept_test_");
        previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        var pool = new HikariDataSource(); pool.setJdbcUrl(url); pool.setUsername("root");
        pool.setPassword("owned-dept-test-only"); pool.setMaximumPoolSize(6);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DynamicRoutingDataSource.class, () -> routing, bd -> bd.setDestroyMethodName(""));
        context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter());
        context.registerBean(ConcurrentMapCacheManager.class, () -> new ConcurrentMapCacheManager()); context.refresh();
        caches = context.getBean(ConcurrentMapCacheManager.class);
        var config = new MybatisConfiguration(new Environment("owned-t25", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new PlusDataPermissionInterceptor());
        config.addInterceptor(interceptor);
        config.addMapper(SysDeptMapper.class); config.addMapper(SysRoleMapper.class); config.addMapper(SysUserMapper.class);
        String xml = "/mapper/system/SysDeptMapper.xml";
        try (var stream = getClass().getResourceAsStream(xml)) {
            assertThat(stream).isNotNull(); new XMLMapperBuilder(stream, config, xml, config.getSqlFragments()).parse();
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var mapperProxy = new ProxyFactory(sessions.getMapper(SysDeptMapper.class));
        mapperProxy.addAdvisor(new DataPermissionPointcutAdvisor());
        var departments = (SysDeptMapper) mapperProxy.getProxy();
        context.getBeanFactory().registerSingleton("sdss", new SysDataScopeServiceImpl(null, departments));
        service = transactional(new SysDeptServiceImpl(departments,
            sessions.getMapper(SysRoleMapper.class), sessions.getMapper(SysUserMapper.class)));
        boundary = transactional(new RollbackBoundary(service));
    }

    @BeforeEach
    void seed() {
        clean();
        insert(ROOT, 0, "0"); insert(OTHER_ROOT, 0, "0");
        insert(A, ROOT, "0," + ROOT); insert(B, A, "0," + ROOT + "," + A);
        insert(C, B, "0," + ROOT + "," + A + "," + B); insert(D, ROOT, "0," + ROOT);
        insert(E, OTHER_ROOT, "0," + OTHER_ROOT);
    }

    @AfterEach
    void clean() {
        if (db == null) return;
        db.execute("drop trigger if exists owned_t25_descendant_failure");
        db.update("delete from sys_dept where dept_id between 9250000 and 9250999");
    }

    @AfterAll
    void close() throws Exception {
        if (routing != null) routing.destroy();
        if (context != null) context.close();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
    }

    @Test
    void serviceRejectsDescendantParentEvenWhenTheCallerBypassesTheDropdown() {
        var before = rows();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            assertThatThrownBy(() -> service.updateDept(move(A, C))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void missingParentIsNotTheRootSentinel() {
        var before = rows();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            assertThatThrownBy(() -> service.updateDept(move(A, 9250999))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void selfNullNegativeAndDeletedParentsAreRejectedWithoutPartialWrites() {
        insert(9250040, ROOT, "0," + ROOT);
        db.update("update sys_dept set del_flag='1' where dept_id=9250040");
        var before = rows();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            for (Long parent : java.util.Arrays.asList(A, null, -1L, 9250040L)) {
                var bo = move(A, D); bo.setParentId(parent);
                assertThatThrownBy(() -> service.updateDept(bo)).isInstanceOf(ServiceException.class);
            }
        }
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void legalCrossRootMoveAndDetachingToRootRewriteEveryDescendant() {
        asAdmin(() -> service.updateDept(move(A, E)));
        assertPath(A, "0," + OTHER_ROOT + "," + E);
        assertPath(B, "0," + OTHER_ROOT + "," + E + "," + A);
        assertPath(C, "0," + OTHER_ROOT + "," + E + "," + A + "," + B);
        assertInvariant();
        asAdmin(() -> service.updateDept(move(A, 0)));
        assertPath(A, "0"); assertPath(B, "0," + A); assertPath(C, "0," + A + "," + B);
        assertInvariant();
    }

    @Test
    void rootsAndChildrenCanBeInsertedAndLeafDeletedThroughTheSameBoundary() {
        asAdmin(() -> service.insertDept(move(9250050, 0)));
        asAdmin(() -> service.insertDept(move(9250051, 9250050)));
        assertPath(9250050, "0"); assertPath(9250051, "0,9250050");
        assertThatThrownBy(() -> asAdmin(() -> service.deleteDeptById(9250050L))).isInstanceOf(ServiceException.class);
        asAdmin(() -> service.deleteDeptById(9250051L));
        asAdmin(() -> service.deleteDeptById(9250050L));
        assertInvariant();
    }

    @Test
    void malformedStoredPathsAndExistingCyclesRequireRepairBeforeMutation() {
        db.update("update sys_dept set ancestors='0,99999' where dept_id=?", B);
        var before = rows();
        assertThatThrownBy(() -> asAdmin(() -> service.updateDept(move(A, E)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
        db.update("update sys_dept set parent_id=? where dept_id=?", C, A);
        before = rows();
        assertThatThrownBy(() -> asAdmin(() -> service.updateDept(move(B, D)))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void descendantWriteFailureRollsBackTheWholeSubtreeAndKeepsCaches() {
        var before = rows(); seedCaches();
        db.execute("create trigger owned_t25_descendant_failure before update on sys_dept for each row begin if old.dept_id=9250012 then signal sqlstate '45000' set message_text='owned descendant failure'; end if; end");
        assertThatThrownBy(() -> asAdmin(() -> service.updateDept(move(A, E)))).isInstanceOf(RuntimeException.class);
        assertThat(rows()).isEqualTo(before); assertCachesPresent();
    }

    @Test
    void outerRollbackDoesNotEvictAndSuccessfulCommitEvictsAfterReturningFromTheService() {
        var before = rows(); seedCaches();
        assertThatThrownBy(() -> asAdmin(() -> boundary.update(move(A, E), this::assertCachesPresent, true)))
            .hasMessage("owned outer rollback");
        assertThat(rows()).isEqualTo(before); assertCachesPresent();
        asAdmin(() -> boundary.update(move(A, E), this::assertCachesPresent, false));
        assertThat(caches.getCache(CacheNames.SYS_DEPT_AND_CHILD).get(ROOT)).isNull();
        for (long id : List.of(A, B, C)) assertThat(caches.getCache(CacheNames.SYS_DEPT).get(id)).isNull();
        assertInvariant();
    }

    @Test
    void simultaneousOppositeMovesWithinOneTreeCannotCreateACycle() throws Exception {
        var results = race(() -> service.updateDept(move(A, D)), () -> service.updateDept(move(D, A)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertInvariant();
    }

    @Test
    void simultaneousOppositeRootMovesUseTheSameOrderedLockDomain() throws Exception {
        var results = race(() -> service.updateDept(move(ROOT, OTHER_ROOT)), () -> service.updateDept(move(OTHER_ROOT, ROOT)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertInvariant();
    }

    @Test
    void concurrentDeleteAndChildInsertCannotLeaveAnOrphan() throws Exception {
        var results = race(() -> service.deleteDeptById(D), () -> service.insertDept(move(9250055, D)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertInvariant();
    }

    @Test
    void concurrentAncestorMoveAndNewDescendantUseTheCommittedParentPath() throws Exception {
        var results = race(() -> service.updateDept(move(A, E)), () -> service.insertDept(move(9250055, C)));
        assertThat(results).contains(true);
        assertInvariant();
    }

    @Test
    void dataScopeIsEnforcedAfterLockingWithoutRequiringUnchangedParentsInScope() {
        try (var login = mockStatic(LoginHelper.class);
             var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            var user = scopedUser("3"); login.when(LoginHelper::getLoginUser).thenReturn(user);
            DataPermissionHelper.setAccess(new DataPermissionAccess(java.util.Set.of("system:dept:edit"), java.util.Set.of()));
            assertThat(service.updateDept(move(A, ROOT))).isEqualTo(1);
            var before = rows();
            assertThatThrownBy(() -> service.updateDept(move(A, D))).hasMessage("没有权限访问部门数据！");
            assertThatThrownBy(() -> service.updateDept(move(B, A))).hasMessage("没有权限访问部门数据！");
            assertThatThrownBy(() -> service.insertDept(move(9250055, D))).hasMessage("没有权限访问部门数据！");
            assertThatThrownBy(() -> service.deleteDeptById(D)).hasMessage("没有权限访问部门数据！");
            assertThatThrownBy(() -> service.updateDept(move(A, 0))).hasMessage("只有超级管理员可以设置根部门");
            assertThat(rows()).isEqualTo(before);
        } finally { DataPermissionHelper.removePermission(); }
    }

    @Test
    void corruptAncestorsCannotGrantAMoveOutsideTheActualDepartmentTree() {
        db.update("update sys_dept set ancestors=? where dept_id=?", "0," + ROOT + "," + A, E);
        var before = rows();
        try (var login = mockStatic(LoginHelper.class);
             var holder = mockStatic(cn.dev33.satoken.context.SaHolder.class)) {
            holder.when(cn.dev33.satoken.context.SaHolder::getStorage).thenReturn(new cn.dev33.satoken.context.mock.SaStorageForMock());
            login.when(LoginHelper::getLoginUser).thenReturn(scopedUser("4"));
            DataPermissionHelper.setAccess(new DataPermissionAccess(java.util.Set.of("system:dept:edit"), java.util.Set.of()));
            assertThat(service.updateDept(move(B, A))).isEqualTo(1);
            assertThatThrownBy(() -> service.updateDept(move(B, E))).hasMessage("部门层级数据异常，请先修复");
            assertThat(rows()).isEqualTo(before);
        } finally { DataPermissionHelper.removePermission(); }
    }

    private LoginUser scopedUser(String scope) {
        var role = new RoleDTO(); role.setRoleId(9250100L); role.setRoleKey("owned-role"); role.setDataScope(scope);
        var user = new LoginUser(); user.setUserId(9250200L); user.setDeptId(A); user.setRoles(List.of(role));
        user.setDataScopeRoleMap(java.util.Map.of("system:dept:edit", List.of(role.getRoleId()))); return user;
    }

    private List<Boolean> race(Callable<Integer> one, Callable<Integer> two) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(one, two).stream().map(work -> executor.submit(() -> {
                ready.countDown(); assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                try { return asAdmin(work) == 1; }
                catch (ServiceException expectedConflict) { return false; }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(futures.getFirst().get(15, TimeUnit.SECONDS), futures.getLast().get(15, TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }

    private void seedCaches() {
        caches.getCache(CacheNames.SYS_DEPT_AND_CHILD).put(ROOT, "old-tree");
        for (long id : List.of(A, B, C)) caches.getCache(CacheNames.SYS_DEPT).put(id, "old-department");
    }

    private void assertCachesPresent() {
        assertThat(caches.getCache(CacheNames.SYS_DEPT_AND_CHILD).get(ROOT)).isNotNull();
        for (long id : List.of(A, B, C)) assertThat(caches.getCache(CacheNames.SYS_DEPT).get(id)).isNotNull();
    }

    private void assertPath(long id, String ancestors) {
        assertThat(db.queryForObject("select ancestors from sys_dept where dept_id=?", String.class, id)).isEqualTo(ancestors);
    }

    private void assertInvariant() {
        var active = rows().stream().filter(row -> "0".equals(row.get("del_flag")))
            .collect(java.util.stream.Collectors.toMap(row -> ((Number) row.get("dept_id")).longValue(), row -> row));
        for (var row : active.values()) {
            var seen = new java.util.HashSet<Long>(); var parents = new java.util.ArrayList<Long>();
            long id = ((Number) row.get("dept_id")).longValue(); seen.add(id);
            long parent = ((Number) row.get("parent_id")).longValue();
            while (parent != 0) {
                assertThat(seen.add(parent)).as("acyclic parent chain for %s", id).isTrue();
                assertThat(active).containsKey(parent); parents.add(parent);
                parent = ((Number) active.get(parent).get("parent_id")).longValue();
            }
            java.util.Collections.reverse(parents);
            String expected = "0" + parents.stream().map(value -> "," + value).collect(java.util.stream.Collectors.joining());
            assertThat(row.get("ancestors")).as("canonical path for %s", id).isEqualTo(expected);
        }
    }

    private static int asAdmin(Callable<Integer> work) {
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            return work.call();
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static class RollbackBoundary {
        private final SysDeptServiceImpl service;
        public RollbackBoundary(SysDeptServiceImpl service) { this.service = service; }
        @DSTransactional
        public int update(SysDeptBo bo, Runnable during, boolean fail) {
            int result = service.updateDept(bo); during.run();
            if (fail) throw new ServiceException("owned outer rollback");
            return result;
        }
    }

    private void insert(long id, long parent, String ancestors) {
        db.update("insert into sys_dept(dept_id,parent_id,ancestors,dept_name,order_num,status,del_flag) values(?,?,?,? ,0,'0','0')",
            id, parent, ancestors, "Owned " + id);
    }

    private List<java.util.Map<String, Object>> rows() {
        return db.queryForList("select dept_id,parent_id,ancestors,status,del_flag from sys_dept where dept_id between 9250000 and 9250999 order by dept_id");
    }

    private static SysDeptBo move(long id, long parent) {
        var bo = new SysDeptBo(); bo.setDeptId(id); bo.setParentId(parent); bo.setDeptName("Owned " + id);
        bo.setOrderNum(0); bo.setStatus("0"); return bo;
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        ProxyFactory proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }
}
