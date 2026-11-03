package com.company.qa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("framework")
class ConfigSupportTest {
    @Test
    void systemPropertiesOverrideNormalizedEnvironmentVariablesAndFiles() {
        Properties system = new Properties();
        system.setProperty("base.url", "https://system.example.com");
        Properties resolved =
                ConfigSupport.resolve(
                        Map.of(
                                "BASE_URL",
                                "https://env.example.com",
                                "API_READ_TIMEOUT_MS",
                                "5000",
                                "DB_PASSWORD",
                                "test-only"),
                        system);
        assertThat(resolved.getProperty("base.url")).isEqualTo("https://system.example.com");
        assertThat(resolved.getProperty("api.read.timeout.ms")).isEqualTo("5000");
        assertThat(resolved.getProperty("db.password")).isEqualTo("test-only");
    }

    @Test
    void selectedEnvironmentOverlaysDefaults() {
        Properties resolved = ConfigSupport.resolve(Map.of("ENV", "staging"), new Properties());
        assertThat(resolved.getProperty("base.url")).isEqualTo("https://staging.example.com");
        assertThat(resolved.getProperty("db.driver")).isEqualTo("org.h2.Driver");
    }

    @Test
    void missingEnvironmentDoesNotFallBackToQa() {
        assertThatThrownBy(() -> ConfigSupport.resolve(Map.of("ENV", "missing"), new Properties()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing configuration");
    }

    @Test
    void rejectsInvalidSettingsBeforeStartingInfrastructure() {
        for (Map<String, String> invalid :
                List.of(
                        Map.of("ENV", "../qa"),
                        Map.of("BASE_URL", "/relative"),
                        Map.of("BROWSER", "unknown"),
                        Map.of("HEADLESS", "yes"),
                        Map.of("TIMEOUT_SECONDS", "0"),
                        Map.of("DB_MIN_IDLE", "99"),
                        Map.of("RABBIT_PORT", "65536"))) {
            assertThatThrownBy(() -> ConfigSupport.resolve(invalid, new Properties()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void defaultConfigurationContainsNoApiCredential() {
        assertThat(ConfigSupport.resolve(Map.of(), new Properties()).getProperty("api.key"))
                .isEmpty();
    }
}
