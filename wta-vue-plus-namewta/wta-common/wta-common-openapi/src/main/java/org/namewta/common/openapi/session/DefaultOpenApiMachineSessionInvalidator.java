package org.namewta.common.openapi.session;

import java.util.Objects;

/**
 * Serializes invalidation with machine-session creation for the same user.
 */
public class DefaultOpenApiMachineSessionInvalidator implements OpenApiMachineSessionInvalidator {

    private final OpenApiMachineSessionOperations sessionOperations;

    public DefaultOpenApiMachineSessionInvalidator(OpenApiMachineSessionOperations sessionOperations) {
        this.sessionOperations = Objects.requireNonNull(sessionOperations, "sessionOperations");
    }

    @Override
    public int invalidateByUserId(Long userId) {
        if (userId == null) {
            throw new OpenApiMachineSessionException();
        }
        try {
            return sessionOperations.withUserLock(userId, () -> sessionOperations.invalidateByUserId(userId));
        } catch (OpenApiMachineSessionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OpenApiMachineSessionException(exception);
        }
    }

}
