package org.namewta.common.web.logging;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.namewta.common.core.service.HttpProtocolPolicy;
import org.springframework.mock.web.*;

import java.util.*;

@Tag("dev")
class OidcProtocolPrivacyTest {
    @Test
    void protocolBodyAndClaimsNeverEnterCaptureOrLog() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        var filter = new SysLogFilter(1024, 1024, events::add);
        filter.setProtocolPolicies(
                List.of(
                        new HttpProtocolPolicy() {
                            public boolean isProtocolPath(String p) {
                                return p.equals("/oidc/userinfo");
                            }

                            public boolean isSensitivePath(String p) {
                                return p.startsWith("/oidc/");
                            }
                        }));
        var request = new MockHttpServletRequest("GET", "/oidc/userinfo");
        request.addParameter("code", "must-not-log");
        var response = new MockHttpServletResponse();
        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    assertThat(req).isSameAs(request);
                    assertThat(res).isSameAs(response);
                    res.getWriter()
                            .write("{\"wta_person\":{\"document_number\":\"must-not-log\"}}");
                });
        assertThat(events).isEmpty();
        assertThat(response.getContentAsString()).contains("must-not-log");
    }
}
