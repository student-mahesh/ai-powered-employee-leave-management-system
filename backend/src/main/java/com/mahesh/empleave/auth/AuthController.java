package com.mahesh.empleave.auth;

import com.mahesh.empleave.audit.AuditService;
import com.mahesh.empleave.security.JwtService;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    record Login(@Email String email, @NotBlank String password) {}
    record Register(@NotBlank @Size(max = 160) String fullName, @Email @NotBlank String email,
                    @NotBlank @Size(min = 8, max = 100) String password) {}
    record Session(String token, String email, String name, Role role) {}
    record Registration(String email, String name, Role role) {}

    @PostMapping("/login")
    @SecurityRequirements
    Session login(@RequestBody @Valid Login request) {
        var user = users.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new Session(jwt.issue(user), user.getEmail(), user.getFullName(), user.getRole());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    Registration register(@RequestBody @Valid Register request) {
        if (users.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        var user = users.save(new AppUser(request.email(), encoder.encode(request.password()), Role.EMPLOYEE, request.fullName()));
        audit.log(user, "EMPLOYEE_REGISTERED", "EMPLOYEE", user.getId());
        return new Registration(user.getEmail(), user.getFullName(), user.getRole());
    }

    @GetMapping("/me")
    Session me(Authentication authentication) {
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        return new Session(null, user.getEmail(), user.getFullName(), user.getRole());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout() {}
}
