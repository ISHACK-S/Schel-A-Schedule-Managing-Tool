package com.schel.config;

import java.time.Duration;

public final class Constants {
    public static final String APPLICATION_NAME = "Schel";
    public static final String SUPABASE_REST_PATH = "/rest/v1";
    public static final String USERS_TABLE = "users";

    public static final String HEADER_API_KEY = "apikey";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";

    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private Constants() {}
}