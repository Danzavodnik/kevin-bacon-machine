package se.zavodnik.bacon.search;

import java.util.List;

/**
 * Search result for one actor. A Bacon number of {@value #NOT_CONNECTED} means there is no path.
 * Getter names are part of the JSON API used by the frontend.
 */
public class ActorResult {

    public static final int NOT_CONNECTED = -1;

    private final String name;
    private final int baconNumber;
    private final int movieCount;
    private final List<PathStep> path;

    public ActorResult(String name, int baconNumber, int movieCount, List<PathStep> path) {
        this.name = name;
        this.baconNumber = baconNumber;
        this.movieCount = movieCount;
        this.path = List.copyOf(path);
    }

    public String getName() {
        return name;
    }

    public int getBaconNumber() {
        return baconNumber;
    }

    public int getMovieCount() {
        return movieCount;
    }

    public List<PathStep> getPath() {
        return path;
    }

    public boolean isConnected() {
        return baconNumber != NOT_CONNECTED;
    }
}
