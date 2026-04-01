package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JavaFileHandle implementation.
 */
class JavaFileHandleTest {

    @Test
    void testExists_FileDoesNotExist(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("nonexistent.txt").toFile();
        JavaFileHandle handle = new JavaFileHandle(file);
        assertFalse(handle.exists());
    }

    @Test
    void testExists_FileExists(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("existing.txt").toFile();
        assertTrue(file.createNewFile());
        JavaFileHandle handle = new JavaFileHandle(file);
        assertTrue(handle.exists());
    }

    @Test
    void testCreate_CreatesNewFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("newfile.txt").toFile();
        JavaFileHandle handle = new JavaFileHandle(file);
        assertFalse(file.exists());

        handle.create();
        assertTrue(file.exists());
    }

    @Test
    void testCreate_ThrowsExceptionIfFileAlreadyExists(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("existing.txt").toFile();
        assertTrue(file.createNewFile());
        JavaFileHandle handle = new JavaFileHandle(file);

        assertThrows(IOException.class, () -> handle.create());
    }

    @Test
    void testDelete_DeletesExistingFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("todelete.txt").toFile();
        assertTrue(file.createNewFile());
        JavaFileHandle handle = new JavaFileHandle(file);

        handle.delete();
        assertFalse(file.exists());
    }

    @Test
    void testDelete_ThrowsExceptionIfFileDoesNotExist(@TempDir Path tempDir) {
        File file = tempDir.resolve("nonexistent.txt").toFile();
        JavaFileHandle handle = new JavaFileHandle(file);

        assertThrows(IOException.class, () -> handle.delete());
    }

    @Test
    void testOpenDataOutputStream_WritesData(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("output.txt").toFile();
        JavaFileHandle handle = new JavaFileHandle(file);

        try (DataOutputStream dos = handle.openDataOutputStream()) {
            dos.writeInt(42);
            dos.writeDouble(3.14);
            dos.writeUTF("Hello");
        }

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void testOpenDataInputStream_ReadsData(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("input.txt").toFile();

        // Write test data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(42);
            dos.writeDouble(3.14);
            dos.writeUTF("Hello");
        }

        // Read test data
        JavaFileHandle handle = new JavaFileHandle(file);
        try (DataInputStream dis = handle.openDataInputStream()) {
            assertEquals(42, dis.readInt());
            assertEquals(3.14, dis.readDouble(), 0.0001);
            assertEquals("Hello", dis.readUTF());
        }
    }

    @Test
    void testTruncate_ReducesFileSize(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("truncate.txt").toFile();

        // Write test data
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[100]);
        }

        assertEquals(100, file.length());

        // Truncate file
        JavaFileHandle handle = new JavaFileHandle(file);
        handle.truncate(50);

        assertEquals(50, file.length());
    }

    @Test
    void testTruncate_CanExpandFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("expand.txt").toFile();

        // Write test data
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[10]);
        }

        assertEquals(10, file.length());

        // Expand file
        JavaFileHandle handle = new JavaFileHandle(file);
        handle.truncate(100);

        assertEquals(100, file.length());
    }

    @Test
    void testOpenDataOutputStream_OverwritesExistingContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("overwrite.txt").toFile();

        // Write initial data
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeUTF("Initial content");
        }

        long initialLength = file.length();

        // Overwrite with new data
        JavaFileHandle handle = new JavaFileHandle(file);
        try (DataOutputStream dos = handle.openDataOutputStream()) {
            dos.writeUTF("New");
        }

        // File should be overwritten, not appended
        assertTrue(file.length() < initialLength);
    }
}
