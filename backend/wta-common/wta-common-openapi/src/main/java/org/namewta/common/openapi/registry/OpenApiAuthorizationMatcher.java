package org.namewta.common.openapi.registry;

import cn.dev33.satoken.strategy.SaStrategy;
import org.namewta.system.api.model.LoginUser;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;

/**
 * Shared matcher for catalog preview and invocation authorization.
 */
public final class OpenApiAuthorizationMatcher {

    public boolean matches(LoginUser user, OpenApiAccessRule rule) {
        if (user == null) {
            return false;
        }
        Collection<String> permissions = user.getMenuPermission();
        Collection<String> roles = user.getRolePermission();
        return rule.permissions().stream().allMatch(item -> permissionMatches(permissions, roles, item))
            && rule.roles().stream().allMatch(item -> valuesMatch(roles, item.values(), item.mode()));
    }

    private static boolean permissionMatches(Collection<String> permissions, Collection<String> roles,
                                               OpenApiAccessRule.PermissionRule rule) {
        boolean permissionsMatch = rule.values().isEmpty()
            ? rule.orRoles().isEmpty()
            : valuesMatch(permissions, rule.values(), rule.mode());
        return permissionsMatch || orRolesMatch(roles, rule.orRoles());
    }

    private static boolean orRolesMatch(Collection<String> roles, List<String> alternatives) {
        return alternatives.stream().anyMatch(expression -> Arrays.stream(expression.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .allMatch(value -> hasElement(roles, value)));
    }

    private static boolean valuesMatch(Collection<String> actual, List<String> required, OpenApiAccessRule.Mode mode) {
        if (required.isEmpty()) {
            return true;
        }
        return mode == OpenApiAccessRule.Mode.AND
            ? required.stream().allMatch(value -> hasElement(actual, value))
            : required.stream().anyMatch(value -> hasElement(actual, value));
    }

    private static boolean hasElement(Collection<String> actual, String expected) {
        return SaStrategy.instance.hasElement.apply(actual == null ? List.of() : List.copyOf(actual), expected);
    }

}
