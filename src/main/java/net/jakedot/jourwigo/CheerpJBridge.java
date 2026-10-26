package net.jakedot.jourwigo;

import java.util.ArrayList;
import java.util.List;

/**
 * CheerpJ bridge class exposing static methods for client-side WebAssembly browser interop.
 */
public final class CheerpJBridge {

    private CheerpJBridge() {
        // utility class
    }

    public static String generateLuaTemplate(String name, String author, String version, String description,
                                             double lat, double lon, double alt) {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(name, author, version, description, lat, lon, alt);
        return CartridgeCompiler.buildLuaScript(meta, null);
    }

    public static byte[] compileGwcBytes(String name, String author, String version, String description,
                                         double lat, double lon, double alt) throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(name, author, version, description, lat, lon, alt);
        return CartridgeCompiler.compileToGwcBytes(meta, null);
    }

    public static byte[] compileGwcFromLuaBytes(String name, String author, String version, String description,
                                                double lat, double lon, double alt,
                                                String luaScript) throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(name, author, version, description, lat, lon, alt);
        return CartridgeCompiler.compileLuaToGwcBytes(meta, luaScript);
    }

    public static String buildBranchingDialogLua(String name, String questionTypeStr,
                                                 String[] initialTexts, String[] initialMedias,
                                                 String questionText, String questionMedia, String[] choices,
                                                 String[] branchChoices, String[] branchTexts, String[] branchMedias) {
        DialogBuilder.QuestionType qType;
        try {
            qType = DialogBuilder.QuestionType.valueOf(questionTypeStr.toUpperCase().trim());
        } catch (Exception e) {
            qType = DialogBuilder.QuestionType.YES_NO;
        }

        DialogBuilder builder = new DialogBuilder(name);

        if (initialTexts != null) {
            for (int i = 0; i < initialTexts.length; i++) {
                String text = initialTexts[i];
                if (text == null || text.isBlank()) continue;
                String media = (initialMedias != null && i < initialMedias.length) ? initialMedias[i] : null;
                builder.addInitialFrame(text, media);
            }
        }

        List<String> choiceList = new ArrayList<>();
        if (choices != null) {
            for (String c : choices) {
                if (c != null && !c.isBlank()) {
                    choiceList.add(c.trim());
                }
            }
        }

        builder.setQuestion(questionText, questionMedia, qType, choiceList);

        if (branchChoices != null) {
            for (int i = 0; i < branchChoices.length; i++) {
                String choice = branchChoices[i];
                if (choice == null || choice.isBlank()) continue;
                String text = (branchTexts != null && i < branchTexts.length) ? branchTexts[i] : "";
                String media = (branchMedias != null && i < branchMedias.length) ? branchMedias[i] : null;
                builder.addBranchFrame(choice, text, media);
            }
        }

        return builder.toLuaCode();
    }

    public static String buildLuaObject(String templateName, String varName, String constructor,
                                        String[] keys, String[] values, String[] types) {
        LuaObjectBuilder.Template template = LuaObjectBuilder.Template.fromDisplayName(templateName);
        LuaObjectBuilder builder = new LuaObjectBuilder(
            varName != null && !varName.isBlank() ? varName : template.getDefaultVarName(),
            constructor != null && !constructor.isBlank() ? constructor : template.getDefaultConstructor()
        );

        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];
                if (k == null || k.isBlank()) continue;
                String v = (values != null && i < values.length) ? values[i] : "";
                String t = (types != null && i < types.length) ? types[i] : "STRING";
                LuaObjectBuilder.PropertyType pType;
                try {
                    pType = LuaObjectBuilder.PropertyType.valueOf(t.toUpperCase().trim());
                } catch (Exception e) {
                    pType = LuaObjectBuilder.PropertyType.STRING;
                }
                builder.addProperty(k, v, pType);
            }
        }
        return builder.toLuaCode();
    }

    public static byte[] compileGwcWithObjectBytes(String name, String author, String version, String description,
                                                   double lat, double lon, double alt,
                                                   String templateName, String varName, String constructor,
                                                   String[] keys, String[] values, String[] types) throws Exception {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(name, author, version, description, lat, lon, alt);
        List<LuaObjectBuilder> objects = new ArrayList<>();

        if (templateName != null || varName != null || keys != null) {
            LuaObjectBuilder.Template template = LuaObjectBuilder.Template.fromDisplayName(templateName);
            LuaObjectBuilder builder = new LuaObjectBuilder(
                varName != null && !varName.isBlank() ? varName : template.getDefaultVarName(),
                constructor != null && !constructor.isBlank() ? constructor : template.getDefaultConstructor()
            );

            if (keys != null) {
                for (int i = 0; i < keys.length; i++) {
                    String k = keys[i];
                    if (k == null || k.isBlank()) continue;
                    String v = (values != null && i < values.length) ? values[i] : "";
                    String t = (types != null && i < types.length) ? types[i] : "STRING";
                    LuaObjectBuilder.PropertyType pType;
                    try {
                        pType = LuaObjectBuilder.PropertyType.valueOf(t.toUpperCase().trim());
                    } catch (Exception e) {
                        pType = LuaObjectBuilder.PropertyType.STRING;
                    }
                    builder.addProperty(k, v, pType);
                }
            }
            objects.add(builder);
        }

        return CartridgeCompiler.compileToGwcBytes(meta, objects);
    }
}
