package org.namewta.common.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.RequestBodyTooLargeException;
import org.namewta.common.core.http.CapturedRequestBody;
import org.namewta.common.json.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 为普通JSON建立唯一有界缓存，上传与持续流继续使用容器原始流。 */
public class RepeatableFilter implements Filter {
    private final int maxBodyBytes;

    public RepeatableFilter(int maxBodyBytes) {
        CapturedRequestBody.requireLimit(maxBodyBytes);
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ServletRequest effective = request;
        if (request instanceof HttpServletRequest httpRequest
            && response instanceof HttpServletResponse httpResponse
            && CapturedRequestBody.isJsonContentType(request.getContentType())) {
            try {
                effective = new RepeatedlyRequestWrapper(httpRequest, response, maxBodyBytes);
            } catch (RequestBodyTooLargeException exception) {
                rejectTooLarge(httpResponse);
                return;
            }
        }
        chain.doFilter(effective, response);
    }

    /** 在业务执行前映射真实413；不使用会重置为200的通用renderString。 */
    public static void rejectTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(413);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtils.toJsonString(R.fail(413, RequestBodyTooLargeException.ERROR_CODE)));
    }
}
