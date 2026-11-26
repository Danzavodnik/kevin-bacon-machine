package se.zavodnik.bacon.images;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Bundled lookup table from actor name to TMDB profile image path.
 *
 * <p>The table is generated once with {@link ImageCacheBuilder} and shipped with the app, so
 * images work without a TMDB API token. Each line is {@code name<TAB>profilePath}; an empty path
 * means TMDB has no image for that name, which saves a pointless API call.
 */
public class ImageCache {

    public static final String RESOURCE = "/images/actor-images.tsv";

    private final Map<String, String> profilePaths;

    ImageCache(Map<String, String> profilePaths) {
        this.profilePaths = profilePaths;
    }

    public static ImageCache loadBundled() {
        try (InputStream in = ImageCache.class.getResourceAsStream(RESOURCE)) {
            return in == null ? new ImageCache(Map.of()) : new ImageCache(parse(in));
        } catch (IOException e) {
            System.err.println("Could not read the image cache: " + e.getMessage());
            return new ImageCache(Map.of());
        }
    }

    static Map<String, String> parse(InputStream in) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) {
                    map.put(line.substring(0, tab), line.substring(tab + 1).trim());
                }
            }
        }
        return map;
    }

    /** True if the cache knows this name, even if the answer is "no image". */
    public boolean contains(String name) {
        return profilePaths.containsKey(name);
    }

    /** The TMDB profile path (e.g. {@code /abc.jpg}), or {@code null} if unknown or without image. */
    public String profilePath(String name) {
        String path = profilePaths.get(name);
        return path == null || path.isEmpty() ? null : path;
    }

    public int size() {
        return profilePaths.size();
    }
}
