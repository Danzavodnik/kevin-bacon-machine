package se.zavodnik.bacon.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.model.Movie;

/**
 * Reads the movie data file into an {@link ActorGraph}.
 *
 * <p>The format is line based: {@code <a>} starts a new actor ({@code "Last, First (I)"}) and
 * every following {@code <t>} line is one of that actor's movies ({@code "Title (Year)"}).
 * Files ending in {@code .gz} are decompressed on the fly.
 */
public final class DataLoader {

    private DataLoader() {
    }

    public static ActorGraph load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.getFileName().toString().endsWith(".gz"));
        }
    }

    public static ActorGraph loadResource(String resource) throws IOException {
        InputStream in = DataLoader.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("Resource not found: " + resource);
        }
        try (in) {
            return load(in, resource.endsWith(".gz"));
        }
    }

    public static ActorGraph load(InputStream input, boolean gzipped) throws IOException {
        ActorGraph graph = new ActorGraph();
        InputStream in = gzipped ? new GZIPInputStream(input, 1 << 16) : input;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16)) {
            Actor currentActor = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<a>")) {
                    currentActor = graph.getOrCreateActor(displayName(line.substring(3).trim()));
                } else if (line.startsWith("<t>") && currentActor != null) {
                    String rawTitle = line.substring(3).trim();
                    Movie movie = graph.getOrCreateMovie(rawTitle, title(rawTitle), year(rawTitle));
                    graph.connect(currentActor, movie);
                }
            }
        }
        graph.finishLoading();
        return graph;
    }

    /** {@code "Bacon, Kevin (I)"} becomes {@code "Kevin Bacon"}. */
    static String displayName(String rawName) {
        String first;
        String last;
        int comma = rawName.indexOf(',');
        if (comma >= 0) {
            last = rawName.substring(0, comma).trim();
            first = rawName.substring(comma + 1).trim();
        } else {
            first = rawName.trim();
            last = "";
        }
        int paren = first.indexOf('(');
        if (paren >= 0) {
            first = first.substring(0, paren).trim();
        }
        if (first.isEmpty()) {
            return last;
        }
        return last.isEmpty() ? first : first + " " + last;
    }

    /** {@code "Footloose (1984)"} becomes {@code "Footloose"}. */
    static String title(String rawTitle) {
        int start = rawTitle.lastIndexOf('(');
        int end = rawTitle.lastIndexOf(')');
        return start >= 0 && end > start ? rawTitle.substring(0, start).trim() : rawTitle;
    }

    /** {@code "Footloose (1984)"} becomes {@code "1984"}; {@code "unknown"} if there is no year. */
    static String year(String rawTitle) {
        int start = rawTitle.lastIndexOf('(');
        int end = rawTitle.lastIndexOf(')');
        if (start < 0 || end <= start) {
            return "unknown";
        }
        // IMDb marks duplicate titles from the same year as "1995/I", "1995/II" ...
        String year = rawTitle.substring(start + 1, end).trim();
        int slash = year.indexOf('/');
        if (slash >= 0) {
            year = year.substring(0, slash);
        }
        return year.isEmpty() || year.startsWith("?") ? "unknown" : year;
    }
}
