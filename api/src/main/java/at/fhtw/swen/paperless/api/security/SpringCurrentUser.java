package at.fhtw.swen.paperless.api.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringCurrentUser implements CurrentUser {

    @Override
    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    @Override
    public String username() {
        return jwt().getClaimAsString("preferred_username");
    }

    @Override
    public boolean hasRole(String role) {
        return authentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + role)::equals);
    }

    private Jwt jwt() {
        Object principal = authentication().getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException("Authenticated principal is not a JWT.");
        }
        return jwt;
    }

    private Authentication authentication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated user.");
        }
        return authentication;
    }
}
