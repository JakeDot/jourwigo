package net.jakedot.jourwigo;

import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.DumpState;
import org.luaj.vm2.compiler.LuaC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Helper utility for compiling Lua source files into Lua 5.1 bytecode.
 */
public class LuaCompilerHelper {

    /**
     * Compiles the given Lua source file into Lua 5.1 bytecode.
     * Uses pure Java Luaj compiler first, falls back to system `luac`, and then pre-compiled resource.
     */
    public static byte[] compileLuaSource(File luaSourceFile, String resourceFallbackPath) throws Exception {
        // 1. Try pure Java Luaj compiler
        try {
            byte[] compiled = compileWithLuaj(luaSourceFile);
            if (compiled != null && compiled.length > 0) {
                return compiled;
            }
        } catch (Exception ignored) {
            // Fallback to next methods if pure Java compilation fails
        }

        // 2. Try system luac command
        byte[] compiled = trySystemLuac(luaSourceFile);
        if (compiled != null && compiled.length > 0) {
            return compiled;
        }

        // 3. Fallback to pre-compiled bytecode resource
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

    private static byte[] compileWithLuaj(File luaSourceFile) throws Exception {
        try (InputStream is = new FileInputStream(luaSourceFile)) {
            Prototype prototype = LuaC.compile(is, "@" + luaSourceFile.getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DumpState.dump(prototype, baos, true);
            return baos.toByteArray();
        }
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
