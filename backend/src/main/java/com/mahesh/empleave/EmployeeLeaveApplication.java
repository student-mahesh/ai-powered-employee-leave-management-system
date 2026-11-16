package com.mahesh.empleave;

import com.mahesh.empleave.user.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class EmployeeLeaveApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeLeaveApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "app.seed-admin.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder, org.springframework.core.env.Environment env) {
        return args -> {
            String email = env.getProperty("app.seed-admin.email");
            if (!users.existsByEmail(email)) {
                users.save(new AppUser(email, encoder.encode(env.getProperty("app.seed-admin.password")), Role.ADMIN, "System Administrator"));
            }
        };
    }
}
