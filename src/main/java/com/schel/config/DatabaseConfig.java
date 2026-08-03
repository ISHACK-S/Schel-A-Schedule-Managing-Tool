package com.schel.config;

public final class DatabaseConfig {
    private static final String ENV_SUPABASE_URL = "https://xnzvpuifxxaagchbyxnk.supabase.co";
    private static final String ENV_SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhuenZwdWlmeHhhYWdjaGJ5eG5rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ4MDU0NTQsImV4cCI6MjEwMDM4MTQ1NH0.NdFdVkjWREB_yx0orVhVIf75gqBSyi3Qw2dokVCfbu0";

    private final String supabaseUrl;
    private final String supabaseApiKey;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        String ENV_SUPABASE_URL = "https://xnzvpuifxxaagchbyxnk.supabase.co";
        String ENV_SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhuenZwdWlmeHhhYWdjaGJ5eG5rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ4MDU0NTQsImV4cCI6MjEwMDM4MTQ1NH0.NdFdVkjWREB_yx0orVhVIf75gqBSyi3Qw2dokVCfbu0";
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