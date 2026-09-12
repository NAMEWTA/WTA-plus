package org.namewta.test.profile.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileStructureContractTest {

    private static final Set<String> CONTROLLER_SURFACES = Set.of("admin", "self", "anonymous");
    private static final Set<String> RETIRED_LAYER_NAMES = Set.of("repository", "manager");
    private static final Set<String> STATEMENT_ELEMENTS = Set.of("select", "insert", "update", "delete");
    private static final Set<String> BASE_MAPPER_METHODS = Arrays.stream(BaseMapper.class.getMethods())
        .map(Method::getName)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Pattern HTTP_CONTROLLER = Pattern.compile(
        "(?m)^\\s*@(?:RestController|Controller)\\b");
    private static final Pattern SA_IGNORE = Pattern.compile("(?m)^\\s*@SaIgnore\\b");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern MAPPER_INTERFACE = Pattern.compile(
        "\\binterface\\s+(\\w+Mapper)\\s+extends[^\\{]*\\bBaseMapperPlus\\s*<", Pattern.DOTALL);
    private static final Pattern SQL_ANNOTATION = Pattern.compile(
        "@(?:Select|Insert|Update|Delete)(?:Provider)?\\b");
    private static final Pattern SERVICE_IMPL_DEPENDENCY = Pattern.compile(
        "\\bprivate\\s+final\\s+\\w+ServiceImpl\\b|import\\s+org\\.dromara\\.profile\\.[^;]+\\.service\\.impl\\.\\w+ServiceImpl;");
    private static final Pattern STRING_CONSTANT = Pattern.compile(
        "(?m)^\\s*(?:(?:public|protected|private|static|final)\\s+)*String\\s+\\w+\\s*=");
    private static final Pattern TABLE_NAME = Pattern.compile("@TableName\\(\"(profile_[a-z0-9_]+)\"\\)");
    private static final Pattern DDL_PROFILE_TABLE = Pattern.compile(
        "(?im)^\\s*create\\s+table\\s+(profile_[a-z0-9_]+)\\s*\\(");

    @Test
    void httpControllersStayInExplicitAccessSurfaces() throws Exception {
        List<String> violations = new ArrayList<>();

        for (ProfileModule module : profileModules()) {
            Path controllerRoot = module.packageRoot().resolve("controller");
            for (Path source : javaFiles(controllerRoot)) {
                String content = Files.readString(source);
                if (!HTTP_CONTROLLER.matcher(content).find()) {
                    continue;
                }

                Path relative = controllerRoot.relativize(source);
                String surface = relative.getNameCount() > 1 ? relative.getName(0).toString() : "<root>";
                if (!CONTROLLER_SURFACES.contains(surface)) {
                    violations.add(display(source) + " is an HTTP Controller outside admin/self/anonymous");
                }

                boolean ignored = SA_IGNORE.matcher(content).find();
                if (ignored && !"anonymous".equals(surface)) {
                    violations.add(display(source) + " uses @SaIgnore outside controller/anonymous");
                }
                if ("anonymous".equals(surface) && !ignored) {
                    violations.add(display(source) + " is an anonymous Controller without @SaIgnore");
                }
            }
        }

        assertThat(violations).as("profile Controller access-surface violations").isEmpty();
    }

    @Test
    void mappersUseBaseMapperPlusAndExternalXmlForEveryCustomMethod() throws Exception {
        List<String> violations = new ArrayList<>();

        for (ProfileModule module : profileModules()) {
            Map<String, MapperSource> mappers = mapperSources(module, violations);
            Path expectedXmlDirectory = module.moduleRoot()
                .resolve("src/main/resources/mapper")
                .resolve(module.name());

            for (Path xml : xmlFiles(module.moduleRoot().resolve("src/main/resources/mapper"))) {
                if (!xml.getParent().normalize().equals(expectedXmlDirectory.normalize())) {
                    violations.add(display(xml) + " must be directly under resources/mapper/" + module.name());
                }
                String mapperName = stripSuffix(xml.getFileName().toString(), ".xml");
                if (!mappers.containsKey(mapperName)) {
                    violations.add(display(xml) + " has no matching Mapper interface");
                }
            }

            for (MapperSource mapper : mappers.values()) {
                String source = Files.readString(mapper.path());
                if (SQL_ANNOTATION.matcher(source).find()) {
                    violations.add(display(mapper.path()) + " contains an annotation-based SQL statement");
                }
                if (source.contains("\"\"\"")) {
                    violations.add(display(mapper.path()) + " contains a Java SQL text block");
                }
                if (STRING_CONSTANT.matcher(source).find()) {
                    violations.add(display(mapper.path()) + " contains a String SQL assembly constant");
                }

                Set<String> customMethods = declaredAbstractMethods(mapper.qualifiedName(), violations);
                Set<String> inheritedCollisions = new LinkedHashSet<>(customMethods);
                inheritedCollisions.retainAll(BASE_MAPPER_METHODS);
                if (!inheritedCollisions.isEmpty()) {
                    violations.add(display(mapper.path())
                        + " redeclares BaseMapper statement ids " + inheritedCollisions);
                }
                if (customMethods.isEmpty()) {
                    continue;
                }

                Path xml = expectedXmlDirectory.resolve(mapper.simpleName() + ".xml");
                if (!Files.isRegularFile(xml)) {
                    violations.add(display(mapper.path()) + " has custom methods but no " + display(xml));
                    continue;
                }

                MapperXml mapperXml = readMapperXml(xml);
                if (!mapper.qualifiedName().equals(mapperXml.namespace())) {
                    violations.add(display(xml) + " namespace is " + mapperXml.namespace()
                        + ", expected " + mapper.qualifiedName());
                }
                Set<String> missingIds = new LinkedHashSet<>(customMethods);
                missingIds.removeAll(mapperXml.statementIds());
                if (!missingIds.isEmpty()) {
                    violations.add(display(xml) + " does not map custom methods " + missingIds);
                }
            }
        }

        assertThat(violations).as("profile Mapper/XML structure violations").isEmpty();
    }

    @Test
    void servicesAndDaosStayOnTheLayeredAxisAndSpringTransactionsRemainRetired() throws Exception {
        List<String> violations = new ArrayList<>();

        for (ProfileModule module : profileModules()) {
            Path serviceRoot = module.packageRoot().resolve("service");
            for (Path source : javaFiles(serviceRoot)) {
                Path parent = source.getParent().normalize();
                if (!parent.equals(serviceRoot.normalize())) {
                    violations.add(display(source)
                        + " must be directly under service; nested production service package is "
                        + display(parent));
                }
            }

            Path mapperResult = module.packageRoot().resolve("mapper/result");
            if (Files.exists(mapperResult)) {
                violations.add(display(mapperResult) + " must use domain/vo instead");
            }
            for (Path source : javaFiles(module.moduleRoot().resolve("src/main/java"))) {
                Path relative = module.packageRoot().relativize(source);
                String fileName = source.getFileName().toString();
                if (fileName.endsWith("DataSupport.java")) {
                    violations.add(display(source)
                        + " uses the retired *DataSupport forwarding abstraction");
                }
                if (fileName.matches(".*(?:Repository|Manager)\\.java")) {
                    violations.add(display(source)
                        + " uses a retired repository/manager layer type name");
                }
                for (Path segment : relative) {
                    if (RETIRED_LAYER_NAMES.contains(segment.toString().toLowerCase())) {
                        violations.add(display(source)
                            + " is under a retired repository/manager package");
                        break;
                    }
                }
                String content = Files.readString(source);
                if (SERVICE_IMPL_DEPENDENCY.matcher(content).find()) {
                    violations.add(display(source)
                        + " depends on a retired ServiceImpl type");
                }
                if (content.contains("org.springframework.transaction.annotation.Transactional")) {
                    violations.add(display(source) + " uses Spring @Transactional instead of @DSTransactional");
                }
            }
        }

        assertThat(violations).as("profile service/transaction structure violations").isEmpty();
    }

    @Test
    void profileEntitiesMirrorTheMySqlBaselineFromTheDomainRoot() throws Exception {
        Set<String> ddlTables = new LinkedHashSet<>();
        Matcher ddlMatcher = DDL_PROFILE_TABLE.matcher(Files.readString(
            SqlBaselinePaths.file("50-namewta-ddl.sql")));
        while (ddlMatcher.find()) {
            ddlTables.add(ddlMatcher.group(1));
        }

        Set<String> entityTables = new LinkedHashSet<>();
        List<String> violations = new ArrayList<>();
        for (ProfileModule module : profileModules()) {
            Path domainRoot = module.packageRoot().resolve("domain");
            for (String retiredDirectory : List.of("entity", "row")) {
                Path retired = domainRoot.resolve(retiredDirectory);
                if (Files.exists(retired)) {
                    violations.add(display(retired) + " is a retired technical domain directory");
                }
            }
            for (Path source : javaFiles(domainRoot)) {
                String content = Files.readString(source);
                Matcher entityMatcher = TABLE_NAME.matcher(content);
                if (!entityMatcher.find()) {
                    continue;
                }
                if (!source.getParent().normalize().equals(domainRoot.normalize())) {
                    violations.add(display(source) + " must place table Entity directly under domain");
                }
                if (!content.contains("extends BaseEntity") || !content.contains("@TableId")
                    || !content.contains("@Version") || !content.contains("@TableLogic")) {
                    violations.add(display(source)
                        + " must extend BaseEntity and map @TableId, @Version, and @TableLogic");
                }
                entityTables.add(entityMatcher.group(1));
            }
        }

        assertThat(ddlTables).as("profile tables in the MySQL 50 DDL baseline").isNotEmpty();
        assertThat(entityTables).as("profile Entity table coverage").containsAll(ddlTables);
        assertThat(violations).as("profile Entity structure violations").isEmpty();
    }

    private static Map<String, MapperSource> mapperSources(ProfileModule module, List<String> violations)
        throws Exception {
        Map<String, MapperSource> mappers = new LinkedHashMap<>();
        for (Path source : javaFiles(module.packageRoot().resolve("mapper"))) {
            String content = Files.readString(source);
            Matcher mapperMatcher = MAPPER_INTERFACE.matcher(content);
            if (!mapperMatcher.find()) {
                continue;
            }
            Matcher packageMatcher = PACKAGE.matcher(content);
            if (!packageMatcher.find()) {
                violations.add(display(source) + " has no package declaration");
                continue;
            }
            String simpleName = mapperMatcher.group(1);
            MapperSource previous = mappers.put(simpleName,
                new MapperSource(source, simpleName, packageMatcher.group(1) + "." + simpleName));
            if (previous != null) {
                violations.add("duplicate profile Mapper name: " + simpleName);
            }
        }

        for (Path source : javaFiles(module.packageRoot().resolve("mapper"))) {
            String content = Files.readString(source);
            if (Pattern.compile("\\binterface\\s+\\w+Mapper\\b").matcher(content).find()
                && !MAPPER_INTERFACE.matcher(content).find()) {
                violations.add(display(source) + " does not extend BaseMapperPlus");
            }
        }
        return mappers;
    }

    private static Set<String> declaredAbstractMethods(String qualifiedName, List<String> violations) {
        try {
            Class<?> mapperType = Class.forName(qualifiedName);
            return Arrays.stream(mapperType.getDeclaredMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(Method::getName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        } catch (ReflectiveOperationException exception) {
            violations.add("cannot inspect Mapper " + qualifiedName + ": " + exception.getMessage());
            return Set.of();
        }
    }

    private static MapperXml readMapperXml(Path xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(xml.toFile());
        Element root = document.getDocumentElement();
        Set<String> statementIds = new LinkedHashSet<>();
        NodeList elements = root.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Node node = elements.item(index);
            if (node instanceof Element element && STATEMENT_ELEMENTS.contains(element.getTagName())
                && element.hasAttribute("id")) {
                statementIds.add(element.getAttribute("id"));
            }
        }
        return new MapperXml(root.getAttribute("namespace"), statementIds);
    }

    private static List<Path> javaFiles(Path root) throws Exception {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .sorted()
                .toList();
        }
    }

    private static List<Path> xmlFiles(Path root) throws Exception {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".xml"))
                .sorted()
                .toList();
        }
    }

    private static List<ProfileModule> profileModules() {
        Path root = repositoryRoot();
        Path profile = root.resolve("wta-modules/wta-profile");
        return List.of(
            new ProfileModule("person", profile.resolve("wta-profile-person"), "org.namewta.profile.person"),
            new ProfileModule("enterprise", profile.resolve("wta-profile-enterprise"),
                "org.namewta.profile.enterprise")
        );
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && (!Files.isRegularFile(current.resolve("wta-admin/pom.xml"))
            || !Files.isDirectory(current.resolve("wta-modules/wta-profile")))) {
            current = current.getParent();
        }
        assertThat(current).as("backend repository root").isNotNull();
        return current;
    }

    private static String display(Path path) {
        Path root = repositoryRoot();
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.startsWith(root) ? root.relativize(normalized).toString() : normalized.toString();
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    private record ProfileModule(String name, Path moduleRoot, String basePackage) {
        private Path packageRoot() {
            return moduleRoot.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        }
    }

    private record MapperSource(Path path, String simpleName, String qualifiedName) {
    }

    private record MapperXml(String namespace, Set<String> statementIds) {
    }
}
