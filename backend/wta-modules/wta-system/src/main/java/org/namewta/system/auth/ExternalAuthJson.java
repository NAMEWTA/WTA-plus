package org.namewta.system.auth;

import org.namewta.common.json.utils.JsonUtils;
import tools.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

/** 接入配置的 JSON 集合转换。 */
public final class ExternalAuthJson {
    private ExternalAuthJson() {
    }

    /** 空数据库值统一为空选项。 */
    public static Map<String, String> options(String json) {
        return json == null || json.isBlank() ? Map.of() :
            Map.copyOf(JsonUtils.parseObject(json, new TypeReference<Map<String, String>>() { }));
    }

    /** 空数据库值统一为空 scope。 */
    public static List<String> scopes(String json) {
        return json == null || json.isBlank() ? List.of() :
            List.copyOf(JsonUtils.parseObject(json, new TypeReference<List<String>>() { }));
    }
}
