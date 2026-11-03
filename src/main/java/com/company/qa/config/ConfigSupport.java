package com.company.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

/** One resolution policy for every client; environment variables use UPPER_SNAKE_CASE. */
public final class ConfigSupport {
    private ConfigSupport() {}

    public static <T extends Config> T load(Class<T> type) {
        return ConfigFactory.create(type, resolve(System.getenv(), System.getProperties()));
    }

    static Properties resolve(Map<String, String> environment, Properties system) {
        String env = system.getProperty("env", environment.getOrDefault("ENV", "qa"));
        if (!env.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid environment name: " + env);
        }
        Properties values = read("qa");
        if (!env.equals("qa")) values.putAll(read(env));
        environment.forEach(
                (key, value) ->
                        values.setProperty(key.toLowerCase(Locale.ROOT).replace('_', '.'), value));
        values.putAll(system);
        values.setProperty("env", env);
        validate(values);
        return values;
    }

    private static Properties read(String env) {
        String resource = "config/" + env + ".properties";
        try (InputStream input =
                ConfigSupport.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null)
                throw new IllegalArgumentException("Missing configuration: " + resource);
            Properties values = new Properties();
            values.load(input);
            return values;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load configuration: " + resource, e);
        }
    }

    private static void validate(Properties values) {
        for (String key : new String[] {"base.url", "api.base.url", "elastic.url"}) {
            String value = values.getProperty(key, "");
            URI uri;
            try {
                uri = URI.create(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid URL setting: " + key, e);
            }
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(
                        key + " must be an absolute HTTP(S) URL without credentials");
            }
        }
        if (!values.getProperty("browser", "chromium")
                .matches("chromium|chrome|firefox|webkit|safari")) {
            throw new IllegalArgumentException(
                    "browser must be chromium, firefox, or webkit (chrome/safari aliases supported)");
        }
        for (String key : new String[] {"headless"}) {
            if (!values.getProperty(key, "true").matches("true|false")) {
                throw new IllegalArgumentException(key + " must be true or false");
            }
        }
        for (String key :
                new String[] {
                    "timeout.seconds",
                    "api.connect.timeout.ms",
                    "api.read.timeout.ms",
                    "db.max.pool.size",
                    "rabbit.port"
                }) {
            try {
                if (Integer.parseInt(values.getProperty(key)) <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be a positive integer", e);
            }
        }
        int idle;
        try {
            idle = Integer.parseInt(values.getProperty("db.min.idle"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("db.min.idle must be a non-negative integer", e);
        }
        if (idle < 0 || idle > Integer.parseInt(values.getProperty("db.max.pool.size"))) {
            throw new IllegalArgumentException(
                    "db.min.idle must be between zero and db.max.pool.size");
        }
        if (Integer.parseInt(values.getProperty("rabbit.port")) > 65535) {
            throw new IllegalArgumentException("rabbit.port must be at most 65535");
        }
        if (!values.getProperty("db.url", "").startsWith("jdbc:")
                || values.getProperty("db.driver", "").isBlank()) {
            throw new IllegalArgumentException(
                    "db.url must be a JDBC URL and db.driver must be configured");
        }
    }
}
