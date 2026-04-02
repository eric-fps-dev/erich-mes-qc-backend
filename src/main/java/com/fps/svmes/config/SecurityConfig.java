package com.fps.svmes.config;

import com.fps.shared.utils.CookieBearerTokenResolver;
import com.fps.svmes.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CookieBearerTokenResolver bearerTokenResolver) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->  auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
/*                                .anyRequest().permitAll()*/
                                .requestMatchers(SecurityConstants.SWAGGER_WHITELIST).permitAll()
                                .requestMatchers(SecurityConstants.SYSTEM_WHITELIST).permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                        .bearerTokenResolver(bearerTokenResolver)
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // config.setAllowedOrigins(List.of(corsProperties.getAllowedOrigins().toArray(new String[0])));
        config.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        config.addAllowedMethod(CorsConfiguration.ALL);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(Customizer.withDefaults())        // Enable CORS
//                .csrf(csrf -> csrf.disable())           // Disable CSRF using the new lambda style
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()           // Allow full access to all endpoints
//                );
//        return http.build();
//    }

//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//
//        // Use allowedOriginPatterns instead of allowedOrigins
//        config.setAllowedOriginPatterns(Arrays.asList("*"));   // Allow all origins with patterns
//        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allow common methods
//        config.setAllowedHeaders(Arrays.asList("*"));          // Allow all headers
//        config.setAllowCredentials(true);                      // Allow credentials (cookies, authorization headers)
//
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }
}
