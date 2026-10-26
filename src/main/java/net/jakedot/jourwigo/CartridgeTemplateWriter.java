package net.jakedot.jourwigo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class CartridgeTemplateWriter {

    private CartridgeTemplateWriter() {
        // utility class
    }

    public static void writeLuaTemplate(Path path,
                                        String name,
                                        String author,
                                        String version,
                                        String description,
                                        double latitude,
                                        double longitude,
                                        double altitude) throws IOException {
        writeLuaTemplate(path, name, author, version, description, latitude, longitude, altitude, Collections.emptyList());
    }

    public static void writeLuaTemplate(Path path,
                                        String name,
                                        String author,
                                        String version,
                                        String description,
                                        double latitude,
                                        double longitude,
                                        double altitude,
                                        List<LuaObjectBuilder> objects) throws IOException {
        Files.writeString(path, buildLuaTemplate(name, author, version, description, latitude, longitude, altitude, objects),
            StandardCharsets.UTF_8);
    }

    static String buildLuaTemplate(String name,
                                   String author,
                                   String version,
                                   String description,
                                   double latitude,
                                   double longitude,
                                   double altitude) {
        return buildLuaTemplate(name, author, version, description, latitude, longitude, altitude, Collections.emptyList());
    }

    static String buildLuaTemplate(String name,
                                   String author,
                                   String version,
                                   String description,
                                   double latitude,
                                   double longitude,
                                   double altitude,
                                   List<LuaObjectBuilder> objects) {
        CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(
            name, author, version, description, latitude, longitude, altitude
        );
        return CartridgeCompiler.buildLuaScript(meta, objects);
    }
}
