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
