package org.namewta.common.openapi.spi;

import org.namewta.system.api.model.LoginUser;

/**
 * Builds the current read-only global authorization snapshot for one user.
 */
@FunctionalInterface
public interface OpenApiAuthorizationResolver {

    LoginUser resolve(Long userId);

}
