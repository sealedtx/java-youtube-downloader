package com.github.kiulian.downloader.model;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2020 Igor Kiulian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #
 */


import com.github.kiulian.downloader.model.formats.Format;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class Utils {

    private static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    private static String removeIllegalChars(String fileName) {
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
        String fileName = removeIllegalChars(name) + "." + format.extension().value();
        File outputFile = new File(outDir, fileName);

        if (!overwrite) {
            int i = 1;
            while (outputFile.exists()) {
                fileName = removeIllegalChars(name) + "(" + i++ + ")" + "." + format.extension().value();
                outputFile = new File(outDir, fileName);
            }
        }

        return outputFile;
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {} 
        }
    }
}
