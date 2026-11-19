package com.github.kiulian.downloader.internal.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only, null-safe view of a parsed JSON value.
 * <p>
 * Navigation never throws and never returns {@code null}: a missing key, a wrong type or an index out of range
 * gives a {@link #isMissing() missing} node, and value accessors on it return the given default. This keeps
 * extraction code short and lets it survive when YouTube moves or drops a field.
 */
public final class JsonNode {

    private static final JsonNode MISSING = new JsonNode(null, true);
    private static final JsonNode NULL = new JsonNode(null, false);

    private final Object value;
    private final boolean missing;

    private JsonNode(Object value, boolean missing) {
        this.value = value;
        this.missing = missing;
    }

    static JsonNode of(Object value) {
        return value == null ? NULL : new JsonNode(value, false);
    }

    /** The node for an absent value. */
    public static JsonNode missing() {
        return MISSING;
    }

    Object value() {
        return value;
    }

    /** Value of an object member, or a missing node. */
    public JsonNode get(String key) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.containsKey(key)) {
                return of(map.get(key));
            }
        }
        return MISSING;
    }

    /** Array element, or a missing node. */
    public JsonNode get(int index) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (index >= 0 && index < list.size()) {
                return of(list.get(index));
            }
        }
        return MISSING;
    }

    /** Follows a path of object keys ({@code String}) and array indexes ({@code Integer}). */
    public JsonNode at(Object... path) {
        JsonNode node = this;
        for (Object step : path) {
            node = step instanceof Integer ? node.get((Integer) step) : node.get(String.valueOf(step));
            if (node.missing) {
                return MISSING;
            }
        }
        return node;
    }

    /** This node if it has a value, otherwise {@code other}. Used for alternative paths. */
    public JsonNode or(JsonNode other) {
        return isPresent() ? this : other;
    }

    /** True if the key, index or path did not exist. */
    public boolean isMissing() {
        return missing;
    }

    /** True if the value exists and is JSON {@code null}. */
    public boolean isNull() {
        return !missing && value == null;
    }

    /** True if the value exists and is not JSON {@code null}. */
    public boolean isPresent() {
        return value != null;
    }

    public boolean isObject() {
        return value instanceof Map;
    }

    public boolean isArray() {
        return value instanceof List;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    /** String value; numbers and booleans as text; {@code null} for objects, arrays and absent values. */
    public String asString() {
        return asString(null);
    }

    /** String value; numbers and booleans as text; {@code defaultValue} for objects, arrays and absent values. */
    public String asString(String defaultValue) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return defaultValue;
    }

    /** Integer number; integer strings such as {@code "1234"} count too (YouTube sends many numbers as strings). */
    public long asLong(long defaultValue) {
        Long number = longOrNull();
        return number != null ? number : defaultValue;
    }

    /** Like {@link #asLong(long)}, and {@code defaultValue} when the number does not fit into an int. */
    public int asInt(int defaultValue) {
        Long number = longOrNull();
        return number != null && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE
                ? number.intValue() : defaultValue;
    }

    /** Number; numeric strings count too. */
    public double asDouble(double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // not a number
            }
        }
        return defaultValue;
    }

    /** Boolean; the strings {@code "true"} and {@code "false"} count too. */
    public boolean asBoolean(boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            if (s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return defaultValue;
    }

    /** Array elements; empty for anything that is not an array. */
    public List<JsonNode> items() {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> list = (List<?>) value;
        List<JsonNode> nodes = new ArrayList<>(list.size());
        for (Object item : list) {
            nodes.add(of(item));
        }
        return nodes;
    }

    /** Object keys in document order; empty for anything that is not an object. */
    public List<String> keys() {
        if (!(value instanceof Map)) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<>();
        for (Object key : ((Map<?, ?>) value).keySet()) {
            keys.add((String) key);
        }
        return keys;
    }

    /** Number of array elements or object members; 0 for anything else. */
    public int size() {
        if (value instanceof List) {
            return ((List<?>) value).size();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        }
        return 0;
    }

    /** All values stored under {@code key} anywhere below this node, in document order. */
    public List<JsonNode> findAll(String key) {
        List<JsonNode> found = new ArrayList<>();
        collect(value, key, found, Integer.MAX_VALUE);
        return found;
    }

    /** The first value stored under {@code key} anywhere below this node, or a missing node. */
    public JsonNode findFirst(String key) {
        List<JsonNode> found = new ArrayList<>(1);
        collect(value, key, found, 1);
        return found.isEmpty() ? MISSING : found.get(0);
    }

    // Depth is bounded by the parser's nesting limit.
    private static boolean collect(Object value, String key, List<JsonNode> found, int limit) {
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (key.equals(entry.getKey())) {
                    found.add(of(entry.getValue()));
                    if (found.size() >= limit) {
                        return true;
                    }
                }
                if (collect(entry.getValue(), key, found, limit)) {
                    return true;
                }
            }
        } else if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (collect(item, key, found, limit)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Long longOrNull() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // not an integer
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonNode)) {
            return false;
        }
        JsonNode other = (JsonNode) o;
        return missing == other.missing && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, missing);
    }

    /** JSON text of this node, {@code <missing>} for a missing node. */
    @Override
    public String toString() {
        return missing ? "<missing>" : Json.write(value);
    }
}
