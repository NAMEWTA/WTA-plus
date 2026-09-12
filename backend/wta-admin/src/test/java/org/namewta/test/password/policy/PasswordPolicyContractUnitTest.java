package org.namewta.test.password.policy;

import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.system.password.PasswordCharacterClass;
import org.namewta.system.password.PasswordPolicy;
import org.namewta.system.password.PasswordPolicyConfigParser;
import org.namewta.system.password.PasswordPolicyProjection;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.password.PasswordViolation;
import org.namewta.system.service.ISysConfigService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class PasswordPolicyContractUnitTest {

    private static final String VALID_RANDOM = """
        {
          "version": 1,
          "minimumLength": 8,
          "maximumLength": 30,
          "requireUppercase": true,
          "requireLowercase": true,
          "requireDigit": true,
          "requireSpecial": true,
          "allowedSpecialCharacters": "@$!%*?&",
          "generator": {
            "length": 12,
            "uppercaseCharacters": "ABCDEFGHJKLMNPQRSTUVWXYZ",
            "lowercaseCharacters": "abcdefghijkmnopqrstuvwxyz",
            "digitCharacters": "23456789",
            "specialCharacters": "@$!%*?&"
          },
          "defaultPassword": { "mode": "RANDOM" }
        }
        """;

    @Test
    void shouldParseProjectAndGeneratePolicyWithoutExposingInternals() {
        PasswordPolicyService service = service(VALID_RANDOM);

        PasswordPolicyProjection projection = service.publicProjection();
        assertEquals(8, projection.minimumLength());
        assertEquals(30, projection.maximumLength());
        assertEquals(List.of(PasswordCharacterClass.UPPERCASE, PasswordCharacterClass.LOWERCASE,
            PasswordCharacterClass.DIGIT, PasswordCharacterClass.SPECIAL), projection.requiredCharacterClasses());
        assertEquals("@$!%*?&", projection.allowedSpecialCharacters());

        String first = service.generateDefaultPassword();
        assertEquals(12, first.length());
        assertTrue(service.validate(first).isEmpty());
        Set<String> samples = IntStream.range(0, 100)
            .mapToObj(ignored -> service.generateDefaultPassword())
            .peek(sample -> assertTrue(service.validate(sample).isEmpty()))
            .collect(Collectors.toSet());
        assertTrue(samples.size() > 95);
        assertFalse(samples.contains("123456"));
        assertFalse(projection.toString().contains("ABCDEFG"));
        assertFalse(projection.toString().contains("fixedValue"));
    }

    @Test
    void shouldReturnEveryViolationInStableOrderAndMapItToFailureData() {
        PasswordPolicyService service = service(VALID_RANDOM);

        List<PasswordViolation> violations = service.validate("a ");
        assertEquals(List.of("PASSWORD_TOO_SHORT", "PASSWORD_MISSING_UPPERCASE",
                "PASSWORD_MISSING_DIGIT", "PASSWORD_MISSING_SPECIAL",
                "PASSWORD_CONTAINS_DISALLOWED_CHARACTER"),
            violations.stream().map(PasswordViolation::reason).toList());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateOrThrow("a "));
        R<?> response = new GlobalExceptionHandler().handleServiceException(exception, null);
        assertEquals("密码不符合安全策略", response.getMsg());
        assertEquals(violations, ((Map<?, ?>) response.getData()).get("violations"));

        List<String> remainingReasons = service.validate("A1!" + "A".repeat(28)).stream()
            .map(PasswordViolation::reason).toList();
        assertEquals(List.of("PASSWORD_TOO_LONG", "PASSWORD_MISSING_LOWERCASE"), remainingReasons);

        R<?> ordinary = new GlobalExceptionHandler().handleServiceException(
            new ServiceException("ordinary-error", 409), null);
        assertEquals(409, ordinary.getCode());
        assertEquals("ordinary-error", ordinary.getMsg());
        assertEquals(null, ordinary.getData());
    }

    @Test
    void shouldRejectMalformedOrImpossibleConfigurationWithoutLeakingValues() {
        PasswordPolicyConfigParser parser = parser();
        List<String> invalid = List.of(
            "not-json",
            VALID_RANDOM.replace("\"version\": 1", "\"version\": 2"),
            VALID_RANDOM.replace("\"minimumLength\": 8", "\"minimumLength\": 7"),
            VALID_RANDOM.replace("\"length\": 12", "\"length\": 31"),
            VALID_RANDOM.replace("\"uppercaseCharacters\": \"ABCDEFGHJKLMNPQRSTUVWXYZ\"",
                "\"uppercaseCharacters\": \"\""),
            VALID_RANDOM.replace("\"specialCharacters\": \"@$!%*?&\"",
                "\"specialCharacters\": \"#\""),
            VALID_RANDOM + " ".repeat(501)
        );

        for (String value : invalid) {
            ServiceException exception = assertThrows(ServiceException.class, () -> parser.parse(value));
            assertEquals("PASSWORD_POLICY_UNAVAILABLE", exception.getMessage());
            assertFalse(exception.toString().contains("ABCDEFGHJKLMNPQRSTUVWXYZ"));
            assertFalse(exception.toString().contains("fixedValue"));
        }
    }

    @Test
    void shouldSupportCompliantFixedDefaultButKeepTemporaryPasswordsRandom() {
        String fixedJson = VALID_RANDOM.replaceAll("\\s+", "").replace("{\"mode\":\"RANDOM\"}",
            "{ \"mode\": \"FIXED\", \"fixedValue\": \"Abcd1234!\" }");
        PasswordPolicyService service = service(fixedJson);

        assertEquals("Abcd1234!", service.generateDefaultPassword());
        String temporary = service.generateTemporaryPassword();
        assertNotEquals("Abcd1234!", temporary);
        assertTrue(service.validate(temporary).isEmpty());

        String weakFixed = fixedJson.replace("Abcd1234!", "123456");
        assertThrows(ServiceException.class, () -> parser().parse(weakFixed));
    }

    private static PasswordPolicyService service(String json) {
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(PasswordPolicy.CONFIG_KEY)).thenReturn(json);
        return new PasswordPolicyService(configService, parser());
    }

    private static PasswordPolicyConfigParser parser() {
        return new PasswordPolicyConfigParser(JsonMapper.builder().build());
    }
}
