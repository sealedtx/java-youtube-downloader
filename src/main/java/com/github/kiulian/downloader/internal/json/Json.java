package com.github.kiulian.downloader.internal.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser and writer for the needs of this library (RFC 8259).
 * <p>
 * Parsed values are plain Java types wrapped in {@link JsonNode}: {@code Map<String, Object>} for objects,
 * {@code List<Object>} for arrays, {@code String}, {@code Long} or {@code Double} for numbers, {@code Boolean}
 * and {@code null}. The parser is strict except that it accepts raw control characters inside strings.
 */
public final class Json {

    /** Deeper input is rejected, so hostile input cannot overflow the stack. */
    static final int MAX_DEPTH = 512;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Json() {
    }

    /**
     * Parses a complete JSON document. Only whitespace may follow the value.
     */
    public static JsonNode parse(String text) {
        Parser parser = new Parser(text, 0);
        Object value = parser.value(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("unexpected data after the JSON value", parser.pos);
        }
        return JsonNode.of(value);
    }

    /**
     * Parses one JSON value that starts at {@code offset} (after optional whitespace) and ignores everything
     * after it. Used for JSON embedded in HTML, such as {@code var ytInitialData = {...};</script>}.
     */
    public static JsonNode parsePrefix(String text, int offset) {
        if (offset < 0 || offset > text.length()) {
            throw new IndexOutOfBoundsException("offset " + offset + " outside of text with length " + text.length());
        }
        return JsonNode.of(new Parser(text, offset).value(0));
    }

    /**
     * Writes {@code null}, {@code String}, {@code Number}, {@code Boolean}, {@code Map}, {@code Iterable} and
     * {@link JsonNode} values as compact JSON.
     */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (depth > MAX_DEPTH) {
            throw new JsonException("cannot write values nested deeper than " + MAX_DEPTH);
        }
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            writeString((String) value, out);
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Number) {
            writeNumber((Number) value, out);
        } else if (value instanceof JsonNode) {
            write(((JsonNode) value).value(), out, depth);
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out, depth + 1);
            }
            out.append('}');
        } else if (value instanceof Iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(item, out, depth + 1);
            }
            out.append(']');
        } else {
            throw new JsonException("cannot write " + value.getClass().getName() + " as JSON");
        }
    }

    private static void writeNumber(Number number, StringBuilder out) {
        if (number instanceof Double || number instanceof Float) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new JsonException("JSON has no representation for " + d);
            }
        }
        out.append(number);
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        out.append("\\u00").append(HEX[c >> 4]).append(HEX[c & 0xF]);
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s, int pos) {
            this.s = s;
            this.pos = pos;
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("nesting deeper than " + MAX_DEPTH, pos);
            }
            skipWhitespace();
            if (pos >= s.length()) {
                throw error("unexpected end of input", pos);
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return object(depth);
                case '[':
                    return array(depth);
                case '"':
                    return string();
                case 't':
                    literal("true");
                    return Boolean.TRUE;
                case 'f':
                    literal("false");
                    return Boolean.FALSE;
                case 'n':
                    literal("null");
                    return null;
                default:
                    if (c == '-' || isDigit(c)) {
                        return number();
                    }
                    throw error("unexpected character '" + c + "'", pos);
            }
        }

        private Map<String, Object> object(int depth) {
            pos++; // '{'
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("expected a string key", pos);
                }
                String key = string();
                skipWhitespace();
                if (peek() != ':') {
                    throw error("expected ':'", pos);
                }
                pos++;
                map.put(key, value(depth + 1));
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("expected ',' or '}'", pos - 1);
                }
            }
        }

        private List<Object> array(int depth) {
            pos++; // '['
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value(depth + 1));
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error("expected ',' or ']'", pos - 1);
                }
            }
        }

        private String string() {
            int start = pos;
            pos++; // opening quote
            int from = pos;
            // fast path: no escapes
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '"') {
                    return s.substring(from, pos++);
                }
                if (c == '\\') {
                    break;
                }
                pos++;
            }
            StringBuilder sb = new StringBuilder(pos - from + 16).append(s, from, pos);
            while (true) {
                if (pos >= s.length()) {
                    throw error("unterminated string", start);
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw error("unterminated string", start);
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(unicodeEscape());
                        break;
                    default:
                        throw error("invalid escape '\\" + e + "'", pos - 2);
                }
            }
        }

        // Surrogate pairs arrive as two escapes and end up as two chars, which is valid UTF-16.
        private char unicodeEscape() {
            if (pos + 4 > s.length()) {
                throw error("invalid unicode escape", pos - 2);
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = hexDigit(s.charAt(pos + i));
                if (digit < 0) {
                    throw error("invalid unicode escape", pos - 2);
                }
                value = value * 16 + digit;
            }
            pos += 4;
            return (char) value;
        }

        private Number number() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            if (peek() == '0') {
                pos++;
            } else if (isDigit(peek())) {
                skipDigits();
            } else {
                throw error("invalid number", start);
            }
            boolean integral = true;
            if (peek() == '.') {
                integral = false;
                pos++;
                if (!isDigit(peek())) {
                    throw error("invalid number", start);
                }
                skipDigits();
            }
            if (peek() == 'e' || peek() == 'E') {
                integral = false;
                pos++;
                if (peek() == '+' || peek() == '-') {
                    pos++;
                }
                if (!isDigit(peek())) {
                    throw error("invalid number", start);
                }
                skipDigits();
            }
            String text = s.substring(start, pos);
            if (integral) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException tooBig) {
                    // falls through to double
                }
            }
            return Double.parseDouble(text);
        }

        private void literal(String word) {
            if (!s.startsWith(word, pos)) {
                throw error("invalid literal", pos);
            }
            pos += word.length();
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    return;
                }
                pos++;
            }
        }

        private void skipDigits() {
            while (isDigit(peek())) {
                pos++;
            }
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        JsonException error(String message, int at) {
            int from = Math.max(0, at - 20);
            int to = Math.min(s.length(), at + 20);
            String near = s.substring(from, to).replaceAll("\\s+", " ");
            return new JsonException("Invalid JSON: " + message + " at position " + at + " near '" + near + "'");
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static int hexDigit(char c) {
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
            if (c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            }
            if (c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            }
            return -1;
        }
    }
}
