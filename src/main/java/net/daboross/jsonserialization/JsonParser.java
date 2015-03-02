/*
 * Original JSON parser design Copyright (c) 2002 JSON.org
 * Overhaul and updates Copyright (c) 2015 David Ross <daboross@daboross.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * The Software shall be used for Good, not Evil.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.daboross.jsonserialization;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class allowing for parsing JSON values from a Reader or String.
 *
 * @author daboross@daboross.net (David Ross)
 */
public class JsonParser {

    private final Reader reader;
    private long characterNumber;
    private long index;
    private long lineNumber;
    private char previous;
    private boolean usePrevious;
    private boolean reachedEof;

    /**
     * Creates a new JsonParser which will read from the given Reader. The reader will not be buffered in JsonParser, so
     * you may want to use a BufferedReader.
     *
     * @param reader The reader to use
     */
    public JsonParser(Reader reader) {
        this.reader = reader;
        this.usePrevious = false;
        this.previous = 0;
        this.index = 0;
        this.characterNumber = 1;
        this.lineNumber = 1;
        this.reachedEof = false;
    }

    /**
     * Creates a new JsonParser which will read from the given String. It is recommended to instead use
     * JsonParser(Reader) if you are reading from a File or other source besides a hardcoded String literal.
     * <p>
     * This will simply call JsonParser(Reader) with a new {@link java.io.StringReader} reading from the given string.
     *
     * @param input The string to read from.
     */
    public JsonParser(String input) {
        this(new StringReader(input));
    }

    /**
     * Back up one character. This provides a sort of lookahead capability, so that you can test for a digit or letter
     * before attempting to parse the next number or identifier.
     *
     * @throws IllegalStateException if .back() has already been used since .next() was last used
     */
    private void back() {
        if (this.usePrevious || this.index <= 0) {
            throw new IllegalStateException("Stepping back two steps is not supported");
        }
        this.index -= 1;
        this.characterNumber -= 1;
        this.usePrevious = true;
        this.reachedEof = false;
    }

    /**
     * Get the next character in the source string.
     *
     * @return The next character, or -1 if past the end of the source string.
     * @throws JsonException if end of file is reached.
     */
    public int nextAllowingEof() throws JsonException, IOException {
        if (reachedEof) {
            return -1;
        }
        char c;
        if (this.usePrevious) {
            this.usePrevious = false;
            c = this.previous;
        } else {
            int i = this.reader.read();

            if (i < 0) { // End of stream
                this.reachedEof = true;
                return -1;
            } else {
                c = (char) i;
            }
        }
        this.index += 1;
        if (this.previous == '\r') {
            this.lineNumber += 1;
            this.characterNumber = c == '\n' ? 0 : 1;
        } else if (c == '\n') {
            this.lineNumber += 1;
            this.characterNumber = 0;
        } else {
            this.characterNumber += 1;
        }
        this.previous = c;
        return c;
    }

    public char next() throws JsonException, IOException {
        int c = nextAllowingEof();
        if (c < 0) {
            throw syntaxError("Unexpected end of file");
        } else {
            return (char) c;
        }
    }

    /**
     * Get the next n characters.
     *
     * @param n The number of characters to take.
     * @return A string of n characters.
     * @throws JsonException If end of file is reached before n characters are read.
     * @throws IOException   If the underlying writer throws an IOException.
     */
    public String next(int n) throws JsonException, IOException {
        if (n == 0) {
            return "";
        }
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = this.next();
        }
        return new String(chars);
    }

    /**
     * Get the next char in the string, skipping whitespace.
     *
     * @return A character
     * @throws JsonException If end of file is reached before a non-whitespace character is read.
     * @throws IOException   If the underlying reader throws an IOException.
     */
    public char nextClean() throws IOException, JsonException {
        while (true) {
            char c = this.next();
            if (c > ' ') {
                return c;
            }
        }
    }

    /**
     * Return the characters up to the next `"` (double quote) character. Backslash processing is performed.
     *
     * @return A String literal.
     * @throws JsonException if a newline or end of file is reached before the end quote is found, or if an unknown
     *                       escape pattern is found.
     * @throws IOException   If the underlying reader throws an IOException.
     */
    public String nextString() throws JsonException, IOException {
        if (nextClean() != '\"') {
            back();
            throw syntaxError("Invalid string: expected `\"`, found `" + previous + "`");
        }
        char c;
        StringBuilder sb = new StringBuilder();
        while (true) {
            c = this.next();
            switch (c) {
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string: Expected end of string (\"), found newline");
                case '\\':
                    c = this.next();
                    switch (c) {
                        case 'b':
                            sb.append('\b');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 'u':
                            sb.append((char) Integer.parseInt(this.next(4), 16));
                            break;
                        case '\"':
                        case '\'':
                        case '\\':
                        case '/':
                            sb.append(c);
                            break;
                        default:
                            throw this.syntaxError("Illegal escape: Expected \\b, \\t, \\n, \\f, \\r, \\u, \\\", \\', \\\\ or \\/, found `" + c + "`");
                    }
                    break;
                case '\"':
                    return sb.toString();
                default:
                    sb.append(c);
            }
        }
    }

    /**
     * Reads a item from the reader.
     *
     * @return A Boolean, Double, Integer, Long, String, List, Map or null.
     * @throws JsonException If there is a syntax error in the item, or end of file is reached before the item is
     *                       terminated.
     * @throws IOException   If the underlying reader throws an IOException.
     */
    public Object nextItem() throws JsonException, IOException {
        char c = this.nextClean();
        this.back();
        switch (c) {
            case '"':
                return this.nextString();
            case '{':
                return parseJsonObject();
            case '[':
                return parseJsonArray();
            default:
                return nextRawString();
        }
    }

    /**
     * Try to parse a raw string into a number, boolean, or null.
     *
     * @return A simple JSON value. Boolean, Integer, Long, Double or null.
     * @throws JsonException if the string up to the next deliminator isn't parsable as an int, double, long and it
     *                       isn't "true", "false" or "null"
     */
    public Object nextRawString() throws IOException, JsonException {
        // Accumulate characters until we reach the end of the text or a formatting character.

        // Strictly speaking, we want to be able to be able to parse *just* a raw string as a valid json value,
        // so we want to allow reachign EOF if the full string is valid as a raw string. Users should be able
        // to use nextItem() on anything, even if the outer most construct is a raw value not an object or array.

        StringBuilder builder = new StringBuilder();
        for (int c = nextAllowingEof(); c >= ' ' && "[]{},:=#".indexOf(c) < 0; c = nextAllowingEof()) {
            builder.append((char) c);
        }
        this.back();

        String result = builder.toString();
        if (result.isEmpty()) {
            if (reachedEof) {
                throw this.syntaxError("Unexpected end of file");
            } else {
                throw this.syntaxError("Missing value: Expected item, found `" + previous + "`");
            }
        }
        if (result.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (result.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (result.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            return Integer.valueOf(result);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.valueOf(result);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.valueOf(result);
        } catch (NumberFormatException ignored) {
        }
        throw this.syntaxError("Invalid item: expected true, false or number, found `" + result + "`");
    }

    /**
     * Parses a json object item from the reader. Note that this implementation *does* allow for trailing commas, though
     * that is the only deviation it allows from the standard specification.
     *
     * @return A new LinkedHashMap containing the keys and values from the json object
     * @throws JsonException If end of file is reached before the map is terminated, or if there are any syntax errors
     *                       in the map or items in the map.
     * @throws IOException   If the underlying reader throws an IOException.
     */
    public Map<String, Object> parseJsonObject() throws JsonException, IOException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        char c; // reused temporary variable
        String key;

        if (nextClean() != '{') {
            back();
            throw syntaxError("Invalid json object: Expected `{`, found `" + previous + "`");
        }
        while (true) {
            c = nextClean();
            switch (c) {
                case '}':
                    return map;
                default:
                    back();
                    key = nextItem().toString();
            }

            // The key is followed by ':'.
            c = nextClean();
            if (c != ':') {
                throw syntaxError("Expected `:` after key, found `" + c + "`");
            }
            if (map.put(key, nextItem()) != null) {
                // if we already had this key
                throw syntaxError("Expected unique key, found duplicate key \"" + key + "\"");
            }

            // Pairs are separated by ','.
            c = nextClean();
            switch (c) {
                case ',':
                    if (nextClean() == '}') {
                        return map;
                    }
                    back();
                    break;
                case '}':
                    return map;
                default:
                    throw syntaxError("Expected `,` or `}`, found `" + c + '`');
            }
        }
    }

    /**
     * Parses a json array from the reader. Note that this implementation *does* allow for trailing commas, though that
     * is the only deviation it allows from the standard specification.
     *
     * @return A (new) List containing the keys and values from the json object
     * @throws JsonException If end of file is reached before the array is terminated, or if there are any syntax errors
     *                       in the array or items in the array.
     * @throws IOException   If the underlying reader throws an IOException.
     */
    public List<Object> parseJsonArray() throws JsonException, IOException {
        List<Object> list = new ArrayList<Object>();
        if (nextClean() != '[') {
            back();
            throw syntaxError("Invalid json array input: expected `[`, found `" + previous + "`");
        }
        if (nextClean() != ']') {
            back();
            while (true) {
                if (nextClean() == ',') {
                    throw syntaxError("Invalid json array: expected item, found `,`");
                } else {
                    back();
                    list.add(nextItem());
                }
                switch (nextClean()) {
                    case ',':
                        if (nextClean() == ']') {
                            return list;
                        }
                        back();
                        break;
                    case ']':
                        return list;
                    default:
                        throw syntaxError("Expected a ',' or ']'");
                }
            }
        }
        return list;
    }

    /**
     * Returns a string in the format of " at {index} [character {character number} line {line number}]"
     *
     * @return The position string
     */
    public String getPositionString() {
        return " at " + this.index + " [character " + this.characterNumber + " line " + this.lineNumber + "]";
    }

    /**
     * Make a JsonException to signal a syntax error.
     *
     * @param message The error message
     * @return A JsonException suitable for throwing
     */
    public JsonException syntaxError(String message) {
        return new JsonException(message + getPositionString());
    }

    /**
     * Make a printable string of this JsonParser.
     *
     * @return A string in the format of "JsonTokener at {index} [character {character} line {line}]"
     */
    @Override
    public String toString() {
        return "JsonTokener" + getPositionString();
    }
}
