package se.zavodnik.bacon.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bipartite graph of actors and movies, plus a name index used for searching.
 *
 * <p>Build it with {@link #getOrCreateActor}, {@link #getOrCreateMovie} and {@link #connect},
 * then call {@link #finishLoading()} once to release the lookup tables that are only needed
 * while loading. That roughly halves the memory used by the full dataset.
 */
public class ActorGraph {

    private final List<Actor> actors = new ArrayList<>();
    private final List<Movie> movies = new ArrayList<>();
    private final Map<String, List<Actor>> searchIndex = new HashMap<>();

    // Only needed while loading; cleared by finishLoading().
    private Map<String, Actor> actorsByName = new HashMap<>();
    private Map<String, Movie> moviesByKey = new HashMap<>();

    /**
     * Returns the actor with this name, creating it if it does not exist yet.
     * Entries in the data that share a display name are treated as the same actor.
     */
    public Actor getOrCreateActor(String name) {
        requireLoading();
        Actor actor = actorsByName.get(name);
        if (actor == null) {
            actor = new Actor(actors.size(), name);
            actors.add(actor);
            actorsByName.put(name, actor);
            searchIndex.computeIfAbsent(normalize(name), key -> new ArrayList<>(1)).add(actor);
        }
        return actor;
    }

    public Movie getOrCreateMovie(String title, String year) {
        return getOrCreateMovie(title + " (" + year + ")", title, year);
    }

    /**
     * Returns the movie stored under {@code key} (the unique title used in the data file, e.g.
     * {@code "Apollo 13 (1995/I)"}), creating it with the given display title and year if needed.
     */
    public Movie getOrCreateMovie(String key, String title, String year) {
        requireLoading();
        Movie movie = moviesByKey.get(key);
        if (movie == null) {
            movie = new Movie(movies.size(), title, year);
            movies.add(movie);
            moviesByKey.put(key, movie);
        }
        return movie;
    }

    public void connect(Actor actor, Movie movie) {
        actor.addMovie(movie);
        movie.addActor(actor);
    }

    /** Frees memory that is only needed while the graph is being built. */
    public void finishLoading() {
        actorsByName = null;
        moviesByKey = null;
        for (Actor actor : actors) {
            actor.trimToSize();
        }
        for (Movie movie : movies) {
            movie.trimToSize();
        }
    }

    /** All actors whose name matches the query, ignoring case, apostrophes and extra spaces. */
    public List<Actor> search(String name) {
        return searchIndex.getOrDefault(normalize(name), List.of());
    }

    public Actor getActor(int id) {
        return actors.get(id);
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }

    public List<Actor> getActors() {
        return actors;
    }

    public int actorCount() {
        return actors.size();
    }

    public int movieCount() {
        return movies.size();
    }

    public static String normalize(String name) {
        return name.replace("'", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void requireLoading() {
        if (actorsByName == null) {
            throw new IllegalStateException("The graph is read-only after finishLoading()");
        }
    }
}
