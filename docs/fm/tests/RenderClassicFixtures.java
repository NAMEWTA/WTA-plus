import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用真实 FreeMarker 渲染 classic 普通表及两种树表；输出只进入测试临时目录。 */
public final class RenderClassicFixtures {
    /** 参数为模板根、隔离输出目录；使用当前工程真实 Entity/BO/VO 合同。 */
    public static void main(String[] args) throws Exception {
        var configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setDirectoryForTemplateLoading(Path.of(args[0]).toFile());
        configuration.setDefaultEncoding("UTF-8");
        // MyBatis 的 #{...} 必须原样保留，只有 ${...} 属于 FreeMarker 插值。
        configuration.setInterpolationSyntax(Configuration.DOLLAR_INTERPOLATION_SYNTAX);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        for (String name : List.of("TestDemo", "TestTree", "SysDept")) {
            boolean tree = !name.equals("TestDemo"), ancestors = name.equals("SysDept");
            String module = ancestors ? "system" : "demo";
            String pk = ancestors ? "deptId" : "id";
            var context = new HashMap<String, Object>();
            context.put("packageName", "org.namewta." + module);
            context.put("ClassName", name);
            context.put("className", Character.toLowerCase(name.charAt(0)) + name.substring(1));
            context.put("functionName", "模板集成验收");
            context.put("author", "NAMEWTA"); context.put("datetime", "2026-09-19");
            context.put("architectureMode", "classic"); context.put("moduleLifecycle", "legacy");
            context.put("table", Map.of("crud", !tree, "tree", tree));
            context.put("tableName", ancestors ? "sys_dept" : tree ? "test_tree" : "test_demo");
            context.put("pkColumn", Map.of("javaField", pk, "capJavaField", ancestors ? "DeptId" : "Id", "javaType", "Long", "columnName", ancestors ? "dept_id" : "id"));
            context.put("columns", List.of());
            for (String flag : List.of("enableUnique", "enableStatus", "enableSort", "hasBetween")) context.put(flag, false);
            context.put("treeParentCode", "parentId"); context.put("treeParentCap", "ParentId");
            context.put("treeParentColumn", Map.of("columnName", "parent_id"));
            context.put("treeRootValueJavaLiteral", "0L"); context.put("treeRootValue", "0");
            context.put("treeAncestorsField", ancestors ? "ancestors" : "");
            context.put("treeAncestorsColumn", ancestors ? "ancestors" : "");
            context.put("treeAncestorsCap", "Ancestors");
            context.put("treeOrderField", "");
            context.put("treeParentIndex", ancestors ? "idx_sys_dept_parent_id" : "idx_test_tree_parent_id");
            var outputs = new java.util.LinkedHashMap<String, String>();
            String base = "src/org/namewta/" + module + "/";
            outputs.put("java/service.java.ftl", base + "service/I" + name + "Service.java");
            outputs.put("java/serviceImpl.java.ftl", base + "service/impl/" + name + "ServiceImpl.java");
            outputs.put("java/mapper.java.ftl", base + "mapper/" + name + "Mapper.java");
            outputs.put("xml/mapper.xml.ftl", "classes/mapper/" + module + "/" + name + "Mapper.xml");
            for (var output : outputs.entrySet()) {
                Path path = Path.of(args[1]).resolve(output.getValue()); Files.createDirectories(path.getParent());
                try (var writer = Files.newBufferedWriter(path)) {
                    configuration.getTemplate(output.getKey()).process(context, writer);
                }
                System.out.println("RENDERED " + output.getValue());
            }
        }
    }
}
