package com.company.qa.utils;

/**
 * Resolves secrets without committing them. Precedence: 1) -Dkey=value 2) environment variable (key
 * upper-cased, dots become underscores: api.key -> API_KEY) 3) configured value.
 */
public final class Secrets {

    private Secrets() {}

    public static String get(String key, String configuredValue) {
        String fromProperty = System.getProperty(key);
        if (fromProperty != null) return fromProperty;

        String fromEnv =
                System.getenv(
                        key.toUpperCase(java.util.Locale.ROOT).replace('.', '_').replace('-', '_'));
        if (fromEnv != null) return fromEnv;

        return configuredValue == null ? "" : configuredValue;
    }
}
