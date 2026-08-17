package net.rasanovum.runeweaver.tests.context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.RuneweaverCodecs;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.hooks.ListHook;
import net.rasanovum.runeweaver.hooks.NamespaceHook;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.rasanovum.runeweaver.tests.TestUtil.*;

public class MutabilityTests {

    @Test
    public void testStandardMutability() throws IOException {
        Index index = new Index("test:standard_mutability");
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject object = generateRandomJsonObject();
        JsonObject goalObject = object.deepCopy();
        goalObject.addProperty("test2", "test2");

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - fileMutabilityTest",
                index::idEquals,
                context -> {
                    assert context.getFile().equals(object) : "File was not serialized correctly";
                    context.getFile().getAsJsonObject().addProperty("test2", "test2");
                }
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(object));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));

        assert resourceMap.containsKey(idWithExt);
        Resource resource = resourceMap.get(idWithExt);
        assert RuneweaverCodecs.JSON_ELEMENT.deserialize(resource).equals(goalObject);
    }

    @Test
    public void testListMutability() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);

        int originalSize = resourceList.size();

        Index index = new Index("test:list_mutability", (int) net.getValue());
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = object.deepCopy();
        goalObject.addProperty("test2", "test2");

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - fileMutabilityTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.getFile().getAsJsonObject().addProperty("test2", "test2");
                }
        );

        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(idWithExt, resourceList);
        }};
        Runeweaver.processHook(new ListHook(resourceMap));

        assert resourceMap.containsKey(idWithExt);
        List<Resource> resource = resourceMap.get(idWithExt);
        assert resource.size() == originalSize : "Wrong number of resources";
        JsonElement element = RuneweaverCodecs.JSON_ELEMENT.deserialize(resource.get((int) net.getValue()));
        System.out.println(element);
        assert element.equals(goalObject) : "Mutability failed";
    }

    @Test
    public void testNamespaceMutability() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);

        int originalSize = resourceList.size();

        Index index = new Index("test:namespace_mutability", (int) net.getValue());
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = object.deepCopy();
        goalObject.addProperty("test2", "test2");

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - fileMutabilityTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.getFile().getAsJsonObject().addProperty("test2", "test2");
                }
        );


        Runeweaver.processHook(new NamespaceHook(resourceList, idWithExt));

        assert resourceList.size() == originalSize : "Wrong number of resources";
        JsonElement element = RuneweaverCodecs.JSON_ELEMENT.deserialize(resourceList.get((int) net.getValue()));
        assert element.equals(goalObject) : "Mutability failed";
    }

}
