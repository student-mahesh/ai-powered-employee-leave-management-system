package com.mahesh.empleave.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "test-signing-secret-that-is-at-least-32-bytes";

    @Test
    void issuedTokenValidatesWithTheSameConfiguredSecret() {
        var jwt = new JwtService(SECRET, 60_000);
        var token = jwt.issue(new AppUser("employee@example.test", "unused", Role.EMPLOYEE, "Employee"));

        assertThat(jwt.subject(token)).isEqualTo("employee@example.test");
    }

    @Test
    void tokenFromAnotherSecretOrAnExpiredTokenIsRejected() {
        var issuer = new JwtService(SECRET, -1);
        var expired = issuer.issue(new AppUser("employee@example.test", "unused", Role.EMPLOYEE, "Employee"));

        assertThatThrownBy(() -> new JwtService("another-signing-secret-that-is-at-least-32-bytes", 60_000).subject(expired))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> issuer.subject(expired)).isInstanceOf(RuntimeException.class);
    }
}
