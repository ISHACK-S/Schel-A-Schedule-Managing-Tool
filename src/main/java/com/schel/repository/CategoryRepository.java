package com.schel.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CategoryRepository {

    private static final String TABLE = "categories";

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final CategoryRepository INSTANCE = new CategoryRepository();

    private CategoryRepository() {
        this.client = SupabaseClient.getInstance();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static CategoryRepository getInstance() {
        return INSTANCE;
    }

    public void create(Category category) throws DatabaseException {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null.");
        }
        client.post(TABLE, category);
    }

    public Category findById(UUID userId, UUID id) throws DatabaseException {
        if (userId == null || id == null) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("id", "eq." + id);
        params.put("user_id", "eq." + userId);
        Category[] categories = getCategories(params);
        return categories.length == 0 ? null : categories[0];
    }

    public Category[] findByUserId(UUID userId) throws DatabaseException {
        if (userId == null) {
            return new Category[0];
        }
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        params.put("order", "name.asc");
        return getCategories(params);
    }

    public boolean update(UUID userId, UUID id, Category category) throws DatabaseException {
        if (userId == null || id == null || category == null) {
            throw new IllegalArgumentException("User, category ID, and category are required.");
        }
        String path = TABLE + "?id=eq." + id + "&user_id=eq." + userId;
        Map<String, String> fields = new HashMap<>();
        fields.put("name", category.getName());
        fields.put("color", category.getColor());
        return parseCategories(client.patchReturning(path, fields)).length > 0;
    }

    public boolean delete(UUID userId, UUID id) throws DatabaseException {
        if (userId == null || id == null) {
            return false;
        }
        String path = TABLE + "?id=eq." + id + "&user_id=eq." + userId;
        return parseCategories(client.deleteReturning(path)).length > 0;
    }

    private Category[] getCategories(Map<String, String> params) throws DatabaseException {
        return parseCategories(client.get(TABLE, params));
    }

    private Category[] parseCategories(String response) throws DatabaseException {
        try {
            return mapper.readValue(response, Category[].class);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to parse categories response", e);
        }
    }
}
