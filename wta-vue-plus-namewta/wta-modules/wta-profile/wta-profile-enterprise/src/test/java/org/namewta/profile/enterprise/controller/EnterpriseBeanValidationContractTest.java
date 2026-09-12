package org.namewta.profile.enterprise.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Positive;
import org.namewta.profile.enterprise.controller.admin.EnterpriseAdminController;
import org.namewta.profile.enterprise.controller.admin.EnterpriseMaterialAdminController;
import org.namewta.profile.enterprise.controller.anonymous.EnterpriseVerificationAnonymousController;
import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.controller.self.EnterpriseMaterialSelfController;
import org.namewta.profile.enterprise.controller.self.EnterpriseTransferController;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminAssignBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminBindingBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminCreateBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminDecisionBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminIdentityBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminMaterialBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminReviseBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminRevokeBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSubmitBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterpriseBeanValidationContractTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
        EnterpriseAdminController.class,
        EnterpriseMaterialAdminController.class,
        EnterpriseApplicationController.class,
        EnterpriseMaterialSelfController.class,
        EnterpriseTransferController.class,
        EnterpriseVerificationAnonymousController.class
    );

    @Test
    void controllersDeclareValidationAtEveryHttpInputBoundary() {
        CONTROLLERS.forEach(controller -> {
            assertThat(controller.isAnnotationPresent(Validated.class)).as(controller.getSimpleName()).isTrue();
            for (var method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        assertThat(parameter.isAnnotationPresent(Valid.class))
                            .as(controller.getSimpleName() + "." + method.getName() + " request body")
                            .isTrue();
                    }
                    if (parameter.isAnnotationPresent(PathVariable.class)
                        && (parameter.getType() == long.class || parameter.getType() == Long.class)) {
                        assertThat(parameter.isAnnotationPresent(Positive.class))
                            .as(controller.getSimpleName() + "." + method.getName() + " numeric path variable")
                            .isTrue();
                    }
                }
            }
        });
    }

    @Test
    void administrativeCommandsRejectMissingOversizedAndNegativeStructure() {
        assertInvalid(new EnterpriseAdminAssignBo(0L, " "), "userId", "reason");
        assertInvalid(new EnterpriseAdminBindingBo(" ", " ", -1),
            "action", "reason", "expectedBindingVersion");
        assertInvalid(new EnterpriseAdminCreateBo(null, 0L, " ",
                Collections.nCopies(11, new EnterpriseAdminMaterialBo(1L, 1L))),
            "identity", "bindUserId", "reason", "materials");
        assertInvalid(new EnterpriseAdminDecisionBo(" ", " "), "decision", "reason");
        assertInvalid(new EnterpriseAdminIdentityBo(
                " ", " ", " ", " ", " ", " ", null, null, null,
                " ", " ", "x".repeat(101), "x".repeat(65), "x".repeat(256),
                BigDecimal.valueOf(-1), "x".repeat(65), "x".repeat(501)),
            "enterpriseName", "unifiedCreditCode", "enterpriseType", "legalRepresentativeName",
            "legalDocumentTypeCode", "legalDocumentNumber", "establishedDate", "registeredAddress",
            "businessScope", "contactName", "contactPhone", "email", "registeredCapital",
            "industryCode", "website");
        assertInvalid(new EnterpriseAdminMaterialBo(0L, -1L), "ossId", "materialNodeId");
        assertInvalid(new EnterpriseAdminReviseBo(null, " ", -1),
            "identity", "reason", "expectedVersion");
        assertInvalid(new EnterpriseAdminRevokeBo(" ", -1), "reason", "expectedVersion");
    }

    @Test
    void selfServiceCommandsPreserveDraftOptionalityButEnforceDeclaredBounds() {
        assertInvalid(new EnterpriseApplicationProbeBo(" "), "unifiedCreditCode");
        assertInvalid(new EnterpriseApplicationSaveBo(
                "x".repeat(256), "x".repeat(65), "x".repeat(65), "x".repeat(101),
                "x".repeat(65), "x".repeat(129), false, null, null, null,
                "x".repeat(501), null, "x".repeat(101), "x".repeat(65), "x".repeat(256),
                BigDecimal.valueOf(-1), "x".repeat(65), "x".repeat(501), -1),
            "enterpriseName", "unifiedCreditCode", "enterpriseType", "legalRepresentativeName",
            "legalDocumentTypeCode", "legalDocumentNumber", "registeredAddress", "contactName",
            "contactPhone", "email", "registeredCapital", "industryCode", "website", "expectedVersion");
        assertInvalid(new EnterpriseApplicationSubmitBo(-1), "expectedVersion");
        assertInvalid(new EnterpriseMaterialSelfController.AttachRequest(0L, -1L),
            "ossId", "materialNodeId");
        assertInvalid(new EnterpriseTransferSendBo(" ", "123", " "),
            "fullName", "documentLastFour", "phone");
        assertInvalid(new EnterpriseTransferConfirmBo(" ", "12345"), "challengeId", "code");
    }

    private void assertInvalid(Object value, String... expectedProperties) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<String> properties = validator.validate(value).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
            assertThat(properties).contains(expectedProperties);
        }
    }
}
