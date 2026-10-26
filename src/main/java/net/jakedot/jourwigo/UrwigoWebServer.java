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
        server.createContext("/build-object", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String lua = handleBuildObjectRequest(body);
            respond(exchange, 200, "text/plain; charset=utf-8", lua);
        });
        server.start();
        return server;
    }

    static String handleBuildObjectRequest(String body) {
        if (body != null && body.trim().startsWith("{")) {
            return handleBuildObjectJson(body.trim());
        }
        Map<String, String> form = parseForm(body);
        String templateName = form.get("template");
        LuaObjectBuilder.Template template = LuaObjectBuilder.Template.fromDisplayName(templateName);

        LuaObjectBuilder builder;
        boolean hasExplicitVar = form.containsKey("varName");
        boolean hasExplicitCons = form.containsKey("constructor");

        if (hasExplicitVar || hasExplicitCons) {
            String varName = form.getOrDefault("varName", template.getDefaultVarName());
            String constructor = form.getOrDefault("constructor", template.getDefaultConstructor());
            builder = new LuaObjectBuilder(varName, constructor);
        } else {
            builder = LuaObjectBuilder.createForTemplate(template);
        }

        boolean hasExplicitProps = false;
        for (int i = 0; ; i++) {
            String k = form.get("propKey_" + i);
            if (k == null) {
                break;
            }
            hasExplicitProps = true;
            String v = form.getOrDefault("propVal_" + i, "");
            String t = form.getOrDefault("propType_" + i, "STRING");
            LuaObjectBuilder.PropertyType pType;
            try {
                pType = LuaObjectBuilder.PropertyType.valueOf(t.toUpperCase().trim());
            } catch (Exception e) {
                pType = LuaObjectBuilder.PropertyType.STRING;
            }
            builder.addProperty(k, v, pType);
        }

        if (!hasExplicitProps && !hasExplicitVar && !hasExplicitCons) {
            builder = LuaObjectBuilder.createForTemplate(template);
        }

        return builder.toLuaCode();
    }

    private static String handleBuildObjectJson(String json) {
        String templateName = extractJsonField(json, "template");
        LuaObjectBuilder.Template template = LuaObjectBuilder.Template.fromDisplayName(templateName);

        String varName = extractJsonField(json, "varName");
        if (varName == null || varName.isEmpty()) {
            varName = template.getDefaultVarName();
        }
        String constructor = extractJsonField(json, "constructor");
        if (constructor == null || constructor.isEmpty()) {
            constructor = template.getDefaultConstructor();
        }

        LuaObjectBuilder builder = new LuaObjectBuilder(varName, constructor);
        // Default properties from template
        LuaObjectBuilder templateDefaults = LuaObjectBuilder.createForTemplate(template);
        for (LuaObjectBuilder.LuaProperty prop : templateDefaults.getProperties()) {
            builder.addProperty(prop.getKey(), prop.getValue(), prop.getType());
        }
        return builder.toLuaCode();
    }

    private static String extractJsonField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int quoteStart = json.indexOf('"', colon + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
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
                input, select, textarea { width: 100%; box-sizing: border-box; margin: .3rem 0 .8rem; padding: .4rem; }
                button { padding: .45rem .8rem; margin-right: .5rem; cursor: pointer; }
                pre { white-space: pre-wrap; border: 1px solid #ddd; background: #fafafa; padding: .8rem; min-height: 120px; }
                .prop-row { display: flex; gap: .5rem; margin-bottom: .5rem; align-items: center; }
                .prop-row input, .prop-row select { margin: 0; }
                .prop-row button { margin: 0; padding: .3rem .6rem; }
              </style>
            </head>
            <body>
              <h1>jourwigo integrated player & editor</h1>
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

              <div class="card">
                <h2>Lua Object Builder</h2>
                <p>Construct any Lua object or Wherigo class instance (Zone, Character, Item, Task, Timer, Media, Input, Command, ZonePoint, Custom Object).</p>
                <form id="builder-form">
                  <label>Select Class / Template</label>
                  <select id="obj-template" name="template">
                    <option value="Zone">Zone</option>
                    <option value="Character">Character (ZCharacter)</option>
                    <option value="Item">Item (ZItem)</option>
                    <option value="Cartridge">Cartridge (ZCartridge)</option>
                    <option value="Task">Task (ZTask)</option>
                    <option value="Timer">Timer (ZTimer)</option>
                    <option value="Media">Media (ZMedia)</option>
                    <option value="Input">Input (ZInput)</option>
                    <option value="Command">Command (ZCommand)</option>
                    <option value="ZonePoint">ZonePoint</option>
                    <option value="Custom Object">Custom Object</option>
                  </select>

                  <label>Variable Name</label>
                  <input id="obj-var-name" name="varName" value="zoneStart" />

                  <label>Constructor / Expression</label>
                  <input id="obj-constructor" name="constructor" value="Wherigo.Zone(Cartridge)" />

                  <label>Properties</label>
                  <div id="obj-properties-container"></div>
                  <button type="button" id="add-prop-btn">+ Add Property</button>
                  <br /><br />
                  <button type="submit">Generate Lua Object Code</button>
                </form>
                <pre id="obj-output">(generated Lua object code appears here)</pre>
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

                const templates = {
                  'Zone': { varName: 'zoneStart', constructor: 'Wherigo.Zone(Cartridge)', props: [{k: 'Name', v: 'Start Zone', t: 'STRING'}, {k: 'Description', v: 'A starting zone.', t: 'STRING'}, {k: 'Visible', v: 'true', t: 'BOOLEAN'}, {k: 'Active', v: 'true', t: 'BOOLEAN'}, {k: 'Points', v: '{\\n    Wherigo.ZonePoint(0.0, 0.0, 0.0)\\n}', t: 'EXPRESSION'}] },
                  'Character': { varName: 'characterGuide', constructor: 'Wherigo.ZCharacter(Cartridge)', props: [{k: 'Name', v: 'Guide NPC', t: 'STRING'}, {k: 'Description', v: 'A friendly guide character.', t: 'STRING'}, {k: 'Gender', v: 'Male', t: 'STRING'}, {k: 'Visible', v: 'true', t: 'BOOLEAN'}] },
                  'Item': { varName: 'itemKey', constructor: 'Wherigo.ZItem(Cartridge)', props: [{k: 'Name', v: 'Golden Key', t: 'STRING'}, {k: 'Description', v: 'A shiny key.', t: 'STRING'}, {k: 'Visible', v: 'true', t: 'BOOLEAN'}] },
                  'Cartridge': { varName: 'cartridge', constructor: 'Wherigo.ZCartridge()', props: [{k: 'Name', v: 'New Cartridge', t: 'STRING'}, {k: 'Description', v: 'Cartridge description', t: 'STRING'}, {k: 'Author', v: 'Author', t: 'STRING'}, {k: 'Version', v: '1.0', t: 'STRING'}, {k: 'Activity', v: 'TourGuide', t: 'STRING'}] },
                  'Task': { varName: 'taskQuest', constructor: 'Wherigo.ZTask(Cartridge)', props: [{k: 'Name', v: 'Task Quest', t: 'STRING'}, {k: 'Description', v: 'Task description', t: 'STRING'}, {k: 'Active', v: 'true', t: 'BOOLEAN'}, {k: 'Complete', v: 'false', t: 'BOOLEAN'}] },
                  'Timer': { varName: 'timerCountdown', constructor: 'Wherigo.ZTimer(Cartridge)', props: [{k: 'Name', v: 'Test Timer', t: 'STRING'}, {k: 'Type', v: 'Countdown', t: 'STRING'}, {k: 'Duration', v: '10', t: 'NUMBER'}] },
                  'Media': { varName: 'mediaInfo', constructor: 'Wherigo.ZMedia(Cartridge)', props: [{k: 'Name', v: 'Sample Media', t: 'STRING'}, {k: 'Filename', v: 'sample.mp3', t: 'STRING'}, {k: 'Type', v: 'mp3', t: 'STRING'}, {k: 'AltText', v: 'Audio description', t: 'STRING'}] },
                  'Input': { varName: 'inputPrompt', constructor: 'Wherigo.ZInput(Cartridge)', props: [{k: 'Name', v: 'Passcode Prompt', t: 'STRING'}, {k: 'Question', v: 'Enter passcode:', t: 'STRING'}, {k: 'InputType', v: 'Text', t: 'STRING'}] },
                  'Command': { varName: 'cmdExamine', constructor: 'Wherigo.ZCommand(Cartridge)', props: [{k: 'Name', v: 'Examine', t: 'STRING'}, {k: 'Text', v: 'Examine object', t: 'STRING'}, {k: 'Enabled', v: 'true', t: 'BOOLEAN'}] },
                  'ZonePoint': { varName: 'zonePoint', constructor: 'Wherigo.ZonePoint(0.0, 0.0, 0.0)', props: [] },
                  'Custom Object': { varName: 'customObj', constructor: '{}', props: [] }
                };

                const templateSelect = document.getElementById('obj-template');
                const varNameInput = document.getElementById('obj-var-name');
                const constructorInput = document.getElementById('obj-constructor');
                const propsContainer = document.getElementById('obj-properties-container');
                const addPropBtn = document.getElementById('add-prop-btn');
                const builderForm = document.getElementById('builder-form');
                const objOutput = document.getElementById('obj-output');

                function renderPropertyRow(key = '', val = '', type = 'STRING') {
                  const row = document.createElement('div');
                  row.className = 'prop-row';
                  row.innerHTML = `
                    <input style="flex:1" placeholder="Key" class="prop-key" value="${key.replace(/"/g, '&quot;')}" />
                    <select class="prop-type" style="width:120px">
                      <option value="STRING" ${type === 'STRING' ? 'selected' : ''}>STRING</option>
                      <option value="NUMBER" ${type === 'NUMBER' ? 'selected' : ''}>NUMBER</option>
                      <option value="BOOLEAN" ${type === 'BOOLEAN' ? 'selected' : ''}>BOOLEAN</option>
                      <option value="EXPRESSION" ${type === 'EXPRESSION' ? 'selected' : ''}>EXPRESSION</option>
                    </select>
                    <input style="flex:2" placeholder="Value" class="prop-val" value="${val.replace(/"/g, '&quot;')}" />
                    <button type="button" class="remove-btn">X</button>
                  `;
                  row.querySelector('.remove-btn').addEventListener('click', () => row.remove());
                  propsContainer.appendChild(row);
                }

                function loadTemplate(name) {
                  const tmpl = templates[name] || templates['Custom Object'];
                  varNameInput.value = tmpl.varName;
                  constructorInput.value = tmpl.constructor;
                  propsContainer.innerHTML = '';
                  tmpl.props.forEach(p => renderPropertyRow(p.k, p.v, p.t));
                }

                templateSelect.addEventListener('change', (e) => loadTemplate(e.target.value));
                addPropBtn.addEventListener('click', () => renderPropertyRow('', '', 'STRING'));

                builderForm.addEventListener('submit', async (e) => {
                  e.preventDefault();
                  const params = new URLSearchParams();
                  params.append('template', templateSelect.value);
                  params.append('varName', varNameInput.value);
                  params.append('constructor', constructorInput.value);

                  const rows = propsContainer.querySelectorAll('.prop-row');
                  rows.forEach((row, i) => {
                    params.append('propKey_' + i, row.querySelector('.prop-key').value);
                    params.append('propType_' + i, row.querySelector('.prop-type').value);
                    params.append('propVal_' + i, row.querySelector('.prop-val').value);
                  });

                  const resp = await fetch('/build-object', {
                    method: 'POST',
                    headers: {'content-type':'application/x-www-form-urlencoded'},
                    body: params.toString()
                  });
                  objOutput.textContent = await resp.text();
                });

                // Load initial template
                loadTemplate('Zone');
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
