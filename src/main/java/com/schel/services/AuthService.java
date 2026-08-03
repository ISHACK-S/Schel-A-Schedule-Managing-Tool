package com.schel.services;

import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.User;
import com.schel.repository.UserRepository;
import com.schel.utils.PasswordHasher;

import java.util.Objects;

public final class AuthService {
    private final UserRepository repository;

    private static final AuthService INSTANCE = new AuthService();

    private AuthService() {
        this.repository = UserRepository.getInstance();
    }

    public static AuthService getInstance() {
        return INSTANCE;
    }

    public User register(String username, String email, String password) throws AuthenticationException, DatabaseException {
        validateRegistrationInput(username, email, password);
        if (repository.findByEmail(email) != null) {
            throw new AuthenticationException("Email already in use");
        }
        if (repository.findByUsername(username) != null) {
            throw new AuthenticationException("Username already in use");
        }
        String hash = PasswordHasher.hash(password);
        User user = new User(null, username, email, hash, null);
        return repository.create(user);
    }

    public User login(String identifier, String password) throws AuthenticationException, DatabaseException {
        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Invalid credentials");
        }
        User user = identifier.contains("@") ? repository.findByEmail(identifier) : repository.findByUsername(identifier);
        if (user == null) {
            throw new AuthenticationException("Invalid credentials");
        }
        boolean ok = PasswordHasher.verify(password, user.getPasswordHash());
        if (!ok) {
            throw new AuthenticationException("Invalid credentials");
        }
        return user;
    }

    public void logout(User user) {
        Objects.requireNonNull(user);
    }

    private void validateRegistrationInput(String username, String email, String password) throws AuthenticationException {
        if (username == null || username.isBlank()) {
            throw new AuthenticationException("Username is required");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new AuthenticationException("Valid email is required");
        }
        if (password == null || password.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters");
        }
    }
}