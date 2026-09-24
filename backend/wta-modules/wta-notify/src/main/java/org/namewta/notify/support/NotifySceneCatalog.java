package org.namewta.notify.support;

import java.util.List;
import java.util.Map;

/**
 * 代码播种的逻辑场景与变量契约。
 */
public final class NotifySceneCatalog {

    private NotifySceneCatalog() {
    }

    /**
     * 播种场景编码。
     *
     * @return 场景编码列表
     */
    public static List<String> sceneCodes() {
        return List.of("auth-captcha", "person-rebind", "enterprise-transfer", "workflow-task", "notice-published", "demo-mail");
    }

    /**
     * 场景标题。
     *
     * @param sceneCode 场景编码
     * @return 标题
     */
    public static String title(String sceneCode) {
        return switch (sceneCode) {
            case "auth-captcha" -> "登录验证码";
            case "person-rebind" -> "个人实名换绑";
            case "enterprise-transfer" -> "企业负责人转移";
            case "workflow-task" -> "工作流待办";
            case "notice-published" -> "公告发布";
            case "demo-mail" -> "邮件演示";
            default -> sceneCode;
        };
    }

    /**
     * 场景变量。
     *
     * @param sceneCode 场景编码
     * @return 变量列表
     */
    public static List<Variable> variables(String sceneCode) {
        return switch (sceneCode) {
            case "auth-captcha" -> List.of(
                new Variable("code", true, "1234", "验证码"),
                new Variable("expireMinutes", true, "5", "有效分钟数"));
            case "person-rebind" -> List.of();
            case "enterprise-transfer" -> List.of(new Variable("code", true, "123456", "转移验证码"));
            case "workflow-task", "notice-published" -> List.of(
                new Variable("title", true, "标题", "业务标题"),
                new Variable("content", true, "正文", "业务正文"),
                new Variable("path", true, "/task", "跳转路径"));
            case "demo-mail" -> List.of(
                new Variable("title", true, "演示标题", "邮件标题"),
                new Variable("content", true, "演示正文", "邮件正文"));
            default -> List.of();
        };
    }

    /**
     * 必填变量名。
     *
     * @param sceneCode 场景编码
     * @return 必填变量
     */
    public static List<String> requiredNames(String sceneCode) {
        return variables(sceneCode).stream().filter(Variable::required).map(Variable::name).toList();
    }

    /**
     * 样例变量。
     *
     * @param sceneCode 场景编码
     * @return 样例
     */
    public static Map<String, String> examples(String sceneCode) {
        return variables(sceneCode).stream().collect(java.util.stream.Collectors.toMap(Variable::name, Variable::example));
    }

    /**
     * 场景变量定义。
     *
     * @param name        变量名
     * @param required    是否必填
     * @param example     样例
     * @param description 说明
     */
    public record Variable(String name, boolean required, String example, String description) {
    }
}
