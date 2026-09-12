package org.namewta.notify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * AC-006：本 change 路径不得扩通知配置页或 common-notify 同步契约。
 */
@Tag("dev")
class NotifyOutboxWakeScopeGateTest {

    private static final String CHANGE_BASE = "6352f7b291aaaf63c977253b7849767b855dd680";

    @Test
    void changeDiffStaysInsideOutboxWakeAndDoesNotTouchConfigOrCommonNotify() throws Exception {
        Path root = gitRoot();
        List<String> paths = new ArrayList<>();
        paths.addAll(git(root, "diff", "--name-only", CHANGE_BASE + "..HEAD"));
        paths.addAll(git(root, "diff", "--name-only"));
        paths.addAll(git(root, "ls-files", "--others", "--exclude-standard"));
        assertFalse(paths.isEmpty(), "scope gate needs a non-empty change diff from " + CHANGE_BASE);
        List<String> prohibited = paths.stream().filter(NotifyOutboxWakeScopeGateTest::prohibited).distinct().toList();
        assertTrue(prohibited.isEmpty(), "AC-006 prohibited paths in change: " + prohibited);
        assertTrue(paths.stream().anyMatch(path -> path.contains("adapter/worker")
                || path.contains("NotifyOutboxWake")
                || path.contains("support/outbox")),
            "change should include Outbox wake/worker files");
    }

    private static boolean prohibited(String path) {
        String normalized = path.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("wta-common-notify")) {
            return true;
        }
        if (lower.contains("notifyconfig") || lower.contains("notify-config")) {
            return true;
        }
        if (lower.contains("notifychannelaccount") || lower.contains("notify_channel_account")) {
            return true;
        }
        if (normalized.contains("controller/admin/NotifyConfig")
            || normalized.contains("usecase/NotifyConfigUseCase")
            || normalized.contains("service/NotifyConfigService")) {
            return true;
        }
        return false;
    }

    private static Path gitRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve(".git")) && Files.isDirectory(dir.resolve("wta-modules"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        fail("cannot locate backend git root from " + System.getProperty("user.dir"));
        return null;
    }

    private static List<String> git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(root.toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line.trim());
                }
            }
        }
        int code = process.waitFor();
        if (code != 0) {
            fail("git " + String.join(" ", args) + " exited " + code + ": " + lines);
        }
        return lines;
    }
}
