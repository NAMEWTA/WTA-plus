package org.namewta.common.web.filter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.utils.NetUtils;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.ip.ClientAddressResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class ClientAddressFilterTest {
    @Test
    void defaultAndUntrustedPeersIgnoreAllForwardingHeaders() {
        var request = request("198.51.100.21", "192.0.2.77");
        for (String header : List.of("Forwarded", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR")) {
            request.addHeader(header, "192.0.2.77");
        }
        assertThat(new ClientAddressResolver(List.of()).resolve(request)).isEqualTo("198.51.100.21");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.21");
        request.removeHeader("X-Forwarded-For");
        request.addHeader("X-Forwarded-For", "not even an address");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.21");
        assertThat(new ClientAddressResolver(List.of()).resolve(request("10.0.0.1", "192.0.2.77")))
            .isEqualTo("10.0.0.1");
    }

    @Test
    void peelsOnlyTrustedHopsFromRightAndSupportsRepeatedHeaders() {
        assertThat(resolver().resolve(request("10.0.0.2", "192.0.2.77, 198.51.100.21, 10.0.0.1")))
            .isEqualTo("198.51.100.21");
        var repeated = request("10.0.0.2", "192.0.2.77, 198.51.100.21");
        repeated.addHeader("X-Forwarded-For", "10.0.0.1");
        assertThat(resolver().resolve(repeated)).isEqualTo("198.51.100.21");
        assertThat(resolver().resolve(request("10.0.0.2", "198.51.100.21")))
            .isEqualTo("198.51.100.21");
        assertThat(resolver().resolve(request("10.0.0.2", "10.0.0.1"))).isEqualTo("10.0.0.1");
        var precise = new ClientAddressResolver(List.of("192.0.2.128/25", "2001:db8:1:0:8000::/65"));
        assertThat(precise.resolve(request("192.0.2.129", "198.51.100.21"))).isEqualTo("198.51.100.21");
        assertThat(precise.resolve(request("192.0.2.127", "198.51.100.21"))).isEqualTo("192.0.2.127");
        assertThat(precise.resolve(request("2001:db8:1:0:8000::1", "198.51.100.21"))).isEqualTo("198.51.100.21");
        assertThat(precise.resolve(request("2001:db8:1:0:7fff::1", "198.51.100.21"))).isEqualTo("2001:db8:1:0:7fff:0:0:1");
    }

    @Test
    void ipv6AndMappedIpv4HaveStableNumericIdentity() {
        var resolver = new ClientAddressResolver(List.of("2001:db8:1::/48", "10.0.0.0/8"));
        String client = resolver.resolve(request("2001:db8:1::2", "2001:DB8:2::1234, 2001:db8:1::1"));
        assertThat(client).isEqualTo("2001:db8:2:0:0:0:0:1234");
        assertThat(NetUtils.isMatchIpRule("2001:db8:2::1234", client)).isTrue();
        assertThat(NetUtils.isMatchIpRule("2001:db8:2::/48", client)).isTrue();
        assertThat(NetUtils.isMatchIpRule("2001:db8:3::/48", client)).isFalse();
        assertThat(resolver.resolve(request("::ffff:10.0.0.2", "::ffff:198.51.100.21")))
            .isEqualTo("198.51.100.21");
        assertThat(resolver.resolve(request("[2001:db8:1::2]", "2001:db8:2::1234"))).isEqualTo(client);
        assertThat(ServletUtils.getClientIP(request("[::1]", "192.0.2.77"))).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(NetUtils.isMatchIpRule("::ffff:198.51.100.21", "198.51.100.21")).isTrue();
        assertThat(NetUtils.isMatchIpRule("localhost/32", "127.0.0.1")).isFalse();
    }

    @Test
    void missingXffUsesPeerAndDoesNotFallBackToOtherHeaders() {
        var request = request("10.0.0.2", null);
        request.addHeader("X-Real-IP", "192.0.2.77");
        request.addHeader("Forwarded", "for=192.0.2.77");
        assertThat(resolver().resolve(request)).isEqualTo("10.0.0.2");
    }

    @Test
    void malformedTrustedChainsFailBeforeAnyBusinessHandler() throws Exception {
        for (String invalid : List.of("", "unknown", "127.1", "2130706433", "010.0.0.1", "256.0.0.1",
            "localhost", "198.51.100.21:443", "[2001:db8::1]", "fe80::1%eth0", "2001::db8::1",
            "192.0.2.77,", ",192.0.2.77", "192.0.2.77,,10.0.0.1")) {
            var request = request("10.0.0.2", invalid);
            var response = new MockHttpServletResponse();
            AtomicInteger calls = new AtomicInteger();
            new ClientAddressFilter(resolver()).doFilter(request, response,
                (req, res) -> calls.incrementAndGet());
            assertThat(response.getStatus()).as(invalid).isEqualTo(400);
            assertThat(calls).hasValue(0);
            assertThat(request.getAttribute(ServletUtils.CLIENT_IP_ATTRIBUTE)).isNull();
        }
    }

    @Test
    void boundsHeaderWorkAndAcceptsExactHopBudget() {
        String full = String.join(",", java.util.Collections.nCopies(32, "10.0.0.1"));
        assertThat(resolver().resolve(request("10.0.0.2", full))).isEqualTo("10.0.0.1");
        assertThatThrownBy(() -> resolver().resolve(request("10.0.0.2", full + ",10.0.0.1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver().resolve(request("10.0.0.2", " ".repeat(4097))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidConfigurationIsRejectedRatherThanBroadeningTrust() {
        for (String invalid : List.of("", "10.0.0.1", "10.0.0.0/33", "::/129", "localhost/32",
            "0.0.0.0/-1", "10.0.0.0/8/", "10.0.0.0/+8", "*/0", "::ffff:10.0.0.0/128")) {
            assertThatThrownBy(() -> new ClientAddressResolver(List.of(invalid)))
                .as(invalid).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new ClientAddressResolver(List.of("0.0.0.0/0"))
            .resolve(request("10.0.0.1", "198.51.100.21"))).isEqualTo("198.51.100.21");
    }

    @Test
    void canonicalResultSurvivesHeaderChangesAndNoRequestFailsClosed() throws Exception {
        var request = request("10.0.0.2", "198.51.100.21");
        new ClientAddressFilter(resolver()).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            request.removeHeader("X-Forwarded-For");
            request.addHeader("X-Forwarded-For", "192.0.2.77");
            assertThat(ServletUtils.getClientIP(request)).isEqualTo("198.51.100.21");
        });
        assertThat(ServletUtils.getClientIP((jakarta.servlet.http.HttpServletRequest) null)).isEmpty();
        assertThatThrownBy(() -> ServletUtils.getClientIP(request, "X-Custom-IP"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private ClientAddressResolver resolver() {
        return new ClientAddressResolver(List.of("10.0.0.0/8"));
    }

    private MockHttpServletRequest request(String peer, String xff) {
        var request = new MockHttpServletRequest("GET", "/test");
        request.setRemoteAddr(peer);
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        return request;
    }
}
