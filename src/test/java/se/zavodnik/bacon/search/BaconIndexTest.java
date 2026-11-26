package se.zavodnik.bacon.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.model.Movie;

/**
 * Small hand-made graph:
 * <pre>
 *   Kevin Bacon --(Movie A)-- Alice --(Movie B)-- Bob --(Movie C)-- Carol
 *   Kevin Bacon --(Movie D)-- Dave --(Movie E)-- Carol        (shortcut: Carol is 2 steps away)
 *   Eve --(Movie F)-- Frank                                     (not connected)
 * </pre>
 */
class BaconIndexTest {

    private ActorGraph graph;
    private BaconIndex index;

    @BeforeEach
    void setUp() {
        graph = new ActorGraph();
        Actor bacon = actor("Kevin Bacon");
        Actor alice = actor("Alice");
        Actor bob = actor("Bob");
        Actor carol = actor("Carol");
        Actor dave = actor("Dave");
        Actor eve = actor("Eve");
        Actor frank = actor("Frank");

        cast("Movie A", bacon, alice);
        cast("Movie B", alice, bob);
        cast("Movie C", bob, carol);
        cast("Movie D", bacon, dave);
        cast("Movie E", dave, carol);
        cast("Movie F", eve, frank);

        graph.finishLoading();
        index = BaconIndex.build(graph);
    }

    @Test
    void kevinBaconHasNumberZeroAndEmptyPath() {
        Actor bacon = find("Kevin Bacon");
        assertEquals(0, index.baconNumber(bacon));
        assertTrue(index.pathToBacon(bacon).isEmpty());
    }

    @Test
    void directCoStarHasNumberOne() {
        assertEquals(1, index.baconNumber(find("Alice")));
        assertEquals(1, index.baconNumber(find("Dave")));
    }

    @Test
    void findsTheShortestOfSeveralPaths() {
        Actor carol = find("Carol");
        assertEquals(2, index.baconNumber(carol));

        List<PathStep> path = index.pathToBacon(carol);
        assertEquals(2, path.size());
        assertEquals("Carol", path.get(0).getFromActor());
        assertEquals("Movie E", path.get(0).getMovieTitle());
        assertEquals("Dave", path.get(0).getToActor());
        assertEquals("Kevin Bacon", path.get(1).getToActor());
    }

    @Test
    void pathStepsAreChained() {
        List<PathStep> path = index.pathToBacon(find("Bob"));
        for (int i = 1; i < path.size(); i++) {
            assertEquals(path.get(i - 1).getToActor(), path.get(i).getFromActor());
        }
    }

    @Test
    void disconnectedActorIsNotConnected() {
        ActorResult result = index.resultFor(find("Eve"));
        assertEquals(ActorResult.NOT_CONNECTED, result.getBaconNumber());
        assertTrue(result.getPath().isEmpty());
        assertEquals(5, index.reachableCount());
    }

    @Test
    void graphWithoutKevinBaconReturnsNotConnected() {
        ActorGraph other = new ActorGraph();
        Actor solo = other.getOrCreateActor("Solo");
        other.finishLoading();

        BaconIndex otherIndex = BaconIndex.build(other);
        assertEquals(ActorResult.NOT_CONNECTED, otherIndex.baconNumber(solo));
    }

    private Actor actor(String name) {
        return graph.getOrCreateActor(name);
    }

    private void cast(String title, Actor... actors) {
        Movie movie = graph.getOrCreateMovie(title, "2000");
        for (Actor actor : actors) {
            graph.connect(actor, movie);
        }
    }

    private Actor find(String name) {
        return graph.search(name).get(0);
    }
}
