package com.github.kiulian.downloader.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesScalars() {
        assertEquals("a", Json.parse("\"a\"").asString());
        assertEquals(12L, Json.parse("12").value());
        assertEquals(-3L, Json.parse("-3").value());
        assertEquals(0L, Json.parse("-0").value());
        assertEquals(1.5, Json.parse("1.5").value());
        assertEquals(1000.0, Json.parse("1e3").value());
        assertEquals(0.025, Json.parse("2.5E-2").value());
        assertEquals(Boolean.TRUE, Json.parse("true").value());
        assertEquals(Boolean.FALSE, Json.parse(" false ").value());
        assertTrue(Json.parse("null").isNull());
    }

    @Test
    void keepsLongPrecisionAndFallsBackToDouble() {
        assertEquals(Long.MAX_VALUE, Json.parse("9223372036854775807").value());
        assertEquals(9.223372036854775808E18, Json.parse("9223372036854775808").value());
    }

    @Test
    void parsesNestedStructuresInOrder() {
        JsonNode node = Json.parse("{ \"b\" : [1, {\"c\": null}, []], \"a\": {} }");
        assertEquals(Arrays.asList("b", "a"), node.keys());
        assertEquals(1, node.at("b", 0).asInt(-1));
        assertTrue(node.at("b", 1, "c").isNull());
        assertEquals(0, node.at("b", 2).size());
        assertTrue(node.get("a").isObject());
    }

    @Test
    void decodesEscapes() {
        assertEquals("a\"b\\c/d\n\t\r\b\fé😀",
                Json.parse("\"a\\\"b\\\\c\\/d\\n\\t\\r\\b\\f\\u00e9\\ud83d\\ude00\"").asString());
    }

    @Test
    void acceptsRawControlCharactersInStrings() {
        assertEquals("a\tb", Json.parse("\"a\tb\"").asString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "{", "[", "[1,]", "[1 2]", "{\"a\":1,}", "{a:1}", "{\"a\" 1}", "{\"a\":}",
            "01", "1.", ".5", "-", "+1", "1e", "tru", "nul", "\"abc", "\"\\x\"", "\"\\u12g4\"", "\"\\u12\"", "1 2", "[1]]",
    })
    void rejectsInvalidJson(String text) {
        assertThrows(JsonException.class, () -> Json.parse(text));
    }

    @Test
    void reportsThePositionOfTheError() {
        JsonException error = assertThrows(JsonException.class, () -> Json.parse("[1,]"));
        assertTrue(error.getMessage().contains("position 3"), error.getMessage());
    }

    @Test
    void limitsNesting() {
        String deepButFine = repeat("[", 500) + repeat("]", 500);
        assertEquals(1, Json.parse(deepButFine).size());

        String tooDeep = repeat("[", 600) + repeat("]", 600);
        JsonException error = assertThrows(JsonException.class, () -> Json.parse(tooDeep));
        assertTrue(error.getMessage().contains("nesting"), error.getMessage());
    }

    @Test
    void parsesJsonEmbeddedInHtml() {
        String html = "<script>var ytInitialData = {\"a\":\"};</script>\",\"b\":[1,{\"c\":\"}\"}]};</script><script>var x = {};</script>";
        int start = html.indexOf("ytInitialData = ") + "ytInitialData = ".length();

        JsonNode data = Json.parsePrefix(html, start);
        assertEquals("};</script>", data.get("a").asString());
        assertEquals("}", data.at("b", 1, "c").asString());
    }

    @Test
    void parsePrefixSkipsLeadingWhitespace() {
        assertEquals(7, Json.parsePrefix("x =   7;", 3).asInt(-1));
    }

    @Test
    void writesCompactJson() {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientName", "VISIONOS");
        client.put("androidSdkVersion", 32);
        client.put("tags", Arrays.asList("a\"b", "\n", null, true));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("context", Collections.singletonMap("client", client));
        body.put("racyCheckOk", true);

        String json = Json.write(body);
        assertEquals("{\"context\":{\"client\":{\"clientName\":\"VISIONOS\",\"androidSdkVersion\":32,"
                + "\"tags\":[\"a\\\"b\",\"\\n\",null,true]}},\"racyCheckOk\":true}", json);
        assertEquals(Json.parse(json), Json.parse(Json.write(Json.parse(json))));
    }

    @Test
    void writesControlCharactersAsUnicodeEscapes() {
        assertEquals("\"\\u0001\\u001f\"", Json.write("\u0001\u001f"));
    }

    @Test
    void refusesValuesWithoutJsonForm() {
        assertThrows(JsonException.class, () -> Json.write(Double.NaN));
        assertThrows(JsonException.class, () -> Json.write(new Object()));
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
