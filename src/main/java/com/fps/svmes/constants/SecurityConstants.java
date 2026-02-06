package com.fps.svmes.constants;

public class SecurityConstants {

    // JWT token defaults
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";

    // Swagger WHITELIST
    public static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };
    // System WHITELIST
    public static final String[] SYSTEM_WHITELIST = {
            "/actuator/health",
            "/auth/callback",
            "/auth/refresh"
    };
}

