package net.rasanovum.runeweaver.tests.context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.RuneweaverException;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.hooks.ListHook;
import net.rasanovum.runeweaver.hooks.NamespaceHook;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.rasanovum.runeweaver.tests.TestUtil.*;

public class CaptureTests {

    private static final Logger log = LoggerFactory.getLogger(CaptureTests.class);

    @Test
    public void testStandardCapturing() {
        Index index = new Index("test:standard_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:standard_other_captured");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");
        JsonObject goalObject = generateRandomJsonObject();

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.get(0);
                    log.info("captured file: {}", capture.getValue());
                    log.info("expected file: {}", goalObject);
                    assert capture.getValue().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(RuneweaverException.class, () -> context.captureFiles(otherIndex)).getCause();
                    log.error("e: ", exception);
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception";
                }
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(goalObject));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));
    }

    @Test
    public void testListCapturing() {
        Index index = new Index("test:list_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(goalObject, net);
        Index otherIndex = new Index("test:list_captured", (int) net.getValue());
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");


        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.get(0);
                    assert capture.getValue().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(RuneweaverException.class, () -> context.captureFiles(otherIndex)).getCause();
                    log.error("e: ", exception);
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception";

                    Index newIndex = new Index(otherIndex.id(), randomListOrdinal(resourceList.size(), (int) net.getValue()));
                    context.captureFiles(newIndex);
                }
        );
        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(idWithExt, generateRandomResourceList(new JsonObject(), new MutableInt()));
            put(otherIdWithExt, resourceList);
        }};
        Runeweaver.processHook(new ListHook(resourceMap));
    }

    @Test
    public void testNamespaceCapturing() {
        Index index = new Index("test:namespace_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(goalObject, net);
        Index otherIndex = new Index("test:namespace_capturing", (int) net.getValue());

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.get(0);
                    assert capture.getValue().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(RuneweaverException.class, () -> context.captureFiles(otherIndex)).getCause();
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception: "+ exception;

                    Index newIndex = new Index(otherIndex.id(), randomListOrdinal(resourceList.size(), (int) net.getValue()));
                    context.captureFiles(newIndex);
                }
        );
        Runeweaver.processHook(new NamespaceHook(resourceList, idWithExt));
    }

}
