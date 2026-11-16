package com.mahesh.empleave.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class FrontendTestEnvironmentGuardTest {
    private final FrontendTestEnvironmentGuard guard = new FrontendTestEnvironmentGuard();

    @Test
    void acceptsOnlyTheConfiguredTestSchema() {
        assertThatCode(() -> verify(validEnvironment())).doesNotThrowAnyException();
    }

    @Test
    void rejectsOperationalApplicationSchema() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/aiemployeeleavemanagementsystem");

        assertRejected(environment, "spring.datasource.url must not target a protected operational schema");
    }

    @Test
    void rejectsUnrelatedApplicationSchema() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/unrelated");

        assertRejected(environment, "spring.datasource.url must target only");
    }

    @Test
    void rejectsMissingApplicationDatasource() {
        MockEnvironment environment = baseEnvironment();

        assertRejected(environment, "Missing required frontend-test property: spring.datasource.url");
    }

    @Test
    void rejectsMissingFlywayDatasource() {
        MockEnvironment environment = validEnvironmentWithout("spring.flyway.url");

        assertRejected(environment, "Missing required frontend-test property: spring.flyway.url");
    }

    @Test
    void rejectsWrongFlywaySchema() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.flyway.url", "jdbc:mysql://localhost:3306/employeeleavemanagementsystem");

        assertRejected(environment, "spring.flyway.url must not target a protected operational schema");
    }

    @Test
    void rejectsAutomaticDatabaseCreationOption() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.datasource.url", testUrl() + "?createDatabaseIfNotExist = true");

        assertRejected(environment, "spring.datasource.url must not create a database automatically");
    }

    @Test
    void rejectsWrongChromaEndpoint() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.ai.chroma-url", "http://127.0.0.1:8000");

        assertRejected(environment, "spring.ai.chroma-url must equal");
    }

    @Test
    void rejectsWrongCorsOrigin() {
        MockEnvironment environment = validEnvironment()
                .withProperty("app.cors-origin", "http://127.0.0.1:5173");

        assertRejected(environment, "app.cors-origin must equal");
    }

    @Test
    void rejectsMissingTestJwtSecret() {
        MockEnvironment environment = validEnvironmentWithout("app.jwt.secret");

        assertRejected(environment, "Missing required frontend-test property: app.jwt.secret");
    }

    private void verify(MockEnvironment environment) {
        guard.postProcessEnvironment(environment, null);
    }

    private void assertRejected(MockEnvironment environment, String message) {
        assertThatThrownBy(() -> verify(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(message);
    }

    private MockEnvironment validEnvironment() {
        return populate(baseEnvironment(), true, true, true);
    }

    private MockEnvironment validEnvironmentWithout(String missingProperty) {
        MockEnvironment environment = baseEnvironment();
        boolean datasource = !"spring.datasource.url".equals(missingProperty);
        boolean flyway = !"spring.flyway.url".equals(missingProperty);
        boolean jwt = !"app.jwt.secret".equals(missingProperty);
        return populate(environment, datasource, flyway, jwt);
    }

    private MockEnvironment baseEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("frontend-test");
        return environment;
    }

    private MockEnvironment populate(MockEnvironment environment, boolean datasource, boolean flyway, boolean jwt) {
        if (datasource) {
            environment.withProperty("spring.datasource.url", testUrl());
        }
        if (flyway) {
            environment.withProperty("spring.flyway.url", testUrl());
        }
        environment.withProperty("spring.datasource.username", "test_app")
                .withProperty("spring.datasource.password", "test-password")
                .withProperty("spring.flyway.user", "test_migrator")
                .withProperty("spring.flyway.password", "test-migrator-password")
                .withProperty("spring.ai.chroma-url", "http://127.0.0.1:8001")
                .withProperty("app.cors-origin", "http://127.0.0.1:5175");
        if (jwt) {
            environment.withProperty("app.jwt.secret", "test-only-secret");
        }
        return environment;
    }

    private String testUrl() {
        return "jdbc:mysql://localhost:3306/aiemployeeleavemanagementsystem_frontend_test";
    }
}
