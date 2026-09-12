package org.namewta.profile.person.domain.application;

import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.exception.PersonApplicationException;

import java.time.LocalDate;
import java.util.Locale;

/** PersonIdentityFields 应用层领域模型。 */
public record PersonIdentityFields(
    String fullName,
    String documentTypeCode,
    String documentNumber,
    String identityKey,
    String gender,
    LocalDate birthDate,
    LocalDate validFrom,
    LocalDate validUntil
) {

    /** 规范化身份字段。 */
    public static PersonIdentityFields normalize(PersonApplicationSaveBo command) {
        if (command == null) {
            throw new PersonApplicationException("PERSON_DRAFT_REQUIRED");
        }
        String documentType = upper(command.documentTypeCode());
        String documentNumber = upper(command.documentNumber());
        String identityKey = documentType == null || documentNumber == null
            ? null : documentType + ":" + documentNumber;
        return new PersonIdentityFields(text(command.fullName()), documentType, documentNumber, identityKey,
            upper(command.gender()), command.birthDate(), command.validFrom(), command.validUntil());
    }

    /** 转换为大写文本。 */
    private static String upper(String value) {
        String normalized = text(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /** 规范化文本内容。 */
    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
