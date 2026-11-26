package se.zavodnik.bacon.images;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.zavodnik.bacon.config.EnvConfig;

/**
 * Resolves actor names to image URLs.
 *
 * <p>The bundled {@link ImageCache} is checked first, so images work out of the box. If the
 * name is not in the cache and a {@code TMDB_TOKEN} is configured, TMDB is queried live and the
 * answer is remembered for the rest of the session.
 */
public class ActorImageService {

    private final ImageCache cache;
    private final TmdbClient tmdb;
    private final Map<String, String> liveResults = new ConcurrentHashMap<>();
    private volatile boolean tokenRejected;

    public ActorImageService(ImageCache cache, TmdbClient tmdb) {
        this.cache = cache;
        this.tmdb = tmdb;
    }

    public static ActorImageService fromConfig() {
        ImageCache cache = ImageCache.loadBundled();
        String token = EnvConfig.get("TMDB_TOKEN");
        System.out.println("Image cache: " + cache.size() + " actors"
                + (token != null
                ? " + live TMDB lookups (token from " + EnvConfig.describeSource("TMDB_TOKEN") + ")"
                : " (set TMDB_TOKEN to look up the rest live)"));
        return new ActorImageService(cache, token != null ? new TmdbClient(token) : null);
    }

    /** Full image URL for the actor, or {@code null} if no image is available. */
    public String imageUrl(String name) {
        String path;
        if (cache.contains(name)) {
            path = cache.profilePath(name);
        } else {
            path = lookupLive(name);
        }
        return path == null || path.isEmpty() ? null : TmdbClient.IMAGE_BASE_URL + path;
    }

    private String lookupLive(String name) {
        if (tmdb == null || tokenRejected) {
            return null;
        }
        String cached = liveResults.get(name);
        if (cached != null) {
            return cached;
        }
        try {
            String path = tmdb.findProfilePath(name);
            liveResults.put(name, path);
            return path;
        } catch (TmdbClient.TmdbException e) {
            if (e.statusCode() == 401) {
                tokenRejected = true;
                System.out.println("TMDB rejected the token (401) - live image lookups are disabled. " + e.getMessage());
            } else {
                System.out.println("Image lookup failed for \"" + name + "\": " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Image lookup failed for \"" + name + "\": " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }
}
