package org.namewta.sso.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSO 读取的 Client 目录视图，不含可回显明文密钥。
 */
public class SsoClientView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String clientId;
    private String clientKey;
    private String status;
    private Boolean ssoEnabled;
    private String ssoAuthMode;
    private String ssoClientKind;
    private List<String> redirectUris;
    private Boolean pkceRequired;
    private Boolean autoConsent;
    private String scope;
    private String ssoSecretHash;
    private String deviceType;
    private Long timeout;
    private Long activeTimeout;
    private String accessPath;
    private String ipWhitelist;
    private Long userTypeId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getSsoEnabled() {
        return ssoEnabled;
    }

    public void setSsoEnabled(Boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    public String getSsoAuthMode() {
        return ssoAuthMode;
    }

    public void setSsoAuthMode(String ssoAuthMode) {
        this.ssoAuthMode = ssoAuthMode;
    }

    public String getSsoClientKind() {
        return ssoClientKind;
    }

    public void setSsoClientKind(String ssoClientKind) {
        this.ssoClientKind = ssoClientKind;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Boolean getPkceRequired() {
        return pkceRequired;
    }

    public void setPkceRequired(Boolean pkceRequired) {
        this.pkceRequired = pkceRequired;
    }

    public Boolean getAutoConsent() {
        return autoConsent;
    }

    public void setAutoConsent(Boolean autoConsent) {
        this.autoConsent = autoConsent;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSsoSecretHash() {
        return ssoSecretHash;
    }

    public void setSsoSecretHash(String ssoSecretHash) {
        this.ssoSecretHash = ssoSecretHash;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getActiveTimeout() {
        return activeTimeout;
    }

    public void setActiveTimeout(Long activeTimeout) {
        this.activeTimeout = activeTimeout;
    }

    public String getAccessPath() {
        return accessPath;
    }

    public void setAccessPath(String accessPath) {
        this.accessPath = accessPath;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public Long getUserTypeId() {
        return userTypeId;
    }

    public void setUserTypeId(Long userTypeId) {
        this.userTypeId = userTypeId;
    }
}
