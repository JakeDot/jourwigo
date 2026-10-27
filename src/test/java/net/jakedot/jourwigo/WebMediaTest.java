package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.Cartridge;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.WebMedia;
import cgeo.geocaching.wherigo.openwig.WherigoLib;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WebMediaTest {

    private TestUI testUi;

    @BeforeEach
    void setUp() {
        testUi = new TestUI();
        Engine.ui = testUi;
        Engine.gps = new TestLocationService();
        Engine.instance = new TestEngine();
        Engine.state = new LuaState(System.out);

        BaseLib.register(Engine.state);
        LuaTableImpl pack = new LuaTableImpl();
        pack.rawset("loaded", new LuaTableImpl());
        Engine.state.getEnvironment().rawset("package", pack);

        WherigoLib.register(Engine.state);
    }

    @Test
    void testDefaultsAndConstructors() {
        WebMedia empty = new WebMedia();
        assertNull(empty.getUri());
        assertEquals("text/plain", empty.getMimeType());

        WebMedia uriOnly = new WebMedia("http://example.com/test.txt");
        assertEquals("http://example.com/test.txt", uriOnly.getUri());
        assertEquals("text/plain", uriOnly.getMimeType());

        WebMedia full = new WebMedia("http://example.com/data.json", "application/json");
        assertEquals("http://example.com/data.json", full.getUri());
        assertEquals("application/json", full.getMimeType());
    }

    @Test
    void testLuaPropertyAccess() {
        WebMedia wm = new WebMedia();
        wm.rawset("Uri", "http://example.com/api");
        assertEquals("http://example.com/api", wm.getUri());
        assertEquals("http://example.com/api", wm.rawget("Uri"));
        assertEquals("http://example.com/api", wm.rawget("URL"));

        wm.rawset("MimeType", "text/html");
        assertEquals("text/html", wm.getMimeType());
        assertEquals("text/html", wm.rawget("MimeType"));
    }

    @Test
    void testReadTextLocalUrl(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("content.txt");
        Files.writeString(file, "Hello from local URI");

        WebMedia wm = new WebMedia(file.toUri().toString());
        assertEquals("Hello from local URI", wm.readText());
    }

    @Test
    void testReadTextInvalidUri() {
        WebMedia wm = new WebMedia("invalid-scheme://nonexistent-host-12345/data");
        assertNull(wm.readText());
    }

    @Test
    void testSerialization() throws IOException {
        WebMedia original = new WebMedia("http://example.com/resource.xml", "application/xml");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        original.serialize(out);
        out.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);

        WebMedia deserialized = new WebMedia();
        deserialized.deserialize(in);

        assertEquals("http://example.com/resource.xml", deserialized.getUri());
        assertEquals("application/xml", deserialized.getMimeType());
    }

    @Test
    void testLuaInstantiationAndMade() {
        Object resObj = Engine.state.call(
                (cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) Engine.state.getEnvironment().rawget("WebMedia"),
                "http://example.org/test.txt", "text/plain", null);

        assertTrue(resObj instanceof WebMedia);
        WebMedia wm = (WebMedia) resObj;
        assertEquals("http://example.org/test.txt", wm.getUri());
        assertEquals("text/plain", wm.getMimeType());

        // Test Wherigo.made(Wherigo.WebMedia, wm)
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable wigTable =
                (cgeo.geocaching.wherigo.kahlua.vm.LuaTable) Engine.state.getEnvironment().rawget("Wherigo");
        cgeo.geocaching.wherigo.kahlua.vm.JavaFunction madeFunc =
                (cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) wigTable.rawget("made");
        Object madeRes = Engine.state.call(madeFunc, WherigoLib.WEBMEDIA, wm, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    private static class TestEngine extends Engine {
        TestEngine() {
            super();
            this.savegame = new cgeo.geocaching.wherigo.openwig.formats.Savegame();
            this.gwcfile = new cgeo.geocaching.wherigo.openwig.formats.CartridgeFile();
            this.gwcfile.device = "Desktop";
            this.cartridge = new Cartridge();
        }
    }

    private static class TestUI implements UI {
        @Override public void refresh() {}
        @Override public void start() {}
        @Override public void end() {}
        @Override public void showError(String msg) {}
        @Override public void debugMsg(String msg) {}
        @Override public void setStatusText(String text) {}
        @Override public void pushDialog(String[] texts, Media[] media, String button1, String button2, cgeo.geocaching.wherigo.kahlua.vm.LuaClosure callback) {}
        @Override public void pushInput(cgeo.geocaching.wherigo.openwig.EventTable input) {}
        @Override public void showScreen(ScreenId screenId, cgeo.geocaching.wherigo.openwig.EventTable details) {}
        @Override public void playSound(byte[] data, String mime) {}
        @Override public void blockForSaving() {}
        @Override public void unblock() {}
        @Override public void command(String cmd) {}
        @Override public String snapPhoto() { return null; }
        @Override public String pickFile(String mimeType) { return null; }
    }

    private static class TestLocationService implements LocationService {
        @Override public double getLatitude() { return 0; }
        @Override public double getLongitude() { return 0; }
        @Override public double getAltitude() { return 0; }
        @Override public double getHeading() { return 0; }
        @Override public double getPrecision() { return 0; }
        @Override public int getState() { return ONLINE; }
        @Override public void connect() {}
        @Override public void disconnect() {}
    }
}
