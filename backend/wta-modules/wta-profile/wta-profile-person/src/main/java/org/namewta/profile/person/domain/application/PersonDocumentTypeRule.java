package org.namewta.profile.person.domain.application;

/** PersonDocumentTypeRule 应用层领域模型。 */
public record PersonDocumentTypeRule(String documentTypeCode, String numberPattern, boolean validityRequired) {
}
