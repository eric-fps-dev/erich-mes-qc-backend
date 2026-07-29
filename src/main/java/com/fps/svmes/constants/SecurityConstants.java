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
            "/auth/refresh",
            "/ws-qc/**"
    };

    // Public WHITELIST - anonymous, unauthenticated form fill-in links.
    // Keep this scoped to PublicQcFormController only; do not widen it to
    // cover any other controller's paths.
    public static final String[] PUBLIC_WHITELIST = {
            "/public/qc-forms/**"
    };
}

