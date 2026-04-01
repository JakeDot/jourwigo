package net.jakedot.jourwigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JavaSeekableFile implementation.
 */
class JavaSeekableFileTest {

    @Test
    void testReadShort_LittleEndian(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write short in little-endian format
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0x1234);
            fos.write(buf.array());
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            short value = sf.readShort();
            assertEquals(0x1234, value);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadInt_LittleEndian(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write int in little-endian format
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(0x12345678);
            fos.write(buf.array());
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            int value = sf.readInt();
            assertEquals(0x12345678, value);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadDouble_LittleEndian(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write double in little-endian format
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            buf.putDouble(3.141592653589793);
            fos.write(buf.array());
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            double value = sf.readDouble();
            assertEquals(3.141592653589793, value, 0.0000001);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadLong_LittleEndian(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write long in little-endian format
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            buf.putLong(0x123456789ABCDEF0L);
            fos.write(buf.array());
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            long value = sf.readLong();
            assertEquals(0x123456789ABCDEF0L, value);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadString_NullTerminated(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write null-terminated string
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("Hello World".getBytes());
            fos.write(0); // null terminator
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            String value = sf.readString();
            assertEquals("Hello World", value);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadString_EmptyString(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write just null terminator
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(0);
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            String value = sf.readString();
            assertEquals("", value);
        } finally {
            sf.close();
        }
    }

    @Test
    void testReadFully(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            byte[] buffer = new byte[10];
            sf.readFully(buffer);
            assertArrayEquals(data, buffer);
        } finally {
            sf.close();
        }
    }

    @Test
    void testRead_SingleByte(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[]{42, 43, 44});
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            assertEquals(42, sf.read());
            assertEquals(43, sf.read());
            assertEquals(44, sf.read());
            assertEquals(-1, sf.read()); // EOF
        } finally {
            sf.close();
        }
    }

    @Test
    void testSeek_ChangesPosition(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[]{10, 20, 30, 40, 50});
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            sf.seek(2);
            assertEquals(30, sf.read());
            sf.seek(4);
            assertEquals(50, sf.read());
            sf.seek(0);
            assertEquals(10, sf.read());
        } finally {
            sf.close();
        }
    }

    @Test
    void testPosition_ReturnsCurrentPosition(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[100]);
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            assertEquals(0, sf.position());
            sf.read();
            assertEquals(1, sf.position());
            sf.seek(50);
            assertEquals(50, sf.position());
        } finally {
            sf.close();
        }
    }

    @Test
    void testSkip_SkipsBytesCorrectly(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[100]);
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            long skipped = sf.skip(10);
            assertEquals(10, skipped);
            assertEquals(10, sf.position());
        } finally {
            sf.close();
        }
    }

    @Test
    void testClose_ClosesUnderlyingFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[10]);
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        sf.close();

        // Attempting to read after close should throw IOException
        assertThrows(IOException.class, () -> sf.read());
    }

    @Test
    void testMultipleReads_InSequence(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.bin").toFile();

        // Write mixed data types
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer buf = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 100);
            buf.putInt(200);
            buf.putLong(300L);
            buf.putDouble(400.5);
            fos.write(buf.array(), 0, buf.position());
        }

        JavaSeekableFile sf = new JavaSeekableFile(file);
        try {
            assertEquals(100, sf.readShort());
            assertEquals(200, sf.readInt());
            assertEquals(300L, sf.readLong());
            assertEquals(400.5, sf.readDouble(), 0.001);
        } finally {
            sf.close();
        }
    }
}
