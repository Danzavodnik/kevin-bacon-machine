package se.zavodnik.bacon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A movie in the graph. Every pair of actors that appear in the same movie is connected through it.
 */
public class Movie {

    private final int id;
    private final String title;
    private final String year;
    private final ArrayList<Actor> actors = new ArrayList<>(2);

    Movie(int id, String title, String year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    /** Dense index (0..movieCount-1). */
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public List<Actor> getActors() {
        return actors;
    }

    void addActor(Actor actor) {
        actors.add(actor);
    }

    void trimToSize() {
        actors.trimToSize();
    }

    @Override
    public String toString() {
        return title + " (" + year + ")";
    }
}
