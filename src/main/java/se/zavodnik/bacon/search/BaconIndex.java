package se.zavodnik.bacon.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.model.Movie;

/**
 * Precomputed shortest paths from every actor to Kevin Bacon.
 *
 * <p>Instead of running a new breadth-first search for every query, the index runs a single BFS
 * <em>from</em> Kevin Bacon when the application starts. Because the graph is undirected, the BFS
 * tree gives every reachable actor both its Bacon number and the next step towards Bacon.
 * A query then just follows those parent pointers: O(path length) instead of O(V + E).
 *
 * <p>All state lives in plain {@code int[]} arrays indexed by actor/movie id, which keeps the
 * memory overhead to a few bytes per node even for millions of actors.
 */
public class BaconIndex {

    private static final int UNVISITED = -1;

    private final ActorGraph graph;
    private final Actor bacon;
    private final int[] distance;
    /** For each actor: the neighbour one step closer to Bacon. */
    private final int[] nextActor;
    /** For each actor: the movie shared with {@code nextActor}. */
    private final int[] viaMovie;
    private final int reachableCount;

    private BaconIndex(ActorGraph graph, Actor bacon) {
        this.graph = graph;
        this.bacon = bacon;

        int actorCount = graph.actorCount();
        distance = new int[actorCount];
        nextActor = new int[actorCount];
        viaMovie = new int[actorCount];
        Arrays.fill(distance, UNVISITED);

        reachableCount = bacon == null ? 0 : breadthFirstSearch(bacon);
    }

    /** Builds the index around the actor named "Kevin Bacon". */
    public static BaconIndex build(ActorGraph graph) {
        return new BaconIndex(graph, findCenter(graph, "Kevin Bacon"));
    }

    /** Builds the index around any actor – useful for tests and "Six degrees of X". */
    public static BaconIndex build(ActorGraph graph, Actor center) {
        return new BaconIndex(graph, center);
    }

    private int breadthFirstSearch(Actor start) {
        boolean[] movieVisited = new boolean[graph.movieCount()];
        int[] queue = new int[graph.actorCount()];
        int head = 0;
        int tail = 0;

        queue[tail++] = start.getId();
        distance[start.getId()] = 0;
        nextActor[start.getId()] = UNVISITED;
        viaMovie[start.getId()] = UNVISITED;

        while (head < tail) {
            Actor current = graph.getActor(queue[head++]);
            int nextDistance = distance[current.getId()] + 1;

            for (Movie movie : current.getMovies()) {
                // Each movie only needs to be expanded once: the first actor to reach it is
                // already the closest one, so later visits cannot find shorter paths.
                if (movieVisited[movie.getId()]) {
                    continue;
                }
                movieVisited[movie.getId()] = true;

                for (Actor coStar : movie.getActors()) {
                    int id = coStar.getId();
                    if (distance[id] == UNVISITED) {
                        distance[id] = nextDistance;
                        nextActor[id] = current.getId();
                        viaMovie[id] = movie.getId();
                        queue[tail++] = id;
                    }
                }
            }
        }
        return tail;
    }

    /** Bacon number of the actor, or {@link ActorResult#NOT_CONNECTED}. */
    public int baconNumber(Actor actor) {
        return distance[actor.getId()];
    }

    /** The shortest path from {@code actor} to Kevin Bacon (empty if none or if it is Bacon). */
    public List<PathStep> pathToBacon(Actor actor) {
        List<PathStep> path = new ArrayList<>();
        if (distance[actor.getId()] == UNVISITED) {
            return path;
        }
        int current = actor.getId();
        while (nextActor[current] != UNVISITED) {
            Actor from = graph.getActor(current);
            Actor to = graph.getActor(nextActor[current]);
            Movie movie = graph.getMovie(viaMovie[current]);
            path.add(new PathStep(from.getName(), movie.getTitle(), movie.getYear(), to.getName()));
            current = to.getId();
        }
        return path;
    }

    public ActorResult resultFor(Actor actor) {
        return new ActorResult(actor.getName(), baconNumber(actor), actor.getMovies().size(), pathToBacon(actor));
    }

    public Actor getCenter() {
        return bacon;
    }

    /** Number of actors with a path to Kevin Bacon (including Bacon himself). */
    public int reachableCount() {
        return reachableCount;
    }

    private static Actor findCenter(ActorGraph graph, String name) {
        Actor best = null;
        for (Actor candidate : graph.search(name)) {
            if (best == null || candidate.getMovies().size() > best.getMovies().size()) {
                best = candidate;
            }
        }
        return best;
    }
}
