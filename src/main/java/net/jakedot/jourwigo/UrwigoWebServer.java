package net.jakedot.jourwigo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Lightweight integrated web editor/player shell using the cgeo/cgeo Wherigo runtime backend.
 */
public final class UrwigoWebServer {

    private UrwigoWebServer() {
        // utility class
    }

    public static void startAndBlock(int port) throws IOException, InterruptedException {
        HttpServer server = start(port);
        System.out.println("jourwigo integrated web server running at http://localhost:" + port);
        new CountDownLatch(1).await();
        server.stop(0);
    }

    public static HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> respond(exchange, 200, "text/html; charset=utf-8", indexPage()));
        server.createContext("/template", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseForm(body);
            String lua = CartridgeTemplateWriter.buildLuaTemplate(
                form.getOrDefault("name", "New Cartridge"),
                form.getOrDefault("author", "Author"),
                form.getOrDefault("version", "1.0"),
                form.getOrDefault("description", "Cartridge description"),
                parseDouble(form.get("latitude"), 0.0),
                parseDouble(form.get("longitude"), 0.0),
                parseDouble(form.get("altitude"), 0.0)
            );
            respond(exchange, 200, "text/plain; charset=utf-8", lua);
        });
        server.start();
        return server;
    }

    static Map<String, String> parseForm(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            String key = decode(eq >= 0 ? pair.substring(0, eq) : pair);
            String value = decode(eq >= 0 ? pair.substring(eq + 1) : "");
            values.put(key, value);
        }
        return values;
    }

    static String indexPage() {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <title>jourwigo integrated editor/player</title>
              <style>
                body { font-family: sans-serif; margin: 1.2rem; max-width: 980px; }
                .card { border: 1px solid #ccc; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
                input, textarea { width: 100%%; box-sizing: border-box; margin: .3rem 0 .8rem; padding: .4rem; }
                button { padding: .45rem .8rem; }
                pre { white-space: pre-wrap; border: 1px solid #ddd; background: #fafafa; padding: .8rem; min-height: 200px; }
              </style>
            </head>
            <body>
              <h1>jourwigo integrated player</h1>
              <p>Uses Wherigo runtime code based on the cgeo/cgeo GitHub repository and exposes an Urwigo-style editor workflow.</p>
              <div class="card">
                <h2>Create cartridge template</h2>
                <form id="template-form">
                  <label>Name</label><input name="name" value="New Cartridge" />
                  <label>Author</label><input name="author" value="Author" />
                  <label>Version</label><input name="version" value="1.0" />
                  <label>Description</label><textarea name="description">Cartridge description</textarea>
                  <label>Latitude</label><input name="latitude" value="0.0" />
                  <label>Longitude</label><input name="longitude" value="0.0" />
                  <label>Altitude</label><input name="altitude" value="0.0" />
                  <button type="submit">Generate Lua template</button>
                </form>
                <pre id="output">(template output appears here)</pre>
              </div>
              <script>
                const form = document.getElementById('template-form');
                const output = document.getElementById('output');
                form.addEventListener('submit', async (e) => {
                  e.preventDefault();
                  const body = new URLSearchParams(new FormData(form)).toString();
                  const resp = await fetch('/template', { method: 'POST', headers: {'content-type':'application/x-www-form-urlencoded'}, body });
                  output.textContent = await resp.text();
                });
              </script>
            </body>
            </html>
            """;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String decode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
