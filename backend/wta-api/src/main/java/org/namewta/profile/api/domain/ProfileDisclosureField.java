package org.namewta.profile.api.domain;

/** 档案对外发布字段的封闭白名单；完整证件号码必须获得管理员显式授权。 */
public enum ProfileDisclosureField {
    PERSON_VERIFIED(ProfileType.PERSON, "person_verified", "认证状态", false),
    PERSON_PROFILE_ID(ProfileType.PERSON, "person_profile_id", "个人档案编号", false),
    PERSON_VERIFIED_AT(ProfileType.PERSON, "person_verified_at", "个人认证时间", false),
    PERSON_FULL_NAME(ProfileType.PERSON, "person_full_name", "姓名", false),
    PERSON_GENDER(ProfileType.PERSON, "person_gender", "性别", false),
    PERSON_BIRTH_DATE(ProfileType.PERSON, "person_birth_date", "出生日期", false),
    PERSON_DOCUMENT_TYPE(ProfileType.PERSON, "person_document_type", "证件类型", false),
    PERSON_DOCUMENT_NUMBER_MASKED(ProfileType.PERSON, "person_document_number_masked", "脱敏证件号码", false),
    PERSON_DOCUMENT_NUMBER(ProfileType.PERSON, "person_document_number", "完整证件号码", true),
    PERSON_VALID_FROM(ProfileType.PERSON, "person_valid_from", "证件有效期起", false),
    PERSON_VALID_UNTIL(ProfileType.PERSON, "person_valid_until", "证件有效期止", false),
    ENTERPRISE_VERIFIED(ProfileType.ENTERPRISE, "enterprise_verified", "企业认证状态", false),
    ENTERPRISE_PROFILE_ID(ProfileType.ENTERPRISE, "enterprise_profile_id", "企业档案编号", false),
    ENTERPRISE_VERIFIED_AT(ProfileType.ENTERPRISE, "enterprise_verified_at", "企业认证时间", false),
    ENTERPRISE_NAME(ProfileType.ENTERPRISE, "enterprise_name", "企业名称", false),
    ENTERPRISE_CREDIT_CODE(ProfileType.ENTERPRISE, "enterprise_credit_code", "统一社会信用代码", false),
    ENTERPRISE_TYPE(ProfileType.ENTERPRISE, "enterprise_type", "单位性质", false),
    ENTERPRISE_LEGAL_REPRESENTATIVE_NAME(ProfileType.ENTERPRISE, "enterprise_legal_representative_name", "法定代表人姓名", false),
    ENTERPRISE_LEGAL_DOCUMENT_TYPE(ProfileType.ENTERPRISE, "enterprise_legal_document_type", "法人证件类型", false),
    ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED(ProfileType.ENTERPRISE, "enterprise_legal_document_number_masked", "脱敏法人证件号码", false),
    ENTERPRISE_LEGAL_DOCUMENT_NUMBER(ProfileType.ENTERPRISE, "enterprise_legal_document_number", "完整法人证件号码", true),
    ENTERPRISE_ESTABLISHED_DATE(ProfileType.ENTERPRISE, "enterprise_established_date", "成立日期", false),
    ENTERPRISE_BUSINESS_TERM_FROM(ProfileType.ENTERPRISE, "enterprise_business_term_from", "营业期限起", false),
    ENTERPRISE_BUSINESS_TERM_UNTIL(ProfileType.ENTERPRISE, "enterprise_business_term_until", "营业期限止", false),
    ENTERPRISE_REGISTERED_ADDRESS(ProfileType.ENTERPRISE, "enterprise_registered_address", "注册地址", false),
    ENTERPRISE_BUSINESS_SCOPE(ProfileType.ENTERPRISE, "enterprise_business_scope", "经营范围", false),
    ENTERPRISE_CONTACT_NAME(ProfileType.ENTERPRISE, "enterprise_contact_name", "联系人", false),
    ENTERPRISE_CONTACT_PHONE(ProfileType.ENTERPRISE, "enterprise_contact_phone", "联系电话", false),
    ENTERPRISE_EMAIL(ProfileType.ENTERPRISE, "enterprise_email", "企业邮箱", false),
    ENTERPRISE_REGISTERED_CAPITAL(ProfileType.ENTERPRISE, "enterprise_registered_capital", "注册资本", false),
    ENTERPRISE_INDUSTRY_CODE(ProfileType.ENTERPRISE, "enterprise_industry_code", "行业编码", false),
    ENTERPRISE_WEBSITE(ProfileType.ENTERPRISE, "enterprise_website", "企业网站", false);

    private final ProfileType profileType;
    private final String claimName;
    private final String label;
    private final boolean sensitive;

    /** 保存字段归属、协议名称及管理界面提示。 */
    ProfileDisclosureField(ProfileType profileType, String claimName, String label, boolean sensitive) {
        this.profileType = profileType;
        this.claimName = claimName;
        this.label = label;
        this.sensitive = sensitive;
    }

    /** 返回拥有字段的档案子域。 */
    public ProfileType profileType() { return profileType; }

    /** 返回稳定的对外 claim 名称。 */
    public String claimName() { return claimName; }

    /** 返回管理界面中文名称。 */
    public String label() { return label; }

    /** 是否为必须额外提示的完整身份证件号码。 */
    public boolean sensitive() { return sensitive; }
}

