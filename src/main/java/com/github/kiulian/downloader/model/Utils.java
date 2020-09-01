package com.github.kiulian.downloader.model;

import com.github.kiulian.downloader.model.formats.Format;

import java.io.File;
import java.io.IOException;

class Utils {
    private static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    static String removeIllegalChars(String fileName) {
        for (char c : ILLEGAL_FILENAME_CHARACTERS) {
            fileName = fileName.replace(c, '_');
        }
        return fileName;
    }

    static void createOutDir(File outDir) throws IOException {
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs)
                throw new IOException("Could not create output directory: " + outDir);
        }
    }

    static File getOutputFile(final String name, Format format, File outDir, boolean overwrite) {
        String fileName = name + "." + format.extension().value();
        File outputFile = new File(outDir, fileName);

        if (!overwrite) {
            int i = 1;
            while (outputFile.exists()) {
                fileName = name + "(" + i++ + ")" + "." + format.extension().value();
                outputFile = new File(outDir, fileName);
            }
        }

        return outputFile;
    }
}
