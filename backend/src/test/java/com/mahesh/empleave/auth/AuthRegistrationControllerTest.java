package com.mahesh.empleave.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahesh.empleave.audit.AuditService;
import com.mahesh.empleave.security.JwtService;
import com.mahesh.empleave.security.SecurityConfig;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthRegistrationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockBean UserRepository users;
    @MockBean JwtService jwt;
    @MockBean AuditService audit;

    @Test void publicRegistrationCreatesOnlyBcryptEmployee() throws Exception {
        when(users.existsByEmail("new.employee@example.test")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Employee\",\"email\":\"new.employee@example.test\",\"password\":\"secure-pass-123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("new.employee@example.test"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        var saved = ArgumentCaptor.forClass(AppUser.class); verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(encoder.matches("secure-pass-123", saved.getValue().getPassword())).isTrue();
    }

    @Test void duplicateEmailReturnsConflictWithoutChangingExistingAccount() throws Exception {
        when(users.existsByEmail("existing@example.test")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Existing\",\"email\":\"existing@example.test\",\"password\":\"secure-pass-123\"}"))
                .andExpect(status().isConflict());
        verify(users, never()).save(any());
    }

    @Test void invalidRegistrationIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verify(users, never()).save(any());
    }

    @Test void clientRoleFieldsCannotEscalateTheNewEmployee() throws Exception {
        when(users.existsByEmail("attacker@example.test")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Attacker\",\"email\":\"attacker@example.test\",\"password\":\"secure-pass-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("EMPLOYEE"));
        var saved = ArgumentCaptor.forClass(AppUser.class); verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test void existingLoginStillWorks() throws Exception {
        var account = new AppUser("employee@example.test", encoder.encode("secure-pass-123"), Role.EMPLOYEE, "Employee");
        when(users.findByEmail("employee@example.test")).thenReturn(java.util.Optional.of(account));
        when(jwt.issue(account)).thenReturn("test-token");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@example.test\",\"password\":\"secure-pass-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test void eachExistingRoleReceivesItsOwnAuthenticatedSessionRole() throws Exception {
        for (var role : Role.values()) {
            String email = role.name().toLowerCase() + "@example.test";
            var account = new AppUser(email, encoder.encode("secure-pass-123"), role, role.name());
            when(users.findByEmail(email)).thenReturn(java.util.Optional.of(account));
            when(jwt.issue(account)).thenReturn("test-token-" + role.name());

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"secure-pass-123\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
        }
    }

    @Test void incorrectLoginPasswordIsRejectedWithoutChangingTheAccount() throws Exception {
        var account = new AppUser("employee@example.test", encoder.encode("secure-pass-123"), Role.EMPLOYEE, "Employee");
        when(users.findByEmail("employee@example.test")).thenReturn(java.util.Optional.of(account));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@example.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid credentials"));
        verify(users, never()).save(any());
    }
}
