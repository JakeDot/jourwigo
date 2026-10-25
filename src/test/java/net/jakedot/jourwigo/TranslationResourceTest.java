package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.Cartridge;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.TranslationResource;
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

class TranslationResourceTest {

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
    void testDefaultLanguageFromFirstTripletLine() {
        String csv = "welcome,de,Willkommen\n" +
                     "welcome,en,Welcome\n" +
                     "goodbye,de,Auf Wiedersehen\n" +
                     "goodbye,en,Goodbye\n";
        TranslationResource resource = new TranslationResource(csv);
        assertEquals("de", resource.getDefaultLanguage());
        assertEquals("Willkommen", resource.translate("welcome"));
        assertEquals("Welcome", resource.translate("welcome", "en"));
        // Request 'fr' which is missing -> falls back to default language 'de' -> 'Auf Wiedersehen'
        assertEquals("Auf Wiedersehen", resource.translate("goodbye", "fr"));
    }

    @Test
    void testTranslationFallback() {
        String csv = "greeting,en,Hello\n" +
                     "greeting,de,Hallo\n" +
                     "farewell,de,Tschüss\n" +
                     "thanks,en,Thank you\n";
        TranslationResource resource = new TranslationResource(csv);

        // First triplet has 'en', so default language is 'en'
        assertEquals("en", resource.getDefaultLanguage());

        // Exact match
        assertEquals("Hello", resource.translate("greeting", "en"));
        assertEquals("Hallo", resource.translate("greeting", "de"));

        // Missing key for requested language 'de' -> falls back to default language 'en'
        assertEquals("Thank you", resource.translate("thanks", "de"));

        // Default language lookup when lang is null or omitted
        assertEquals("Hello", resource.translate("greeting"));
        assertEquals("Thank you", resource.translate("thanks"));

        // Missing key entirely
        assertNull(resource.translate("nonexistent"));
        assertNull(resource.translate("nonexistent", "en"));
    }

    @Test
    void testCsvParsingSemicolonAndQuotes() {
        String csv = "\"welcome\";\"fr\";\"Bienvenue, tout le monde!\"\n" +
                     "welcome;en;\"Welcome \"\"everyone\"\"!\"\n" +
                     "farewell;fr;Au revoir";

        TranslationResource resource = new TranslationResource(csv);
        assertEquals("fr", resource.getDefaultLanguage());
        assertEquals("Bienvenue, tout le monde!", resource.translate("welcome", "fr"));
        assertEquals("Welcome \"everyone\"!", resource.translate("welcome", "en"));
        assertEquals("Au revoir", resource.translate("farewell", "de")); // falls back to default 'fr'
    }

    @Test
    void testMediaSourceConstructor(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("translations.csv");
        Files.writeString(csvFile, "item_key,es,Hola\nitem_key,en,Hello\n");

        Media media = new Media(csvFile.toString(), "csv");
        TranslationResource resource = new TranslationResource(media);

        assertEquals("es", resource.getDefaultLanguage());
        assertEquals("Hola", resource.translate("item_key"));
        assertEquals("Hello", resource.translate("item_key", "en"));
    }

    @Test
    void testLuaCallingTranslateDotAndColonSyntax() {
        String csv = "btn_ok,en,OK\n" +
                     "btn_ok,de,Ja\n" +
                     "btn_cancel,en,Cancel\n";

        // Create TranslationResource in Lua via Wherigo.TranslationResource
        Object resObj = Engine.state.call(
                (cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) Engine.state.getEnvironment().rawget("TranslationResource"),
                csv, null, null);

        assertTrue(resObj instanceof TranslationResource);
        TranslationResource resource = (TranslationResource) resObj;
        assertEquals("en", resource.getDefaultLanguage());

        // Call resource.translate("btn_ok", "de") from Lua
        cgeo.geocaching.wherigo.kahlua.vm.JavaFunction translateFunc =
                (cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) resource.rawget("translate");

        // Dot syntax: resource.translate("btn_ok", "de")
        Object dotRes = Engine.state.call(translateFunc, "btn_ok", "de", null);
        assertEquals("Ja", dotRes);

        // Colon syntax: resource:translate("btn_ok", "de") -> passes resource as first arg
        Object colonRes = Engine.state.call(translateFunc, resource, "btn_ok", "de");
        assertEquals("Ja", colonRes);

        // Colon syntax fallback: resource:translate("btn_cancel", "de") -> falls back to 'en'
        Object fallbackRes = Engine.state.call(translateFunc, resource, "btn_cancel", "de");
        assertEquals("Cancel", fallbackRes);
    }

    @Test
    void testSerialization() throws IOException {
        String csv = "key1,en,Value1\nkey1,de,Wert1\nkey2,en,Value2\n";
        TranslationResource original = new TranslationResource(csv);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        original.serialize(out);
        out.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);

        TranslationResource deserialized = new TranslationResource();
        deserialized.deserialize(in);

        assertEquals("en", deserialized.getDefaultLanguage());
        assertEquals("Value1", deserialized.translate("key1", "en"));
        assertEquals("Wert1", deserialized.translate("key1", "de"));
        assertEquals("Value2", deserialized.translate("key2", "fr"));
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
