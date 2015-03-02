/*
 * Tests for JsonParser
 * Copyright (c) 2015 David Ross <daboross@daboross.net>
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.daboross.jsonserialization;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Tests for {@link JsonParser}.
 *
 * @author daboross@daboross.net (David Ross)
 */
public class JsonParserTest {

    @Test
    public void testObjectParsing() throws IOException, JsonException {
        String serializedForm = "{\"key\": \"value\"}";
        Map<String, Object> map = new JsonParser(serializedForm).parseJsonObject();
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("key", "value");
        assertEquals(map, expected);
    }

    @Test
    public void testArrayParsing() throws IOException, JsonException {
        String serializedForm = "[\"value1\", \"value2\"]";
        List<Object> map = new JsonParser(serializedForm).parseJsonArray();
        List<Object> expected = Arrays.<Object>asList("value1", "value2");
        assertEquals(map, expected);
    }

    @Test()
    public void testLiteralParsing() throws IOException, JsonException {
        assertEquals(new JsonParser("true").nextItem(), Boolean.TRUE);
        assertEquals(new JsonParser("false").nextItem(), Boolean.FALSE);
        assertEquals(new JsonParser("null").nextItem(), null);
    }

    @Test()
    public void testDoubleParsing() throws IOException, JsonException {
        Double testDouble = 0.01;
        assertEquals(new JsonParser(testDouble.toString()).nextItem(), testDouble);
    }

    @Test()
    public void testIntegerParsing() throws IOException, JsonException {
        Integer testInt = 1;
        assertEquals(new JsonParser(testInt.toString()).nextItem(), testInt);
    }

    @Test()
    public void testLongParsing() throws IOException, JsonException {
        Long testLong = Long.MAX_VALUE;
        assertEquals(new JsonParser(testLong.toString()).nextItem(), testLong);
    }

    @Test(expected = JsonException.class)
    public void testArrayIsNotObject() throws IOException, JsonException {
        String serializedForm = "[\"value1\", \"value2\"]";
        new JsonParser(serializedForm).parseJsonObject();
    }

    @Test(expected = JsonException.class)
    public void testObjectIsNotArray() throws IOException, JsonException {
        String serializedForm = "{\"key\": \"value\"}";
        new JsonParser(serializedForm).parseJsonArray();
    }

    @Test(expected = JsonException.class)
    public void testUnkownLiteral() throws IOException, JsonException {
        String serializedForm = "some_literal_string";
        new JsonParser(serializedForm).nextItem();
    }
}
