package com.booking.resourcebooking.security;

import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "unit-test-secret-key-must-be-long-enough-for-hmac-sha-256-1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);
    }

    private User sampleUser() {
        return User.builder().id(1L).username("alice").password("hashed").role(Role.USER).build();
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        User user = sampleUser();
        String token = jwtUtil.generateToken(user, "USER");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void isTokenValid_forMatchingUser_returnsTrue() {
        User user = sampleUser();
        String token = jwtUtil.generateToken(user, "USER");

        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_forDifferentUser_returnsFalse() {
        User user = sampleUser();
        User otherUser = User.builder().id(2L).username("bob").password("hashed").role(Role.USER).build();
        String token = jwtUtil.generateToken(user, "USER");

        assertThat(jwtUtil.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_forExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        User user = sampleUser();
        String token = jwtUtil.generateToken(user, "USER");

        assertThat(jwtUtil.isTokenValid(token, user)).isFalse();
    }

    @Test
    void malformedToken_isRejected() {
        User user = sampleUser();
        assertThat(jwtUtil.isTokenValid("not-a-real-jwt", user)).isFalse();
    }
}
