package org.namewta.test.oss.owner;

import org.namewta.system.api.OssService;
import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Business OSS Owner 清单与源码架构棘轮。
 */
@Tag("dev")
class BusinessOssOwnerArchitectureUnitTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern TABLE_NAME = Pattern.compile("@TableName\\(\"([^\"]+)\"\\)");
    private static final Pattern FIELD = Pattern.compile("private\\s+(Long|String)\\s+(\\w+)\\s*;");
    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+);");
    private static final Pattern TYPE = Pattern.compile("(?:public\\s+)?(?:final\\s+)?class\\s+(\\w+)");
    private static final Pattern CREATE_TABLE = Pattern.compile("(?i)^\\s*create\\s+table\\s+`?([a-z0-9_]+)`?\\s*\\(");
    private static final Pattern SQL_COLUMN = Pattern.compile("^\\s*`?([a-z][a-z0-9_]*)`?\\s+([a-z]+(?:\\([^)]*\\))?).*$",
        Pattern.CASE_INSENSITIVE);

    @Test
    void manifestCoversPersistentCarriersProductionCallersAndEvidence() throws Exception {
        Manifest manifest = loadManifest();
        Path repository = repositoryRoot();

        validateManifest(manifest, repository);
        assertCarrierCoverage(scanPersistentCarrierCandidates(repository), manifest);
        assertCallerCoverage(scanReconcileCallers(repository), manifest);

        assertTrue(ownerCoordinates(manifest).containsAll(Set.of(
            "sys_user.avatar",
            "flow_his_task.ext",
            "profile_material_ref.oss_id"
        )));
    }

    @Test
    void publicApiOnlyExposesCollectionReconciliation() throws Exception {
        Set<String> publicMethods = Stream.of(OssService.class.getMethods())
            .map(Method::getName)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(publicMethods.contains("reconcileReferences"));
        assertFalse(publicMethods.contains("bind"));
        assertFalse(publicMethods.contains("unbind"));

        String implementation = Files.readString(repositoryRoot().resolve(
            "wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssServiceImpl.java"));
        assertFalse(implementation.contains("public OssReferenceState bind("));
        assertFalse(implementation.contains("public OssReferenceState unbind("));
    }

    @Test
    void malformedManifestAndUnknownDiscoveriesFailClosed() throws Exception {
        Manifest valid = loadManifest();
        Owner first = valid.owners().getFirst();
        IllegalArgumentException absent = assertThrows(IllegalArgumentException.class,
            () -> validateManifest(null, repositoryRoot()));
        assertTrue(absent.getMessage().contains("manifest"));

        Manifest missingOwners = new Manifest(valid.schemaVersion(), List.of(),
            valid.carrierAllowlist(), valid.callerAllowlist());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
            () -> validateManifest(missingOwners, repositoryRoot()));
        assertTrue(missing.getMessage().contains("owners"));

        Manifest duplicate = new Manifest(valid.schemaVersion(), List.of(first, first),
            valid.carrierAllowlist(), valid.callerAllowlist());
        IllegalArgumentException duplicated = assertThrows(IllegalArgumentException.class,
            () -> validateManifest(duplicate, repositoryRoot()));
        assertTrue(duplicated.getMessage().contains("duplicate owner"));

        IllegalArgumentException carrier = assertThrows(IllegalArgumentException.class,
            () -> assertCarrierCoverage(Set.of("new_business.attachment_oss_ids"), valid));
        assertTrue(carrier.getMessage().contains("new_business.attachment_oss_ids"));

        IllegalArgumentException caller = assertThrows(IllegalArgumentException.class,
            () -> assertCallerCoverage(Set.of("org.example.UnknownOwner"), valid));
        assertTrue(caller.getMessage().contains("org.example.UnknownOwner"));

        Owner noTest = new Owner(first.id(), first.module(), first.table(), first.carrier(), first.encoding(),
            first.primaryKey(), first.ownerService(), first.callerClass(), first.insertStrategy(),
            first.updateStrategy(), first.deleteStrategy(), first.restoreStrategy(), first.carrierSource(), "missing.java");
        Manifest missingTest = new Manifest(valid.schemaVersion(), List.of(noTest),
            valid.carrierAllowlist(), valid.callerAllowlist());
        IllegalArgumentException evidence = assertThrows(IllegalArgumentException.class,
            () -> validateManifest(missingTest, repositoryRoot()));
        assertTrue(evidence.getMessage().contains("contractTest"));
    }

    @Test
    void cleanupConfigurationCannotBeEnabledByThisBaseline() throws Exception {
        String application = Files.readString(repositoryRoot().resolve("wta-admin/src/main/resources/application.yml"));

        assertSafeCleanupConfiguration(application);
        IllegalArgumentException enabled = assertThrows(IllegalArgumentException.class,
            () -> assertSafeCleanupConfiguration("oss:\n  lifecycle:\n    cleanup-enabled: true\n    cleanup-dry-run: true\n"));
        assertTrue(enabled.getMessage().contains("cleanup-enabled"));
    }

    private void validateManifest(Manifest manifest, Path repository) {
        require(manifest != null, "manifest is required");
        require(manifest.schemaVersion() == 1, "schemaVersion must be 1");
        require(manifest.owners() != null && !manifest.owners().isEmpty(), "owners must not be empty");
        require(manifest.carrierAllowlist() != null, "carrierAllowlist is required");
        require(manifest.callerAllowlist() != null, "callerAllowlist is required");

        Set<String> ids = new HashSet<>();
        Set<String> coordinates = new HashSet<>();
        for (Owner owner : manifest.owners()) {
            require(nonBlank(owner.id()), "owner id is required");
            require(ids.add(owner.id()), "duplicate owner id: " + owner.id());
            require(nonBlank(owner.module()), "module is required for " + owner.id());
            require(nonBlank(owner.table()), "table is required for " + owner.id());
            require(nonBlank(owner.carrier()), "carrier is required for " + owner.id());
            require(coordinates.add(coordinate(owner)), "duplicate owner carrier: " + coordinate(owner));
            require(nonBlank(owner.encoding()), "encoding is required for " + owner.id());
            require(nonBlank(owner.primaryKey()), "primaryKey is required for " + owner.id());
            require(nonBlank(owner.ownerService()), "ownerService is required for " + owner.id());
            require(nonBlank(owner.callerClass()), "callerClass is required for " + owner.id());
            require(owner.ownerService().equals(owner.callerClass()),
                "ownerService must own reconciliation calls for " + owner.id());
            require(owner.ownerService().startsWith(modulePackage(owner.module())),
                "ownerService does not belong to module " + owner.module() + ": " + owner.ownerService());
            require(nonBlank(owner.insertStrategy()), "insertStrategy is required for " + owner.id());
            require(nonBlank(owner.updateStrategy()), "updateStrategy is required for " + owner.id());
            require(nonBlank(owner.deleteStrategy()), "deleteStrategy is required for " + owner.id());
            require(nonBlank(owner.restoreStrategy()), "restoreStrategy is required for " + owner.id());
            Path carrierSource = resolveSource(repository, owner.carrierSource());
            requireExistingSource(repository, owner.carrierSource(), "carrierSource", owner.id());
            requireExistingSource(repository, owner.contractTest(), "contractTest", owner.id());
            String source = read(carrierSource);
            require(source.contains(owner.carrier()) || source.contains(toCamelCase(owner.carrier())),
                "unknown carrier " + coordinate(owner) + " in " + owner.carrierSource());
            require(source.contains(owner.primaryKey()) || source.contains(toCamelCase(owner.primaryKey())),
                "unknown primaryKey " + owner.table() + "." + owner.primaryKey() + " in " + owner.carrierSource());
        }
        validateAllowlist(manifest.carrierAllowlist(), "carrier allowlist");
        validateCallerAllowlist(manifest.callerAllowlist());
    }

    private Set<String> scanPersistentCarrierCandidates(Path repository) throws Exception {
        Set<String> candidates = new LinkedHashSet<>();
        for (Path file : productionJavaFiles(repository)) {
            String source = Files.readString(file);
            Matcher tableMatcher = TABLE_NAME.matcher(source);
            if (!tableMatcher.find()) {
                continue;
            }
            String table = tableMatcher.group(1);
            Matcher fieldMatcher = FIELD.matcher(source);
            while (fieldMatcher.find()) {
                String type = fieldMatcher.group(1);
                String field = fieldMatcher.group(2);
                if (isCarrierCandidate(type, field)) {
                    candidates.add(table + "." + toSnakeCase(field));
                }
            }
        }
        try (Stream<Path> schemas = Files.walk(SqlBaselinePaths.root())) {
            for (Path schema : schemas.filter(path -> path.toString().endsWith(".sql")).toList()) {
                scanSchemaCarrierCandidates(schema, candidates);
            }
        }
        return candidates;
    }

    private void scanSchemaCarrierCandidates(Path schema, Set<String> candidates) throws Exception {
        String currentTable = null;
        boolean inBlockComment = false;
        for (String rawLine : Files.readAllLines(schema)) {
            String line = rawLine;
            if (inBlockComment) {
                int end = line.indexOf("*/");
                if (end < 0) {
                    continue;
                }
                inBlockComment = false;
                line = line.substring(end + 2);
            }
            while (true) {
                int start = line.indexOf("/*");
                if (start < 0) {
                    break;
                }
                int end = line.indexOf("*/", start + 2);
                if (end < 0) {
                    line = line.substring(0, start);
                    inBlockComment = true;
                    break;
                }
                line = line.substring(0, start) + line.substring(end + 2);
            }
            int lineComment = line.indexOf("--");
            if (lineComment >= 0) {
                line = line.substring(0, lineComment);
            }
            Matcher tableMatcher = CREATE_TABLE.matcher(line);
            if (tableMatcher.find()) {
                currentTable = tableMatcher.group(1).toLowerCase();
                continue;
            }
            if (currentTable == null) {
                continue;
            }
            if (line.stripLeading().startsWith(")")) {
                currentTable = null;
                continue;
            }
            Matcher columnMatcher = SQL_COLUMN.matcher(line);
            if (!columnMatcher.matches()) {
                continue;
            }
            String field = columnMatcher.group(1).toLowerCase();
            String type = columnMatcher.group(2).toLowerCase();
            if (isSqlCarrierCandidate(type, field)) {
                candidates.add(currentTable + "." + field);
            }
        }
    }

    private Set<String> scanReconcileCallers(Path repository) throws Exception {
        Set<String> callers = new LinkedHashSet<>();
        for (Path file : productionJavaFiles(repository)) {
            String source = Files.readString(file);
            if (!source.contains(".reconcileReferences(")) {
                continue;
            }
            Matcher packageMatcher = PACKAGE.matcher(source);
            Matcher typeMatcher = TYPE.matcher(source);
            require(packageMatcher.find() && typeMatcher.find(), "cannot resolve reconcile caller: " + file);
            callers.add(packageMatcher.group(1) + "." + typeMatcher.group(1));
        }
        return callers;
    }

    private List<Path> productionJavaFiles(Path repository) throws Exception {
        try (Stream<Path> files = Files.walk(repository)) {
            return files.filter(this::isProductionJava)
                .toList();
        }
    }

    private boolean isProductionJava(Path path) {
        if (!path.getFileName().toString().endsWith(".java")) {
            return false;
        }
        if (path.getNameCount() > 0 && java.util.stream.IntStream.range(0, path.getNameCount())
            .anyMatch(index -> path.getName(index).toString().equals("specdev-worktree"))) {
            return false;
        }
        for (int index = 0; index <= path.getNameCount() - 3; index++) {
            if (path.getName(index).toString().equals("src")
                && path.getName(index + 1).toString().equals("main")
                && path.getName(index + 2).toString().equals("java")) {
                return true;
            }
        }
        return false;
    }

    private void assertCarrierCoverage(Set<String> discovered, Manifest manifest) {
        Set<String> registered = ownerCoordinates(manifest);
        Set<String> allowed = manifest.carrierAllowlist().stream()
            .map(CarrierAllowlist::candidate).collect(java.util.stream.Collectors.toSet());
        Set<String> unknown = new LinkedHashSet<>(discovered);
        unknown.removeAll(registered);
        unknown.removeAll(allowed);
        require(unknown.isEmpty(), "unregistered persistent OSS carriers: " + unknown);

        Set<String> staleAllowlist = new LinkedHashSet<>(allowed);
        staleAllowlist.removeAll(discovered);
        require(staleAllowlist.isEmpty(), "stale carrier allowlist entries: " + staleAllowlist);
    }

    private void assertCallerCoverage(Set<String> discovered, Manifest manifest) {
        Set<String> registered = manifest.owners().stream().map(Owner::callerClass)
            .collect(java.util.stream.Collectors.toSet());
        Set<String> allowed = manifest.callerAllowlist().stream().map(CallerAllowlist::callerClass)
            .collect(java.util.stream.Collectors.toSet());
        Set<String> unknown = new LinkedHashSet<>(discovered);
        unknown.removeAll(registered);
        unknown.removeAll(allowed);
        require(unknown.isEmpty(), "unregistered reconcileReferences callers: " + unknown);

        Set<String> stale = new LinkedHashSet<>(registered);
        stale.addAll(allowed);
        stale.removeAll(discovered);
        require(stale.isEmpty(), "stale reconcileReferences caller entries: " + stale);
    }

    private void assertSafeCleanupConfiguration(String yaml) {
        require(Pattern.compile("(?m)^\\s*cleanup-enabled:\\s*false\\s*$").matcher(yaml).find(),
            "cleanup-enabled must remain false");
        require(Pattern.compile("(?m)^\\s*cleanup-dry-run:\\s*true\\s*$").matcher(yaml).find(),
            "cleanup-dry-run must remain true");
    }

    private void validateAllowlist(List<CarrierAllowlist> allowlist, String label) {
        Set<String> values = new HashSet<>();
        for (CarrierAllowlist entry : allowlist) {
            require(nonBlank(entry.candidate()), label + " candidate is required");
            require(nonBlank(entry.reason()), label + " reason is required for " + entry.candidate());
            require(values.add(entry.candidate()), "duplicate " + label + ": " + entry.candidate());
        }
    }

    private void validateCallerAllowlist(List<CallerAllowlist> allowlist) {
        Set<String> values = new HashSet<>();
        for (CallerAllowlist entry : allowlist) {
            require(nonBlank(entry.callerClass()), "caller allowlist class is required");
            require(nonBlank(entry.reason()), "caller allowlist reason is required for " + entry.callerClass());
            require(values.add(entry.callerClass()), "duplicate caller allowlist: " + entry.callerClass());
        }
    }

    private boolean isCarrierCandidate(String type, String field) {
        String lower = field.toLowerCase();
        return lower.contains("oss") && lower.endsWith("id")
            || lower.contains("oss") && lower.endsWith("ids")
            || lower.contains("attachment") && lower.endsWith("ids")
            || "Long".equals(type) && "avatar".equals(field);
    }

    private boolean isSqlCarrierCandidate(String type, String field) {
        return field.contains("oss") && (field.endsWith("_id") || field.endsWith("_ids"))
            || field.contains("attachment") && field.endsWith("_ids")
            || "avatar".equals(field) && (type.startsWith("bigint") || type.startsWith("int8")
            || type.startsWith("number"));
    }

    private Set<String> ownerCoordinates(Manifest manifest) {
        return manifest.owners().stream().map(this::coordinate)
            .collect(java.util.stream.Collectors.toSet());
    }

    private String coordinate(Owner owner) {
        return owner.table() + "." + owner.carrier();
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private String toCamelCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char current : value.toCharArray()) {
            if (current == '_') {
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(current) : current);
                upper = false;
            }
        }
        return result.toString();
    }

    private String modulePackage(String module) {
        require(module.startsWith("wta-") && module.length() > "wta-".length(),
            "invalid module: " + module);
        return "org.namewta." + module.substring("wta-".length()).replace('-', '.') + ".";
    }

    private Manifest loadManifest() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/oss/business-oss-owners.json")) {
            assertNotNull(input, "business OSS owner manifest is missing");
            return JSON.readValue(input, Manifest.class);
        }
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }

    private void requireExistingSource(Path repository, String relativePath, String field, String owner) {
        require(nonBlank(relativePath), field + " is required for " + owner);
        require(Files.isRegularFile(resolveSource(repository, relativePath)),
            field + " does not exist for " + owner + ": " + relativePath);
    }

    private Path resolveSource(Path repository, String relativePath) {
        String sqlPrefix = "release-artifacts/docker/infrastructure/mysql/init/";
        if (relativePath.startsWith(sqlPrefix)) {
            return SqlBaselinePaths.file(relativePath.substring(sqlPrefix.length()));
        }
        return repository.resolve(relativePath);
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot read " + path, e);
        }
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record Manifest(int schemaVersion, List<Owner> owners,
                            List<CarrierAllowlist> carrierAllowlist,
                            List<CallerAllowlist> callerAllowlist) {
    }

    private record Owner(String id, String module, String table, String carrier, String encoding,
                         String primaryKey, String ownerService, String callerClass,
                         String insertStrategy, String updateStrategy, String deleteStrategy,
                         String restoreStrategy, String carrierSource, String contractTest) {
    }

    private record CarrierAllowlist(String candidate, String reason) {
    }

    private record CallerAllowlist(String callerClass, String reason) {
    }
}
