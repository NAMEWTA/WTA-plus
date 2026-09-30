package org.namewta.oidc.adapter.codec;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.io.*;
import java.util.Base64;

/** 对经认证加密存储的框架状态做有界反序列化；不接受浏览器提供的序列化数据。 */
public final class OidcAuthorizationCodec {
    /** 工具类不允许实例化，调用方使用静态协议操作。 */
    private OidcAuthorizationCodec() {}

    /** 序列化可信框架授权状态，调用方随后执行认证加密。 */
    public static String encode(OAuth2Authorization value) {
        try (var bytes = new ByteArrayOutputStream();
                var out =
                        new ObjectOutputStream(bytes) {
                            {
                                enableReplaceObject(true);
                            }

                            @Override
                            protected Object replaceObject(Object value) {
                                // Framework claim conversion may materialize issuer as URL. Persist
                                // its
                                // protocol string value, avoiding URL deserialization/DNS behavior.
                                if (value instanceof java.net.URL url) return url.toExternalForm();
                                if (value instanceof java.net.URI uri) return uri.toASCIIString();
                                return value;
                            }
                        }) {
            out.writeObject(value);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("OIDC state serialization failed", e);
        }
    }

    /** 只解析通过认证解密的有界授权状态并限制允许类型。 */
    public static OAuth2Authorization decode(String value) {
        if (value.length() > 1398104)
            throw new IllegalArgumentException("OIDC state exceeds limit");
        byte[] bytes = Base64.getDecoder().decode(value);
        if (bytes.length > 1048576) throw new IllegalArgumentException("OIDC state exceeds limit");
        var rejected = new java.util.concurrent.atomic.AtomicReference<String>("limits");
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(
                    info -> {
                        if (info.depth() > 30
                                || info.references() > 20000
                                || info.streamBytes() > 1048576)
                            return ObjectInputFilter.Status.REJECTED;
                        Class<?> type = info.serialClass();
                        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                        while (type.isArray()) type = type.getComponentType();
                        String name = type.getName();
                        boolean allowed =
                                type.isPrimitive()
                                        || name.startsWith("java.lang.")
                                        || name.startsWith("java.time.")
                                        || name.startsWith("java.util.")
                                        || name.startsWith("org.springframework.security.oauth2.")
                                        || name.equals(
                                                "org.springframework.security.authentication.UsernamePasswordAuthenticationToken")
                                        || name.equals(
                                                "org.springframework.security.authentication.AbstractAuthenticationToken")
                                        || name.equals(
                                                "org.springframework.security.core.authority.SimpleGrantedAuthority")
                                        || name.equals(
                                                "org.springframework.security.core.authority.FactorGrantedAuthority")
                                        || name.equals("org.namewta.oidc.domain.OidcPrincipal");
                        if (!allowed) rejected.set(name);
                        return allowed
                                ? ObjectInputFilter.Status.ALLOWED
                                : ObjectInputFilter.Status.REJECTED;
                    });
            return (OAuth2Authorization) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("OIDC state rejected type=" + rejected.get(), e);
        }
    }
}
