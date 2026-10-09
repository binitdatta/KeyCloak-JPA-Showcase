package com.rollingstone.idpsync.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps Keycloak's realm_access.roles claim onto Spring Security
 * GrantedAuthorities. A Keycloak realm role "idp-sync-service" becomes the
 * authority "ROLE_IDP_SYNC_SERVICE", which SecurityConfig requires for every
 * endpoint in this API.
 *
 * Setup in Keycloak (see README.md for full steps):
 *   1. Realm roles -> create "idp-sync-service"
 *   2. Clients -> create a confidential client (e.g. "entra-idp-sync-client")
 *      with Service Accounts enabled
 *   3. That client's Service account roles -> assign "idp-sync-service"
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT).replace('-', '_'))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
