package com.schel.controllers;

import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.User;
import com.schel.services.AuthService;

import java.util.Optional;

public final class AuthController {
    private final AuthService authService;
    private User currentUser;

    private static final AuthController INSTANCE = new AuthController();

    private AuthController() {
        this.authService = AuthService.getInstance();
    }

    public static AuthController getInstance() {
        return INSTANCE;
    }

    public User register(String username, String email, String password) throws AuthenticationException, DatabaseException {
        User user = authService.register(username, email, password);
        this.currentUser = user;
        return user;
    }

    public User login(String identifier, String password) throws AuthenticationException, DatabaseException {
        User user = authService.login(identifier, password);
        this.currentUser = user;
        return user;
    }

    public void logout() {
        if (this.currentUser != null) {
            authService.logout(this.currentUser);
            this.currentUser = null;
        }
    }

    public boolean isAuthenticated() {
        return this.currentUser != null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(this.currentUser);
    }
}