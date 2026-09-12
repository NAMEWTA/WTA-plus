package org.namewta.test.profile.contract;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileModuleGraphContractTest {

    @Test
    void reactorContainsProfileAggregateBomAndBothLeaves() throws Exception {
        Path root = repositoryRoot();

        assertThat(Files.readString(root.resolve("wta-modules/pom.xml")))
            .contains("<module>wta-profile</module>");
        assertThat(Files.readString(root.resolve("wta-modules/wta-profile/pom.xml")))
            .contains("<module>wta-profile-bom</module>")
            .contains("<module>wta-profile-person</module>")
            .contains("<module>wta-profile-enterprise</module>");
        assertThat(Files.readString(root.resolve("wta-modules/wta-profile/wta-profile-bom/pom.xml")))
            .contains("<artifactId>wta-profile-person</artifactId>")
            .contains("<artifactId>wta-profile-enterprise</artifactId>");
    }

    @Test
    void fullAndCoreBundlesBothAssembleProfileWhileWorkflowRemainsFullOnly() throws Exception {
        String adminPom = Files.readString(repositoryRoot().resolve("wta-admin/pom.xml"));
        String full = profileBody(adminPom, "bundle-full");
        String core = profileBody(adminPom, "bundle-core");

        assertThat(full).contains("wta-profile-person", "wta-profile-enterprise", "wta-workflow");
        assertThat(core).contains("wta-profile-person", "wta-profile-enterprise")
            .doesNotContain("wta-workflow");
    }

    private static String profileBody(String pom, String id) {
        int idIndex = pom.indexOf("<id>" + id + "</id>");
        int start = pom.lastIndexOf("<profile>", idIndex);
        int end = pom.indexOf("</profile>", idIndex);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(idIndex);
        return pom.substring(start, end);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("wta-admin/pom.xml"))) {
            current = current.getParent();
        }
        assertThat(current).as("backend repository root").isNotNull();
        return current;
    }
}
