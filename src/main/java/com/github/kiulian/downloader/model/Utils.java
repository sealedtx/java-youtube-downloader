package com.github.kiulian.downloader.model;




import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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

    // 1:32:54
    public static int parseLengthSeconds(String text) {
        try {
            int length = 0;
            int beginIndex = 0;
            if (text.length() > 2) {
                int endIndex;
                if (text.length() > 5) {
                    // contains hours
                    endIndex = text.indexOf(':');
                    length += Integer.parseInt(text.substring(0, endIndex)) * 3600;
                    beginIndex = endIndex + 1;
                }
                endIndex = text.indexOf(':', beginIndex);
                length += Integer.parseInt(text.substring(beginIndex, endIndex)) * 60;
                beginIndex = endIndex + 1;
            }
            length += Integer.parseInt(text.substring(beginIndex));
            return length;
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            return -1;
        }
    }

    public static long parseViewCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String value = text.replaceAll("[^0-9]", "");
        if (!value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public static String parseRuns(JSONObject container) {
        if (container == null) {
            return null;
        }
        JSONArray runs = container.getJSONArray("runs");
        if (runs == null) {
            return null;
        } else if (runs.size() == 1) {
            return runs.getJSONObject(0).getString("text");
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < runs.size(); i++) {
                builder.append(runs.getJSONObject(i).getString("text"));
            }
            return builder.toString();
        }
    }

    public static List<String> parseThumbnails(JSONObject container) {
        if (container == null) {
            return null;
        }
        JSONArray jsonThumbnails = container.getJSONArray("thumbnails");
        if (jsonThumbnails == null) {
            return null;
        } else {
            List<String> thumbnails = new ArrayList<String>(jsonThumbnails.size());
            for (int i = 0; i < jsonThumbnails.size(); i++) {
                JSONObject jsonThumbnail = jsonThumbnails.getJSONObject(i);
                if (jsonThumbnail.containsKey("url")) {
                    thumbnails.add(jsonThumbnail.getString("url"));
                }
            }
            return thumbnails;
        }
    }
}
