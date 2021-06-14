package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.File;
import java.util.UUID;

import static com.github.kiulian.downloader.model.Utils.removeIllegalChars;

public class RequestVideoFileDownload extends Request<RequestVideoFileDownload, File> {

    private File outputDirectory = new File("videos");
    private boolean overwrite = false;
    private String fileName = UUID.randomUUID().toString();

    private final Format format;

    public RequestVideoFileDownload(Format format) {
        this.format = format;
    }

    public RequestVideoFileDownload saveTo(File directory) {
        this.outputDirectory = directory;
        return this;
    }

    public RequestVideoFileDownload renameTo(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public RequestVideoFileDownload overwriteIfExists(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public Format getFormat() {
        return format;
    }

    public File getOutputFile() {
        String originalName = removeIllegalChars(fileName);
        String fileName = originalName + "." + format.extension().value();
        File outputFile = new File(outputDirectory, fileName);

        if (!overwrite) {
            int i = 1;
            while (outputFile.exists()) {
                fileName = originalName + "(" + i++ + ")" + "." + format.extension().value();
                outputFile = new File(outputFile.getParentFile(), fileName);
            }
        }
        return outputFile;
    }
}
