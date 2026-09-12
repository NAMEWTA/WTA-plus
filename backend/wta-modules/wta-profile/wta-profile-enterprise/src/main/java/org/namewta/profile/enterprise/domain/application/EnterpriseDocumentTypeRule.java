package org.namewta.profile.enterprise.domain.application;

/** EnterpriseDocumentTypeRule 应用层领域模型。 */
public record EnterpriseDocumentTypeRule(String documentTypeCode, String numberPattern, boolean validityRequired) {
}
