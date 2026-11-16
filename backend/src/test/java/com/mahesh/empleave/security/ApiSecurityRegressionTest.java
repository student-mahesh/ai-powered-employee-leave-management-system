package com.mahesh.empleave.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahesh.empleave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ApiSecurityRegressionTest.ProtectedApiController.class)
@Import({SecurityConfig.class, ApiSecurityRegressionTest.ProtectedApiController.class})
@TestPropertySource(properties = "app.cors-origin=http://127.0.0.1:5175")
class ApiSecurityRegressionTest {
    @Autowired MockMvc mvc;
    @MockBean JwtService jwt;
    @MockBean UserRepository users;

    @Test void anonymousProtectedApiRequestReturnsUnauthorized() throws Exception { mvc.perform(get("/api/security-regression/admin")).andExpect(status().isUnauthorized()); }
    @Test void authenticatedUserWithoutRoleReturnsForbidden() throws Exception { mvc.perform(get("/api/security-regression/admin").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden()); }
    @Test void authorizedUserReceivesExistingResponse() throws Exception { mvc.perform(get("/api/security-regression/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string("allowed")); }
    @Test void configuredFrontendOriginCanPreflightLogin() throws Exception { mvc.perform(options("/api/auth/login").header("Origin", "http://127.0.0.1:5175").header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type")).andExpect(status().isOk()); }
    @Test void invalidBearerTokenReturnsUnauthorized() throws Exception { when(jwt.subject("invalid-token")).thenThrow(new IllegalArgumentException("invalid token")); mvc.perform(get("/api/security-regression/admin").header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized()); }
    @Test void expiredBearerTokenReturnsUnauthorized() throws Exception { when(jwt.subject("expired-token")).thenThrow(new IllegalArgumentException("expired token")); mvc.perform(get("/api/security-regression/admin").header("Authorization", "Bearer expired-token")).andExpect(status().isUnauthorized()); }

    @RestController public static class ProtectedApiController {
        @GetMapping("/api/security-regression/admin") @PreAuthorize("hasRole('ADMIN')") String adminOnly() { return "allowed"; }
    }
}
