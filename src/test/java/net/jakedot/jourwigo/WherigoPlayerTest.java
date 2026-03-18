package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.ZonePoint;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

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
    void testConsoleUIScreenNameMappings() throws Exception {
        Method screenName = ConsoleUI.class.getDeclaredMethod("screenName", int.class);
        screenName.setAccessible(true);

        assertEquals("Main", screenName.invoke(null, UI.MAINSCREEN));
        assertEquals("Detail", screenName.invoke(null, UI.DETAILSCREEN));
        assertEquals("Inventory", screenName.invoke(null, UI.INVENTORYSCREEN));
        assertEquals("Item", screenName.invoke(null, UI.ITEMSCREEN));
        assertEquals("Locations", screenName.invoke(null, UI.LOCATIONSCREEN));
        assertEquals("Tasks", screenName.invoke(null, UI.TASKSCREEN));
        assertEquals("Unknown(999)", screenName.invoke(null, 999));
    }
}
