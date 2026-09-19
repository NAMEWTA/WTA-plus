package org.namewta.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class ServletUtilsClientAddressTest {
    @Test
    void directClientCannotChooseItsAddressWithHeaders() {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "getRemoteAddr" -> "198.51.100.21";
                    case "getHeader" -> "192.0.2.77";
                    default -> null;
                };
            });
        assertEquals("198.51.100.21", ServletUtils.getClientIP(request));
    }
}
