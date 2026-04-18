package com.youssef.ecomeraauthservice.config;

import com.youssef.ecomeraauthservice.security.JwtAuthEntryPoint;
import com.youssef.ecomeraauthservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.youssef.ecomeraauthservice.user.Permission.*;
import static com.youssef.ecomeraauthservice.user.Role.ADMIN;
import static com.youssef.ecomeraauthservice.user.Role.MANAGER;
import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String ADMIN_ENDPOINT = "/api/v1/admin/**";
    private static final String MANAGEMENT_ENDPOINT = "/api/v1/management/**";


    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(ss -> ss.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(req -> req
                        .requestMatchers(WHITE_LIST).permitAll()

                        .requestMatchers(ADMIN_ENDPOINT).hasRole(ADMIN.name())
                        .requestMatchers(GET, ADMIN_ENDPOINT).hasAuthority(ADMIN_READ.name())
                        .requestMatchers(POST, ADMIN_ENDPOINT).hasAuthority(ADMIN_CREATE.name())
                        .requestMatchers(PUT, ADMIN_ENDPOINT).hasAuthority(ADMIN_UPDATE.name())
                        .requestMatchers(PATCH, ADMIN_ENDPOINT).hasAuthority(ADMIN_UPDATE.name())
                        .requestMatchers(DELETE, ADMIN_ENDPOINT).hasAuthority(ADMIN_DELETE.name())

                        .requestMatchers(MANAGEMENT_ENDPOINT).hasAnyRole(ADMIN.name(), MANAGER.name())
                        .requestMatchers(GET, MANAGEMENT_ENDPOINT).hasAnyAuthority(ADMIN_READ.name(), MANAGER_READ.name())
                        .requestMatchers(POST, MANAGEMENT_ENDPOINT).hasAnyAuthority(ADMIN_CREATE.name(), MANAGER_CREATE.name())
                        .requestMatchers(PUT, MANAGEMENT_ENDPOINT).hasAnyAuthority(ADMIN_UPDATE.name(), MANAGER_UPDATE.name())
                        .requestMatchers(PATCH, MANAGEMENT_ENDPOINT).hasAnyAuthority(ADMIN_UPDATE.name(), MANAGER_UPDATE.name())
                        .requestMatchers(DELETE, MANAGEMENT_ENDPOINT).hasAnyAuthority(ADMIN_DELETE.name(), MANAGER_DELETE.name())

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthEntryPoint))

                .build();
    }
}
