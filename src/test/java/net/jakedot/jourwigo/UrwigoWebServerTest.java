package net.jakedot.jourwigo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UrwigoWebServerTest {

    private HttpServer server;
    private int port;
    private HttpClient client;

    @BeforeEach
    public void setUp() throws IOException {
        port = findFreePort();
        server = UrwigoWebServer.start(port);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testIndexPageGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("jourwigo integrated player & editor"));
        assertTrue(response.body().contains("Lua Object Builder"));
    }

    @Test
    public void testCartridgeTemplatePost() throws Exception {
        String body = "name=My+Test+Cartridge&author=Tester&version=1.2.3&description=Sample+Desc&latitude=51.5&longitude=-0.1&altitude=10.0";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/template"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Cartridge.Name = \"My Test Cartridge\""));
        assertTrue(response.body().contains("Cartridge.Author = \"Tester\""));
    }

    @Test
    public void testBuildObjectFormPost() throws Exception {
        String body = "template=Item&varName=keyItem&constructor=Wherigo.ZItem(Cartridge)&propKey_0=Name&propVal_0=Golden+Key&propType_0=STRING&propKey_1=Visible&propVal_1=true&propType_1=BOOLEAN";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/build-object"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String lua = response.body();
        assertTrue(lua.contains("keyItem = Wherigo.ZItem(Cartridge)"));
        assertTrue(lua.contains("keyItem.Name = \"Golden Key\""));
        assertTrue(lua.contains("keyItem.Visible = true"));
    }

    @Test
    public void testBuildObjectJsonPost() throws Exception {
        String json = "{\"template\":\"Zone\",\"varName\":\"zoneMain\",\"constructor\":\"Wherigo.Zone(Cartridge)\"}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/build-object"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String lua = response.body();
        assertTrue(lua.contains("zoneMain = Wherigo.Zone(Cartridge)"));
        assertTrue(lua.contains("zoneMain.Name = \"Start Zone\""));
    }

    @Test
    public void testCompileCartridgeLuaPost() throws Exception {
        String body = "name=Compiled+Web+Cartridge&author=WebAuthor&template=Item&varName=webItem&constructor=Wherigo.ZItem(Cartridge)&propKey_0=Name&propVal_0=Web+Key&propType_0=STRING";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/compile-cartridge"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String lua = response.body();
        assertTrue(lua.contains("Cartridge.Name = \"Compiled Web Cartridge\""));
        assertTrue(lua.contains("webItem = Wherigo.ZItem(Cartridge)"));
        assertTrue(lua.contains("webItem.Name = \"Web Key\""));
    }

    @Test
    public void testCompileCartridgeGwcPost() throws Exception {
        String body = "name=Compiled+Gwc+Cartridge&author=WebAuthor&template=Zone&varName=startZone&format=gwc";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/compile-cartridge"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        byte[] bytes = response.body();
        assertTrue(bytes.length > 20);
        assertEquals(0x02, bytes[0]);
        assertEquals(0x0a, bytes[1]);
        assertEquals('C', (char) bytes[2]);
    }

    @Test
    public void testParseFormHelper() {
        Map<String, String> form = UrwigoWebServer.parseForm("a=1&b=hello%20world&c=");
        assertEquals("1", form.get("a"));
        assertEquals("hello world", form.get("b"));
        assertEquals("", form.get("c"));
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8085;
        }
    }
}
