package se.zavodnik.bacon.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EnvConfigTest {

    @Test
    void parsesCommentsQuotesExportAndBom() throws IOException {
        Path file = Files.createTempFile("test", ".env");
        try {
            Files.writeString(file, "﻿# comment\r\n"
                    + "TMDB_TOKEN=abc.def\r\n"
                    + "export PORT=9090\n"
                    + "QUOTED=\"hello world\"\n"
                    + "SINGLE='x'\n"
                    + "not a pair\n", StandardCharsets.UTF_8);

            Map<String, String> values = EnvConfig.parse(file);

            assertEquals("abc.def", values.get("TMDB_TOKEN"));
            assertEquals("9090", values.get("PORT"));
            assertEquals("hello world", values.get("QUOTED"));
            assertEquals("x", values.get("SINGLE"));
            assertFalse(values.containsKey("not a pair"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
