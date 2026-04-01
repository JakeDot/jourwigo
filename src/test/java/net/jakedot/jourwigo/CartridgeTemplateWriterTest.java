package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CartridgeTemplateWriter.
 */
class CartridgeTemplateWriterTest {

    @Test
    void testBuildLuaTemplate_BasicTemplate() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Test Cart",
            "Test Author",
            "1.0",
            "Test Description",
            51.5,
            -0.12,
            10.0
        );

        assertNotNull(lua);
        assertTrue(lua.contains("Cartridge.Name = \"Test Cart\""));
        assertTrue(lua.contains("Cartridge.Author = \"Test Author\""));
        assertTrue(lua.contains("Cartridge.Version = \"1.0\""));
        assertTrue(lua.contains("Cartridge.Description = \"Test Description\""));
        assertTrue(lua.contains("Wherigo.ZonePoint(51.5, -0.12, 10.0)"));
        assertTrue(lua.contains("function OnStart()"));
        assertTrue(lua.contains("Welcome to Test Cart"));
    }

    @Test
    void testBuildLuaTemplate_EscapesQuotes() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "My \"Quest\"",
            "Author",
            "1.0",
            "Description",
            0, 0, 0
        );

        assertTrue(lua.contains("Cartridge.Name = \"My \\\"Quest\\\"\""));
        assertTrue(lua.contains("Welcome to My \\\"Quest\\\""));
    }

    @Test
    void testBuildLuaTemplate_EscapesBackslashes() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name",
            "A\\B",
            "1.0",
            "Description",
            0, 0, 0
        );

        assertTrue(lua.contains("Cartridge.Author = \"A\\\\B\""));
    }

    @Test
    void testBuildLuaTemplate_EscapesNewlines() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name",
            "Author",
            "1.0",
            "Line1\nLine2\nLine3",
            0, 0, 0
        );

        assertTrue(lua.contains("Cartridge.Description = \"Line1\\nLine2\\nLine3\""));
    }

    @Test
    void testBuildLuaTemplate_HandlesNullValues() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            null,
            null,
            null,
            null,
            0, 0, 0
        );

        assertNotNull(lua);
        assertTrue(lua.contains("Cartridge.Name = \"\""));
        assertTrue(lua.contains("Cartridge.Author = \"\""));
        assertTrue(lua.contains("Cartridge.Version = \"\""));
        assertTrue(lua.contains("Cartridge.Description = \"\""));
    }

    @Test
    void testBuildLuaTemplate_HandlesNegativeCoordinates() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name",
            "Author",
            "1.0",
            "Description",
            -51.5,
            -122.3,
            -5.0
        );

        assertTrue(lua.contains("Wherigo.ZonePoint(-51.5, -122.3, -5.0)"));
    }

    @Test
    void testBuildLuaTemplate_HandlesZeroCoordinates() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name",
            "Author",
            "1.0",
            "Description",
            0.0,
            0.0,
            0.0
        );

        assertTrue(lua.contains("Wherigo.ZonePoint(0.0, 0.0, 0.0)"));
    }

    @Test
    void testBuildLuaTemplate_ContainsAllRequiredFields() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name",
            "Author",
            "1.0",
            "Description",
            0, 0, 0
        );

        // Check for all essential Lua structure
        assertTrue(lua.contains("Cartridge = Wherigo.ZCartridge()"));
        assertTrue(lua.contains("Cartridge.Name"));
        assertTrue(lua.contains("Cartridge.Description"));
        assertTrue(lua.contains("Cartridge.Activity = \"TourGuide\""));
        assertTrue(lua.contains("Cartridge.StartingLocationDescription = \"Start\""));
        assertTrue(lua.contains("Cartridge.StartingLocation"));
        assertTrue(lua.contains("Cartridge.Version"));
        assertTrue(lua.contains("Cartridge.Author"));
        assertTrue(lua.contains("function OnStart()"));
        assertTrue(lua.contains("Wherigo.MessageBox"));
    }

    @Test
    void testBuildLuaTemplate_HandlesComplexEscaping() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "Name with \"quotes\" and \\backslashes\\",
            "Author",
            "1.0",
            "Multi\nLine\nDescription",
            0, 0, 0
        );

        assertTrue(lua.contains("Name with \\\"quotes\\\" and \\\\backslashes\\\\"));
        assertTrue(lua.contains("Multi\\nLine\\nDescription"));
    }

    @Test
    void testWriteLuaTemplate_CreatesFile(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("test.lua");

        CartridgeTemplateWriter.writeLuaTemplate(
            outputFile,
            "Test Cart",
            "Author",
            "1.0",
            "Description",
            51.5,
            -0.12,
            10.0
        );

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertNotNull(content);
        assertTrue(content.contains("Cartridge.Name = \"Test Cart\""));
    }

    @Test
    void testWriteLuaTemplate_OverwritesExistingFile(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("test.lua");

        // Write first version
        CartridgeTemplateWriter.writeLuaTemplate(
            outputFile,
            "First Version",
            "Author",
            "1.0",
            "Description",
            0, 0, 0
        );

        // Write second version (should overwrite)
        CartridgeTemplateWriter.writeLuaTemplate(
            outputFile,
            "Second Version",
            "Author",
            "2.0",
            "Description",
            0, 0, 0
        );

        String content = Files.readString(outputFile);
        assertTrue(content.contains("Second Version"));
        assertFalse(content.contains("First Version"));
    }

    @Test
    void testWriteLuaTemplate_UTF8Encoding(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("test.lua");

        // Test with Unicode characters
        CartridgeTemplateWriter.writeLuaTemplate(
            outputFile,
            "Café ☕",
            "Author",
            "1.0",
            "Description with émojis 🎮",
            0, 0, 0
        );

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("Café ☕"));
        assertTrue(content.contains("émojis 🎮"));
    }
}
