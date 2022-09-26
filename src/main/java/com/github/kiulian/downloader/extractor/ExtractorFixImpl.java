package com.github.kiulian.downloader.extractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorFixImpl implements ExtractorFix {

    private static final Pattern TEXT_NUMBER_REGEX = Pattern.compile("[0-9]+[0-9, ']*");

    @Override
    public long extractIntegerFromText(String text) {
        Matcher matcher = TEXT_NUMBER_REGEX.matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }

}
