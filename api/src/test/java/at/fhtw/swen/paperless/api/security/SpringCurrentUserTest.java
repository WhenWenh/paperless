package at.fhtw.swen.paperless.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringCurrentUserTest {

    private final SpringCurrentUser currentUser =
            new SpringCurrentUser();


    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void id_shouldReturnSubjectAsUuid() {

        UUID id = UUID.randomUUID();

        authenticate(
                id.toString(),
                "john",
                List.of("USER")
        );


        assertThat(currentUser.id())
                .isEqualTo(id);
    }


    @Test
    void username_shouldReturnPreferredUsername() {

        authenticate(
                UUID.randomUUID().toString(),
                "john",
                List.of()
        );


        assertThat(currentUser.username())
                .isEqualTo("john");
    }


    @Test
    void hasRole_shouldReturnTrueForExistingRole() {

        authenticate(
                UUID.randomUUID().toString(),
                "john",
                List.of("USER")
        );


        assertThat(
                currentUser.hasRole("USER")
        ).isTrue();
    }


    @Test
    void hasRole_shouldReturnFalseForMissingRole() {

        authenticate(
                UUID.randomUUID().toString(),
                "john",
                List.of("USER")
        );


        assertThat(
                currentUser.hasRole("ADMIN")
        ).isFalse();
    }


    @Test
    void id_shouldFailWithoutAuthentication() {

        assertThatThrownBy(
                () -> currentUser.id()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "No authenticated user."
                );
    }


    private void authenticate(
            String subject,
            String username,
            List<String> roles
    ) {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim(
                        "preferred_username",
                        username
                )
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300)
                )
                .build();


        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt,
                        roles.stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                                .toList()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}
