package se.zavodnik.bacon.search;

import java.util.List;

import se.zavodnik.bacon.model.ActorGraph;

/**
 * Entry point for searches: finds the actor matching a name and returns the Bacon result.
 */
public class BaconService {

    private final ActorGraph graph;
    private final BaconIndex index;

    public BaconService(ActorGraph graph, BaconIndex index) {
        this.graph = graph;
        this.index = index;
    }

    /** Results for the actor with the given name, or an empty list if there is none. */
    public List<ActorResult> search(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return graph.search(name).stream()
                .map(index::resultFor)
                .toList();
    }
}
