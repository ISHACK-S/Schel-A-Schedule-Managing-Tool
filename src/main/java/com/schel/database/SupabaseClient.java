package com.schel.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schel.config.Constants;
import com.schel.config.DatabaseConfig;
import com.schel.exceptions.DatabaseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

public final class SupabaseClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final DatabaseConfig config;

    private static final SupabaseClient INSTANCE = new SupabaseClient();

    private SupabaseClient() {
        this.config = DatabaseConfig.getInstance();
        this.client = HttpClient.newBuilder()
                .connectTimeout(Constants.HTTP_TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
        // Ensure Java Time types serialize as ISO-8601 strings
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static SupabaseClient getInstance() {
        return INSTANCE;
    }

    public String get(String path) throws DatabaseException {
        return get(path, null);
    }

    public String get(String path, Map<String, String> queryParams) throws DatabaseException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path, queryParams))
                .timeout(Constants.HTTP_TIMEOUT)
                .header(Constants.HEADER_API_KEY, config.getSupabaseApiKey())
                .header(Constants.HEADER_AUTHORIZATION, config.getAuthorizationHeaderValue())
                .header(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .GET()
                .build();
        return send(request);
    }

    public String post(String path, Object body) throws DatabaseException {
        String json = toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path, null))
                .timeout(Constants.HTTP_TIMEOUT)
                .header(Constants.HEADER_API_KEY, config.getSupabaseApiKey())
                .header(Constants.HEADER_AUTHORIZATION, config.getAuthorizationHeaderValue())
                .header(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .header(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public String patch(String path, Object body) throws DatabaseException {
        String json = toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path, null))
                .timeout(Constants.HTTP_TIMEOUT)
                .header(Constants.HEADER_API_KEY, config.getSupabaseApiKey())
                .header(Constants.HEADER_AUTHORIZATION, config.getAuthorizationHeaderValue())
                .header(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .header(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public String delete(String path) throws DatabaseException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path, null))
                .timeout(Constants.HTTP_TIMEOUT)
                .header(Constants.HEADER_API_KEY, config.getSupabaseApiKey())
                .header(Constants.HEADER_AUTHORIZATION, config.getAuthorizationHeaderValue())
                .header(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .DELETE()
                .build();
        return send(request);
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        String base = config.getSupabaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String rest = Constants.SUPABASE_REST_PATH;
        String p = path == null ? "" : path;
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(base).append(rest).append(p);
        if (queryParams != null && !queryParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                String encodedKey = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
                String encodedValue = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8);
                joiner.add(encodedKey + "=" + encodedValue);
            }
            sb.append("?").append(joiner.toString());
        }
        return URI.create(sb.toString());
    }

    private String toJson(Object obj) throws DatabaseException {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to serialize request body", e);
        }
    }

    private String send(HttpRequest request) throws DatabaseException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();
            if (status >= 400) {
                throw new DatabaseException("Supabase request failed with status " + status + ": " + body);
            }
            return body == null ? "" : body;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Supabase request failed", e);
        }
    }
}