package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.*;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test running a sample cartridge compiled from Lua source,
 * verifying all implemented types exposed to Lua.
 */
class SampleCartridgeIntegrationTest {

    private File createdAudioFile;

    @AfterEach
    void tearDown() {
        Engine.kill();
        if (createdAudioFile != null && createdAudioFile.exists()) {
            createdAudioFile.delete();
        }
    }

    @Test
    void testSampleCartridgeCompiledFromLuaSourceExposesAllTypes(@TempDir Path tempDir) throws Exception {
        // Create sample audio file in working dir for media play testing
        createdAudioFile = new File("sample_audio.mp3");
        try (FileOutputStream fos = new FileOutputStream(createdAudioFile)) {
            fos.write(new byte[]{1, 2, 3, 4});
        }
        createdAudioFile.deleteOnExit();

        // 1. Obtain Lua source file from resources
        File luaSourceFile = tempDir.resolve("sample_cartridge.lua").toFile();
        try (InputStream is = getClass().getResourceAsStream("/cartridges/sample_cartridge.lua");
             FileOutputStream fos = new FileOutputStream(luaSourceFile)) {
            assertNotNull(is, "Sample Lua source file resource /cartridges/sample_cartridge.lua must exist");
            is.transferTo(fos);
        }

        // 2. Compile Lua source file into Lua 5.1 bytecode
        byte[] bytecode = LuaCompilerHelper.compileLuaSource(luaSourceFile, "/cartridges/sample_cartridge.lbc");
        assertNotNull(bytecode, "Compiled bytecode must not be null");
        assertTrue(bytecode.length > 0, "Compiled bytecode must contain data");

        // 3. Build .gwc cartridge binary
        File gwcFile = tempDir.resolve("sample_cartridge.gwc").toFile();
        new GwcBuilder()
            .setName("Integration Test Cartridge")
            .setDescription("A sample cartridge using all Lua-exposed types.")
            .setAuthor("Jourwigo")
            .setVersion("1.0.0")
            .setCoordinates(51.5074, -0.1278)
            .buildGwcFile(bytecode, gwcFile);

        assertTrue(gwcFile.exists() && gwcFile.length() > 0, "GWC file must be generated");

        // 4. Load CartridgeFile
        JavaSeekableFile seekableGwc = new JavaSeekableFile(gwcFile);
        File saveFile = tempDir.resolve("sample_cartridge.wgs").toFile();
        JavaFileHandle saveHandle = new JavaFileHandle(saveFile);

        CartridgeFile cartridgeFile = CartridgeFile.read(seekableGwc, saveHandle);
        assertNotNull(cartridgeFile);
        assertEquals("Integration Test Cartridge", cartridgeFile.name);
        assertEquals("Jourwigo", cartridgeFile.author);

        // 5. Initialize Engine with Mock UI and Location Service
        MockIntegrationUI mockUI = new MockIntegrationUI(tempDir);
        ConsoleLocationService locationService = new ConsoleLocationService(51.5074, -0.1278, 10.0);
        ByteArrayOutputStream engineLog = new ByteArrayOutputStream();

        Engine engine = Engine.newInstance(cartridgeFile, engineLog, mockUI, locationService);
        assertNotNull(engine);

        // 6. Run Engine thread and wait for execution
        engine.start();

        // Wait up to 5 seconds for OnStart event completion
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (Engine.instance != null && Engine.instance.cartridge != null) {
                LuaTable cartTable = Engine.instance.cartridge.table;
                Object onStartCalled = cartTable.rawget("OnStartCalled");
                if (Boolean.TRUE.equals(onStartCalled)) {
                    break;
                }
            }
            Thread.sleep(100);
        }

        if (Engine.instance == null || Engine.instance.cartridge == null) {
            System.err.println("Engine log output:\n" + engineLog.toString());
            if (mockUI.errorMsg != null) {
                System.err.println("MockUI error: " + mockUI.errorMsg);
            }
        }

        // 7. Assert Engine and Cartridge state
        Cartridge cartridge = Engine.instance.cartridge;
        assertNotNull(cartridge, "Cartridge must be initialized");

        // Verify ZCartridge object
        assertEquals("Integration Test Cartridge", cartridge.name);

        // Verify Zone
        assertEquals(1, cartridge.zones.size());
        Zone zone = (Zone) cartridge.zones.get(0);
        assertEquals("Start Zone", zone.name);
        assertTrue(zone.isVisible());

        // Verify Things (ZCharacter and ZItem)
        assertEquals(2, cartridge.things.size());
        boolean foundCharacter = false;
        boolean foundItem = false;
        for (Object thingObj : cartridge.things) {
            Thing thing = (Thing) thingObj;
            if ("Guide NPC".equals(thing.name) && thing.isCharacter()) {
                foundCharacter = true;
            } else if ("Golden Key".equals(thing.name) && !thing.isCharacter()) {
                foundItem = true;
            }
        }
        assertTrue(foundCharacter, "Guide NPC character must be registered");
        assertTrue(foundItem, "Golden Key item must be registered");

        // Verify ZTimer
        assertEquals(1, cartridge.timers.size());
        Timer timer = (Timer) cartridge.timers.get(0);
        assertEquals("Test Timer", timer.name);

        // Verify ZTask
        assertEquals(1, cartridge.tasks.size());
        Task task = (Task) cartridge.tasks.get(0);
        assertEquals("Complete Integration Test", task.name);

        // Verify Lua execution outcomes in Lua state table
        LuaTable cartTable = cartridge.table;
        assertEquals(Boolean.TRUE, cartTable.rawget("OnStartCalled"), "OnStart must have been called");
        assertEquals(Boolean.TRUE, cartTable.rawget("TestMadeMedia"), "Wherigo.made(ZMedia, media) must return true");
        assertEquals(Boolean.TRUE, cartTable.rawget("TestMadeZone"), "Wherigo.made(Zone, zone) must return true");
        assertEquals(Boolean.TRUE, cartTable.rawget("TestEquals"), "Wherigo.NoCaseEquals must return true");
        assertEquals("Welcome to Wherigo", cartTable.rawget("TranslatedWelcome"), "TranslationResource must translate key");
        assertEquals(Boolean.TRUE, cartTable.rawget("ZoneContainsResult"), "Zone:Contains must return true for character inside zone");

        assertNotNull(cartTable.rawget("TranslatedLat"), "TranslatePoint result latitude must be populated");
        assertNotNull(cartTable.rawget("VectorDist"), "VectorToPoint distance must be populated");
        assertNotNull(cartTable.rawget("VectorBearing"), "VectorToPoint bearing must be populated");

        // Verify UI callbacks
        assertTrue(mockUI.statusTexts.contains("Running Sample Cartridge Integration Test"), "Status text should be set");
        assertTrue(mockUI.screensShown.contains(UI.ScreenId.MAINSCREEN), "MAINSCREEN should have been shown");
        assertTrue(mockUI.dialogPushed, "Dialog should have been pushed");
        assertTrue(mockUI.inputPushed, "Input prompt should have been pushed");
        assertTrue(mockUI.soundsPlayed > 0, "Audio sound playback should have been triggered");
        assertTrue(mockUI.commandsIssued.contains("Save"), "Command('Save') should have been issued");
        assertTrue(mockUI.photoSnapped, "SnapPhoto should have been called");
        assertTrue(mockUI.filePicked, "PickFile should have been called");

        seekableGwc.close();
    }

    /**
     * Mock UI for testing integration interactions.
     */
    private static class MockIntegrationUI implements UI {
        final Path tempDir;
        final List<String> statusTexts = new ArrayList<>();
        final List<ScreenId> screensShown = new ArrayList<>();
        final List<String> commandsIssued = new ArrayList<>();
        boolean dialogPushed = false;
        boolean inputPushed = false;
        int soundsPlayed = 0;
        boolean photoSnapped = false;
        boolean filePicked = false;
        String errorMsg = null;

        MockIntegrationUI(Path tempDir) {
            this.tempDir = tempDir;
        }

        @Override public void refresh() {}
        @Override public void start() {}
        @Override public void end() {}

        @Override
        public void showError(String msg) {
            this.errorMsg = msg;
        }

        @Override public void debugMsg(String msg) {}

        @Override
        public void setStatusText(String text) {
            if (text != null) statusTexts.add(text);
        }

        @Override
        public void pushDialog(String[] texts, Media[] media, String button1, String button2, LuaClosure callback) {
            dialogPushed = true;
            if (callback != null) {
                Engine.invokeCallback(callback, "Button1");
            }
        }

        @Override
        public void pushInput(EventTable input) {
            inputPushed = true;
        }

        @Override
        public void showScreen(ScreenId screenId, EventTable details) {
            screensShown.add(screenId);
        }

        @Override
        public void playSound(byte[] data, String mime) {
            soundsPlayed++;
        }

        @Override public void blockForSaving() {}
        @Override public void unblock() {}

        @Override
        public void command(String cmd) {
            if (cmd != null) commandsIssued.add(cmd);
        }

        @Override
        public String snapPhoto() {
            photoSnapped = true;
            return tempDir.resolve("photo.jpg").toAbsolutePath().toString();
        }

        @Override
        public String pickFile(String mimeType) {
            filePicked = true;
            return tempDir.resolve("file.png").toAbsolutePath().toString();
        }
    }
}
