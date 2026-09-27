package com.schel.repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;

public final class CategoryRepository {

    private static final String TABLE = "categories";

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final CategoryRepository INSTANCE = new CategoryRepository();

    private CategoryRepository() {
        this.client = SupabaseClient.getInstance();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        SimpleModule categoryTimestampModule = new SimpleModule();
        categoryTimestampModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
            @Override
            public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                if (value == null) {
                    return (OffsetDateTime) context.handleUnexpectedToken(OffsetDateTime.class, parser);
                }
                try {
                    return OffsetDateTime.parse(value);
                } catch (java.time.format.DateTimeParseException noOffset) {
                    try {
                        return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
                    } catch (java.time.format.DateTimeParseException invalidTimestamp) {
                        throw JsonMappingException.from(parser, "Invalid category created_at timestamp", invalidTimestamp);
                    }
                }
            }
        });
        this.mapper.registerModule(categoryTimestampModule);
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
            logCategoryParseFailure(response, e);
            throw new DatabaseException("Failed to parse categories response", e);
        }
    }

    private void logCategoryParseFailure(String response, JsonProcessingException exception) {
        System.err.println("Category response parse failure: " + exception.getClass().getSimpleName());
        if (exception instanceof com.fasterxml.jackson.databind.JsonMappingException mappingException) {
            System.err.println("Category response field path: " + mappingException.getPathReference());
        }

        try {
            JsonNode root = mapper.readTree(response);
            System.err.println("Category response root type: " + (root == null ? "null" : root.getNodeType()));
            if (root != null && root.isArray()) {
                System.err.println("Category response row count: " + root.size());
                if (root.size() > 0 && root.get(0).isObject()) {
                    root.get(0).fields().forEachRemaining(entry -> System.err.println(
                            "Category response field type " + entry.getKey() + ": " + entry.getValue().getNodeType()));
                }
            }
        } catch (JsonProcessingException diagnosticException) {
            System.err.println("Category response structure could not be inspected: "
                    + diagnosticException.getClass().getSimpleName());
        }
    }
}
