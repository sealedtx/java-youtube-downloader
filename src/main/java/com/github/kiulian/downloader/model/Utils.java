package com.github.kiulian.downloader.model;




import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class Utils {

    private static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public static String removeIllegalChars(String fileName) {
        for (char c : ILLEGAL_FILENAME_CHARACTERS) {
            fileName = fileName.replace(c, '_');
        }
        return fileName;
    }

    public static void createOutDir(File outDir) throws IOException {
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs)
                throw new IOException("Could not create output directory: " + outDir);
        }
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {} 
        }
    }
}
