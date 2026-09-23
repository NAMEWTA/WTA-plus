package org.namewta.notify.support;

import org.namewta.common.core.exception.ServiceException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件模板变量占用位置校验与渲染。
 */
public final class NotifyTemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");

    private NotifyTemplateRenderer() {
    }

    /**
     * 校验模板只使用已声明变量，且包含全部必填变量。
     *
     * @param template      主题或正文
     * @param allowed       允许的变量名
     * @param requiredNames 必填变量名
     */
    public static void validate(String template, List<String> allowed, List<String> requiredNames) {
        String text = template == null ? "" : template;
        List<String> used = tokens(text);
        for (String name : used) {
            if (!allowed.contains(name)) {
                throw new ServiceException("不允许使用未声明变量 ${" + name + "}");
            }
        }
        for (String required : requiredNames) {
            if (!used.contains(required)) {
                throw new ServiceException("缺少必填变量 ${" + required + "}");
            }
        }
    }

    /**
     * 用变量值替换 token。
     *
     * @param template 模板
     * @param params   变量
     * @return 渲染结果
     */
    public static String render(String template, Map<String, String> params) {
        if (template == null) {
            return "";
        }
        Map<String, String> values = params == null ? Map.of() : params;
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * 提取模板中的变量名。
     *
     * @param template 模板
     * @return 变量名
     */
    public static List<String> tokens(String template) {
        List<String> names = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(template == null ? "" : template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * 将任意模板参数转为字符串映射。
     *
     * @param raw 原始参数
     * @return 字符串参数
     */
    public static Map<String, String> stringify(Map<String, ?> raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null) {
            return values;
        }
        raw.forEach((key, value) -> values.put(key, value == null ? "" : String.valueOf(value)));
        return values;
    }

    /** 逻辑模板快照只记录可解释的标识与变量名，绝不复制验证码或其他变量值。 */
    public static String smsSnapshot(String sceneCode, String templateCode, Collection<String> variableNames) {
        List<String> names = variableNames == null ? List.of() : variableNames.stream().sorted().toList();
        return "sms:scene=" + sceneCode + ";template=" + templateCode + ";variables=" + String.join(",", names);
    }
}
