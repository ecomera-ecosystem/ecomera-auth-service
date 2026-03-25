package com.youssef.ecomeraauthservice.security;

import com.youssef.ecomeraauthservice.token.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Skip authentication for auth endpoints
        if (request.getServletPath().contains("api/v1/auth")) {
            filterChain.doFilter(request, response);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Extract the JWT from the header
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);

        // If email is present but not authenticated, validate the token and set auth in security context
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            boolean isTokenActive = tokenRepository.findByValue(jwt)
                    .map(t -> !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            // Update context with valid auth if token valid and active
            if (jwtService.isTokenValid(jwt, userDetails) && isTokenActive) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // No credentials needed, token is validated
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetails(request));

                // Set the auth in the security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Next filter in the chain
        filterChain.doFilter(request, response);
    }

}
