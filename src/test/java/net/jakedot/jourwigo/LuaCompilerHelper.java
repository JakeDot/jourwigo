package net.jakedot.jourwigo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Helper utility for compiling Lua source files into Lua 5.1 bytecode.
 */
public class LuaCompilerHelper {

    /**
     * Compiles the given Lua source file into Lua 5.1 bytecode.
     * Tries system `luac` / `luac5.1` first, and falls back to pre-compiled bytecode resource.
     */
    public static byte[] compileLuaSource(File luaSourceFile, String resourceFallbackPath) throws Exception {
        // Try system luac command
        byte[] compiled = trySystemLuac(luaSourceFile);
        if (compiled != null && compiled.length > 0) {
            return compiled;
        }

        // Fallback to pre-compiled bytecode resource
        if (resourceFallbackPath != null) {
            InputStream is = LuaCompilerHelper.class.getResourceAsStream(resourceFallbackPath);
            if (is != null) {
                try (is) {
                    return is.readAllBytes();
                }
            }
        }

        throw new IllegalStateException("Failed to compile Lua source and no pre-compiled fallback resource found.");
    }

    private static byte[] trySystemLuac(File luaSourceFile) {
        String[] luacCmds = new String[] { "luac", "luac5.1" };
        for (String cmd : luacCmds) {
            try {
                File tempOut = File.createTempFile("compiled_", ".lbc");
                tempOut.deleteOnExit();

                ProcessBuilder pb = new ProcessBuilder(cmd, "-o", tempOut.getAbsolutePath(), luaSourceFile.getAbsolutePath());
                Process process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);

                if (finished && process.exitValue() == 0 && tempOut.exists() && tempOut.length() > 0) {
                    byte[] bytes = Files.readAllBytes(tempOut.toPath());
                    tempOut.delete();
                    return bytes;
                }
            } catch (Exception ignored) {
                // Command not found or failed, try next
            }
        }
        return null;
    }
}
