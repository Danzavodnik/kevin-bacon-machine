package se.zavodnik.bacon.search;

/**
 * One hop on the way to Kevin Bacon: {@code fromActor} and {@code toActor} both appear in the movie.
 * Getter names are part of the JSON API used by the frontend.
 */
public class PathStep {

    private final String fromActor;
    private final String movieTitle;
    private final String movieYear;
    private final String toActor;

    public PathStep(String fromActor, String movieTitle, String movieYear, String toActor) {
        this.fromActor = fromActor;
        this.movieTitle = movieTitle;
        this.movieYear = movieYear;
        this.toActor = toActor;
    }

    public String getFromActor() {
        return fromActor;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getMovieYear() {
        return movieYear;
    }

    public String getToActor() {
        return toActor;
    }

    @Override
    public String toString() {
        return fromActor + " -> " + movieTitle + " (" + movieYear + ") -> " + toActor;
    }
}
