package se.zavodnik.bacon.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import se.zavodnik.bacon.model.Actor;
import se.zavodnik.bacon.model.ActorGraph;

class DataLoaderTest {

    private static final String SAMPLE = """
            <a>Bacon, Kevin (I)
            <t>Footloose (1984)
            <t>Apollo 13 (1995/I)
            <a>Hanks, Tom
            <t>Apollo 13 (1995/I)
            <a>Cher
            <t>Unfinished Project (????)
            <a>Bacon, Kevin (II)
            <t>Short Film (2010)
            """;

    @Test
    void parsesDisplayNames() {
        assertEquals("Kevin Bacon", DataLoader.displayName("Bacon, Kevin (I)"));
        assertEquals("Tom Hanks", DataLoader.displayName("Hanks, Tom"));
        assertEquals("Cher", DataLoader.displayName("Cher"));
    }

    @Test
    void parsesTitlesAndYears() {
        assertEquals("Footloose", DataLoader.title("Footloose (1984)"));
        assertEquals("1984", DataLoader.year("Footloose (1984)"));
        assertEquals("1995", DataLoader.year("Apollo 13 (1995/I)"));
        assertEquals("unknown", DataLoader.year("Unfinished Project (????)"));
        assertEquals("unknown", DataLoader.year("No Year"));
    }

    @Test
    void buildsGraphWithSharedMovies() throws IOException {
        ActorGraph graph = load(SAMPLE, false);

        assertEquals(3, graph.actorCount());
        assertEquals(4, graph.movieCount());

        Actor tom = graph.search("tom hanks").get(0);
        Actor kevin = graph.search("Kevin Bacon").get(0);
        assertEquals(1, tom.getMovies().size());
        assertTrue(kevin.getMovies().contains(tom.getMovies().get(0)), "Apollo 13 should be shared");
    }

    @Test
    void mergesEntriesWithTheSameName() throws IOException {
        ActorGraph graph = load(SAMPLE, false);

        assertEquals(1, graph.search("Kevin Bacon").size());
        assertEquals(3, graph.search("Kevin Bacon").get(0).getMovies().size());
    }

    @Test
    void readsGzippedData() throws IOException {
        ActorGraph graph = load(SAMPLE, true);
        assertEquals(3, graph.actorCount());
    }

    @Test
    void loadsBundledDemoDataset() throws IOException {
        ActorGraph graph = Dataset.demo().graph();
        assertTrue(graph.actorCount() > 50);
        assertEquals(1, graph.search("Kevin Bacon").size());
    }

    private static ActorGraph load(String text, boolean gzip) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (gzip) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (GZIPOutputStream out = new GZIPOutputStream(buffer)) {
                out.write(bytes);
            }
            bytes = buffer.toByteArray();
        }
        return DataLoader.load(new ByteArrayInputStream(bytes), gzip);
    }
}
