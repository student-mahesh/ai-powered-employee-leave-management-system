package com.mahesh.empleave.config;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/** Fails before datasource or Flyway auto-configuration if frontend-test is not fully isolated. */
public final class FrontendTestEnvironmentGuard implements EnvironmentPostProcessor, Ordered {
    static final String TEST_SCHEMA = "aiemployeeleavemanagementsystem_frontend_test";
    private static final Set<String> PROTECTED_SCHEMAS = Set.of(
            "aiemployeeleavemanagementsystem",
            "employeeleavemanagementsystem");
    private static final Pattern MYSQL_SCHEMA = Pattern.compile("^jdbc:mysql://[^/]+/([^?]+)(?:\\?.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTO_CREATE_DATABASE = Pattern.compile("[?&]createdatabaseifnotexist\\s*=\\s*true(?:[&#]|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("frontend-test"))) {
            return;
        }

        requireTestUrl(environment, "spring.datasource.url");
        requireTestUrl(environment, "spring.flyway.url");
        requireValue(environment, "spring.datasource.username");
        requireValue(environment, "spring.datasource.password");
        requireValue(environment, "spring.flyway.user");
        requireValue(environment, "spring.flyway.password");
        requireValue(environment, "app.jwt.secret");
        requireExactValue(environment, "spring.ai.chroma-url", "http://127.0.0.1:8001");
        requireExactValue(environment, "app.cors-origin", "http://127.0.0.1:5175");
    }

    private static void requireTestUrl(ConfigurableEnvironment environment, String property) {
        String url = requireValue(environment, property);
        if (AUTO_CREATE_DATABASE.matcher(url).find()) {
            throw new IllegalStateException(property + " must not create a database automatically in frontend-test");
        }

        Matcher matcher = MYSQL_SCHEMA.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException(property + " must contain a verifiable MySQL schema in frontend-test");
        }
        String schema = matcher.group(1);
        if (PROTECTED_SCHEMAS.contains(schema)) {
            throw new IllegalStateException(property + " must not target a protected operational schema");
        }
        if (!TEST_SCHEMA.equals(schema)) {
            throw new IllegalStateException(property + " must target only " + TEST_SCHEMA);
        }
    }

    private static String requireValue(ConfigurableEnvironment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalStateException("Missing required frontend-test property: " + property);
        }
        return value;
    }

    private static void requireExactValue(ConfigurableEnvironment environment, String property, String expected) {
        if (!expected.equals(requireValue(environment, property))) {
            throw new IllegalStateException(property + " must equal " + expected + " in frontend-test");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
