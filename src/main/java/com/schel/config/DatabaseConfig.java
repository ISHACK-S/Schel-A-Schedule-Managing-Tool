package com.schel.config;

public final class DatabaseConfig {

    private static final String ENV_SUPABASE_URL = "SUPABASE_URL";
    private static final String ENV_SUPABASE_API_KEY = "SUPABASE_API_KEY";
    private static final String DEFAULT_SUPABASE_URL = "https://xnzvpuifxxaagchbyxnk.supabase.co";
    private static final String DEFAULT_SUPABASE_API_KEY = "sb_secret_iE2Pwx_EA7AopBpnSsyzUg_KeOxhaYM";
    private static final String PLACEHOLDER_SUPABASE_URL = "YOUR_SUPABASE_URL_HERE";
    private static final String PLACEHOLDER_SUPABASE_API_KEY = "YOUR_SUPABASE_SECRET_KEY_HERE";

    private final String supabaseUrl;
    private final String supabaseApiKey;
    private final boolean configured;

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        String envUrl = readEnv(ENV_SUPABASE_URL);
        String envApiKey = readEnv(ENV_SUPABASE_API_KEY);

        this.supabaseUrl = selectConfiguredValue(envUrl, DEFAULT_SUPABASE_URL);
        this.supabaseApiKey = selectConfiguredValue(envApiKey, DEFAULT_SUPABASE_API_KEY);
        this.configured = !isNullOrBlank(supabaseUrl)
                && !isNullOrBlank(supabaseApiKey)
                && !isPlaceholder(supabaseUrl)
                && !isPlaceholder(supabaseApiKey);
    }

    private static String readEnv(String key) {
        String value = System.getenv(key);
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
        return trimmed.equalsIgnoreCase(PLACEHOLDER_SUPABASE_URL)
                || trimmed.equalsIgnoreCase(PLACEHOLDER_SUPABASE_API_KEY);
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
        return configured
                ? "Bearer " + supabaseApiKey
                : "";
    }
}