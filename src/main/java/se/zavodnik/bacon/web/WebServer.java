package se.zavodnik.bacon.web;

import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import se.zavodnik.bacon.images.ActorImageService;
import se.zavodnik.bacon.search.BaconService;

/**
 * HTTP layer: serves the frontend and a small JSON API.
 *
 * <ul>
 *   <li>{@code GET /api/bacon?actor=Kevin Hart} – Bacon number and path for every matching actor</li>
 *   <li>{@code GET /api/actor-image?name=Kevin Hart} – image URL as plain text, 404 if none</li>
 *   <li>{@code GET /api/stats} – size of the loaded dataset</li>
 * </ul>
 */
public final class WebServer {

    private WebServer() {
    }

    public static Javalin create(BaconService baconService, ActorImageService images, Map<String, Object> stats) {
        return Javalin.create(config -> {

            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "public";
                staticFiles.location = Location.CLASSPATH;
            });

            config.routes.get("/api/bacon", ctx -> {
                String actor = ctx.queryParam("actor");
                if (actor == null || actor.isBlank()) {
                    ctx.status(400);
                    ctx.result("Query parameter 'actor' is required.");
                    return;
                }
                ctx.json(baconService.search(actor));
            });

            config.routes.get("/api/actor-image", ctx -> {
                String name = ctx.queryParam("name");
                if (name == null || name.isBlank()) {
                    ctx.status(400);
                    ctx.result("Query parameter 'name' is required.");
                    return;
                }
                String url = images.imageUrl(name);
                if (url == null) {
                    ctx.status(404);
                    ctx.result("No image found.");
                    return;
                }
                ctx.result(url);
            });

            config.routes.get("/api/stats", ctx -> ctx.json(stats));
        });
    }
}
