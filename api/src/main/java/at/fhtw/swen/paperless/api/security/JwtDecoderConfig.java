package at.fhtw.swen.paperless.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(
                        "http://keycloak:8080/realms/paperless/protocol/openid-connect/certs"
                ).build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "http://localhost:9445/realms/paperless"
                )
        );

        return decoder;
    }
}
