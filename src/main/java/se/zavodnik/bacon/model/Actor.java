package se.zavodnik.bacon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An actor in the graph. Actors are the nodes; movies connect them.
 */
public class Actor {

    private final int id;
    private final String name;
    private final ArrayList<Movie> movies = new ArrayList<>(2);

    Actor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Dense index (0..actorCount-1), used by the search to store state in plain arrays. */
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    void addMovie(Movie movie) {
        movies.add(movie);
    }

    void trimToSize() {
        movies.trimToSize();
    }

    @Override
    public String toString() {
        return name;
    }
}
