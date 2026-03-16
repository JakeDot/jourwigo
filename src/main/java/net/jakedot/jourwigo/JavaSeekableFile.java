package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.platform.SeekableFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Standard Java implementation of {@link SeekableFile} using {@link RandomAccessFile}.
 * Reads binary data in little-endian byte order as required by the GWC cartridge format.
 */
public class JavaSeekableFile implements SeekableFile {

    private final RandomAccessFile raf;
    private final ByteBuffer buf4 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    public JavaSeekableFile(File file) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
    }

    @Override
    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }

    @Override
    public long position() throws IOException {
        return raf.getFilePointer();
    }

    @Override
    public long skip(long what) throws IOException {
        long skipped = 0;
        while (skipped < what) {
            int s = raf.skipBytes((int) Math.min(what - skipped, Integer.MAX_VALUE));
            if (s <= 0) break;
            skipped += s;
        }
        return skipped;
    }

    @Override
    public short readShort() throws IOException {
        buf4.clear();
        buf4.limit(2);
        raf.readFully(buf4.array(), 0, 2);
        return buf4.getShort(0);
    }

    @Override
    public int readInt() throws IOException {
        buf4.clear();
        buf4.limit(4);
        raf.readFully(buf4.array(), 0, 4);
        return buf4.getInt(0);
    }

    @Override
    public double readDouble() throws IOException {
        buf4.clear();
        buf4.limit(8);
        raf.readFully(buf4.array(), 0, 8);
        return buf4.getDouble(0);
    }

    @Override
    public long readLong() throws IOException {
        buf4.clear();
        buf4.limit(8);
        raf.readFully(buf4.array(), 0, 8);
        return buf4.getLong(0);
    }

    @Override
    public void readFully(byte[] buf) throws IOException {
        raf.readFully(buf);
    }

    @Override
    public String readString() throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = raf.read()) > 0) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    public void close() throws IOException {
        raf.close();
    }
}
