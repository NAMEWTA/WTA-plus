package org.namewta.test.notify.caller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class NotifyCallerMigrationContractUnitTest {

    @Test
    void notificationCallersUseApplicationServiceAndDeclareApiDependency() throws Exception {
        String captcha = source("wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java");
        String workflow = source("wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwCommonServiceImpl.java");
        String mailDemo = source("wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java");
        String smsDemo = source("wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java");
        String rebind = source("wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java");
        String transfer = source("wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java");
        String notice = source("wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java");

        assertAll(
            () -> assertTrue(captcha.contains("NotificationApplicationService")),
            () -> assertFalse(captcha.contains("MailBuilder")),
            () -> assertFalse(captcha.contains("SmsFactory")),
            () -> assertTrue(workflow.contains("NotificationApplicationService")),
            () -> assertFalse(workflow.contains("MailBuilder")),
            () -> assertTrue(mailDemo.contains("NotificationApplicationService")),
            () -> assertFalse(mailDemo.contains("MailBuilder")),
            () -> assertTrue(smsDemo.contains("NotificationApplicationService")),
            () -> assertFalse(smsDemo.contains("smsBlend.sendMessage")),
            () -> assertFalse(captcha.contains("您本次验证码为")),
            () -> assertTrue(captcha.contains("\"auth-captcha\"")),
            () -> assertTrue(captcha.contains("Map.of(\"code\", code, \"expireMinutes\"")),
            () -> assertTrue(rebind.contains("\"person-rebind\"")),
            () -> assertTrue(rebind.contains("java.util.Map.of()")),
            () -> assertFalse(rebind.contains("SmsFactory")),
            () -> assertTrue(transfer.contains("\"enterprise-transfer\"")),
            () -> assertTrue(transfer.contains("Map.of(\"code\", code)")),
            () -> assertFalse(transfer.contains("您的验证码")),
            () -> assertTrue(notice.contains("\"notice-published\"")),
            () -> assertTrue(notice.contains("params.put(\"title\"")),
            () -> assertTrue(notice.contains("params.put(\"content\"")),
            () -> assertTrue(workflow.contains("Map.of(\"title\", subject, \"content\", message, \"path\""))
        );

        assertTrue(source("wta-admin/pom.xml").contains("<artifactId>wta-api</artifactId>"));
        String workflowPom = source("wta-modules/wta-workflow/pom.xml");
        assertTrue(workflowPom.contains("<artifactId>wta-api</artifactId>"));
        assertFalse(workflowPom.contains("<artifactId>wta-common-mail</artifactId>"));
        assertFalse(workflowPom.contains("<artifactId>wta-common-sms</artifactId>"));
        String demoPom = source("wta-modules/wta-demo/pom.xml");
        assertTrue(demoPom.contains("<artifactId>wta-api</artifactId>"));
        assertFalse(demoPom.contains("<artifactId>wta-common-mail</artifactId>"));
        assertTrue(demoPom.contains("<artifactId>wta-common-sms</artifactId>"));
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(repositoryRoot().resolve(relativePath));
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
