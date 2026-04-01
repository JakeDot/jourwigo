package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.*;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for core Wherigo engine classes.
 */
class EngineComponentsTest {

    private LuaState state;

    @BeforeEach
    void setUp() {
        state = new LuaState();
    }

    @Test
    void testAction_Construction() {
        Action action = new Action();
        assertNotNull(action);
    }

    @Test
    void testAction_NameProperty() {
        Action action = new Action();
        action.rawset("Name", "Test Action");
        assertEquals("Test Action", action.rawget("Name"));
    }

    @Test
    void testAction_EnabledProperty() {
        Action action = new Action();
        action.rawset("Enabled", true);
        assertEquals(true, action.rawget("Enabled"));
    }

    @Test
    void testContainer_Construction() {
        Container container = new Container();
        assertNotNull(container);
    }

    @Test
    void testContainer_NameProperty() {
        Container container = new Container();
        container.rawset("Name", "Test Container");
        assertEquals("Test Container", container.rawget("Name"));
    }

    @Test
    void testThing_Construction() {
        Thing thing = new Thing();
        assertNotNull(thing);
    }

    @Test
    void testThing_NameProperty() {
        Thing thing = new Thing();
        thing.rawset("Name", "Test Thing");
        assertEquals("Test Thing", thing.rawget("Name"));
    }

    @Test
    void testThing_VisibleProperty() {
        Thing thing = new Thing();
        thing.rawset("Visible", true);
        assertEquals(true, thing.rawget("Visible"));
    }

    @Test
    void testTask_Construction() {
        Task task = new Task();
        assertNotNull(task);
    }

    @Test
    void testTask_NameProperty() {
        Task task = new Task();
        task.rawset("Name", "Test Task");
        assertEquals("Test Task", task.rawget("Name"));
    }

    @Test
    void testTask_CompleteProperty() {
        Task task = new Task();
        task.rawset("Complete", false);
        assertEquals(false, task.rawget("Complete"));
    }

    @Test
    void testPlayer_Construction() {
        Player player = new Player();
        assertNotNull(player);
    }

    @Test
    void testPlayer_NameProperty() {
        Player player = new Player();
        player.rawset("Name", "Test Player");
        assertEquals("Test Player", player.rawget("Name"));
    }

    @Test
    void testMedia_Construction() {
        Media media = new Media();
        assertNotNull(media);
    }

    @Test
    void testZone_Construction() {
        Zone zone = new Zone();
        assertNotNull(zone);
    }

    @Test
    void testZone_NameProperty() {
        Zone zone = new Zone();
        zone.rawset("Name", "Test Zone");
        assertEquals("Test Zone", zone.rawget("Name"));
    }

    @Test
    void testTimer_Construction() {
        Timer timer = new Timer();
        assertNotNull(timer);
    }

    @Test
    void testTimer_NameProperty() {
        Timer timer = new Timer();
        timer.rawset("Name", "Test Timer");
        assertEquals("Test Timer", timer.rawget("Name"));
    }

    @Test
    void testEventTable_Construction() {
        EventTable eventTable = new EventTable();
        assertNotNull(eventTable);
    }

    @Test
    void testEventTable_RawGetSet() {
        EventTable eventTable = new EventTable();
        eventTable.rawset("OnClick", "test_function");
        assertEquals("test_function", eventTable.rawget("OnClick"));
    }
}
