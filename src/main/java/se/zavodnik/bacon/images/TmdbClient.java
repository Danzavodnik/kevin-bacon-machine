package se.zavodnik.bacon.images;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimal client for TMDB's person search, authenticated with an API Read Access Token.
 */
public class TmdbClient {

    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";
    private static final String SEARCH_URL = "https://api.themoviedb.org/3/search/person?include_adult=false&query=";
    private static final int MAX_RETRIES = 3;

    private final String token;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public TmdbClient(String token) {
        this.token = token;
    }

    /**
     * Searches TMDB for a person and returns the profile image path of the best match,
     * or an empty string if TMDB has no image.
     *
     * @throws TmdbException if TMDB rejects the request (for example an invalid token)
     */
    public String findProfilePath(String name) throws IOException, InterruptedException {
        String url = SEARCH_URL + URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                long waitSeconds = response.headers().firstValueAsLong("Retry-After").orElse(2);
                Thread.sleep(waitSeconds * 1000);
                continue;
            }
            if (response.statusCode() != 200) {
                throw new TmdbException(response.statusCode(), response.body());
            }
            return bestProfilePath(mapper.readTree(response.body()), name);
        }
    }

    /** Prefers an exact name match; otherwise TMDB's first (most popular) result. */
    private static String bestProfilePath(JsonNode root, String name) {
        JsonNode results = root.path("results");
        JsonNode best = null;
        for (JsonNode person : results) {
            if (name.equalsIgnoreCase(person.path("name").asText())) {
                best = person;
                break;
            }
        }
        if (best == null && results.size() > 0) {
            best = results.get(0);
        }
        if (best == null) {
            return "";
        }
        JsonNode path = best.path("profile_path");
        return path.isTextual() ? path.asText() : "";
    }

    public static class TmdbException extends IOException {
        private static final long serialVersionUID = 1L;

        private final int statusCode;

        TmdbException(int statusCode, String body) {
            super("TMDB responded " + statusCode + ": " + (body.length() > 200 ? body.substring(0, 200) : body));
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
