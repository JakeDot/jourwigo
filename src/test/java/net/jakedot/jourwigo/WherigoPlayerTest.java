package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.ZonePoint;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import cgeo.geocaching.wherigo.kahlua.stdlib.CoroutineLib;
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Basic sanity tests for the Wherigo engine components.
 */
public class WherigoPlayerTest {

    @Test
    void testZonePointDistance() {
        ZonePoint a = new ZonePoint(51.5074, -0.1278, 0); // London
        ZonePoint b = new ZonePoint(48.8566, 2.3522, 0);  // Paris
        double dist = a.distance(b);
        // London to Paris is roughly 340 km
        assertTrue(dist > 300_000 && dist < 400_000,
            "Expected London-Paris distance ~340km, got " + dist);
    }

    @Test
    void testZonePointCopy() {
        ZonePoint original = new ZonePoint(10.0, 20.0, 30.0);
        ZonePoint copy = ZonePoint.copy(original);
        assertNotNull(copy);
        assertEquals(original.latitude,  copy.latitude,  1e-9);
        assertEquals(original.longitude, copy.longitude, 1e-9);
        assertEquals(original.altitude,  copy.altitude,  1e-9);
        assertNotSame(original, copy);
    }

    @Test
    void testZonePointRawGetSet() {
        ZonePoint zp = new ZonePoint();
        zp.rawset("latitude",  1.5);
        zp.rawset("longitude", 2.5);
        zp.rawset("altitude",  3.5);
        assertEquals(1.5, ((Double) zp.rawget("latitude")),  1e-9);
        assertEquals(2.5, ((Double) zp.rawget("longitude")), 1e-9);
        assertEquals(3.5, ((Double) zp.rawget("altitude")),  1e-9);
    }

    @Test
    void testZonePointFriendlyDistance() {
        String d = ZonePoint.makeFriendlyDistance(1234.5);
        assertNotNull(d);
        assertTrue(d.contains("m") || d.contains("km"));
    }

    @Test
    void testConsoleLocationService() {
        ConsoleLocationService loc = new ConsoleLocationService(51.5, -0.12, 5.0);
        assertEquals(51.5,  loc.getLatitude(),  1e-9);
        assertEquals(-0.12, loc.getLongitude(), 1e-9);
        assertEquals(5.0,   loc.getAltitude(),  1e-9);
        assertEquals(0.0,   loc.getHeading(),   1e-9);

        loc.setPosition(48.8, 2.3, 35.0);
        assertEquals(48.8, loc.getLatitude(),  1e-9);
        assertEquals(2.3,  loc.getLongitude(), 1e-9);
        assertEquals(35.0, loc.getAltitude(),  1e-9);
    }

    @Test
    void testZonePointTranslate() {
        ZonePoint origin = new ZonePoint(0.0, 0.0, 0.0);
        // Translate 1000 metres due North (azimuth = 0°)
        ZonePoint translated = origin.translate(0.0, 1000.0);
        assertNotNull(translated);
        // Should have moved north (positive latitude change)
        assertTrue(translated.latitude > origin.latitude);
        // Longitude should be approximately the same
        assertEquals(origin.longitude, translated.longitude, 0.01);
    }

    @Test
    void testDistanceConversions() {
        // 1 mile = 1609.344 metres
        double metres = ZonePoint.convertDistanceFrom(1.0, "miles");
        assertEquals(1609.344, metres, 0.001);

        // Convert back
        double miles = ZonePoint.convertDistanceTo(1609.344, "miles");
        assertEquals(1.0, miles, 0.001);
    }

    @Test
    void testConsoleUIScreenNameMappings() {
        ConsoleUI ui = new ConsoleUI();
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream printStream = new PrintStream(capturedOut);
        try {
            System.setOut(printStream);
            ui.showScreen(UI.ScreenId.MAINSCREEN, null);
            ui.showScreen(UI.ScreenId.DETAILSCREEN, null);
            ui.showScreen(UI.ScreenId.INVENTORYSCREEN, null);
            ui.showScreen(UI.ScreenId.ITEMSCREEN, null);
            ui.showScreen(UI.ScreenId.LOCATIONSCREEN, null);
            ui.showScreen(UI.ScreenId.TASKSCREEN, null);
            ui.showScreen(UI.ScreenId.UNKNOWN, null);
        } finally {
            System.setOut(originalOut);
        }

        String output = capturedOut.toString();
        assertTrue(output.contains("=== Screen: Main ==="));
        assertTrue(output.contains("=== Screen: Detail ==="));
        assertTrue(output.contains("=== Screen: Inventory ==="));
        assertTrue(output.contains("=== Screen: Item ==="));
        assertTrue(output.contains("=== Screen: Locations ==="));
        assertTrue(output.contains("=== Screen: Tasks ==="));
        assertTrue(output.contains("=== Screen: Unknown ==="));
    }

    @Test
    void testUIScreenIdFromCode() {
        assertEquals(UI.ScreenId.MAINSCREEN, UI.ScreenId.fromCode(UI.MAINSCREEN));
        assertEquals(UI.ScreenId.DETAILSCREEN, UI.ScreenId.fromCode(UI.DETAILSCREEN));
        assertEquals(UI.ScreenId.UNKNOWN, UI.ScreenId.fromCode(999));
    }

    @Test
    void testUIScreenIdFromOrdinalWithNull() {
        assertEquals(UI.ScreenId.UNKNOWN, UI.ScreenId.fromOrdinal(null));
    }

    @Test
    void testCartridgeTemplateWriterBuildsExpectedLuaTemplate() {
        String lua = CartridgeTemplateWriter.buildLuaTemplate(
            "My \"Quest\"",
            "A\\B",
            "1.2",
            "Line1\nLine2",
            51.5,
            -0.12,
            10.0
        );

        assertTrue(lua.contains("Cartridge.Name = \"My \\\"Quest\\\"\""));
        assertTrue(lua.contains("Cartridge.Author = \"A\\\\B\""));
        assertTrue(lua.contains("Cartridge.Description = \"Line1\\nLine2\""));
        assertTrue(lua.contains("Wherigo.ZonePoint(51.5, -0.12, 10.0)"));
    }

    @Test
    void testGuiParseDoubleFallback() {
        assertEquals(3.14, UrwigoDesktopGui.parseDouble("3.14", 0.0), 1e-9);
        assertEquals(2.0, UrwigoDesktopGui.parseDouble("not-a-number", 2.0), 1e-9);
    }

    @Test
    void testWebServerParsesFormBody() {
        Map<String, String> values = UrwigoWebServer.parseForm("name=My+Cart&author=Jake&description=Line1%0ALine2");
        assertEquals("My Cart", values.get("name"));
        assertEquals("Jake", values.get("author"));
        assertEquals("Line1\nLine2", values.get("description"));
    }

    @Test
    void testWebServerIndexMentionsCgeoRuntime() {
        String html = UrwigoWebServer.indexPage();
        assertTrue(html.contains("cgeo/cgeo"));
        assertTrue(html.contains("Create cartridge template"));
    }

    @Test
    void testParseWebPortFallbackAndBounds() {
        assertEquals(8080, UrwigoDesktopGui.parseWebPort("8080", 80));
        assertEquals(80, UrwigoDesktopGui.parseWebPort("-1", 80));
        assertEquals(80, UrwigoDesktopGui.parseWebPort("70000", 80));
        assertEquals(80, UrwigoDesktopGui.parseWebPort("abc", 80));
        assertEquals(80, UrwigoDesktopGui.parseWebPort("   ", 80));
    }

    @Test
    void testCoroutineLibRegistrationUsesLowercaseNames() {
        LuaState state = new LuaState();
        LuaTable coroutine = (LuaTable) state.getEnvironment().rawget("coroutine");

        assertNotNull(coroutine);
        assertSame(CoroutineLib.CREATE, coroutine.rawget("create"));
        assertSame(CoroutineLib.RESUME, coroutine.rawget("resume"));
        assertSame(CoroutineLib.YIELD, coroutine.rawget("yield"));
        assertSame(CoroutineLib.STATUS, coroutine.rawget("status"));
        assertSame(CoroutineLib.RUNNING, coroutine.rawget("running"));
        assertNull(coroutine.rawget("CREATE"));

        assertEquals("coroutine.create", CoroutineLib.CREATE.toString());
        assertEquals("coroutine.yield", CoroutineLib.YIELD.toString());
    }

    @Test
    void testTableLibRegistrationUsesLowercaseNames() {
        LuaState state = new LuaState();
        LuaTable table = (LuaTable) state.getEnvironment().rawget("table");

        assertNotNull(table);
        assertSame(TableLib.CONCAT, table.rawget("concat"));
        assertSame(TableLib.INSERT, table.rawget("insert"));
        assertSame(TableLib.REMOVE, table.rawget("remove"));
        assertSame(TableLib.MAXN, table.rawget("maxn"));
        assertNull(table.rawget("CONCAT"));

        assertEquals("table.concat", TableLib.CONCAT.toString());
        assertEquals("table.maxn", TableLib.MAXN.toString());
    }
}
