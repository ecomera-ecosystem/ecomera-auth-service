package com.ecomera.auth.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Schema(description = "User role with hierarchical permissions for role-based access control")
public enum Role {

    @Schema(description = "Standard user with basic access to user endpoints")
    USER(Set.of()),

    @Schema(description = "Manager with full access to management endpoints (create, read, update, delete)")
    MANAGER(Set.of(
            Permission.MANAGER_CREATE,
            Permission.MANAGER_READ,
            Permission.MANAGER_UPDATE,
            Permission.MANAGER_DELETE
    )),

    @Schema(description = "Administrator with full system access (all admin and manager permissions)")
    ADMIN(Set.of(
            Permission.ADMIN_CREATE,
            Permission.ADMIN_READ,
            Permission.ADMIN_UPDATE,
            Permission.ADMIN_DELETE,
            Permission.MANAGER_CREATE,
            Permission.MANAGER_READ,
            Permission.MANAGER_UPDATE,
            Permission.MANAGER_DELETE
    ));

    @JsonIgnore  // Internal implementation detail, not exposed in API
    private final Set<Permission> permissions;

    @JsonIgnore  // Internal method, not serialized
    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(p -> new SimpleGrantedAuthority(p.toString()))
                .collect(Collectors.toList());


        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
