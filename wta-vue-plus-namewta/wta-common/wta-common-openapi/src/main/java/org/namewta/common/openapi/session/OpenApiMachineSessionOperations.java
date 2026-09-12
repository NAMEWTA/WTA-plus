package org.namewta.common.openapi.session;

import org.namewta.system.api.model.LoginUser;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Internal session operations that deliberately never expose a machine token.
 */
public interface OpenApiMachineSessionOperations {

    LoginUser find(VerifiedOpenApiIdentity identity);

    void create(VerifiedOpenApiIdentity identity, LoginUser loginUser, Duration ttl);

    <T> T inRequestScope(VerifiedOpenApiIdentity identity, Supplier<T> callback);

    <T> T withUserLock(Long userId, Supplier<T> callback);

    int invalidateByUserId(Long userId);

}
