package org.namewta.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.ip.ClientAddressResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 在授权、限流和审计前固定可信来源地址；非法可信链不进入业务处理。 */
public final class ClientAddressFilter extends OncePerRequestFilter {
    private final ClientAddressResolver resolver;

    public ClientAddressFilter(ClientAddressResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String address;
        try {
            address = resolver.resolve(request);
        } catch (IllegalArgumentException ignored) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        request.setAttribute(ServletUtils.CLIENT_IP_ATTRIBUTE, address);
        chain.doFilter(request, response);
    }
}
