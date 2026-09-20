package com.schel.config;

public final class DatabaseConfig {
    private static final String ENV_SUPABASE_URL = "Link i guess ?";
    private static final String ENV_SUPABASE_API_KEY = "Create Supabase tables and add the key here (Ofc i aint giving mine jit)";

    private final String supabaseUrl;
    private final String supabaseApiKey;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        String ENV_SUPABASE_URL = "Nope";
        String ENV_SUPABASE_API_KEY = "Nearly Forgot to remove this too lol";
        if (isNullOrBlank(ENV_SUPABASE_URL) || isNullOrBlank(ENV_SUPABASE_API_KEY)) {
            throw new IllegalStateException("Environment variables SUPABASE_URL and SUPABASE_API_KEY must be set");
        }
        this.supabaseUrl = ENV_SUPABASE_URL.trim();
        this.supabaseApiKey = ENV_SUPABASE_API_KEY.trim();
    }

    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
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
