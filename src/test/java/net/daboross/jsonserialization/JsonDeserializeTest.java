package net.daboross.jsonserialization;

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
public class JsonDeserializeTest {

    private final String data;

    public JsonDeserializeTest(String data) {
        this.data = data;
    }

    @Test
    public void testDeserialization() throws IOException, JsonException {
        Object deserialized = new JsonParser(data).nextItem();
        StringWriter writer = new StringWriter();
        JsonSerialization.writeJsonValue(writer, deserialized, 0, 0);
    }

    @Parameters
    public static Collection<Object[]> getData() {
        try {
            String targetRoot = JsonDeserializeTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            Path reserializationTestFilesPath = Paths.get(targetRoot, "deserialization-test-files");

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
