package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcPrincipal;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.system.api.domain.AccountIdentity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 实时 WTA 账户与核准资料投影，不向资料端请求未授权字段。 */
@Service
@RequiredArgsConstructor
public class OidcIdentityService {
    private final org.namewta.oidc.port.OidcIdentityPort identity;

    /** 读取可信 SSO 会话快照，缺失或失效时为空。 */
    public SsoSessionSnapshot session(String sid) {
        return sid == null ? null : identity.session(sid);
    }

    /** 根据当前正常账户和真实 SSO 认证时间构造协议身份。 */
    public OidcPrincipal principal(String sid, Set<String> allowed, String subject) {
        var s = session(sid);
        if (s == null || identity.account(s.userId()) == null)
            throw new OAuth2AuthenticationException("login_required");
        return new OidcPrincipal(
                subject,
                s.userId(),
                sid,
                s.authenticatedAt().getEpochSecond(),
                s.expiresAt().getEpochSecond(),
                Set.copyOf(allowed));
    }

    /** 校验当前身份或记录仍然有效，失效时拒绝继续。 */
    public AccountIdentity require(OidcPrincipal p) {
        var s = session(p.sessionId());
        var user = identity.account(p.userId());
        if (s == null
                || user == null
                || !p.userId().equals(s.userId())
                || p.authTime() != s.authenticatedAt().getEpochSecond())
            throw new OAuth2AuthenticationException("invalid_grant");
        return user;
    }

    /** 签发事务先锁会话，再锁应用，所有路径保持同一锁顺序。 */
    public void lockSession(OidcPrincipal p) {
        identity.lockSession(p.sessionId());
    }

    /** 删除指定 SSO 会话，不扩大到其他浏览器。 */
    public void logout(String sid) {
        identity.logout(sid);
    }

    /** 以授权快照、当前字段策略与 scope 的交集发布当前资料。 */
    public Map<String, Object> userInfo(
            OidcPrincipal p, Collection<String> current, Set<String> scopes) {
        var user = require(p);
        var allow =
                org.namewta.oidc.support.OidcFieldCatalog.permitted(
                        p.allowedFields(), current, scopes);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sub", p.name());
        put(result, allow, "preferred_username", user.username());
        put(result, allow, "nickname", user.nickname());
        put(result, allow, "email", user.email());
        put(result, allow, "phone_number", user.phoneNumber());
        if (allow.contains("picture"))
            put(result, allow, "picture", identity.picture(user.avatarId()));
        Set<ProfileDisclosureField> selected = EnumSet.noneOf(ProfileDisclosureField.class);
        for (var f : ProfileDisclosureField.values())
            if (allow.contains(f.claimName())) selected.add(f);
        if (!selected.isEmpty()) {
            var d = identity.profile(p.userId(), selected);
            if (d != null) {
                var person = d.person();
                var enterprise = d.enterprise();
                if (person != null) put(result, allow, "person_verified", person.verified());
                if (person != null) put(result, allow, "person_profile_id", person.profileId());
                if (person != null) put(result, allow, "person_verified_at", person.verifiedAt());
                if (person != null) put(result, allow, "person_full_name", person.fullName());
                if (person != null) put(result, allow, "person_gender", person.gender());
                if (person != null) put(result, allow, "person_birth_date", person.birthDate());
                if (person != null)
                    put(result, allow, "person_document_type", person.documentType());
                if (person != null)
                    put(
                            result,
                            allow,
                            "person_document_number_masked",
                            person.documentNumberMasked());
                if (person != null)
                    put(result, allow, "person_document_number", person.documentNumber());
                if (person != null) put(result, allow, "person_valid_from", person.validFrom());
                if (person != null) put(result, allow, "person_valid_until", person.validUntil());
                if (enterprise != null)
                    put(result, allow, "enterprise_verified", enterprise.verified());
                if (enterprise != null)
                    put(result, allow, "enterprise_profile_id", enterprise.profileId());
                if (enterprise != null)
                    put(result, allow, "enterprise_verified_at", enterprise.verifiedAt());
                if (enterprise != null) put(result, allow, "enterprise_name", enterprise.name());
                if (enterprise != null)
                    put(result, allow, "enterprise_credit_code", enterprise.creditCode());
                if (enterprise != null) put(result, allow, "enterprise_type", enterprise.type());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_legal_representative_name",
                            enterprise.legalRepresentativeName());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_legal_document_type",
                            enterprise.legalDocumentType());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_legal_document_number_masked",
                            enterprise.legalDocumentNumberMasked());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_legal_document_number",
                            enterprise.legalDocumentNumber());
                if (enterprise != null)
                    put(result, allow, "enterprise_established_date", enterprise.establishedDate());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_business_term_from",
                            enterprise.businessTermFrom());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_business_term_until",
                            enterprise.businessTermUntil());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_registered_address",
                            enterprise.registeredAddress());
                if (enterprise != null)
                    put(result, allow, "enterprise_business_scope", enterprise.businessScope());
                if (enterprise != null)
                    put(result, allow, "enterprise_contact_name", enterprise.contactName());
                if (enterprise != null)
                    put(result, allow, "enterprise_contact_phone", enterprise.contactPhone());
                if (enterprise != null) put(result, allow, "enterprise_email", enterprise.email());
                if (enterprise != null)
                    put(
                            result,
                            allow,
                            "enterprise_registered_capital",
                            enterprise.registeredCapital());
                if (enterprise != null)
                    put(result, allow, "enterprise_industry_code", enterprise.industryCode());
                if (enterprise != null)
                    put(result, allow, "enterprise_website", enterprise.website());
            }
        }
        return result;
    }

    /** 字段经最终白名单检查后才能输出；资料使用分组对象，长整型标识保留精度。 */
    private void put(Map<String, Object> result, Set<String> allowed, String key, Object value) {
        if (!allowed.contains(key)
                || value == null
                || value instanceof String text && text.isBlank()) return;
        if (value instanceof java.time.temporal.TemporalAccessor || value instanceof Long)
            value = value.toString();
        if (key.startsWith("person_") || key.startsWith("enterprise_")) {
            String group = key.startsWith("person_") ? "person" : "enterprise";
            @SuppressWarnings("unchecked")
            Map<String, Object> nested =
                    (Map<String, Object>)
                            result.computeIfAbsent(
                                    "wta_" + group, k -> new LinkedHashMap<String, Object>());
            nested.put(key.substring(group.length() + 1), value);
        } else result.put(key, value);
    }
}
