package se.zavodnik.bacon.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;
import se.zavodnik.bacon.model.Movie;

class BaconServiceTest {

    private BaconService service;

    @BeforeEach
    void setUp() {
        ActorGraph graph = new ActorGraph();
        Actor bacon = graph.getOrCreateActor("Kevin Bacon");
        Actor oBrien = graph.getOrCreateActor("Conan O'Brien");

        Movie show = graph.getOrCreateMovie("Late Show", "2010");
        graph.connect(bacon, show);
        graph.connect(oBrien, show);
        graph.finishLoading();

        service = new BaconService(graph, BaconIndex.build(graph));
    }

    @Test
    void searchIgnoresCaseSpacesAndApostrophes() {
        assertEquals(1, service.search("  CONAN   obrien ").size());
        assertEquals(1, service.search("Conan O'Brien").get(0).getBaconNumber());
    }

    @Test
    void unknownOrEmptyNameGivesNoResults() {
        assertTrue(service.search("Nobody Here").isEmpty());
        assertTrue(service.search("   ").isEmpty());
        assertTrue(service.search(null).isEmpty());
    }
}
