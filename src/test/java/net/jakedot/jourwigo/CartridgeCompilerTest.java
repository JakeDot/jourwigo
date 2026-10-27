package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CartridgeCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testBuildLuaScriptWithMultipleObjects() {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(
            "Compiled Cartridge", "Author Name", "2.0", "Compiler Test Description", 52.0, 13.0, 100.0
        );

        List<LuaObjectBuilder> objects = new ArrayList<>();
        objects.add(LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ZONE));
        objects.add(LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ITEM));
        objects.add(LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.CHARACTER));

        String script = CartridgeCompiler.buildLuaScript(meta, objects);

        assertNotNull(script);
        assertTrue(script.contains("Cartridge.Name = \"Compiled Cartridge\""));
        assertTrue(script.contains("Cartridge.Author = \"Author Name\""));
        assertTrue(script.contains("zoneStart = Wherigo.Zone(Cartridge)"));
        assertTrue(script.contains("itemKey = Wherigo.ZItem(Cartridge)"));
        assertTrue(script.contains("characterGuide = Wherigo.ZCharacter(Cartridge)"));
    }

    @Test
    public void testCompileToGwcBytes() throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(
            "Gwc Test", "Tester", "1.0", "Gwc Description", 10.0, 20.0, 30.0
        );

        List<LuaObjectBuilder> objects = List.of(LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ITEM));

        byte[] gwcBytes = CartridgeCompiler.compileToGwcBytes(meta, objects);

        assertNotNull(gwcBytes);
        assertTrue(gwcBytes.length > 20, "GWC byte array should be larger than header size");

        // Verify GWC magic header: 0x02 0x0a CART
        assertEquals(0x02, gwcBytes[0]);
        assertEquals(0x0a, gwcBytes[1]);
        assertEquals('C', (char) gwcBytes[2]);
        assertEquals('A', (char) gwcBytes[3]);
        assertEquals('R', (char) gwcBytes[4]);
        assertEquals('T', (char) gwcBytes[5]);
    }

    @Test
    public void testCompileToGwcFileAndReadWithIntegratedPlayer() throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(
            "Playable Cartridge", "Jourwigo Compiler", "1.0.0", "Cartridge made with builder", 51.5, -0.1, 10.0
        );

        List<LuaObjectBuilder> objects = List.of(
            LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ZONE),
            LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ITEM),
            LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.TASK)
        );

        File targetGwc = tempDir.resolve("playable.gwc").toFile();
        CartridgeCompiler.compileToGwcFile(meta, objects, targetGwc);

        assertTrue(targetGwc.exists());
        assertTrue(targetGwc.length() > 0);

        JavaFileHandle savefh = IntegratedPlayerService.buildSaveHandle(targetGwc);
        try (CartridgeFile cf = IntegratedPlayerService.readCartridge(targetGwc, savefh)) {
            assertNotNull(cf);
            assertEquals("Playable Cartridge", cf.name);
            assertEquals("Jourwigo Compiler", cf.author);
            assertEquals("1.0.0", cf.version);
        }
    }
}
