package org.namewta.system.openapi.credential.service;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.namewta.common.core.constant.HttpStatus;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.EncryptedSecret;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.GeneratedCredential;
import org.namewta.system.openapi.credential.domain.SysOpenApiCredential;
import org.namewta.system.openapi.credential.mapper.SysOpenApiCredentialMapper;
import org.namewta.system.openapi.credential.model.OpenApiCredentialCreateRequest;
import org.namewta.system.openapi.credential.model.OpenApiCredentialIssued;
import org.namewta.system.openapi.credential.model.OpenApiCredentialSummary;
import org.namewta.system.openapi.credential.model.OpenApiCredentialUserRow;
import org.namewta.system.openapi.credential.model.OpenApiCredentialUserSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Transactional lifecycle for the single active credential owned by a user.
 */
@Service
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
public class SystemOpenApiCredentialService {

    private static final String ENABLED = "0";
    private static final String DISABLED = "1";

    private final SysOpenApiCredentialMapper mapper;
    private final OpenApiCredentialCrypto crypto;
    private final OpenApiMachineSessionInvalidator sessionInvalidator;
    private final IdentifierGenerator identifierGenerator;

    public SystemOpenApiCredentialService(SysOpenApiCredentialMapper mapper,
                                          OpenApiCredentialCrypto crypto,
                                          OpenApiMachineSessionInvalidator sessionInvalidator,
                                          IdentifierGenerator identifierGenerator) {
        this.mapper = mapper;
        this.crypto = crypto;
        this.sessionInvalidator = sessionInvalidator;
        this.identifierGenerator = identifierGenerator;
    }

    public OpenApiCredentialSummary get(Long ownerUserId) {
        return summary(mapper.selectByOwnerUserId(requiredOwner(ownerUserId)));
    }

    public List<OpenApiCredentialUserSummary> users(String keyword, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        return mapper.selectUserSummaries(trimToNull(keyword), limit).stream()
            .map(SystemOpenApiCredentialService::userSummary)
            .toList();
    }

    @DSTransactional
    public OpenApiCredentialIssued create(Long ownerUserId, OpenApiCredentialCreateRequest request) {
        Long owner = requiredOwner(ownerUserId);
        if (!mapper.existsActiveOwner(owner)) {
            throw new ServiceException("OpenAPI credential is unavailable", HttpStatus.NOT_FOUND);
        }
        if (mapper.selectByOwnerUserId(owner) != null) {
            throw conflict();
        }
        GeneratedCredential generated = crypto.generate(owner);
        SysOpenApiCredential credential = new SysOpenApiCredential();
        credential.setOpenApiCredentialId(identifierGenerator.nextId(null).longValue());
        credential.setOwnerUserId(owner);
        credential.setAppKey(generated.appKey());
        credential.setAppName(request.appName().trim());
        applyEncrypted(credential, generated.encrypted());
        credential.setStatus(ENABLED);
        credential.setExpiresAt(request.expiresAt());
        credential.setRemark(trimToNull(request.remark()));
        credential.setVersion(0);
        credential.setDelFlag("0");
        try {
            if (mapper.insert(credential) != 1) {
                throw conflict();
            }
        } catch (DuplicateKeyException e) {
            throw conflict();
        }
        return issued(credential, generated.appSecret());
    }

    @DSTransactional
    public OpenApiCredentialIssued reset(Long ownerUserId) {
        SysOpenApiCredential credential = requiredCredential(ownerUserId);
        String secret = crypto.generateSecret();
        applyEncrypted(credential, crypto.encrypt(credential.getOwnerUserId(), credential.getAppKey(), secret));
        updateAndInvalidate(credential);
        return issued(credential, secret);
    }

    @DSTransactional
    public OpenApiCredentialSummary enable(Long ownerUserId) {
        return changeStatus(ownerUserId, ENABLED);
    }

    @DSTransactional
    public OpenApiCredentialSummary disable(Long ownerUserId) {
        return changeStatus(ownerUserId, DISABLED);
    }

    @DSTransactional
    public void delete(Long ownerUserId) {
        SysOpenApiCredential credential = requiredCredential(ownerUserId);
        if (mapper.deleteById(credential) != 1) {
            throw conflict();
        }
        sessionInvalidator.invalidateByUserId(credential.getOwnerUserId());
    }

    private OpenApiCredentialSummary changeStatus(Long ownerUserId, String status) {
        SysOpenApiCredential credential = requiredCredential(ownerUserId);
        credential.setStatus(status);
        updateAndInvalidate(credential);
        return summary(credential);
    }

    private void updateAndInvalidate(SysOpenApiCredential credential) {
        if (mapper.updateById(credential) != 1) {
            throw conflict();
        }
        sessionInvalidator.invalidateByUserId(credential.getOwnerUserId());
    }

    private SysOpenApiCredential requiredCredential(Long ownerUserId) {
        SysOpenApiCredential credential = mapper.selectByOwnerUserId(requiredOwner(ownerUserId));
        if (credential == null) {
            throw new ServiceException("OpenAPI credential is unavailable", HttpStatus.NOT_FOUND);
        }
        return credential;
    }

    private static Long requiredOwner(Long ownerUserId) {
        if (ownerUserId == null) {
            throw new ServiceException("OpenAPI credential is unavailable", HttpStatus.NOT_FOUND);
        }
        return ownerUserId;
    }

    private static void applyEncrypted(SysOpenApiCredential credential, EncryptedSecret encrypted) {
        credential.setSecretCiphertext(encrypted.ciphertext());
        credential.setSecretNonce(encrypted.nonce());
        credential.setSecretTag(encrypted.tag());
        credential.setKekVersion(encrypted.kekVersion());
    }

    private static OpenApiCredentialSummary summary(SysOpenApiCredential credential) {
        if (credential == null) {
            return null;
        }
        return new OpenApiCredentialSummary(credential.getOpenApiCredentialId(), credential.getOwnerUserId(),
            credential.getAppKey(), credential.getAppName(), credential.getStatus(), credential.getExpiresAt(),
            credential.getRemark(), credential.getCreateTime(), credential.getUpdateTime());
    }

    private static OpenApiCredentialIssued issued(SysOpenApiCredential credential, String secret) {
        return new OpenApiCredentialIssued(credential.getOpenApiCredentialId(), credential.getOwnerUserId(),
            credential.getAppKey(), secret, credential.getAppName(), credential.getStatus(),
            credential.getExpiresAt(), credential.getRemark(), credential.getCreateTime(), credential.getUpdateTime());
    }

    private static OpenApiCredentialUserSummary userSummary(OpenApiCredentialUserRow row) {
        OpenApiCredentialSummary credential = row.getCredentialId() == null ? null
            : new OpenApiCredentialSummary(row.getCredentialId(), row.getUserId(), row.getAppKey(), row.getAppName(),
                row.getStatus(), row.getExpiresAt(), row.getRemark(), row.getCreateTime(), row.getUpdateTime());
        return new OpenApiCredentialUserSummary(row.getUserId(), row.getUserName(), row.getNickName(), credential);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static ServiceException conflict() {
        return new ServiceException("OpenAPI credential already changed", HttpStatus.CONFLICT);
    }
}
