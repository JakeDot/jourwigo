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
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals(cgeo.geocaching.wherigo.openwig.Media.class, media.getClass());
        assertEquals("/tmp/photo.jpg", media.getFilePath());
        assertEquals("/tmp/photo.jpg", media.rawget("Filename"));
        assertEquals("jpg", media.rawget("Type"));

        // Verify Wherigo.made(Wherigo.ZMedia, media) returns true
        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);

        // Verify calling show method on Media
        testUi.lastDialogTexts = null;
        media.show();
        assertNotNull(testUi.lastDialogTexts);
    }

    @Test
    void testPickFile_Image() {
        testUi.nextPickFileReturn = "/tmp/picked_file.png";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "image/png", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals(cgeo.geocaching.wherigo.openwig.Media.class, media.getClass());
        assertEquals("/tmp/picked_file.png", media.getFilePath());
        assertEquals("png", media.rawget("Type"));

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testPickFile_Audio() {
        testUi.nextPickFileReturn = "/tmp/sound.mp3";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "audio/mpeg", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals("/tmp/sound.mp3", media.getFilePath());
        assertEquals("mp3", media.rawget("Type"));

        // Verify stop command
        testUi.lastCommand = null;
        media.stop();
        assertEquals("StopSound", testUi.lastCommand);

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testPickFile_Video() {
        testUi.nextPickFileReturn = "/tmp/video.mp4";
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "video/mp4", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals("/tmp/video.mp4", media.getFilePath());
        assertEquals("mp4", media.rawget("Type"));

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testPickFile_Text() throws java.io.IOException {
        java.io.File tempFile = java.io.File.createTempFile("test_media", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), "Hello Wherigo Text!");

        testUi.nextPickFileReturn = tempFile.getAbsolutePath();
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "text/plain", null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals(tempFile.getAbsolutePath(), media.getFilePath());
        assertEquals("txt", media.rawget("Type"));
        assertEquals("Hello Wherigo Text!", media.readText());
        assertEquals("Hello Wherigo Text!", media.rawget("Text"));

        // Test calling Read Lua function
        Object readFunc = media.rawget("Read");
        assertTrue(readFunc instanceof cgeo.geocaching.wherigo.kahlua.vm.JavaFunction);
        Object readRes = Engine.state.call((cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) readFunc, media, null, null);
        assertEquals("Hello Wherigo Text!", readRes);

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testPickFile_Null() {
        testUi.nextPickFileReturn = null;
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "image/*", null, null);
        assertNull(res);
    }

    @Test
    void testRecordAudio() {
        testUi.nextRecordAudioReturn = "/tmp/recorded_voice.wav";
        Object res = Engine.state.call(MediaPickerLib.RECORD_AUDIO, null, null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals("/tmp/recorded_voice.wav", media.getFilePath());
        assertEquals("wav", media.rawget("Type"));

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testRecordVideo() {
        testUi.nextRecordVideoReturn = "/tmp/recorded_clip.mp4";
        Object res = Engine.state.call(MediaPickerLib.RECORD_VIDEO, null, null, null);
        assertTrue(res instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) res;
        assertEquals("/tmp/recorded_clip.mp4", media.getFilePath());
        assertEquals("mp4", media.rawget("Type"));

        Object madeRes = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, madeRes);
    }

    @Test
    void testLuaCartridge_MediaPickerAndCharacterPicture() {
        cgeo.geocaching.wherigo.openwig.Cartridge cartridge = new cgeo.geocaching.wherigo.openwig.Cartridge();
        Engine.instance.cartridge = cartridge;

        // 1. Create a character in Lua via Wherigo.ZCharacter
        Object characterObj = Engine.state.call(WherigoLib.ZCHARACTER, cartridge, null, null);
        assertTrue(characterObj instanceof cgeo.geocaching.wherigo.openwig.Thing);
        cgeo.geocaching.wherigo.openwig.Thing character = (cgeo.geocaching.wherigo.openwig.Thing) characterObj;
        character.name = "Detective Bob";

        // 2. Request a file picker in Lua via Wherigo.PickFile
        testUi.nextPickFileReturn = "/tmp/bob_photo.png";
        Object mediaObj = Engine.state.call(MediaPickerLib.PICK_FILE, "image/png", null, null);
        assertTrue(mediaObj instanceof cgeo.geocaching.wherigo.openwig.Media);
        cgeo.geocaching.wherigo.openwig.Media media = (cgeo.geocaching.wherigo.openwig.Media) mediaObj;

        // Verify media is recognized as ZMedia
        Object isMedia = Engine.state.call(WherigoLib.MADE, WherigoLib.ZMEDIA, media, null);
        assertEquals(Boolean.TRUE, isMedia);

        // 3. Set the character's Media picture (as cartridge scripts do)
        character.rawset("Media", media);
        assertEquals(media, character.media);
        assertEquals(media, character.rawget("Media"));

        // 4. Verify accessing properties and methods on character.Media from Lua/Java
        cgeo.geocaching.wherigo.openwig.Media charMedia = (cgeo.geocaching.wherigo.openwig.Media) character.rawget("Media");
        assertEquals("/tmp/bob_photo.png", charMedia.rawget("Filename"));
        assertEquals("png", charMedia.rawget("Type"));

        // 5. Trigger Show on the character's media
        testUi.lastDialogTexts = null;
        Object showFunc = charMedia.rawget("Show");
        assertTrue(showFunc instanceof cgeo.geocaching.wherigo.kahlua.vm.JavaFunction);
        Engine.state.call((cgeo.geocaching.wherigo.kahlua.vm.JavaFunction) showFunc, charMedia, null, null);
        assertNotNull(testUi.lastDialogTexts);
    }

    @Test
    void testLuaCartridge_SnapPhotoAndItemIcon() {
        cgeo.geocaching.wherigo.openwig.Cartridge cartridge = new cgeo.geocaching.wherigo.openwig.Cartridge();
        Engine.instance.cartridge = cartridge;

        // Create an item in Lua via Wherigo.ZItem
        Object itemObj = Engine.state.call(WherigoLib.ZITEM, cartridge, null, null);
        assertTrue(itemObj instanceof cgeo.geocaching.wherigo.openwig.Thing);
        cgeo.geocaching.wherigo.openwig.Thing item = (cgeo.geocaching.wherigo.openwig.Thing) itemObj;

        // Snap photo via Wherigo.SnapPhoto
        Object photoMedia = Engine.state.call(MediaPickerLib.SNAP_PHOTO, null, null, null);
        assertTrue(photoMedia instanceof cgeo.geocaching.wherigo.openwig.Media);

        // Set item's Icon / Media
        item.rawset("Icon", photoMedia);
        assertEquals(photoMedia, item.icon);
        assertEquals("/tmp/photo.jpg", ((cgeo.geocaching.wherigo.openwig.Media) item.rawget("Icon")).getFilePath());
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
        String nextRecordAudioReturn = "/tmp/recorded_audio.wav";
        String nextRecordVideoReturn = "/tmp/recorded_video.mp4";
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

        @Override
        public String recordAudio() {
            return nextRecordAudioReturn;
        }

        @Override
        public String recordVideo() {
            return nextRecordVideoReturn;
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
