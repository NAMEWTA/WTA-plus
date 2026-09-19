package org.namewta.common.web.filter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.namewta.common.core.http.CapturedRequestBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 请求包装器，统一清洗参数与 JSON 请求体中的 HTML 标签内容。
 *
 * @author wta
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final CapturedRequestBody view;

    /**
     * 使用原始请求构造 XSS 包装器。
     *
     * @param request 原始请求
     * @param maxBodyBytes 未捕获正文的默认上限
     * @throws IOException 读取或视图字节预算失败，必须在进入业务前处理
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request, int maxBodyBytes) throws IOException {
        super(request);
        if (isJsonRequest()) {
            CapturedRequestBody raw = CapturedRequestBody.find(request);
            if (raw == null) raw = CapturedRequestBody.capture(request, maxBodyBytes);
            view = raw.utf8View(HtmlUtil.cleanHtmlTag(raw.utf8()).trim());
        } else {
            view = null;
        }
    }

    /**
     * 获取并清洗单个请求参数。
     *
     * @param name 参数名
     * @return 清洗后的参数值
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value == null) {
            return null;
        }
        return HtmlUtil.cleanHtmlTag(value).trim();
    }

    /**
     * 获取并清洗整组请求参数。
     *
     * @return 清洗后的参数映射
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> valueMap = super.getParameterMap();
        if (MapUtil.isEmpty(valueMap)) {
            return valueMap;
        }
        // 避免某些容器不允许改参数的情况 copy一份重新改
        Map<String, String[]> map = new HashMap<>(valueMap.size());
        map.putAll(valueMap);
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String[] values = entry.getValue();
            if (values != null) {
                int length = values.length;
                String[] escapseValues = new String[length];
                for (int i = 0; i < length; i++) {
                    // 防xss攻击和过滤前后空格
                    escapseValues[i] = HtmlUtil.cleanHtmlTag(values[i]).trim();
                }
                map.put(entry.getKey(), escapseValues);
            }
        }
        return map;
    }

    /**
     * 获取并清洗指定参数的多值数组。
     *
     * @param name 参数名
     * @return 清洗后的参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtil.isEmpty(values)) {
            return values;
        }
        int length = values.length;
        String[] escapseValues = new String[length];
        for (int i = 0; i < length; i++) {
            // 防xss攻击和过滤前后空格
            escapseValues[i] = HtmlUtil.cleanHtmlTag(values[i]).trim();
        }
        return escapseValues;
    }

    /**
     * 获取输入流并在 JSON 场景下对请求体执行清洗。
     *
     * @return 清洗后的输入流
     * @throws IOException 读取请求体异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return view == null ? super.getInputStream() : view.openStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return view == null ? super.getReader()
            : new BufferedReader(new InputStreamReader(view.openStream(), StandardCharsets.UTF_8));
    }

    @Override public int getContentLength() { return view == null ? super.getContentLength() : view.length(); }
    @Override public long getContentLengthLong() { return view == null ? super.getContentLengthLong() : view.length(); }

    /**
     * 判断当前请求是否为 JSON 请求。
     *
     * @return true 表示 JSON 请求
     */
    public boolean isJsonRequest() {
        return CapturedRequestBody.isJsonContentType(super.getContentType());
    }
}
