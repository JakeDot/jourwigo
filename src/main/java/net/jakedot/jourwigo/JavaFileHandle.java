package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.platform.FileHandle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Standard Java implementation of {@link FileHandle} using {@link java.io.File}.
 * Used by the savegame system to read and write game state.
 */
public class JavaFileHandle implements FileHandle {

    private final File file;

    public JavaFileHandle(File file) {
        this.file = file;
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(new FileInputStream(file));
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(new FileOutputStream(file, false));
    }

    @Override
    public boolean exists() throws IOException {
        return file.exists();
    }

    @Override
    public void create() throws IOException {
        if (!file.createNewFile()) {
            throw new IOException("Could not create file: " + file.getAbsolutePath());
        }
    }

    @Override
    public void delete() throws IOException {
        if (!file.delete()) {
            throw new IOException("Could not delete file: " + file.getAbsolutePath());
        }
    }

    @Override
    public void truncate(long len) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(len);
        }
    }
}
