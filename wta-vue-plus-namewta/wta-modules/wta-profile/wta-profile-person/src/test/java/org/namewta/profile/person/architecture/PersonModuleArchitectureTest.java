package org.namewta.profile.person.architecture;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class PersonModuleArchitectureTest {

    private static final Path MODULE = Path.of(System.getProperty("basedir"));
    private static final Path JAVA = MODULE.resolve("src/main/java/org/dromara/profile/person");
    private static final Path XML = MODULE.resolve("src/main/resources/mapper/person");
    private static final Pattern MAPPER_METHOD = Pattern.compile(
        "(?ms)^ {4}(?:[\\w<>,.?\\[\\] ]+\\s+)+(\\w+)\\s*\\([^;{}]*?\\);");

    @Test
    void controllersUseOnlyExplicitAccessSurfaces() throws Exception {
        Path controllers = JAVA.resolve("controller");
        try (Stream<Path> files = Files.walk(controllers)) {
            List<Path> controllersFound = files
                .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                .toList();
            assertThat(controllersFound).isNotEmpty();
            for (Path controller : controllersFound) {
                String surface = controllers.relativize(controller).getName(0).toString();
                String source = Files.readString(controller);
                assertThat(surface).isIn("admin", "self", "anonymous");
                if (surface.equals("anonymous")) {
                    assertThat(controller.getFileName().toString()).contains("Anonymous");
                    assertThat(source).contains("@SaIgnore");
                } else {
                    assertThat(source).doesNotContain("@SaIgnore");
                }
            }
        }
    }

    @Test
    void domainRootContainsOnlyCompleteMybatisEntities() throws Exception {
        try (Stream<Path> files = Files.list(JAVA.resolve("domain"))) {
            for (Path entity : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(entity);
                assertThat(source).contains("extends BaseEntity", "@TableName", "@TableId", "@Version", "@TableLogic");
            }
        }
        assertThat(JAVA.resolve("domain/entity")).doesNotExist();
        assertThat(JAVA.resolve("domain/row")).doesNotExist();
        assertThat(JAVA.resolve("domain/model/read")).isDirectory();
        assertThat(JAVA.resolve("service/persistence")).doesNotExist();
    }

    @Test
    void readModelsHaveDedicatedPackageAndStayOutOfHttpBoundary() throws Exception {
        Path readRoot = JAVA.resolve("domain/model/read");
        try (Stream<Path> files = Files.walk(readRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertThat(file.getFileName().toString()).matches(".*(Row|AdminRows)\\.java");
                assertThat(Files.readString(file))
                    .contains("package org.namewta.profile.person.domain.model.read;");
            }
        }
        for (Path boundary : List.of(JAVA.resolve("controller"), JAVA.resolve("usecase"))) {
            try (Stream<Path> files = Files.walk(boundary)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(file))
                        .doesNotContain("import org.namewta.profile.person.domain.model.read.");
                }
            }
        }
        try (Stream<Path> files = Files.list(XML)) {
            for (Path xml : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
                assertThat(Files.readString(xml))
                    .doesNotMatch("(?s).*org\\.dromara\\.profile\\.person\\.domain\\.vo\\..*(Row|Projection).*");
            }
        }
    }

    @Test
    void enforcesTheFiveLayerDependencyDirection() throws Exception {
        assertThat(JAVA.resolve("usecase")).isDirectory();
        assertThat(JAVA.resolve("service")).isDirectory();
        assertThat(JAVA.resolve("dao")).isDirectory();
        assertThat(JAVA.resolve("mapper")).isDirectory();

        try (Stream<Path> files = Files.list(JAVA.resolve("service"))) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith("ServiceImpl.java")))
                .as("layered production service must not use ServiceImpl naming")
                .isEmpty();
        }
        try (Stream<Path> files = Files.walk(JAVA.resolve("service"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("import org.namewta.profile.person.mapper.",
                    "IService", "BaseMapper", "QueryWrapper");
            }
        }
        try (Stream<Path> files = Files.walk(JAVA.resolve("usecase"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(file)).doesNotContain("import org.namewta.profile.person.dao.",
                    "import org.namewta.profile.person.mapper.",
                    "import org.namewta.profile.person.service.impl.");
            }
        }
        try (Stream<Path> files = Files.list(JAVA.resolve("dao"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).contains("import org.namewta.profile.person.mapper.")
                    .doesNotContain("import org.namewta.profile.person.service.",
                        "import org.namewta.profile.person.usecase.");
                assertThat(source).contains("@Repository");
            }
        }
        try (Stream<Path> files = Files.walk(JAVA.resolve("controller"))) {
            for (Path file : files.filter(path -> path.toString().endsWith("Controller.java")).toList()) {
                assertThat(Files.readString(file)).contains("import org.namewta.profile.person.usecase.")
                    .doesNotContain("import org.namewta.profile.person.service.");
            }
        }
        try (Stream<Path> files = Files.walk(JAVA)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith("DataSupport.java")))
                .isEmpty();
        }
    }

    @Test
    void mapperMethodsAreBackedByMatchingXmlWithoutAnnotationSql() throws Exception {
        try (Stream<Path> files = Files.list(JAVA.resolve("mapper"))) {
            for (Path mapper : files.filter(path -> path.toString().endsWith("Mapper.java")).toList()) {
                String source = Files.readString(mapper);
                assertThat(source).contains("extends BaseMapperPlus<");
                assertThat(source).doesNotContain("org.namewta.profile.person.domain.vo.");
                assertThat(source).doesNotContain("@Select", "@Insert", "@Update", "@Delete", "Provider.class");
                assertThat(source).doesNotMatch("(?s).*static\\s+final\\s+String\\s+\\w*SQL\\w*.*");

                String mapperName = mapper.getFileName().toString().replace(".java", "");
                Path xml = XML.resolve(mapperName + ".xml");
                assertThat(xml).exists();
                assertThat(xmlNamespace(xml)).isEqualTo("org.namewta.profile.person.mapper." + mapperName);

                Set<String> ids = xmlStatementIds(xml);
                Matcher matcher = MAPPER_METHOD.matcher(source);
                Set<String> methods = new HashSet<>();
                while (matcher.find()) {
                    methods.add(matcher.group(1));
                }
                assertThat(ids).containsExactlyInAnyOrderElementsOf(methods);
            }
        }
    }

    @Test
    void sharedMaterialMapperDoesNotReadPersonOrEnterpriseOwnerTables() throws Exception {
        String source = Files.readString(XML.resolve("ProfileMaterialMapper.xml"));
        assertThat(source).doesNotContain("profile_person_", "profile_enterprise_");
        String personApplicationXml = Files.readString(XML.resolve("PersonApplicationMapper.xml"));
        assertThat(personApplicationXml)
            .contains("lockMaterialWorkingOwner", "lockMaterialImmutableOwner", "lockActiveIdentityMatch",
                "b.status = 'ACTIVE'", "right(trim(p.document_number), 4)")
            .doesNotContain("b.status = 'EFFECTIVE'");
    }

    @Test
    void mybatisLoadsEveryMapperXmlAndRegistersEveryStatement() throws Exception {
        Configuration configuration = new Configuration();
        try (Stream<Path> files = Files.list(XML)) {
            for (Path xml : files.filter(path -> path.toString().endsWith("Mapper.xml")).toList()) {
                try (var input = Files.newInputStream(xml)) {
                    new XMLMapperBuilder(input, configuration, xml.toString(), configuration.getSqlFragments())
                        .parse();
                }
                String namespace = xmlNamespace(xml);
                for (String id : xmlStatementIds(xml)) {
                    assertThat(configuration.hasStatement(namespace + "." + id)).isTrue();
                }
            }
        }
    }

    private String xmlNamespace(Path xml) throws Exception {
        return document(xml).getDocumentElement().getAttribute("namespace");
    }

    private Set<String> xmlStatementIds(Path xml) throws Exception {
        Set<String> ids = new HashSet<>();
        var root = document(xml).getDocumentElement();
        for (String tag : List.of("select", "insert", "update", "delete")) {
            var nodes = root.getElementsByTagName(tag);
            for (int index = 0; index < nodes.getLength(); index++) {
                ids.add(nodes.item(index).getAttributes().getNamedItem("id").getNodeValue());
            }
        }
        return ids;
    }

    private org.w3c.dom.Document document(Path xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(xml.toFile());
    }
}
