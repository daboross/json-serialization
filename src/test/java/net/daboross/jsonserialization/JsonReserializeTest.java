package net.daboross.jsonserialization;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class JsonReserializeTest {

    private final String data;

    public JsonReserializeTest(String data) {
        this.data = data;
    }

    @Test
    public void testDeserialization() throws IOException, JsonException {
        new JsonParser(data).nextItem();
    }

    @Test
    public void testSerialization() throws IOException, JsonException {
        Object deserialized = new JsonParser(data).nextItem();
        StringWriter writer = new StringWriter();
        JsonSerialization.writeJsonValue(writer, deserialized, 0, 0);
        String reserialized = writer.toString();
        // TODO: a more general way to ignore whitespace differences
        assertEquals(data.replaceAll("\\s+", ""), reserialized.replaceAll("\\s+", ""));
    }

    @Parameters
    public static Collection<Object[]> getData() {
        try {
            String targetRoot = JsonReserializeTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            Path reserializationTestFilesPath = Paths.get(targetRoot, "reserialization-test-files");

            List<Object[]> list = new ArrayList<>();
            Files.walk(reserializationTestFilesPath).filter(Files::isRegularFile).map((f) -> {
                try {
                    return new Object[]{new String(Files.readAllBytes(f))};
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).forEach(list::add);
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
