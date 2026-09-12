package org.namewta.common.openapi.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OpenApiProtocolTest {

    private static final String SECRET = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void matchesPublishedUnicodeAndRepeatedQueryVector() {
        OpenApiRequest request = new OpenApiRequest(
            "nw_live_123",
            "1788172800",
            "AAECAwQFBgcICQoLDA0ODw",
            "post",
            "/system//openApi/%7euser/文件",
            "tag=z&empty=&tag=%E4%B8%AD%E6%96%87&plus=a+b&bare",
            "{\"name\":\"NAMEWTA\"}".getBytes(StandardCharsets.UTF_8));

        String canonical = new OpenApiCanonicalizer().canonicalize(request);

        assertThat(canonical).isEqualTo("""
            NAMEWTA-HMAC-SHA256
            v1
            nw_live_123
            1788172800
            AAECAwQFBgcICQoLDA0ODw
            POST
            /system//openApi/~user/%E6%96%87%E4%BB%B6
            bare=&empty=&plus=a%2Bb&tag=%E4%B8%AD%E6%96%87&tag=z
            541d56686b266d9c02c3fdb7882e52958f719bc25eedd77cbfe57c92bb99b625""");
        assertThat(new OpenApiSigner(new OpenApiCanonicalizer()).sign(request, SECRET))
            .isEqualTo("ZDWDgeVlxdBvYm9jdzsbJYfITNdMmcxbXZmfBB5IJ5Y");
    }

    @Test
    void preservesEmptyBodyAndRejectsMalformedEncoding() {
        OpenApiRequest empty = new OpenApiRequest(
            "app", "1788172800", "AAECAwQFBgcICQoLDA0ODw", "GET", "/health/", "", new byte[0]);

        assertThat(new OpenApiCanonicalizer().canonicalize(empty))
            .endsWith("\n/health/\n\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThatThrownBy(() -> new OpenApiCanonicalizer().canonicalizePath("/broken/%GG"))
            .isInstanceOf(OpenApiAuthenticationException.class);
    }

    @Test
    void verifiesInConstantShapeAndRejectsTampering() {
        OpenApiRequest request = new OpenApiRequest(
            "app", "1788172800", "AAECAwQFBgcICQoLDA0ODw", "GET", "/items", "id=1", new byte[0]);
        OpenApiSigner signer = new OpenApiSigner(new OpenApiCanonicalizer());
        String signature = signer.sign(request, SECRET);

        assertThat(signer.verify(request, SECRET, signature)).isTrue();
        assertThat(signer.verify(new OpenApiRequest(
            "app", "1788172800", "AAECAwQFBgcICQoLDA0ODw", "GET", "/items", "id=2", new byte[0]),
            SECRET, signature)).isFalse();
        assertThat(signer.verify(request, SECRET, "not-base64!" )).isFalse();
    }

    @Test
    void annotationCanOnlyTargetMethods() {
        Target target = org.namewta.common.openapi.annotation.OpenApi.class.getAnnotation(Target.class);
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

}
