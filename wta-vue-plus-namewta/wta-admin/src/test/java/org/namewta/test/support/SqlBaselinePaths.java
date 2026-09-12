package org.namewta.test.support;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 定位父仓库唯一维护的 MySQL 初始化基座。
 */
public final class SqlBaselinePaths {

    public static final String SQL_ROOT_PROPERTY = "namewta.sql.root";
    private static final Path RELATIVE_SQL_ROOT = Path.of(
        "release-artifacts", "docker", "infrastructure", "mysql", "init");

    private SqlBaselinePaths() {
    }

    public static Path root() {
        String configured = System.getProperty(SQL_ROOT_PROPERTY);
        Path root = configured == null || configured.isBlank()
            ? defaultRoot()
            : Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("MySQL 基座目录不存在：" + root
                + "；请在聚合仓库运行测试，或通过 -D" + SQL_ROOT_PROPERTY + "=<目录> 显式指定");
        }
        return root;
    }

    public static Path file(String filename) {
        Path file = root().resolve(filename).normalize();
        if (!file.getParent().equals(root()) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalStateException("MySQL 基座文件不存在或无法读取：" + file);
        }
        return file;
    }

    private static Path defaultRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null && !Files.isRegularFile(current.resolve("mvnw"))) {
            current = current.getParent();
        }
        if (current == null || current.getParent() == null) {
            throw new IllegalStateException("无法定位后端仓库；请通过 -D" + SQL_ROOT_PROPERTY + "=<目录> 显式指定");
        }
        return current.getParent().resolve(RELATIVE_SQL_ROOT).normalize();
    }
}
