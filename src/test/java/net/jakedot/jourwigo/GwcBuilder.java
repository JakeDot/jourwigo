package net.jakedot.jourwigo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for creating GWC cartridge files for testing.
 */
public class GwcBuilder {

    private String name = "Test Cartridge";
    private String description = "Test Cartridge Description";
    private String author = "Jourwigo";
    private String version = "1.0.0";
    private double latitude = 51.5074;
    private double longitude = -0.1278;
    private String code = "123456";

    public GwcBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public GwcBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public GwcBuilder setAuthor(String author) {
        this.author = author;
        return this;
    }

    public GwcBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    public GwcBuilder setCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    public GwcBuilder setCode(String code) {
        this.code = code;
        return this;
    }

    public byte[] buildGwcBytes(byte[] bytecode) throws IOException {
        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();

        // Header content (excluding header length prefix)
        writeDouble(headerBaos, latitude);
        writeDouble(headerBaos, longitude);
        headerBaos.write(new byte[8]); // 8 zeroes
        headerBaos.write(new byte[8]); // 8 unknown bytes
        writeShort(headerBaos, (short) 0); // splashId
        writeShort(headerBaos, (short) 0); // iconId
        writeNullString(headerBaos, "Wherigo"); // type
        writeNullString(headerBaos, "JourwigoPlayer"); // member
        headerBaos.write(new byte[8]); // 8 unknown bytes
        writeNullString(headerBaos, name);
        writeNullString(headerBaos, "12345678-1234-1234-1234-123456789012"); // GUID
        writeNullString(headerBaos, description);
        writeNullString(headerBaos, "Start description");
        writeNullString(headerBaos, version);
        writeNullString(headerBaos, author);
        writeNullString(headerBaos, "https://github.com/JakeDot/jourwigo");
        writeNullString(headerBaos, "MIDP-2.0/CLDC-1.1");
        headerBaos.write(new byte[4]); // 4 unknown bytes
        writeNullString(headerBaos, code);

        byte[] headerBytes = headerBaos.toByteArray();

        // Calculate offsets
        // Header starts at position: 7 (magic) + 2 (files count) + 2 (id) + 4 (offset) = 15
        int headerPos = 15;
        int bytecodeOffset = headerPos + 4 + headerBytes.length; // 4 bytes for header length int

        ByteArrayOutputStream gwcBaos = new ByteArrayOutputStream();

        // 1. Magic
        gwcBaos.write(new byte[] { 0x02, 0x0a, 0x43, 0x41, 0x52, 0x54, 0x00 });

        // 2. Scan offsets table
        writeShort(gwcBaos, (short) 1); // 1 file
        writeShort(gwcBaos, (short) 0); // id = 0
        writeInt(gwcBaos, bytecodeOffset);

        // 3. Scan header table
        writeInt(gwcBaos, headerBytes.length);
        gwcBaos.write(headerBytes);

        // 4. File 0 (bytecode) data at bytecodeOffset
        writeInt(gwcBaos, bytecode.length);
        gwcBaos.write(bytecode);

        return gwcBaos.toByteArray();
    }

    public File buildGwcFile(byte[] bytecode, File outputFile) throws IOException {
        byte[] gwcBytes = buildGwcBytes(bytecode);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(gwcBytes);
        }
        return outputFile;
    }

    private static void writeShort(ByteArrayOutputStream baos, short value) {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(value);
        baos.write(bb.array(), 0, 2);
    }

    private static void writeInt(ByteArrayOutputStream baos, int value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        baos.write(bb.array(), 0, 4);
    }

    private static void writeDouble(ByteArrayOutputStream baos, double value) {
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(value);
        baos.write(bb.array(), 0, 8);
    }

    private static void writeNullString(ByteArrayOutputStream baos, String str) throws IOException {
        if (str != null) {
            baos.write(str.getBytes(StandardCharsets.UTF_8));
        }
        baos.write(0);
    }
}
