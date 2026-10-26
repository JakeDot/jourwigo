package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DialogBuilderTest {

    @Test
    public void testDefault3FrameDialog() {
        DialogBuilder db = DialogBuilder.createDefault3FrameDialog();

        assertEquals("dialogBranchingDemo", db.getName());
        assertEquals(3, db.getInitialFrames().size());
        assertEquals("Welcome traveler! You have arrived at the mystical ancient ruins.", db.getInitialFrames().get(0).getText());
        assertEquals("mediaIntro", db.getInitialFrames().get(0).getMediaVar());
        assertEquals("mediaRuins", db.getInitialFrames().get(1).getMediaVar());
        assertEquals("mediaGuardian", db.getInitialFrames().get(2).getMediaVar());

        assertNotNull(db.getQuestion());
        assertEquals("Do you wish to enter the ruins and search for the treasure?", db.getQuestion().getText());
        assertEquals("mediaQuestion", db.getQuestion().getMediaVar());
        assertEquals(DialogBuilder.QuestionType.YES_NO, db.getQuestion().getType());
        assertEquals(List.of("Yes", "No"), db.getQuestion().getChoices());

        assertEquals(2, db.getBranchFrames("Yes").size());
        assertEquals("You step bravely into the shadows of the ruin...", db.getBranchFrames("Yes").get(0).getText());
        assertEquals("mediaEnter", db.getBranchFrames("Yes").get(0).getMediaVar());

        assertEquals(2, db.getBranchFrames("No").size());
        assertEquals("You decide to stay safe and turn back.", db.getBranchFrames("No").get(0).getText());
        assertEquals("mediaTurnBack", db.getBranchFrames("No").get(0).getMediaVar());

        String lua = db.toLuaCode();
        assertTrue(lua.contains("dialogBranchingDemo = function()"));
        assertTrue(lua.contains("Wherigo.MessageBox({"));
        assertTrue(lua.contains("Buttons = {\"Yes\", \"No\"}"));
        assertTrue(lua.contains("Callback = function("));
    }

    @Test
    public void testMergingGraphDialog() {
        DialogBuilder db = DialogBuilder.createMergingGraphDialog();

        assertEquals("dialogMergeDemo", db.getName());
        assertEquals(5, db.getNodes().size());

        DialogBuilder.FrameNode startNode = (DialogBuilder.FrameNode) db.getNode("node_start");
        assertNotNull(startNode);
        assertEquals("node_question", startNode.getNextNodeId());

        DialogBuilder.QuestionNode qNode = (DialogBuilder.QuestionNode) db.getNode("node_question");
        assertNotNull(qNode);
        assertEquals("node_explore", qNode.getChoiceTarget("Explore Path"));
        assertEquals("node_rest", qNode.getChoiceTarget("Rest Here"));

        DialogBuilder.FrameNode exploreNode = (DialogBuilder.FrameNode) db.getNode("node_explore");
        DialogBuilder.FrameNode restNode = (DialogBuilder.FrameNode) db.getNode("node_rest");

        // Verify branch merging: both choice targets point to common merged node
        assertEquals("node_merged", exploreNode.getNextNodeId());
        assertEquals("node_merged", restNode.getNextNodeId());

        String lua = db.toLuaCode();
        assertTrue(lua.contains("dialogMergeDemo = function()"));
        assertTrue(lua.contains("local step_1, step_2, step_3, step_4, step_5"));
        assertTrue(lua.contains("Buttons = {\"Explore Path\", \"Rest Here\"}"));
    }

    @Test
    public void testMultipleChoiceQuestionLuaGeneration() {
        DialogBuilder db = new DialogBuilder("questChoiceDialog");
        db.addInitialFrame("Frame 1 text", "media1");
        db.addInitialFrame("Frame 2 text", "media2");
        db.addInitialFrame("Frame 3 text", "media3");

        db.setQuestion("Which path will you choose?", "mediaQuestion", DialogBuilder.QuestionType.MULTIPLE_CHOICE, List.of("Path North", "Path East", "Path West"));

        db.addBranchFrame("Path North", "You walk North into the snowy mountains.", "mediaNorth");
        db.addBranchFrame("Path East", "You travel East towards the sunny sea.", "mediaEast");
        db.addBranchFrame("Path West", "You venture West into the dark woods.", "mediaWest");

        String lua = db.toLuaCode();
        assertTrue(lua.contains("questChoiceDialog = function()"));
        assertTrue(lua.contains("MultipleChoice"));
        assertTrue(lua.contains("Path North"));
        assertTrue(lua.contains("Path East"));
        assertTrue(lua.contains("Path West"));
        assertTrue(lua.contains("Wherigo.GetInput("));
    }

    @Test
    public void testCheerpJBridgeBuildBranchingDialogLua() {
        String lua = CheerpJBridge.buildBranchingDialogLua(
            "testDialog",
            "YES_NO",
            new String[]{"Intro 1", "Intro 2", "Intro 3"},
            new String[]{"m1", "m2", "m3"},
            "Do you proceed?",
            "mQ",
            new String[]{"Yes", "No"},
            new String[]{"Yes", "No"},
            new String[]{"Proceeding!", "Cancelling!"},
            new String[]{"mYes", "mNo"}
        );

        assertTrue(lua.contains("testDialog = function()"));
        assertTrue(lua.contains("Intro 1"));
        assertTrue(lua.contains("Intro 2"));
        assertTrue(lua.contains("Intro 3"));
        assertTrue(lua.contains("Do you proceed?"));
        assertTrue(lua.contains("Proceeding!"));
        assertTrue(lua.contains("Cancelling!"));
    }

    @Test
    public void testLuaObjectBuilderTemplateIntegration() {
        LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.BRANCHING_DIALOG);
        assertEquals("dialogBranchingDemo", builder.getVariableName());
        String lua = builder.toLuaCode();
        assertTrue(lua.contains("dialogBranchingDemo = function()"));
        assertTrue(lua.contains("Wherigo.MessageBox({"));
    }

    @Test
    public void testDialogueTreeStructureState() {
        DialogBuilder db = DialogBuilder.createDefault3FrameDialog();

        // Level 0: Leftmost Initial Column
        List<DialogBuilder.DialogFrame> initialFrames = db.getInitialFrames();
        assertEquals(3, initialFrames.size());
        assertEquals("mediaIntro", initialFrames.get(0).getMediaVar());
        assertEquals("mediaRuins", initialFrames.get(1).getMediaVar());
        assertEquals("mediaGuardian", initialFrames.get(2).getMediaVar());

        // Level 1: Middle Question Node
        DialogBuilder.Question q = db.getQuestion();
        assertNotNull(q);
        assertEquals(DialogBuilder.QuestionType.YES_NO, q.getType());
        assertEquals("varRuinsAccepted", db.getResultVariable());

        // Level 2: Rightmost Branch Nodes
        List<DialogBuilder.DialogFrame> yesBranch = db.getBranchFrames("Yes");
        List<DialogBuilder.DialogFrame> noBranch = db.getBranchFrames("No");
        assertEquals(2, yesBranch.size());
        assertEquals(2, noBranch.size());
    }

    @Test
    public void testResultVariableLuaGeneration() {
        DialogBuilder db = DialogBuilder.createDefault3FrameDialog();
        db.setResultVariable("varUserAnswer");

        assertEquals("varUserAnswer", db.getResultVariable());
        String lua = db.toLuaCode();

        assertTrue(lua.contains("varUserAnswer = \"Yes\""));
        assertTrue(lua.contains("varUserAnswer = \"No\""));
    }

    @Test
    public void testYesNoBranchingDialogExecutionInKahluaVM() throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata("DialogTest", "Author", "1.0", "Desc", 0.0, 0.0, 0.0);
        DialogBuilder db = DialogBuilder.createDefault3FrameDialog();

        List<LuaObjectBuilder> objs = new ArrayList<>();
        LuaObjectBuilder dialogObj = new LuaObjectBuilder(db.getName(), "-- Branching Dialogue Function");
        dialogObj.addProperty("Code", db.toLuaCode(), LuaObjectBuilder.PropertyType.EXPRESSION);
        objs.add(dialogObj);

        byte[] gwc = CartridgeCompiler.compileToGwcBytes(meta, objs);
        File tempGwc = File.createTempFile("dialog_test_", ".gwc");
        tempGwc.deleteOnExit();
        java.nio.file.Files.write(tempGwc.toPath(), gwc);

        JavaFileHandle savefh = IntegratedPlayerService.buildSaveHandle(tempGwc);
        CartridgeFile cartridgeFile = IntegratedPlayerService.readCartridge(tempGwc, savefh);

        List<String> pushedDialogs = new ArrayList<>();
        TestUI ui = new TestUI(pushedDialogs);
        TestLocationService locService = new TestLocationService();

        Engine engine = IntegratedPlayerService.createEngine(cartridgeFile, ui, locService, "test-device", "1.0");
        assertNotNull(engine);
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

    private static class TestUI implements UI {
        private final List<String> pushedDialogs;

        public TestUI(List<String> pushedDialogs) {
            this.pushedDialogs = pushedDialogs;
        }

        @Override public void refresh() {}
        @Override public void start() {}
        @Override public void end() {}
        @Override public void showError(String msg) {}
        @Override public void debugMsg(String msg) {}
        @Override public void setStatusText(String text) {}

        @Override
        public void pushDialog(String[] texts, Media[] media, String button1, String button2, LuaClosure callback) {
            if (texts != null && texts.length > 0) {
                pushedDialogs.add(texts[0]);
            }
            if (callback != null) {
                Engine.invokeCallback(callback, "Button1");
            }
        }

        @Override
        public void pushInput(EventTable input) {
            String choice = "Yes";
            Engine.callEvent(input, "OnGetInput", choice);
        }

        @Override public void showScreen(ScreenId screenId, EventTable details) {}
        @Override public void playSound(byte[] data, String mime) {}
        @Override public void blockForSaving() {}
        @Override public void unblock() {}
        @Override public void command(String cmd) {}
    }
}
