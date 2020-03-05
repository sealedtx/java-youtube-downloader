package com.github.kiulian.downloader.model;

import com.github.kiulian.downloader.model.formats.Format;

import java.io.File;
import java.io.IOException;

class Utils {
    private static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    static String removeIllegalChars(String filename) {
        for (char c : ILLEGAL_FILENAME_CHARACTERS) {
            filename = filename.replace(c, '_');
        }
        return filename;
    }

    static File getOutputFile(VideoDetails videoDetails, Format format, File outDir) throws IOException {
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs)
                throw new IOException("Could not create output directory: " + outDir);
        }

        String fileName = videoDetails.title() + "." + format.extension().value();
        File outputFile = new File(outDir, removeIllegalChars(fileName));

        int i = 1;
        while (outputFile.exists()) {
            fileName = videoDetails.title() + "(" + i++ + ")" + "." + format.extension().value();
            outputFile = new File(outDir, removeIllegalChars(fileName));
        }

        return outputFile;
    }
}
