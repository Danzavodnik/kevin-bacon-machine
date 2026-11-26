package se.zavodnik.bacon;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import se.zavodnik.bacon.config.EnvConfig;
import se.zavodnik.bacon.data.Dataset;
import se.zavodnik.bacon.images.ActorImageService;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.search.BaconIndex;
import se.zavodnik.bacon.search.BaconService;
import se.zavodnik.bacon.web.WebServer;

/**
 * Starts the Kevin Bacon Machine.
 *
 * <p>Options: {@code --demo} loads the small bundled dataset, {@code --no-browser} skips opening
 * the browser. The port is read from {@code PORT} (default 8080).
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        boolean demo = Arrays.asList(args).contains("--demo");
        boolean openBrowser = !Arrays.asList(args).contains("--no-browser")
                && EnvConfig.get("NO_BROWSER") == null;
        int port = EnvConfig.getInt("PORT", 8080);

        long start = System.nanoTime();
        Dataset dataset = Dataset.load(demo);
        ActorGraph graph = dataset.graph();
        System.out.printf("Loaded %,d actors and %,d movies from %s in %.1f s%n",
                graph.actorCount(), graph.movieCount(), dataset.description(), seconds(start));

        start = System.nanoTime();
        BaconIndex index = BaconIndex.build(graph);
        if (index.getCenter() == null) {
            System.out.println("Warning: Kevin Bacon is not in this dataset - every search will return 'not connected'.");
        }
        System.out.printf("Indexed %,d actors connected to Kevin Bacon in %.1f s%n",
                index.reachableCount(), seconds(start));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dataset", demo ? "demo" : "full");
        stats.put("actors", graph.actorCount());
        stats.put("movies", graph.movieCount());
        stats.put("connectedToBacon", index.reachableCount());

        BaconService service = new BaconService(graph, index);
        ActorImageService images = ActorImageService.fromConfig();

        WebServer.create(service, images, stats).start(port);
        String url = "http://localhost:" + port + "/";
        System.out.println("Kevin Bacon Machine is running at " + url + "  (Ctrl+C to stop)");

        if (openBrowser) {
            openInBrowser(url);
        }
    }

    private static void openInBrowser(String url) {
        try {
            if (!GraphicsEnvironment.isHeadless()
                    && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            // Not critical – the URL is printed above.
        }
    }

    private static double seconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1e9;
    }
}
