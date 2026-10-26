package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CheerpJBridgeTest {

    @Test
    public void testGenerateLuaTemplate() {
        String lua = CheerpJBridge.generateLuaTemplate("Test Cartridge", "Tester", "1.0", "Desc", 51.5, -0.12, 10.0);
        assertNotNull(lua);
        assertTrue(lua.contains("Cartridge.Name = \"Test Cartridge\""));
        assertTrue(lua.contains("Cartridge.Author = \"Tester\""));
    }

    @Test
    public void testCompileGwcBytes() throws Exception {
        byte[] bytes = CheerpJBridge.compileGwcBytes("Test Cartridge", "Tester", "1.0", "Desc", 51.5, -0.12, 10.0);
        assertNotNull(bytes);
        assertTrue(bytes.length > 20);
        assertEquals(0x02, bytes[0]);
        assertEquals(0x0a, bytes[1]);
        assertEquals('C', (char) bytes[2]);
    }

    @Test
    public void testBuildLuaObject() {
        String[] keys = new String[] { "Name", "Visible" };
        String[] values = new String[] { "Golden Key", "true" };
        String[] types = new String[] { "STRING", "BOOLEAN" };

        String lua = CheerpJBridge.buildLuaObject("Item", "itemKey", "Wherigo.ZItem(Cartridge)", keys, values, types);
        assertNotNull(lua);
        assertTrue(lua.contains("itemKey = Wherigo.ZItem(Cartridge)"));
        assertTrue(lua.contains("itemKey.Name = \"Golden Key\""));
        assertTrue(lua.contains("itemKey.Visible = true"));
    }

    @Test
    public void testCompileGwcWithObjectBytes() throws Exception {
        String[] keys = new String[] { "Name" };
        String[] values = new String[] { "Start Zone" };
        String[] types = new String[] { "STRING" };

        byte[] bytes = CheerpJBridge.compileGwcWithObjectBytes("Cartridge With Object", "Author", "1.0", "Desc", 0.0, 0.0, 0.0,
            "Zone", "zoneStart", "Wherigo.Zone(Cartridge)", keys, values, types);

        assertNotNull(bytes);
        assertTrue(bytes.length > 20);
    }
}
