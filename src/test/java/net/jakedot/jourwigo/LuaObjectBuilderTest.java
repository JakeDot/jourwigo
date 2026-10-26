package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LuaObjectBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    public void testAllTemplatesCreation() {
        for (LuaObjectBuilder.Template template : LuaObjectBuilder.Template.values()) {
            LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(template);
            assertNotNull(builder, "Builder should not be null for template: " + template);
            assertNotNull(builder.getVariableName(), "Var name should not be null for " + template);
            assertNotNull(builder.getConstructorExpression(), "Constructor should not be null for " + template);

            String luaCode = builder.toLuaCode();
            assertNotNull(luaCode);
            assertTrue(luaCode.contains(builder.getVariableName() + " = "), "Lua code should assign variable for " + template);
        }
    }

    @Test
    public void testVariableTemplateProperties() {
        LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.VARIABLE);
        assertEquals("varState", builder.getVariableName());
        assertEquals("\"\"", builder.getConstructorExpression());
        String lua = builder.toLuaCode();
        assertTrue(lua.contains("varState = \"\""));
    }

    @Test
    public void testZoneTemplateProperties() {
        LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ZONE);
        assertEquals("zoneStart", builder.getVariableName());
        assertEquals("Wherigo.Zone(Cartridge)", builder.getConstructorExpression());

        String code = builder.toLuaCode();
        assertTrue(code.contains("zoneStart = Wherigo.Zone(Cartridge)"));
        assertTrue(code.contains("zoneStart.Name = \"Start Zone\""));
        assertTrue(code.contains("zoneStart.Visible = true"));
        assertTrue(code.contains("zoneStart.Active = true"));
    }

    @Test
    public void testCharacterTemplateProperties() {
        LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.CHARACTER);
        assertEquals("characterGuide", builder.getVariableName());

        String code = builder.toLuaCode();
        assertTrue(code.contains("characterGuide = Wherigo.ZCharacter(Cartridge)"));
        assertTrue(code.contains("characterGuide.Name = \"Guide NPC\""));
        assertTrue(code.contains("characterGuide.Gender = \"Male\""));
    }

    @Test
    public void testItemTemplateProperties() {
        LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.ITEM);
        assertEquals("itemKey", builder.getVariableName());

        String code = builder.toLuaCode();
        assertTrue(code.contains("itemKey = Wherigo.ZItem(Cartridge)"));
        assertTrue(code.contains("itemKey.Name = \"Golden Key\""));
    }

    @Test
    public void testMediaSubTemplatesProperties() {
        LuaObjectBuilder imgBuilder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.MEDIA_IMAGE);
        assertEquals("mediaImage", imgBuilder.getVariableName());
        String imgCode = imgBuilder.toLuaCode();
        assertTrue(imgCode.contains("mediaImage = Wherigo.ZMedia(Cartridge)"));
        assertTrue(imgCode.contains("mediaImage.Category = \"Image\""));

        LuaObjectBuilder audioBuilder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.MEDIA_AUDIO);
        assertEquals("mediaAudio", audioBuilder.getVariableName());
        String audioCode = audioBuilder.toLuaCode();
        assertTrue(audioCode.contains("mediaAudio.Category = \"Audio\""));

        LuaObjectBuilder videoBuilder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.MEDIA_VIDEO);
        assertEquals("mediaVideo", videoBuilder.getVariableName());
        String videoCode = videoBuilder.toLuaCode();
        assertTrue(videoCode.contains("mediaVideo.Category = \"Video\""));

        LuaObjectBuilder textBuilder = LuaObjectBuilder.createForTemplate(LuaObjectBuilder.Template.MEDIA_TEXT);
        assertEquals("mediaText", textBuilder.getVariableName());
        String textCode = textBuilder.toLuaCode();
        assertTrue(textCode.contains("mediaText.Category = \"Text\""));
        assertTrue(textCode.contains("mediaText.Text = \"Sample text content\""));
    }

    @Test
    public void testCustomPropertyAdditionAndRemoval() {
        LuaObjectBuilder builder = new LuaObjectBuilder("myObj", "{}");
        builder.addProperty("Prop1", "Hello \"World\"", LuaObjectBuilder.PropertyType.STRING);
        builder.addProperty("Prop2", "42", LuaObjectBuilder.PropertyType.NUMBER);
        builder.addProperty("Prop3", "yes", LuaObjectBuilder.PropertyType.BOOLEAN);
        builder.addProperty("Prop4", "Wherigo.Player", LuaObjectBuilder.PropertyType.EXPRESSION);

        assertEquals(4, builder.getProperties().size());

        String code = builder.toLuaCode();
        assertTrue(code.contains("myObj.Prop1 = \"Hello \\\"World\\\"\""));
        assertTrue(code.contains("myObj.Prop2 = 42"));
        assertTrue(code.contains("myObj.Prop3 = true"));
        assertTrue(code.contains("myObj.Prop4 = Wherigo.Player"));

        builder.removeProperty("Prop2");
        assertEquals(3, builder.getProperties().size());
        assertFalse(builder.toLuaCode().contains("myObj.Prop2"));

        builder.removeProperty(0);
        assertEquals(2, builder.getProperties().size());

        builder.clearProperties();
        assertEquals(0, builder.getProperties().size());
    }

    @Test
    public void testPropertyValueFormatting() {
        assertEquals("\"test\\\\value\"", LuaObjectBuilder.formatPropertyValue("test\\value", LuaObjectBuilder.PropertyType.STRING));
        assertEquals("123.45", LuaObjectBuilder.formatPropertyValue("123.45", LuaObjectBuilder.PropertyType.NUMBER));
        assertEquals("0", LuaObjectBuilder.formatPropertyValue("not_a_number", LuaObjectBuilder.PropertyType.NUMBER));
        assertEquals("true", LuaObjectBuilder.formatPropertyValue("true", LuaObjectBuilder.PropertyType.BOOLEAN));
        assertEquals("true", LuaObjectBuilder.formatPropertyValue("1", LuaObjectBuilder.PropertyType.BOOLEAN));
        assertEquals("false", LuaObjectBuilder.formatPropertyValue("false", LuaObjectBuilder.PropertyType.BOOLEAN));
        assertEquals("false", LuaObjectBuilder.formatPropertyValue("no", LuaObjectBuilder.PropertyType.BOOLEAN));
        assertEquals("math.max(1, 2)", LuaObjectBuilder.formatPropertyValue("math.max(1, 2)", LuaObjectBuilder.PropertyType.EXPRESSION));
    }

    @Test
    public void testTemplateFromDisplayName() {
        assertEquals(LuaObjectBuilder.Template.ZONE, LuaObjectBuilder.Template.fromDisplayName("Zone"));
        assertEquals(LuaObjectBuilder.Template.CHARACTER, LuaObjectBuilder.Template.fromDisplayName("Character"));
        assertEquals(LuaObjectBuilder.Template.ITEM, LuaObjectBuilder.Template.fromDisplayName("Item"));
        assertEquals(LuaObjectBuilder.Template.CUSTOM, LuaObjectBuilder.Template.fromDisplayName("UnknownType"));
    }

    @Test
    public void testGeneratedLuaCodeCompiles() throws Exception {
        for (LuaObjectBuilder.Template template : LuaObjectBuilder.Template.values()) {
            LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(template);
            String fullLua = "Wherigo = { Zone = function() return {} end, ZCharacter = function() return {} end, ZItem = function() return {} end, ZCartridge = function() return {} end, ZTask = function() return {} end, ZTimer = function() return {} end, ZMedia = function() return {} end, ZInput = function() return {} end, ZCommand = function() return {} end, ZonePoint = function() return {} end }\n"
                + "Cartridge = {}\n"
                + builder.toLuaCode();

            File luaFile = tempDir.resolve("test_" + template.name() + ".lua").toFile();
            Files.writeString(luaFile.toPath(), fullLua);

            byte[] compiled = LuaCompilerHelper.compileLuaSource(luaFile);
            assertNotNull(compiled);
            assertTrue(compiled.length > 0, "Compiled bytecode should not be empty for template: " + template);
        }
    }
}
