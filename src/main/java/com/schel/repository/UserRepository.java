package com.schel.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.config.Constants;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.User;

import java.util.HashMap;
import java.util.Map;

public final class UserRepository {

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final UserRepository INSTANCE = new UserRepository();

    private UserRepository() {
        this.client = SupabaseClient.getInstance();

        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static UserRepository getInstance() {
        return INSTANCE;
    }

    public User findByEmail(String email) throws DatabaseException {
        if (email == null || email.isBlank()) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        params.put("email", "eq." + email);

        String resp = client.get(Constants.USERS_TABLE, params);

        try {
            User[] users = mapper.readValue(resp, User[].class);
            return users.length == 0 ? null : users[0];
        } catch (JsonProcessingException e) {
            System.out.println("Supabase Response:");
            System.out.println(resp);
            System.out.println("Jackson exception while parsing user response:");
            e.printStackTrace(System.out);
            throw new DatabaseException("Failed to parse user response: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) throws DatabaseException {
        if (username == null || username.isBlank()) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        params.put("username", "eq." + username);

        String resp = client.get(Constants.USERS_TABLE, params);

        try {
            User[] users = mapper.readValue(resp, User[].class);
            return users.length == 0 ? null : users[0];
        } catch (JsonProcessingException e) {
            System.out.println("Supabase Response:");
            System.out.println(resp);
            System.out.println("Jackson exception while parsing user response:");
            e.printStackTrace(System.out);
            throw new DatabaseException("Failed to parse user response: " + e.getMessage(), e);
        }
    }

    public User create(User user) throws DatabaseException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        client.post(Constants.USERS_TABLE, user);

        return findByEmail(user.getEmail());
    }
}