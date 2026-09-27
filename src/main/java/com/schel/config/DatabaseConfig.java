package com.schel.config;

public final class DatabaseConfig {
    private static final String ENV_SUPABASE_URL = "SUPABASE_URL";
    private static final String ENV_SUPABASE_API_KEY = "SUPABASE_API_KEY";

    private final String supabaseUrl;
    private final String supabaseApiKey;
    private final boolean configured;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        this.supabaseUrl = readEnv(ENV_SUPABASE_URL);
        this.supabaseApiKey = readEnv(ENV_SUPABASE_API_KEY);
        this.configured = !isNullOrBlank(supabaseUrl) && !isNullOrBlank(supabaseApiKey);
    }

    private static String readEnv(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseApiKey() {
        return supabaseApiKey;
    }

    public String getAuthorizationHeaderValue() {
        return configured ? "Bearer " + supabaseApiKey : "";
    }
}
