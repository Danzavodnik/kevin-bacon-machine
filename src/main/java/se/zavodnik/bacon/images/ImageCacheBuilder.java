package se.zavodnik.bacon.images;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import se.zavodnik.bacon.config.EnvConfig;
import se.zavodnik.bacon.data.Dataset;
import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.search.BaconIndex;

/**
 * Maintainer tool that pre-fetches TMDB image paths so the app can show images without a token.
 *
 * <p>Which actors are worth caching? Shortest paths almost always run through well-connected
 * "hub" actors, and every path ends with one of Kevin Bacon's co-stars. The builder therefore
 * caches the demo actors, everyone with Bacon number 0–1, and the N actors with the most movies.
 *
 * <p>Run with a {@code TMDB_TOKEN} in {@code .env}:
 * <pre>mvn -q compile exec:java -Dexec.mainClass=se.zavodnik.bacon.images.ImageCacheBuilder -Dexec.args="20000"</pre>
 * The tool is resumable: names already in the output file are skipped.
 */
public final class ImageCacheBuilder {

    private static final Path OUTPUT = Path.of("src", "main", "resources", "images", "actor-images.tsv");
    private static final int THREADS = 8;
    private static final int REQUESTS_PER_SECOND = 40;

    private ImageCacheBuilder() {
    }

    public static void main(String[] args) throws Exception {
        int topN = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;
        String token = EnvConfig.get("TMDB_TOKEN");
        if (token == null) {
            System.err.println("TMDB_TOKEN is not set. Add it to .env (see .env.example).");
            System.exit(1);
        }

        Map<String, String> existing = readExisting();
        Set<String> names = selectNames(topN);
        names.removeAll(existing.keySet());
        System.out.printf("%d names already cached, %d to look up.%n", existing.size(), names.size());

        TmdbClient tmdb = new TmdbClient(token);
        RateLimiter limiter = new RateLimiter(REQUESTS_PER_SECOND);
        AtomicInteger done = new AtomicInteger();
        AtomicInteger withImage = new AtomicInteger();
        int total = names.size();

        Files.createDirectories(OUTPUT.getParent());
        try (BufferedWriter out = Files.newBufferedWriter(OUTPUT, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            for (String name : names) {
                pool.submit(() -> {
                    try {
                        limiter.acquire();
                        String path = tmdb.findProfilePath(name);
                        synchronized (out) {
                            out.write(name + "\t" + path);
                            out.newLine();
                        }
                        if (!path.isEmpty()) {
                            withImage.incrementAndGet();
                        }
                    } catch (TmdbClient.TmdbException e) {
                        if (e.statusCode() == 401) {
                            System.err.println("TMDB rejected the token: " + e.getMessage());
                            pool.shutdownNow();
                        }
                    } catch (Exception e) {
                        System.err.println("Skipping \"" + name + "\": " + e.getMessage());
                    }
                    int count = done.incrementAndGet();
                    if (count % 500 == 0 || count == total) {
                        System.out.printf("  %d / %d (%d with image)%n", count, total, withImage.get());
                        synchronized (out) {
                            try {
                                out.flush();
                            } catch (IOException ignored) {
                                // flushed again on close
                            }
                        }
                    }
                    return null;
                });
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.DAYS);
        }

        sortOutputFile();
        System.out.println("Done. Commit " + OUTPUT + " to ship the images with the app.");
    }

    private static Set<String> selectNames(int topN) throws IOException {
        Set<String> names = new LinkedHashSet<>();

        // 1. Everyone in the bundled demo dataset.
        for (Actor actor : Dataset.demo().graph().getActors()) {
            names.add(actor.getName());
        }

        // 2. Kevin Bacon and all of his co-stars: the last step of every path.
        Dataset full = Dataset.load(false);
        ActorGraph graph = full.graph();
        BaconIndex index = BaconIndex.build(graph);
        for (Actor actor : graph.getActors()) {
            int number = index.baconNumber(actor);
            if (number == 0 || number == 1) {
                names.add(actor.getName());
            }
        }

        // 3. The most prolific actors: the hubs that most paths pass through.
        List<Actor> byMovieCount = new ArrayList<>(graph.getActors());
        byMovieCount.sort(Comparator.comparingInt((Actor a) -> a.getMovies().size()).reversed());
        for (int i = 0; i < Math.min(topN, byMovieCount.size()); i++) {
            names.add(byMovieCount.get(i).getName());
        }
        return names;
    }

    private static Map<String, String> readExisting() throws IOException {
        if (!Files.exists(OUTPUT)) {
            return new TreeMap<>();
        }
        try (InputStream in = Files.newInputStream(OUTPUT)) {
            return new TreeMap<>(ImageCache.parse(in));
        }
    }

    /** Rewrites the file sorted and de-duplicated, which keeps Git diffs small. */
    private static void sortOutputFile() throws IOException {
        Map<String, String> all = readExisting();
        List<String> lines = new ArrayList<>(all.size());
        all.forEach((name, path) -> lines.add(name + "\t" + path));
        Files.write(OUTPUT, lines, StandardCharsets.UTF_8);
    }

    /** Spaces requests evenly so we stay below TMDB's rate limit. */
    private static final class RateLimiter {
        private final long intervalNanos;
        private long nextSlot = System.nanoTime();

        RateLimiter(int perSecond) {
            this.intervalNanos = 1_000_000_000L / perSecond;
        }

        void acquire() throws InterruptedException {
            long wait;
            synchronized (this) {
                long now = System.nanoTime();
                nextSlot = Math.max(nextSlot, now);
                wait = nextSlot - now;
                nextSlot += intervalNanos;
            }
            if (wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }
        }
    }
}
