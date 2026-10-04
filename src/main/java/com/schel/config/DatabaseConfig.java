package com.schel.config;

import io.github.cdimascio.dotenv.Dotenv;

public final class DatabaseConfig {

    private static final String ENV_SUPABASE_URL = "SUPABASE_URL";
    private static final String ENV_SUPABASE_API_KEY = "SUPABASE_API_KEY";
    private static final Dotenv DOTENV = Dotenv.configure()
            .directory(System.getProperty("user.dir"))
            .ignoreIfMissing()
            .load();

    private final String supabaseUrl;
    private final String supabaseApiKey;
    private final boolean configured;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        String envUrl = readEnv(ENV_SUPABASE_URL);
        String envApiKey = readEnv(ENV_SUPABASE_API_KEY);

        this.supabaseUrl = selectConfiguredValue(envUrl, readDotenv(ENV_SUPABASE_URL));
        this.supabaseApiKey = selectConfiguredValue(envApiKey, readDotenv(ENV_SUPABASE_API_KEY));
        this.configured = !isNullOrBlank(supabaseUrl)
                && !isNullOrBlank(supabaseApiKey)
                && !isPlaceholder(supabaseUrl)
                && !isPlaceholder(supabaseApiKey);
        if (configured) {
            System.out.println("Supabase configuration loaded successfully.");
        }
    }

    private static String readEnv(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static String readDotenv(String key) {
        String value = DOTENV.get(key);
        return value == null ? "" : value.trim();
    }

    private static String selectConfiguredValue(String envValue, String fallbackValue) {
        if (!isNullOrBlank(envValue)) {
            return envValue;
        }
        return fallbackValue == null ? "" : fallbackValue.trim();
    }

    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isPlaceholder(String value) {
        if (isNullOrBlank(value)) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.equalsIgnoreCase("your_supabase_project_url")
                || trimmed.equalsIgnoreCase("your_supabase_api_key")
                || trimmed.equalsIgnoreCase("YOUR_SUPABASE_URL_HERE")
                || trimmed.equalsIgnoreCase("YOUR_SUPABASE_SECRET_KEY_HERE");
    }

    public static DatabaseConfig getInstance() {
        if (!INSTANCE.configured) {
            throw new IllegalStateException(
                    "Missing Supabase configuration. Set SUPABASE_URL and SUPABASE_API_KEY "
                            + "as environment variables or in the project-root .env file.");
        }
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
        return configured
                ? "Bearer " + supabaseApiKey
                : "";
    }
}