package com.github.kiulian.downloader.internal;

import com.github.kiulian.downloader.model.search.field.DurationField;
import com.github.kiulian.downloader.model.search.field.FeatureField;
import com.github.kiulian.downloader.model.search.field.FormatField;
import com.github.kiulian.downloader.model.search.field.SearchField;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.model.search.field.UploadDateField;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Expected values are the ones the 3.x tests checked, without their double URL encoding of '='.
class SearchParamsTest {

    @Test
    void encodesSingleFilters() {
        assertFilter(UploadDateField.HOUR, "EgIIAQ==");
        assertFilter(UploadDateField.DAY, "EgIIAg==");
        assertFilter(UploadDateField.WEEK, "EgIIAw==");
        assertFilter(UploadDateField.MONTH, "EgIIBA==");
        assertFilter(UploadDateField.YEAR, "EgIIBQ==");
        assertFilter(TypeField.VIDEO, "EgIQAQ==");
        assertFilter(TypeField.CHANNEL, "EgIQAg==");
        assertFilter(TypeField.PLAYLIST, "EgIQAw==");
        assertFilter(TypeField.MOVIE, "EgIQBA==");
        assertFilter(DurationField.UNDER_4_MINUTES, "EgIYAQ==");
        assertFilter(DurationField.FROM_4_TO_20_MINUTES, "EgIYAw==");
        assertFilter(DurationField.OVER_20_MINUTES, "EgIYAg==");
        assertFilter(FeatureField.LIVE, "EgJAAQ==");
        assertFilter(FeatureField.SUBTITLES, "EgIoAQ==");
        assertFilter(FeatureField.CREATIVE_COMMONS, "EgIwAQ==");
        assertFilter(FeatureField.LOCATION, "EgO4AQE=");
        assertFilter(FeatureField.PURCHASED, "EgJIAQ==");
        assertFilter(FormatField._4K, "EgJwAQ==");
        assertFilter(FormatField.HD, "EgIgAQ==");
        assertFilter(FormatField._360, "EgJ4AQ==");
        assertFilter(FormatField.VR180, "EgPQAQE=");
        assertFilter(FormatField._3D, "EgI4AQ==");
        assertFilter(FormatField.HDR, "EgPIAQE=");
    }

    @Test
    void encodesSortOrder() {
        assertEquals("CAE=", SearchParams.encode(SortField.RATING, none(), false));
        assertEquals("CAI=", SearchParams.encode(SortField.UPLOAD_DATE, none(), false));
        assertEquals("CAM=", SearchParams.encode(SortField.VIEW_COUNT, none(), false));
    }

    @Test
    void encodesCombinations() {
        assertEquals("CAISBBABGAE=", SearchParams.encode(SortField.UPLOAD_DATE,
                Arrays.asList(TypeField.VIDEO, DurationField.UNDER_4_MINUTES), false));
        assertEquals("CAMSBggCEAEYAg==", SearchParams.encode(SortField.VIEW_COUNT,
                Arrays.asList(TypeField.VIDEO, UploadDateField.DAY, DurationField.OVER_20_MINUTES), false));
        assertEquals("CAMSCxABOAFwAXgByAEB", SearchParams.encode(SortField.VIEW_COUNT,
                Arrays.asList(TypeField.VIDEO, FormatField._4K, FormatField._360, FormatField._3D, FormatField.HDR), false));
    }

    @Test
    void encodesExactQuery() {
        assertEquals("QgIIAQ==", SearchParams.encode(null, none(), true));
        assertEquals("CAISAhABQgIIAQ==", SearchParams.encode(SortField.UPLOAD_DATE,
                Collections.singletonList(TypeField.VIDEO), true));
    }

    @Test
    void laterFilterOfTheSameCategoryWins() {
        assertEquals(SearchParams.encode(null, Collections.singletonList(TypeField.PLAYLIST), false),
                SearchParams.encode(null, Arrays.asList(TypeField.VIDEO, TypeField.PLAYLIST), false));
    }

    @Test
    void returnsNullWhenThereIsNothingToEncode() {
        assertNull(SearchParams.encode(null, none(), false));
    }

    private static void assertFilter(SearchField field, String expected) {
        assertEquals(expected, SearchParams.encode(null, Collections.singletonList(field), false), field.name());
    }

    private static List<SearchField> none() {
        return Collections.emptyList();
    }
}
