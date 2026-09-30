package org.namewta.oidc.adapter.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 在 Servlet 容器解析协议表单前限制原文体积，覆盖无 Content-Length 的分块请求。 */
public final class OidcFormBodyFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES = 32 * 1024;
    private static final int MAX_PARAMETERS = 64;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest bounded;
        try {
            if (request.getContentLengthLong() > MAX_BYTES) throw new TooLarge();
            byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
            if (body.length > MAX_BYTES) throw new TooLarge();
            if (body.length != 0
                    && (request.getContentType() == null
                            || !request.getContentType()
                                    .split(";", 2)[0]
                                    .equalsIgnoreCase("application/x-www-form-urlencoded"))) {
                reject(response, 415);
                return;
            }
            Map<String, List<String>> decoded = new LinkedHashMap<>();
            parse(request.getQueryString(), decoded);
            parse(new String(body, StandardCharsets.UTF_8), decoded);
            Map<String, String[]> values = new LinkedHashMap<>();
            decoded.forEach((key, list) -> values.put(key, list.toArray(String[]::new)));
            bounded =
                    new HttpServletRequestWrapper(request) {
                        @Override
                        public String getParameter(String name) {
                            String[] found = values.get(name);
                            return found == null ? null : found[0];
                        }

                        @Override
                        public String[] getParameterValues(String name) {
                            String[] found = values.get(name);
                            return found == null ? null : found.clone();
                        }

                        @Override
                        public Map<String, String[]> getParameterMap() {
                            return Collections.unmodifiableMap(values);
                        }

                        @Override
                        public Enumeration<String> getParameterNames() {
                            return Collections.enumeration(values.keySet());
                        }
                    };
        } catch (TooLarge failure) {
            reject(response, 413);
            return;
        } catch (IllegalArgumentException failure) {
            reject(response, 400);
            return;
        }
        chain.doFilter(bounded, response);
    }

    /** 在有界原文内解析表单并保留重复参数，使框架继续执行重复校验。 */
    private void parse(String raw, Map<String, List<String>> values) {
        if (raw == null || raw.isEmpty()) return;
        if (raw.length() > MAX_BYTES) throw new TooLarge();
        int total = values.values().stream().mapToInt(List::size).sum();
        for (String entry : raw.split("&", -1)) {
            if (entry.isEmpty()) continue;
            if (++total > MAX_PARAMETERS) throw new TooLarge();
            String[] pair = entry.split("=", 2);
            String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value =
                    URLDecoder.decode(pair.length == 1 ? "" : pair[1], StandardCharsets.UTF_8);
            values.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
    }

    /** 请求体或格式越界时只返回标准错误，不回显任何凭据内容。 */
    private void reject(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"invalid_request\"}");
    }

    /** 有界解析在读取阶段中止，避免进入 Servlet 容器的无界表单解析。 */
    private static final class TooLarge extends IllegalArgumentException {}
}
