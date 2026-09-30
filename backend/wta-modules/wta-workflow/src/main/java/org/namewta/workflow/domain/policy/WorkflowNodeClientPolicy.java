package org.namewta.workflow.domain.policy;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;

/** 解析现有设计器节点扩展，不把可编辑流程变量当作客户端授权事实。 */
public final class WorkflowNodeClientPolicy {
    public static final String CODE = "WorkflowClientPk";
    public static final String INITIATOR = "INITIATOR";

    private WorkflowNodeClientPolicy() { }

    /** 获取唯一的客户端配置；重复或缺失项拒绝。 */
    public static String configuredClient(String ext) {
        if (ext == null || ext.isBlank()) throw new ServiceException("流程节点未配置办理客户端");
        var entries = JsonUtils.parseArrayMap(ext);
        if (entries == null) throw new ServiceException("流程节点客户端配置无效");
        var matching = entries.stream().filter(entry -> CODE.equals(entry.getStr("code"))).toList();
        if (matching.size() != 1 || matching.getFirst().getStr("value") == null) {
            throw new ServiceException("流程节点必须配置一个办理客户端");
        }
        return matching.getFirst().getStr("value").strip();
    }

    /** 将明确的客户端主键转换为正整数。 */
    public static Long clientPk(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // 使用统一配置错误，避免泄露底层转换实现。
        }
        throw new ServiceException("流程节点办理客户端无效");
    }
}
