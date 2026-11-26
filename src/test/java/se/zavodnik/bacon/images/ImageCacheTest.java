package se.zavodnik.bacon.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageCacheTest {

    private static ImageCache cache(String tsv) throws IOException {
        return new ImageCache(ImageCache.parse(new ByteArrayInputStream(tsv.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void returnsCachedPath() throws IOException {
        ImageCache cache = cache("Kevin Bacon\t/bacon.jpg\nTom Hanks\t/hanks.jpg\n");
        assertEquals("/bacon.jpg", cache.profilePath("Kevin Bacon"));
        assertEquals(2, cache.size());
    }

    @Test
    void emptyPathMeansKnownWithoutImage() throws IOException {
        ImageCache cache = cache("Unknown Extra\t\n");
        assertTrue(cache.contains("Unknown Extra"));
        assertNull(cache.profilePath("Unknown Extra"));
    }

    @Test
    void missingNameIsNotContained() throws IOException {
        ImageCache cache = cache("Kevin Bacon\t/bacon.jpg\n");
        assertFalse(cache.contains("Tom Hanks"));
    }

    @Test
    void serviceUsesCacheWithoutToken() throws IOException {
        ActorImageService service = new ActorImageService(cache("Kevin Bacon\t/bacon.jpg\nNo Image\t\n"), null);
        assertEquals(TmdbClient.IMAGE_BASE_URL + "/bacon.jpg", service.imageUrl("Kevin Bacon"));
        assertNull(service.imageUrl("No Image"));
        assertNull(service.imageUrl("Not Cached"));
    }
}
