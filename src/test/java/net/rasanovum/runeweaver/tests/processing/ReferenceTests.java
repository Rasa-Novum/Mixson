package net.rasanovum.runeweaver.tests.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.ResourceReference;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static net.rasanovum.runeweaver.tests.TestUtil.createDummyResource;
import static net.rasanovum.runeweaver.tests.TestUtil.generateRandomJsonObject;

public class ReferenceTests {

    @Test
    public void testReferenceProcessing() {
        Index index = new Index("test:reference_run");
        Identifier indexExt = index.id().withSuffix(".json");

        JsonObject value = generateRandomJsonObject();

        ResourceReference<JsonElement> reference = Runeweaver.registerReference(
                Runeweaver.DEFAULT_PRIORITY,
                index,
                "testReferenceRun"
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(indexExt, createDummyResource(value));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));

        assert reference.retrieve().isPresent() : "Reference was not run";
        JsonObject refObject = reference.consume().orElseThrow().getAsJsonObject();
        assert refObject.equals(value) : "Reference was not serialized correctly";

    }

}
