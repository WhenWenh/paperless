package at.fhtw.swen.paperless.api.security;

import java.util.UUID;

public interface CurrentUser {

    UUID id();
    String username();
    boolean hasRole(String role);
}
