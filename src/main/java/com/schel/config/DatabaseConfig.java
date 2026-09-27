package com.schel.config;

public final class DatabaseConfig {
    private static final String ENV_SUPABASE_URL = "SUPABASE_URL";
    private static final String ENV_SUPABASE_API_KEY = "SUPABASE_API_KEY";

    private final String supabaseUrl;
    private final String supabaseApiKey;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        String supabaseUrl = System.getenv(ENV_SUPABASE_URL);
        String supabaseApiKey = System.getenv(ENV_SUPABASE_API_KEY);
        if (isNullOrBlank(supabaseUrl) || isNullOrBlank(supabaseApiKey)) {
            throw new IllegalStateException("Environment variables SUPABASE_URL and SUPABASE_API_KEY must be set");
        }
        this.supabaseUrl = supabaseUrl.trim();
        this.supabaseApiKey = supabaseApiKey.trim();
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseApiKey() {
        return supabaseApiKey;
    }

    public String getAuthorizationHeaderValue() {
        return "Bearer " + supabaseApiKey;
    }
}
