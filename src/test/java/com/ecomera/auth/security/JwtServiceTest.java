package com.ecomera.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    private static final String SIGNING_KEY = "ThisIsASecretKeyForTestingPurposesThatIsAtLeast256BitsLong!!";
    private static final long ACCESS_TTL = 3600000L;
    private static final long REFRESH_TTL = 604800000L;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "signingKey", SIGNING_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenTtl", ACCESS_TTL);
        ReflectionTestUtils.setField(jwtService, "refreshTokenTtl", REFRESH_TTL);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("test@example.com");
    }

    @Test
    void generateToken_shouldIncludeExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", "12345");
        extraClaims.put("roles", "USER");

        String token = jwtService.generateToken(extraClaims, userDetails);

        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("userId", String.class)).isEqualTo("12345");
        assertThat(claims.get("roles", String.class)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_shouldCreateValidTokenWithLongerExpiry() {
        String token = jwtService.generateRefreshToken(userDetails);

        assertThat(token).isNotBlank();

        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("test@example.com");

        long ttl = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttl).isGreaterThanOrEqualTo(REFRESH_TTL - 1000);
    }

    @Test
    void extractUsername_shouldReturnSubject() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenMatchesUserAndNotExpired() {
        String token = jwtService.generateToken(userDetails);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameMismatch() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = User.builder()
                .username("other@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenExpired() {
        ReflectionTestUtils.setField(jwtService, "accessTokenTtl", -3600000L);

        String token = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void getKey_shouldThrowIllegalStateException_whenSigningKeyIsNull() {
        JwtService badService = new JwtService();
        ReflectionTestUtils.setField(badService, "signingKey", null);

        assertThatThrownBy(() -> badService.extractUsername("some.token.here"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT Secret Key is not set");
    }

    @Test
    void generateToken_shouldCreateTokensWithDifferentSubjects() {
        UserDetails user1 = User.builder()
                .username("alice@example.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        UserDetails user2 = User.builder()
                .username("bob@example.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        assertThat(jwtService.extractUsername(token1)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("bob@example.com");
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SIGNING_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
