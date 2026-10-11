package at.fhtw.swen.paperless.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();


    @Test
    void convert_shouldMapRealmRolesToAuthorities() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("123e4567-e89b-12d3-a456-426614174000")
                .claim(
                        "preferred_username",
                        "john"
                )
                .claim(
                        "realm_access",
                        Map.of(
                                "roles",
                                List.of(
                                        "USER",
                                        "ADMIN"
                                )
                        )
                )
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300)
                )
                .build();


        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken)
                        converter.convert(jwt);


        assertThat(authentication.getName())
                .isEqualTo("john");

        assertThat(authentication.getAuthorities())
                .containsExactlyInAnyOrder(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                );
    }


    @Test
    void convert_shouldReturnEmptyAuthoritiesWithoutRealmAccess() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("123")
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300)
                )
                .build();


        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken)
                        converter.convert(jwt);


        assertThat(authentication.getAuthorities())
                .isEmpty();
    }


    @Test
    void convert_shouldIgnoreNonStringRoles() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("123")
                .claim(
                        "realm_access",
                        Map.of(
                                "roles",
                                List.of(
                                        "USER",
                                        123,
                                        true
                                )
                        )
                )
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300)
                )
                .build();


        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken)
                        converter.convert(jwt);


        assertThat(authentication.getAuthorities())
                .containsExactly(
                        new SimpleGrantedAuthority("ROLE_USER")
                );
    }
}
