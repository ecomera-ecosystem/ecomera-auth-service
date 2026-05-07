package com.ecomera.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.signing-key}")
    private String signingKey;
    @Value("${jwt.access-token-ttl:3600000}")
    private long accessTokenTtl;
    @Value("${jwt.refresh-token-ttl:604800000}")
    private long refreshTokenTtl;

    private SecretKey getKey() {
        if (signingKey == null || signingKey.isEmpty()) {
            throw new IllegalStateException("JWT Secret Key is not set!");
        }
        return Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // Extract the subject (email) from the token
    }


    /**
     * @param claimsResolver a getter function that convert the Claims Map into the desired type T
     * @return a generic type T containing the extracted claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser() // 1. Create a new JwtParserBuilder instance
                .verifyWith(getKey()) // 2. Provide the secret key used to verify the token's signature
                .build() // 3. Build the parser with the given key
                .parseSignedClaims(token) // 4. Parse the JWT and validate the signature
                .getPayload(); // 5. Extract the payload (Claims) from the token
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, accessTokenTtl);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenTtl);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .claims(extraClaims) // Set any extra claims (custom data)
                .subject(userDetails.getUsername()) // Set the subject
                .issuedAt(new Date(System.currentTimeMillis())) // Set the issued date
                .expiration(new Date(System.currentTimeMillis() + expiration)) //
                .signWith(getKey()) // Sign the token with the key
                .compact(); // Compact the token: it generates the token as a string
    }

}
