package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.MediaPickerLib;
import cgeo.geocaching.wherigo.openwig.WherigoLib;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaPickerLibTest {

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
    void testSnapPhoto() {
        Object snapPhoto = Engine.state.getEnvironment().rawget("Wherigo");
        assertNotNull(snapPhoto);
        Object res = Engine.state.call(MediaPickerLib.SNAP_PHOTO, null, null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.ImageMedia);
        cgeo.geocaching.wherigo.openwig.ImageMedia media = (cgeo.geocaching.wherigo.openwig.ImageMedia) res;
        assertEquals("/tmp/photo.jpg", media.getFilePath());
        assertEquals("/tmp/photo.jpg", media.rawget("Filename"));
        assertEquals("jpg", media.rawget("Type"));

        // Verify calling show / play method on ImageMedia
        testUi.lastDialogTexts = null;
        media.show();
        assertNotNull(testUi.lastDialogTexts);
    }

    @Test
    void testPickFile_Image() {
        testUi.nextPickFileReturn = "/tmp/picked_file.png";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "image/png", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.ImageMedia);
        cgeo.geocaching.wherigo.openwig.ImageMedia media = (cgeo.geocaching.wherigo.openwig.ImageMedia) res;
        assertEquals("/tmp/picked_file.png", media.getFilePath());
        assertEquals("png", media.rawget("Type"));
    }

    @Test
    void testPickFile_Audio() {
        testUi.nextPickFileReturn = "/tmp/sound.mp3";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "audio/mpeg", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.AudioMedia);
        cgeo.geocaching.wherigo.openwig.AudioMedia media = (cgeo.geocaching.wherigo.openwig.AudioMedia) res;
        assertEquals("/tmp/sound.mp3", media.getFilePath());
        assertEquals("mp3", media.rawget("Type"));

        // Verify stop command
        testUi.lastCommand = null;
        media.stop();
        assertEquals("StopSound", testUi.lastCommand);
    }

    @Test
    void testPickFile_Video() {
        testUi.nextPickFileReturn = "/tmp/video.mp4";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "video/mp4", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.VideoMedia);
        cgeo.geocaching.wherigo.openwig.VideoMedia media = (cgeo.geocaching.wherigo.openwig.VideoMedia) res;
        assertEquals("/tmp/video.mp4", media.getFilePath());
        assertEquals("mp4", media.rawget("Type"));
    }

    @Test
    void testPickFile_Text() throws java.io.IOException {
        java.io.File tempFile = java.io.File.createTempFile("test_media", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), "Hello Wherigo Text!");

        testUi.nextPickFileReturn = tempFile.getAbsolutePath();
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "text/plain", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.TextMedia);
        cgeo.geocaching.wherigo.openwig.TextMedia media = (cgeo.geocaching.wherigo.openwig.TextMedia) res;
        assertEquals(tempFile.getAbsolutePath(), media.getFilePath());
        assertEquals("txt", media.rawget("Type"));
        assertEquals("Hello Wherigo Text!", media.readText());
        assertEquals("Hello Wherigo Text!", media.rawget("Text"));

        // Test calling Read Lua function
        Object readFunc = media.rawget("Read");
        assertTrue(readFunc instanceof cgeo.geocaching.wherigo.kahlua.vm.JavaFunction);
        Object readRes = Engine.state.call((cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) readFunc, media, null, null);
        assertEquals("Hello Wherigo Text!", readRes);
    }

    @Test
    void testPickFile_Null() {
        testUi.nextPickFileReturn = null;
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "image/*", null, null);
        assertNull(res);
    }

    private static class TestEngine extends Engine {
        TestEngine() {
            super();
            this.savegame = new cgeo.geocaching.wherigo.openwig.formats.Savegame();
            this.gwcfile = new cgeo.geocaching.wherigo.openwig.formats.CartridgeFile();
            this.gwcfile.device = "Desktop";
        }
    }

    private static class TestUI implements UI {
        String nextPickFileReturn = "/tmp/picked_file.jpg";
        String[] lastDialogTexts;
        String lastCommand;

        @Override public void refresh() {}
        @Override public void start() {}
        @Override public void end() {}
        @Override public void showError(String msg) {}
        @Override public void debugMsg(String msg) {}
        @Override public void setStatusText(String text) {}
        @Override public void pushDialog(String[] texts, cgeo.geocaching.wherigo.openwig.Media[] media, String button1, String button2, cgeo.geocaching.wherigo.kahlua.vm.LuaClosure callback) {
            this.lastDialogTexts = texts;
        }
        @Override public void pushInput(cgeo.geocaching.wherigo.openwig.EventTable input) {}
        @Override public void showScreen(ScreenId screenId, cgeo.geocaching.wherigo.openwig.EventTable details) {}
        @Override public void playSound(byte[] data, String mime) {}
        @Override public void blockForSaving() {}
        @Override public void unblock() {}
        @Override public void command(String cmd) {
            this.lastCommand = cmd;
        }

        @Override
        public String snapPhoto() {
            return "/tmp/photo.jpg";
        }

        @Override
        public String pickFile(String mimeType) {
            return nextPickFileReturn;
        }
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
