import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/** 实际执行渲染后的 Service/Mapper/XML；不得连接已存在的业务数据库。 */
public final class ClassicTemplateMySqlChecks {
    private static final long ROOT = 9271001, CHILD = 9271002, LEAF = 9271003, OTHER = 9271010;
    private static int passed;
    private final JdbcTemplate db;
    private final Object service;
    private final Class<?> boClass;
    private final String table, pk;
    private final boolean ancestors;

    /** 从隔离输出加载生成类型，避免误测工作树原始 Service。 */
    private ClassicTemplateMySqlChecks(DynamicRoutingDataSource routing, String name, Path generated) throws Exception {
        ancestors = name.equals("SysDept"); String module = ancestors ? "system" : "demo";
        table = ancestors ? "sys_dept" : name.equals("TestTree") ? "test_tree" : "test_demo";
        pk = ancestors ? "dept_id" : "id"; db = new JdbcTemplate(routing);
        String base = "org.namewta." + module;
        Class<?> mapper = Class.forName(base + ".mapper." + name + "Mapper");
        Class<?> target = Class.forName(base + ".service.impl." + name + "ServiceImpl");
        assertThat(Path.of(target.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath()).isEqualTo(generated.toRealPath());
        var config = new MybatisConfiguration(new Environment("owned-template-checks", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true); config.addMapper(mapper);
        String xml = "/mapper/" + module + "/" + name + "Mapper.xml";
        try (var input = getClass().getResourceAsStream(xml)) {
            assertThat(input).isNotNull(); new XMLMapperBuilder(input, config, xml, config.getSqlFragments()).parse();
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var proxy = new ProxyFactory(target.getConstructor(mapper).newInstance(sessions.getMapper(mapper)));
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        service = proxy.getProxy(); boClass = Class.forName(base + ".domain.bo." + name + "Bo");
    }

    /** 参数为 owned JDBC URL、生成 class 目录；测试对象不注册到产品 Spring 容器。 */
    public static void main(String[] args) throws Exception {
        assertThat(args[0]).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_tree_test_");
        var pool = new HikariDataSource(); pool.setJdbcUrl(args[0]); pool.setUsername("root");
        pool.setPassword("owned-tree-test-only"); pool.setMaximumPoolSize(6);
        var routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter()); context.refresh();
            for (String name : List.of("TestDemo", "TestTree", "SysDept")) {
                var checks = new ClassicTemplateMySqlChecks(routing, name, Path.of(args[1]));
                try {
                    if (name.equals("TestDemo")) checks.ordinaryCrud(); else checks.treeChecks();
                } finally { checks.clean(); }
            }
        } finally { routing.destroy(); }
        System.out.println("TEMPLATE_RESULT tests=" + passed + " failures=0 skipped=0");
    }

    /** 普通表生成结果仍可执行完整 CRUD。 */
    private void ordinaryCrud() throws Exception {
        clean(); Object bo = boClass.getConstructor().newInstance();
        set(bo, "Id", Long.class, ROOT); set(bo, "DeptId", Long.class, 100L); set(bo, "UserId", Long.class, 1L);
        set(bo, "TestKey", String.class, "template"); set(bo, "Value", String.class, "before"); set(bo, "OrderNum", Integer.class, 0);
        assertThat(write("insertByBo", bo)).isEqualTo(true);
        assertThat(invoke("queryById", new Class<?>[]{Long.class}, ROOT)).isNotNull();
        set(bo, "Value", String.class, "after"); assertThat(write("updateByBo", bo)).isEqualTo(true);
        assertThat(db.queryForObject("select value from test_demo where id=?", String.class, ROOT)).isEqualTo("after");
        assertThat(remove(List.of(ROOT), true)).isEqualTo(true);
        assertThat(invoke("queryById", new Class<?>[]{Long.class}, ROOT)).isNull(); pass("ordinary CRUD");
    }

    /** 两种树均通过父节点、环、批删及 SQL 故障回滚；含路径树另验子树移动。 */
    private void treeChecks() throws Exception {
        seed(); var before = rows();
        for (Long parent : java.util.Arrays.asList(null, -1L, 9271999L, OTHER)) {
            assertThatThrownBy(() -> write("insertByBo", node(OTHER, parent))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before); pass("reject invalid insert parents");

        seed(); before = rows();
        for (Long parent : java.util.Arrays.asList(null, -1L, CHILD, LEAF, 9271999L)) {
            assertThatThrownBy(() -> write("updateByBo", node(CHILD, parent))).isInstanceOf(ServiceException.class);
        }
        assertThat(rows()).isEqualTo(before); pass("reject invalid move and descendant cycle");

        seed(); before = rows();
        assertThatThrownBy(() -> remove(List.of(CHILD), false)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> remove(List.of(LEAF, CHILD), true)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> remove(List.of(LEAF, 9271999L), true)).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before);
        assertThat(remove(List.of(LEAF, LEAF), false)).isEqualTo(true);
        assertThat(remove(List.of(CHILD), false)).isEqualTo(true); pass("parent deletion refusal and leaf deletion");

        seed(); write("insertByBo", node(OTHER, 0L)); write("insertByBo", node(OTHER + 1, OTHER));
        write("updateByBo", node(CHILD, OTHER + 1));
        assertThat(db.queryForObject("select parent_id from " + table + " where " + pk + "=?", Long.class, CHILD)).isEqualTo(OTHER + 1);
        if (ancestors) {
            assertPath(CHILD, "0," + OTHER + "," + (OTHER + 1));
            assertPath(LEAF, "0," + OTHER + "," + (OTHER + 1) + "," + CHILD);
        }
        write("updateByBo", node(CHILD, 0L));
        if (ancestors) { assertPath(CHILD, "0"); assertPath(LEAF, "0," + CHILD); }
        assertThat(db.queryForObject("select count(*) from " + table + " where " + pk + " between 9271000 and 9271999 and " + (ancestors ? "dept_name" : "tree_name") + "='same-name'", Integer.class)).isEqualTo(5);
        pass("cross-root move and detach preserve subtree; duplicate names allowed");

        seed(); db.update("update " + table + " set parent_id=? where " + pk + "=?", LEAF, CHILD); before = rows();
        assertThatThrownBy(() -> write("insertByBo", node(OTHER, LEAF))).isInstanceOf(ServiceException.class);
        assertThat(rows()).isEqualTo(before); pass("existing corrupt parent chain refused");

        seed(); write("insertByBo", node(OTHER, ROOT)); before = rows();
        db.execute("create trigger owned_template_failure before update on " + table + " for each row begin if old." + pk + "=" + OTHER + " and new.del_flag=1 then signal sqlstate '45000' set message_text='owned template failure'; end if; end");
        assertThatThrownBy(() -> remove(List.of(LEAF, OTHER), true)).isInstanceOf(RuntimeException.class);
        assertThat(rows()).isEqualTo(before); pass("failed leaf batch rolls back");

        if (ancestors) {
            seed(); write("insertByBo", node(OTHER, 0L)); before = rows();
            db.execute("create trigger owned_template_failure before update on sys_dept for each row begin if old.dept_id=" + CHILD + " and new.parent_id<>old.parent_id then signal sqlstate '45000' set message_text='owned move failure'; end if; end");
            assertThatThrownBy(() -> write("updateByBo", node(CHILD, OTHER))).isInstanceOf(RuntimeException.class);
            assertThat(rows()).isEqualTo(before); pass("failed move rolls back earlier descendant paths");
            seed(); db.update("update sys_dept set ancestors='0,999' where dept_id=?", LEAF); before = rows();
            assertThatThrownBy(() -> write("updateByBo", node(CHILD, 0L))).isInstanceOf(ServiceException.class);
            assertThat(rows()).isEqualTo(before); pass("stale descendant path rejects move");
        }

        seed(); write("insertByBo", node(OTHER, ROOT));
        race(() -> write("updateByBo", node(CHILD, OTHER)), () -> write("updateByBo", node(OTHER, CHILD)));
        assertInvariant(); pass("opposite sibling moves allow exactly one winner");

        seed(); write("insertByBo", node(OTHER, 0L));
        race(() -> write("updateByBo", node(ROOT, OTHER)), () -> write("updateByBo", node(OTHER, ROOT)));
        assertInvariant(); pass("opposite root moves cannot introduce a cycle");

        seed();
        race(() -> remove(List.of(LEAF), true), () -> write("insertByBo", node(OTHER, LEAF)));
        assertInvariant(); pass("leaf deletion and child insertion cannot leave an orphan");
    }

    /** 并发命令只接受明确业务冲突；SQL 死锁、超时或基础设施异常均失败。 */
    private void race(Callable<Object> first, Callable<Object> second) throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (var command : List.of(first, second)) futures.add(executor.submit(() -> {
                ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try { assertThat(command.call()).isEqualTo(true); return true; }
                catch (ServiceException conflict) { return false; }
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            int winners = 0;
            for (var future : futures) if (future.get(20, TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
        }
    }

    /** 最终数据库必须无环、无孤儿，ancestors 必须与真实父链完全一致。 */
    private void assertInvariant() {
        var active = rows().stream().filter(row -> "0".equals(String.valueOf(row.get("del_flag")))).toList();
        var parents = new java.util.HashMap<Long, Long>();
        for (var row : active) parents.put(((Number) row.get(pk)).longValue(), ((Number) row.get("parent_id")).longValue());
        for (var row : active) {
            long id = ((Number) row.get(pk)).longValue(); var seen = new HashSet<Long>(); seen.add(id);
            var chain = new ArrayList<Long>(); long parent = parents.get(id);
            while (parent != 0) {
                assertThat(seen.add(parent)).isTrue(); assertThat(parents).containsKey(parent);
                chain.add(parent); parent = parents.get(parent);
            }
            java.util.Collections.reverse(chain);
            String expected = "0" + chain.stream().map(value -> "," + value).collect(java.util.stream.Collectors.joining());
            if (ancestors) assertThat(row.get("ancestors")).isEqualTo(expected);
        }
    }

    /** 将反射包装还原为真实服务异常，避免把反射失败误认成业务拒绝。 */
    private Object invoke(String method, Class<?>[] types, Object... values) throws Exception {
        try { return service.getClass().getMethod(method, types).invoke(service, values); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw (Error) error.getCause();
        }
    }

    /** 调用生成的保存方法。 */
    private Object write(String method, Object bo) throws Exception { return invoke(method, new Class<?>[]{boClass}, bo); }
    /** 调用生成的批量删除。 */
    private Object remove(Collection<Long> ids, Boolean valid) throws Exception { return invoke("deleteWithValidByIds", new Class<?>[]{Collection.class, Boolean.class}, ids, valid); }
    /** 构造当前工程真实 BO，结构字段由模板负责。 */
    private Object node(Long id, Long parent) throws Exception {
        Object bo = boClass.getConstructor().newInstance();
        set(bo, ancestors ? "DeptId" : "Id", Long.class, id); set(bo, "ParentId", Long.class, parent);
        set(bo, ancestors ? "DeptName" : "TreeName", String.class, "same-name");
        if (ancestors) set(bo, "OrderNum", Integer.class, 0);
        else { set(bo, "DeptId", Long.class, 100L); set(bo, "UserId", Long.class, 1L); }
        return bo;
    }
    /** 设置已存在的 BO 属性，错误属性直接使测试失败。 */
    private static void set(Object bo, String field, Class<?> type, Object value) throws Exception { bo.getClass().getMethod("set" + field, type).invoke(bo, value); }
    /** 只清理测试器的固定 ID 范围及专属触发器。 */
    private void clean() { db.execute("drop trigger if exists owned_template_failure"); db.update("delete from " + table + " where " + pk + " between 9271000 and 9271999"); }
    /** 通过生成的新增路径建立父子孙结构。 */
    private void seed() throws Exception { clean(); write("insertByBo", node(ROOT, 0L)); write("insertByBo", node(CHILD, ROOT)); write("insertByBo", node(LEAF, CHILD)); }
    /** 完整持久化快照验证无部分提交。 */
    private List<Map<String, Object>> rows() { return db.queryForList("select * from " + table + " where " + pk + " between 9271000 and 9271999 order by " + pk); }
    /** 校验数据库路径与实际父边一致。 */
    private void assertPath(long id, String expected) { assertThat(db.queryForObject("select ancestors from sys_dept where dept_id=?", String.class, id)).isEqualTo(expected); }
    /** 只有断言全部通过才计数。 */
    private void pass(String scenario) { passed++; System.out.println("TEMPLATE_PASS " + table + ": " + scenario); }
}
