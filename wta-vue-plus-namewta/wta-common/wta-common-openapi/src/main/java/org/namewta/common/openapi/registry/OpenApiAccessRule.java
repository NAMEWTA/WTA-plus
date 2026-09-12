package org.namewta.common.openapi.registry;

import java.util.List;

/**
 * Raw Sa-Token constraints attached to one mapped handler.
 */
public record OpenApiAccessRule(List<PermissionRule> permissions, List<RoleRule> roles) {

    public OpenApiAccessRule {
        permissions = List.copyOf(permissions);
        roles = List.copyOf(roles);
    }

    public record PermissionRule(List<String> values, Mode mode, List<String> orRoles) {
        public PermissionRule {
            values = List.copyOf(values);
            orRoles = List.copyOf(orRoles);
        }
    }

    public record RoleRule(List<String> values, Mode mode) {
        public RoleRule {
            values = List.copyOf(values);
        }
    }

    public enum Mode {
        AND,
        OR
    }

}
