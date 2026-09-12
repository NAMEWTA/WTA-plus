package org.namewta.common.openapi.session;

import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.system.api.model.LoginUser;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cache-aside bridge from a verified credential to the standard authorization context.
 */
public class OpenApiMachineSessionBridge {

    private static final String MACHINE_USER_TYPE = "openapi";

    private final OpenApiAuthorizationResolver authorizationResolver;
    private final OpenApiMachineSessionOperations sessionOperations;
    private final Duration sessionTtl;

    public OpenApiMachineSessionBridge(OpenApiAuthorizationResolver authorizationResolver,
                                       OpenApiMachineSessionOperations sessionOperations,
                                       Duration sessionTtl) {
        this.authorizationResolver = Objects.requireNonNull(authorizationResolver, "authorizationResolver");
        this.sessionOperations = Objects.requireNonNull(sessionOperations, "sessionOperations");
        this.sessionTtl = requirePositive(sessionTtl);
    }

    public <T> T execute(VerifiedOpenApiIdentity identity, Supplier<T> callback) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(callback, "callback");
        ensureSession(identity);
        return sessionOperations.inRequestScope(identity, callback);
    }

    private void ensureSession(VerifiedOpenApiIdentity identity) {
        try {
            if (sessionOperations.find(identity) != null) {
                return;
            }
            sessionOperations.withUserLock(identity.ownerUserId(), () -> {
                if (sessionOperations.find(identity) == null) {
                    LoginUser snapshot = authorizationResolver.resolve(identity.ownerUserId());
                    validateSnapshot(identity, snapshot);
                    sessionOperations.create(identity, snapshot, sessionTtl);
                }
                return null;
            });
        } catch (OpenApiMachineSessionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OpenApiMachineSessionException(exception);
        }
    }

    private static void validateSnapshot(VerifiedOpenApiIdentity identity, LoginUser snapshot) {
        if (snapshot == null
            || !identity.ownerUserId().equals(snapshot.getUserId())
            || !MACHINE_USER_TYPE.equals(snapshot.getUserType())
            || snapshot.getClientPk() != null
            || snapshot.getClientKey() != null) {
            throw new OpenApiMachineSessionException();
        }
    }

    private static Duration requirePositive(Duration ttl) {
        Objects.requireNonNull(ttl, "sessionTtl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
        return ttl;
    }

}
