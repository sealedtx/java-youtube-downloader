package com.github.kiulian.downloader.internal.json;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonNodeTest {

    private final JsonNode doc = Json.parse("{\"contents\":{\"tabs\":[{\"title\":\"Videos\",\"count\":\"42\"}]},"
            + "\"n\":7,\"flag\":\"true\",\"empty\":null}");

    @Test
    void navigationThroughMissingValuesNeverThrows() {
        JsonNode missing = doc.get("nope").get(3).at("a", 0, "b").get("c");
        assertTrue(missing.isMissing());
        assertFalse(missing.isPresent());
        assertNull(missing.asString());
        assertEquals(-1, missing.asLong(-1));
        assertEquals(Collections.emptyList(), missing.items());
        assertEquals(Collections.emptyList(), missing.keys());
        assertEquals(0, missing.size());
    }

    @Test
    void wrongTypesAreMissingToo() {
        assertTrue(doc.get("n").get("x").isMissing());
        assertTrue(doc.get("contents").get(0).isMissing());
        assertTrue(doc.at("contents", "tabs", 5).isMissing());
        assertTrue(doc.at("contents", "tabs", -1).isMissing());
    }

    @Test
    void followsPathsOfKeysAndIndexes() {
        assertEquals("Videos", doc.at("contents", "tabs", 0, "title").asString());
        assertSame(JsonNode.missing(), doc.at("contents", "tabs", 0, "title", "deeper"));
    }

    @Test
    void orPicksTheFirstPresentAlternative() {
        JsonNode title = doc.at("contents", "tabs", 0, "title");
        assertEquals(title, doc.get("newTitle").or(title));
        assertEquals(title, doc.get("empty").or(title));
        assertEquals(title, title.or(doc.get("n")));
    }

    @Test
    void convertsLeniently() {
        assertEquals(42, doc.at("contents", "tabs", 0, "count").asInt(-1));
        assertEquals("7", doc.get("n").asString());
        assertTrue(doc.get("flag").asBoolean(false));
        assertEquals(7.0, doc.get("n").asDouble(0));
        assertEquals(-1, doc.at("contents", "tabs", 0, "title").asLong(-1));
        assertEquals(-1, Json.parse("12345678901").asInt(-1));
        assertNull(doc.get("contents").asString());
        assertEquals("fallback", doc.get("empty").asString("fallback"));
    }

    @Test
    void findsKeysAnywhereInDocumentOrder() {
        JsonNode node = Json.parse("{\"a\":{\"k\":1,\"b\":[{\"k\":2},{\"x\":{\"k\":3}}]},\"k\":4}");
        List<Long> found = new ArrayList<>();
        for (JsonNode k : node.findAll("k")) {
            found.add(k.asLong(-1));
        }
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), found);
        assertEquals(1, node.findFirst("k").asInt(-1));
        assertTrue(node.findFirst("absent").isMissing());
    }

    @Test
    void comparesByValueAndPrintsJson() {
        assertEquals(Json.parse("{\"a\":[1,2]}"), Json.parse(" { \"a\" : [ 1 , 2 ] } "));
        assertEquals("{\"a\":[1,2]}", Json.parse("{\"a\":[1,2]}").toString());
        assertEquals("<missing>", JsonNode.missing().toString());
    }
}
