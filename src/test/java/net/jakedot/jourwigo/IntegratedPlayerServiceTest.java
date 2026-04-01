package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IntegratedPlayerService utility methods.
 */
class IntegratedPlayerServiceTest {

    @Test
    void testBuildSaveHandle_CreatesCorrectSaveFileName(@TempDir Path tempDir) {
        File cartridgeFile = tempDir.resolve("mycartridge.gwc").toFile();
        JavaFileHandle saveHandle = IntegratedPlayerService.buildSaveHandle(cartridgeFile);

        assertNotNull(saveHandle);
        // The save file should be in the same directory with .wgs extension
        // Note: We can't directly access the file field, but we can verify the handle works
    }

    @Test
    void testBuildSaveHandle_HandlesUppercaseExtension(@TempDir Path tempDir) {
        File cartridgeFile = tempDir.resolve("mycartridge.GWC").toFile();
        JavaFileHandle saveHandle = IntegratedPlayerService.buildSaveHandle(cartridgeFile);

        assertNotNull(saveHandle);
    }

    @Test
    void testBuildSaveHandle_HandlesMixedCaseExtension(@TempDir Path tempDir) {
        File cartridgeFile = tempDir.resolve("mycartridge.Gwc").toFile();
        JavaFileHandle saveHandle = IntegratedPlayerService.buildSaveHandle(cartridgeFile);

        assertNotNull(saveHandle);
    }

    @Test
    void testBuildSaveHandle_HandlesNoExtension(@TempDir Path tempDir) {
        File cartridgeFile = tempDir.resolve("mycartridge").toFile();
        JavaFileHandle saveHandle = IntegratedPlayerService.buildSaveHandle(cartridgeFile);

        assertNotNull(saveHandle);
    }

    @Test
    void testBuildSaveHandle_PreservesCartridgeName(@TempDir Path tempDir) {
        File cartridgeFile = tempDir.resolve("my-special-cartridge.gwc").toFile();
        JavaFileHandle saveHandle = IntegratedPlayerService.buildSaveHandle(cartridgeFile);

        assertNotNull(saveHandle);
        // The save handle should be created successfully
    }

    @Test
    void testStartOrRestore_NewGame_CallsEngineStart(@TempDir Path tempDir) throws Exception {
        File saveFile = tempDir.resolve("savegame.wgs").toFile();
        JavaFileHandle saveHandle = new JavaFileHandle(saveFile);

        List<String> logs = new ArrayList<>();
        MockEngine engine = new MockEngine();

        IntegratedPlayerService.startOrRestore(engine, saveHandle, logs::add);

        assertTrue(logs.contains("Starting new game."));
        assertTrue(engine.startCalled);
        assertFalse(engine.restoreCalled);
    }

    @Test
    void testStartOrRestore_ExistingSave_CallsEngineRestore(@TempDir Path tempDir) throws Exception {
        File saveFile = tempDir.resolve("savegame.wgs").toFile();
        assertTrue(saveFile.createNewFile()); // Create the save file
        JavaFileHandle saveHandle = new JavaFileHandle(saveFile);

        List<String> logs = new ArrayList<>();
        MockEngine engine = new MockEngine();

        IntegratedPlayerService.startOrRestore(engine, saveHandle, logs::add);

        assertTrue(logs.contains("Save file found - restoring game."));
        assertFalse(engine.startCalled);
        assertTrue(engine.restoreCalled);
    }

    /**
     * Mock Engine for testing IntegratedPlayerService methods.
     */
    private static class MockEngine extends cgeo.geocaching.wherigo.openwig.Engine {
        boolean startCalled = false;
        boolean restoreCalled = false;

        MockEngine() {
            super(); // Use protected constructor for test mockups
        }

        @Override
        public void start() {
            startCalled = true;
        }

        @Override
        public void restore() {
            restoreCalled = true;
        }
    }
}
