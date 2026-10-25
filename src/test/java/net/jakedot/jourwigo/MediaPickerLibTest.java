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
        assertEquals("/tmp/photo.jpg", res);
    }

    @Test
    void testPickFile() {
        Object res = Engine.state.call(MediaPickerLib.PICK_FILE, "image/*", null, null);
        assertEquals("/tmp/picked_file.jpg", res);
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
        @Override public void refresh() {}
        @Override public void start() {}
        @Override public void end() {}
        @Override public void showError(String msg) {}
        @Override public void debugMsg(String msg) {}
        @Override public void setStatusText(String text) {}
        @Override public void pushDialog(String[] texts, cgeo.geocaching.wherigo.openwig.Media[] media, String button1, String button2, cgeo.geocaching.wherigo.kahlua.vm.LuaClosure callback) {}
        @Override public void pushInput(cgeo.geocaching.wherigo.openwig.EventTable input) {}
        @Override public void showScreen(ScreenId screenId, cgeo.geocaching.wherigo.openwig.EventTable details) {}
        @Override public void playSound(byte[] data, String mime) {}
        @Override public void blockForSaving() {}
        @Override public void unblock() {}
        @Override public void command(String cmd) {}

        @Override
        public String snapPhoto() {
            return "/tmp/photo.jpg";
        }

        @Override
        public String pickFile(String mimeType) {
            return "/tmp/picked_file.jpg";
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
