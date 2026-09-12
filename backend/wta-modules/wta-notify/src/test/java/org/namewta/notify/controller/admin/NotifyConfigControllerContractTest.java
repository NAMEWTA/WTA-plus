package org.namewta.notify.controller.admin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通知配置 HTTP 合同：查询 GET、变更 POST、密钥请求不落日志。
 */
@Tag("dev")
class NotifyConfigControllerContractTest {

    @Test
    void mutationsArePostWithLogAndDoNotSaveSecretBodies() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/namewta/notify/controller/admin/NotifyConfigController.java"));
        assertTrue(source.contains("@RequestMapping(\"/notify/config\")"));
        assertTrue(source.contains("@GetMapping(\"/account/list\")"));
        assertTrue(source.contains("@GetMapping(\"/account/{accountId}\")"));
        assertTrue(source.contains("@GetMapping(\"/scene/list\")"));
        assertTrue(source.contains("@PostMapping(\"/account\")"));
        assertTrue(source.contains("@PostMapping(\"/account/edit\")"));
        assertTrue(source.contains("@PostMapping(\"/account/changeStatus\")"));
        assertTrue(source.contains("@PostMapping(\"/account/remove\")"));
        assertTrue(source.contains("@PostMapping(\"/scene/save\")"));
        assertTrue(source.contains("@PostMapping(\"/test/account\")"));
        assertTrue(source.contains("@PostMapping(\"/test/template\")"));
        assertFalse(source.contains("@PutMapping"));
        assertFalse(source.contains("@DeleteMapping"));
        long secretSafeLogs = source.lines()
            .filter(line -> line.contains("isSaveRequestData = false"))
            .count();
        assertTrue(secretSafeLogs >= 7);
        assertTrue(source.contains("notify:config:list"));
        assertTrue(source.contains("notify:config:test"));
    }
}
