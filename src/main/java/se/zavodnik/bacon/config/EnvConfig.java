package se.zavodnik.bacon.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads configuration such as API tokens without keeping them in the source code.
 *
 * <p>Values are looked up in this order:
 * <ol>
 *   <li>an environment variable with the same name</li>
 *   <li>a {@code .env} file – first in the working directory, then upwards from the location of
 *       the jar/classes (so it is found whether the app is started from an IDE, the project folder
 *       or {@code target/})</li>
 * </ol>
 * The {@code .env} file is git-ignored; see {@code .env.example} for the supported keys.
 */
public final class EnvConfig {

    private static Path loadedFrom;
    private static final Map<String, String> DOT_ENV = loadFirstDotEnv();

    private EnvConfig() {
    }

    /** The value for {@code key}, or {@code null} if it is not set. */
    public static String get(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = DOT_ENV.get(key);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Ignoring invalid number for " + key + ": " + value);
            return defaultValue;
        }
    }

    /** Where the value of {@code key} came from, for log messages. Never returns the value itself. */
    public static String describeSource(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return "environment variable";
        }
        return DOT_ENV.containsKey(key) ? String.valueOf(loadedFrom) : "not set";
    }

    private static Map<String, String> loadFirstDotEnv() {
        for (Path candidate : candidateFiles()) {
            if (Files.isRegularFile(candidate)) {
                loadedFrom = candidate;
                return parse(candidate);
            }
        }
        return Map.of();
    }

    private static Set<Path> candidateFiles() {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Path.of(".env").toAbsolutePath().normalize());
        try {
            Path location = Path.of(EnvConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dir = Files.isDirectory(location) ? location : location.getParent();
            for (int i = 0; i < 4 && dir != null; i++) {
                candidates.add(dir.resolve(".env").toAbsolutePath().normalize());
                dir = dir.getParent();
            }
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            // Unknown code location: the working directory is enough.
        }
        return candidates;
    }

    /** Parses {@code KEY=value} lines; supports comments, {@code export}, quotes and a UTF-8 BOM. */
    static Map<String, String> parse(Path file) {
        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.strip();
                if (line.startsWith("﻿")) {
                    line = line.substring(1);
                }
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).strip();
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2
                        && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
        }
        return values;
    }
}
