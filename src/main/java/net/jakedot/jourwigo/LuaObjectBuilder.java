package net.jakedot.jourwigo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Flexible builder for constructing Lua objects and Wherigo class instances (Zone, Character, Item, etc.).
 */
public class LuaObjectBuilder {

    public enum PropertyType {
        STRING,
        NUMBER,
        BOOLEAN,
        EXPRESSION
    }

    public static class LuaProperty {
        private String key;
        private String value;
        private PropertyType type;

        public LuaProperty(String key, String value, PropertyType type) {
            this.key = key != null ? key.trim() : "";
            this.value = value != null ? value : "";
            this.type = type != null ? type : PropertyType.STRING;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key != null ? key.trim() : "";
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value != null ? value : "";
        }

        public PropertyType getType() {
            return type;
        }

        public void setType(PropertyType type) {
            this.type = type != null ? type : PropertyType.STRING;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LuaProperty property = (LuaProperty) o;
            return Objects.equals(key, property.key) &&
                   Objects.equals(value, property.value) &&
                   type == property.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, type);
        }
    }

    public enum Template {
        ZONE("Zone", "zoneStart", "Wherigo.Zone(Cartridge)"),
        CHARACTER("Character", "characterGuide", "Wherigo.ZCharacter(Cartridge)"),
        ITEM("Item", "itemKey", "Wherigo.ZItem(Cartridge)"),
        CARTRIDGE("Cartridge", "cartridge", "Wherigo.ZCartridge()"),
        TASK("Task", "taskQuest", "Wherigo.ZTask(Cartridge)"),
        TIMER("Timer", "timerCountdown", "Wherigo.ZTimer(Cartridge)"),
        MEDIA("Media", "mediaInfo", "Wherigo.ZMedia(Cartridge)"),
        MEDIA_IMAGE("Media (Image)", "mediaImage", "Wherigo.ZMedia(Cartridge)"),
        MEDIA_AUDIO("Media (Audio)", "mediaAudio", "Wherigo.ZMedia(Cartridge)"),
        MEDIA_VIDEO("Media (Video)", "mediaVideo", "Wherigo.ZMedia(Cartridge)"),
        MEDIA_TEXT("Media (Text)", "mediaText", "Wherigo.ZMedia(Cartridge)"),
        INPUT("Input", "inputPrompt", "Wherigo.ZInput(Cartridge)"),
        COMMAND("Command", "cmdExamine", "Wherigo.ZCommand(Cartridge)"),
        ZONEPOINT("ZonePoint", "zonePoint", "Wherigo.ZonePoint(0.0, 0.0, 0.0)"),
        BRANCHING_DIALOG("Branching Dialog", "dialogBranchingDemo", "-- Branching Dialogue"),
        VARIABLE("Variable", "varState", "\"\""),
        CUSTOM("Custom Object", "customObj", "{}");

        private final String displayName;
        private final String defaultVarName;
        private final String defaultConstructor;

        Template(String displayName, String defaultVarName, String defaultConstructor) {
            this.displayName = displayName;
            this.defaultVarName = defaultVarName;
            this.defaultConstructor = defaultConstructor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultVarName() {
            return defaultVarName;
        }

        public String getDefaultConstructor() {
            return defaultConstructor;
        }

        public static Template fromDisplayName(String displayName) {
            if (displayName == null) {
                return CUSTOM;
            }
            for (Template t : values()) {
                if (t.displayName.equalsIgnoreCase(displayName.trim()) || t.name().equalsIgnoreCase(displayName.trim())) {
                    return t;
                }
            }
            return CUSTOM;
        }
    }

    private String variableName;
    private String constructorExpression;
    private final List<LuaProperty> properties = new ArrayList<>();

    public LuaObjectBuilder() {
        this("customObj", "{}");
    }

    public LuaObjectBuilder(String variableName, String constructorExpression) {
        this.variableName = variableName != null ? variableName.trim() : "obj";
        this.constructorExpression = constructorExpression != null ? constructorExpression.trim() : "{}";
    }

    public static LuaObjectBuilder createForTemplate(Template template) {
        if (template == null) {
            template = Template.CUSTOM;
        }
        LuaObjectBuilder builder = new LuaObjectBuilder(template.getDefaultVarName(), template.getDefaultConstructor());
        switch (template) {
            case ZONE:
                builder.addProperty("Name", "Start Zone", PropertyType.STRING);
                builder.addProperty("Description", "A starting zone.", PropertyType.STRING);
                builder.addProperty("Visible", "true", PropertyType.BOOLEAN);
                builder.addProperty("Active", "true", PropertyType.BOOLEAN);
                builder.addProperty("Points", "{\n    Wherigo.ZonePoint(0.0, 0.0, 0.0)\n}", PropertyType.EXPRESSION);
                break;
            case CHARACTER:
                builder.addProperty("Name", "Guide NPC", PropertyType.STRING);
                builder.addProperty("Description", "A friendly guide character.", PropertyType.STRING);
                builder.addProperty("Gender", "Male", PropertyType.STRING);
                builder.addProperty("Visible", "true", PropertyType.BOOLEAN);
                break;
            case ITEM:
                builder.addProperty("Name", "Golden Key", PropertyType.STRING);
                builder.addProperty("Description", "A shiny key.", PropertyType.STRING);
                builder.addProperty("Visible", "true", PropertyType.BOOLEAN);
                break;
            case CARTRIDGE:
                builder.addProperty("Name", "New Cartridge", PropertyType.STRING);
                builder.addProperty("Description", "Cartridge description", PropertyType.STRING);
                builder.addProperty("Author", "Author", PropertyType.STRING);
                builder.addProperty("Version", "1.0", PropertyType.STRING);
                builder.addProperty("Activity", "TourGuide", PropertyType.STRING);
                break;
            case TASK:
                builder.addProperty("Name", "Task Quest", PropertyType.STRING);
                builder.addProperty("Description", "Task description", PropertyType.STRING);
                builder.addProperty("Active", "true", PropertyType.BOOLEAN);
                builder.addProperty("Complete", "false", PropertyType.BOOLEAN);
                break;
            case TIMER:
                builder.addProperty("Name", "Test Timer", PropertyType.STRING);
                builder.addProperty("Type", "Countdown", PropertyType.STRING);
                builder.addProperty("Duration", "10", PropertyType.NUMBER);
                break;
            case MEDIA:
                builder.addProperty("Name", "Sample Media", PropertyType.STRING);
                builder.addProperty("Filename", "sample.mp3", PropertyType.STRING);
                builder.addProperty("Type", "mp3", PropertyType.STRING);
                builder.addProperty("AltText", "Audio description", PropertyType.STRING);
                break;
            case MEDIA_IMAGE:
                builder.addProperty("Name", "Image Media", PropertyType.STRING);
                builder.addProperty("Filename", "sample.png", PropertyType.STRING);
                builder.addProperty("Type", "png", PropertyType.STRING);
                builder.addProperty("AltText", "Image description", PropertyType.STRING);
                builder.addProperty("Category", "Image", PropertyType.STRING);
                break;
            case MEDIA_AUDIO:
                builder.addProperty("Name", "Audio Media", PropertyType.STRING);
                builder.addProperty("Filename", "sample.mp3", PropertyType.STRING);
                builder.addProperty("Type", "mp3", PropertyType.STRING);
                builder.addProperty("AltText", "Audio description", PropertyType.STRING);
                builder.addProperty("Category", "Audio", PropertyType.STRING);
                break;
            case MEDIA_VIDEO:
                builder.addProperty("Name", "Video Media", PropertyType.STRING);
                builder.addProperty("Filename", "sample.mp4", PropertyType.STRING);
                builder.addProperty("Type", "mp4", PropertyType.STRING);
                builder.addProperty("AltText", "Video description", PropertyType.STRING);
                builder.addProperty("Category", "Video", PropertyType.STRING);
                break;
            case MEDIA_TEXT:
                builder.addProperty("Name", "Text Media", PropertyType.STRING);
                builder.addProperty("Filename", "sample.txt", PropertyType.STRING);
                builder.addProperty("Type", "txt", PropertyType.STRING);
                builder.addProperty("AltText", "Text description", PropertyType.STRING);
                builder.addProperty("Text", "Sample text content", PropertyType.STRING);
                builder.addProperty("Category", "Text", PropertyType.STRING);
                break;
            case INPUT:
                builder.addProperty("Name", "Passcode Prompt", PropertyType.STRING);
                builder.addProperty("Question", "Enter passcode:", PropertyType.STRING);
                builder.addProperty("InputType", "Text", PropertyType.STRING);
                break;
            case COMMAND:
                builder.addProperty("Name", "Examine", PropertyType.STRING);
                builder.addProperty("Text", "Examine object", PropertyType.STRING);
                builder.addProperty("Enabled", "true", PropertyType.BOOLEAN);
                break;
            case ZONEPOINT:
                // ZonePoint constructor takes parameters directly
                break;
            case BRANCHING_DIALOG:
                DialogBuilder db = DialogBuilder.createDefault3FrameDialog();
                builder.setVariableName(db.getName());
                builder.setConstructorExpression("-- Branching Dialogue Function");
                builder.addProperty("Code", db.toLuaCode(), PropertyType.EXPRESSION);
                break;
            case VARIABLE:
                builder.addProperty("Value", "", PropertyType.STRING);
                break;
            case CUSTOM:
            default:
                break;
        }
        return builder;
    }

    public String getVariableName() {
        return variableName;
    }

    public LuaObjectBuilder setVariableName(String variableName) {
        this.variableName = variableName != null ? variableName.trim() : "obj";
        return this;
    }

    public String getConstructorExpression() {
        return constructorExpression;
    }

    public LuaObjectBuilder setConstructorExpression(String constructorExpression) {
        this.constructorExpression = constructorExpression != null ? constructorExpression.trim() : "{}";
        return this;
    }

    public List<LuaProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public LuaObjectBuilder addProperty(String key, String value, PropertyType type) {
        if (key != null && !key.trim().isEmpty()) {
            properties.add(new LuaProperty(key, value, type));
        }
        return this;
    }

    public LuaObjectBuilder removeProperty(int index) {
        if (index >= 0 && index < properties.size()) {
            properties.remove(index);
        }
        return this;
    }

    public LuaObjectBuilder removeProperty(String key) {
        if (key != null) {
            properties.removeIf(p -> p.getKey().equalsIgnoreCase(key.trim()));
        }
        return this;
    }

    public LuaObjectBuilder clearProperties() {
        properties.clear();
        return this;
    }

    public String toLuaCode() {
        StringBuilder sb = new StringBuilder();
        String var = (variableName == null || variableName.isBlank()) ? "obj" : variableName;
        String constructor = (constructorExpression == null || constructorExpression.isBlank()) ? "{}" : constructorExpression;

        if (constructor.startsWith("--") || "custom".equalsIgnoreCase(var)) {
            for (LuaProperty prop : properties) {
                if ("Code".equalsIgnoreCase(prop.getKey())) {
                    return prop.getValue() + "\n";
                }
            }
        }

        if ("\"\"".equals(constructor) || "0".equals(constructor) || "false".equals(constructor) || "nil".equals(constructor)) {
            LuaProperty valProp = properties.stream().filter(p -> "Value".equalsIgnoreCase(p.getKey())).findFirst().orElse(null);
            if (valProp != null) {
                sb.append(var).append(" = ").append(formatPropertyValue(valProp.getValue(), valProp.getType())).append("\n");
                return sb.toString();
            }
        }

        sb.append(var).append(" = ").append(constructor).append("\n");

        for (LuaProperty prop : properties) {
            String key = prop.getKey();
            if (key == null || key.isBlank() || "Value".equalsIgnoreCase(key) && ("\"\"".equals(constructor) || "0".equals(constructor))) {
                continue;
            }
            sb.append(var).append(".").append(key).append(" = ");
            sb.append(formatPropertyValue(prop.getValue(), prop.getType()));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String formatPropertyValue(String val, PropertyType type) {
        if (type == null) {
            type = PropertyType.STRING;
        }
        if (val == null) {
            val = "";
        }
        return switch (type) {
            case STRING -> "\"" + escapeLua(val) + "\"";
            case NUMBER -> {
                String trimmed = val.trim();
                try {
                    Double.parseDouble(trimmed);
                    yield trimmed;
                } catch (NumberFormatException e) {
                    yield "0";
                }
            }
            case BOOLEAN -> {
                String trimmed = val.trim().toLowerCase();
                yield ("true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed)) ? "true" : "false";
            }
            case EXPRESSION -> val.trim();
        };
    }

    public static String escapeLua(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n");
    }
}
