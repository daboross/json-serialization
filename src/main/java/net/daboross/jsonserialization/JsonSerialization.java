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
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * Class allowing for serializing Java Maps/Lists as json objects/arrays to a writer.
 *
 * @author daboross@daboross.net (David Ross)
 */
public class JsonSerialization {

    /**
     * Writes a number to the given writer in a format valid for json.
     *
     * @param writer The writer to write to.
     * @param number The number to write.
     * @throws JsonException If number is not finite.
     * @throws IOException   If the writer throws an IOException.
     */
    public static void writeNumber(Writer writer, Number number) throws JsonException, IOException {
        if (!Double.isFinite(number.doubleValue())) {
            throw new JsonException("Expected finite number, found `" + number + "`");
        }
        writer.write(number.toString());
    }

    /**
     * Writes a string to the given writer as an escaped json string.
     *
     * @param writer The writer to write to.
     * @param string The string to write.
     * @throws IOException If the writer throws an IOException.
     */
    public static void writeString(Writer writer, String string) throws IOException {
        writer.write('\"');
        char previousChar = 0;
        for (char c : string.toCharArray()) {
            switch (c) {
                case '\\':
                case '\"':
                    writer.write('\\');
                    writer.write(c);
                    break;
                case '/':
                    if (previousChar == '<') {
                        writer.write('\\');
                    }
                    writer.write(c);
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        writer.write("\\u");
                        String hexStringTemp = Integer.toHexString(c);
                        writer.write("0000", 0, 4 - hexStringTemp.length());
                        writer.write(hexStringTemp);
                    } else {
                        writer.write(c);
                    }
            }
            previousChar = c;
        }
        writer.write('\"');
    }

    private static void writeIndent(Writer writer, int indentAmount) throws IOException {
        for (int i = 0; i < indentAmount; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Writes a json object string from the given Map to the given writer. Please note that this assumes that no data
     * structures are cyclical, and that all iterables are finite.
     *
     * @param writer The writer to write to.
     * @param values The value map. All keys will be proccessed with String.valueOf(), and values will be formatted
     *               depending on type. Of values taken from the map: All Iterables will be treated as json arrays, and
     *               all maps will be formatted as json maps.
     * @throws JsonException If a value of an unknown type is found.
     * @throws IOException   If the writer throws an IOException.
     */
    public static Writer writeJsonObject(Writer writer, Map<?, ?> values, int indentFactor, int indent) throws JsonException, IOException {
        boolean commanate = false;
        final int length = values.size();
        Iterator<? extends Map.Entry<?, ?>> entries = values.entrySet().iterator();
        writer.write('{');

        if (length == 1) {
            Map.Entry entry = entries.next();
            writeString(writer, String.valueOf(entry.getKey()));
            writer.write(':');
            if (indentFactor > 0) {
                writer.write(' ');
            }
            writeJsonValue(writer, entry.getValue(), indentFactor, indent);
        } else if (length != 0) {
            final int newindent = indent + indentFactor;
            while (entries.hasNext()) {
                Map.Entry entry = entries.next();
                if (commanate) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                writeIndent(writer, newindent);
                writeString(writer, String.valueOf(entry.getKey()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeJsonValue(writer, entry.getValue(), indentFactor, newindent);
                commanate = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            writeIndent(writer, indent);
        }
        writer.write('}');
        return writer;
    }

    /**
     * Writes a json array string from the given Iterable to the given writer.　Please note that this assumes that no
     * data structures are cyclical, and that all iterables are finite.
     *
     * @param writer   The writer to write to.
     * @param iterable The iterable to produce values to put in the json array. Of values taken from the iterable: All
     *                 Iterables will be formatted as json arrays, all Maps will be formatted as json maps. A
     *                 JsonException is thrown if a value which is neither Iterable, Map, Number, String, Boolean nor
     *                 null is found.
     * @throws JsonException If a value of an unknown type is found.
     * @throws IOException   If the underlying writer throws an IOException.
     */
    public static Writer writeJsonArray(Writer writer, Iterable<?> iterable, int indentFactor, int currentIndent) throws JsonException, IOException {
        boolean alreadyDeclaredValue = false;
        writer.write('[');

        final int newIndent = currentIndent + indentFactor;
        for (Object obj : iterable) {
            if (alreadyDeclaredValue) {
                writer.write(',');
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            writeIndent(writer, newIndent);
            writeJsonValue(writer, obj, indentFactor, newIndent);
            alreadyDeclaredValue = true;
        }
        if (indentFactor > 0) {
            writer.write('\n');
        }
        writeIndent(writer, currentIndent);

        writer.write(']');
        return writer;
    }

    /**
     * Writes a json array string from the given object to the given writer. Please note that this assumes that no data
     * structures are cyclical, and that all iterables are finite.
     *
     * @param writer The writer to write to.
     * @param value  The value to format. All Iterables will be formatted as json arrays, all Maps will be formatted as
     *               json maps. A JsonException is thrown if value is neither Iterable, Map, Number, String, Boolean nor
     *               null is found.
     * @throws JsonException If value is of an unknown type, or a Map or Iterable value produces a value of an unkonwn
     *                       type.
     * @throws IOException   If the underlying writer throws an IOException.
     */
    public static Writer writeJsonValue(Writer writer, Object value, int indentFactor, int indent) throws JsonException, IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof Map) {
            writeJsonObject(writer, (Map) value, indent, indent);
        } else if (value instanceof Iterable) {
            writeJsonArray(writer, (Iterable) value, indentFactor, indent);
        } else if (value instanceof Number) {
            writeNumber(writer, (Number) value);
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof String) {
            writeString(writer, value.toString());
        } else {
            throw new JsonException("Invalid value: expected null, Map, Iterable, Number, Boolean or String, found `" + value.toString() + "` (`" + value.getClass() + "`)");
        }
        return writer;
    }
}
