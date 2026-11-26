package se.zavodnik.bacon.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import se.zavodnik.bacon.config.EnvConfig;
import se.zavodnik.bacon.model.ActorGraph;

/**
 * Decides which movie data to load and fetches the full dataset on first run.
 *
 * <p>
 * 
 * 
 * The full dataset (2.8 million actors, ~108 MB compressed) is too large for
 * the Git
 * 
 * repository, so it is published as a GitHub release asset and downloaded
 * o
 * {@code data/moviedata.txt.gz} the first time the app starts. A small demo
 * dataset is bundled
 * in the jar and used when {@code --demo} is passed or the download is not
 * possible.
 */
public final class Dataset {

    public static final String DEFAULT_URL = "https://github.com/DanZavodnik/kevin-bacon-machine/releases/latest/download/moviedata.txt.gz";

    static final String DEMO_RESOURCE = "/data/demo-moviedata.txt";
    static final Path LOCAL_FILE = Path.of("data", "moviedata.txt.gz");

    private final String description;
    private final ActorGraph graph;

    private Dataset(String description, ActorGraph graph) {
        this.description = description;
        this.graph = graph;
    }

    public String description() {
        return description;
    }

    public ActorGraph graph() {
        return graph;
    }

    public static Dataset demo() throws IOException {
        return new Dataset("demo dataset", DataLoader.loadResource(DEMO_RESOURCE));
    }

    /**
     * Loads the dataset chosen by configuration:
     * {@code DATA_FILE} (a local file) if set, otherwise the full dataset in
     * {@code data/}
     * (downloaded if missing), falling back to the demo dataset if that fails.
     **/

    public static Dataset load(boolean demoRequested) throws IOException {
        if (demoRequested || "demo".equalsIgnoreCase(EnvConfig.get("DATASET"))) {
            return demo();
        }

        String dataFile = EnvConfig.get("DATA_FILE");
        if (dataFile != null) {
            Path path = Path.of(dataFile);
            return new Dataset(path.toString(), DataLoader.load(path));
        }

        Path local = findLocalFile();
        if (local == null) {
            try {
                local = download(EnvConfig.get("DATASET_URL", DEFAULT_URL), LOCAL_FILE);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                System.out.println("Could not download the full dataset (" + reason + ").");
                System.out.println("Falling back to the demo dataset. See README.md for how to get the full data.");
                return demo();
            }
        }
        return new Dataset(local.toString(), DataLoader.load(local));
    }

    private static Path findLocalFile() {
        if (Files.isRegularFile(LOCAL_FILE)) {
            return LOCAL_FILE;
        }
        Path uncompressed = Path.of("data", "moviedata.txt");
        return Files.isRegularFile(uncompressed) ? uncompressed : null;
    }

    static Path download(String url, Path target) throws IOException, InterruptedException {
        System.out.println("Full dataset not found - downloading it once from " + url);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path partial = target.resolveSibling(target.getFileName() + ".part");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            response.body().close();
                        throw new IOException("HTTP " + response.statusCode());
        }
                

        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        try (InputStream in = response.body();
                var out = Files.newOutputStream(partial)) {
            byte[] buffer = new byte[1 << 16];
            long done = 0;
            int lastPercent = -1;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                if (total > 0) {
                    int percent = (int) (done * 100 / total);
                    if (percent / 10 != lastPercent / 10) {
                        System.out.printf("  %d%% (%d / %d MB)%n", percent, done >> 20, total >> 20);
                        lastPercent = percent;
                    }
                }
            }
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Saved to " + target.toAbsolutePath());
        return target;
    }
}
