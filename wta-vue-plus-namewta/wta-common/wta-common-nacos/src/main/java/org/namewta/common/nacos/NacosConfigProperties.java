package org.namewta.common.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可选 Nacos Config 客户端参数。
 */
@ConfigurationProperties(prefix = "nacos.config")
public class NacosConfigProperties {

    private boolean enabled;
    private String serverAddr = NacosConfigConstants.DEFAULT_SERVER_ADDR;
    private String username = "";
    private String password = "";
    private String group = NacosConfigConstants.DEFAULT_GROUP;
    private String dataId = NacosConfigConstants.DEFAULT_DATA_ID;
    private long timeoutMs = NacosConfigConstants.DEFAULT_TIMEOUT_MS;
    private Map<String, String> namespaces = defaultNamespaces();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    private static Map<String, String> defaultNamespaces() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("local", "local");
        defaults.put("dev", "dev");
        defaults.put("prod", "prod");
        return defaults;
    }
}
