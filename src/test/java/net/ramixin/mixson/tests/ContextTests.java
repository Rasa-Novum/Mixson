package net.ramixin.mixson.tests;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.enums.Lifecycle;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.MixsonCodecs;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.ramixin.mixson.tests.Util.*;
import static net.ramixin.mixson.tests.Util.generateRandomJsonObject;

public class ContextTests {

    @Test
    public void testFileMutability() throws IOException {
        ResourceLocation resourceId = ResourceLocation.parse("test:mutability");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");
        JsonObject object = generateRandomJsonObject();
        JsonObject goalObject = generateRandomJsonObject();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "fileMutabilityTest",
                ErrorPolciy.THROW,
                context -> {
                    assert context.getFile().equals(object);
                    context.getFile().getAsJsonObject().addProperty("test2", "test2");
                }
        );
        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
            put(resourceIdWithExt, createDummyResource(object));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert resourceMap.containsKey(resourceIdWithExt);
        Resource resource = resourceMap.get(resourceIdWithExt);
        assert MixsonCodecs.JSON_ELEMENT.deserialize(resource).equals(goalObject);
    }

    @Test
    public void testResourceCreation() throws IOException {
        ResourceLocation resourceId = ResourceLocation.parse("test:creation");
        ResourceLocation resourceIdWithExtension = resourceId.withSuffix(".json");
        JsonObject goalObject = generateRandomJsonObject();
        goalObject.addProperty("test", "test");
        goalObject.addProperty("test2", "test2");

        ResourceLocation goalId = ResourceLocation.parse("test:goal_object");
        ResourceLocation goalIdWithExtension = goalId.withSuffix(".json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "fileCreationTest",
                ErrorPolciy.THROW,
                context -> context.createResource(new Index(goalId), goalObject)
        );
        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
            put(resourceIdWithExtension, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        System.out.println(resourceMap);
        assert resourceMap.containsKey(goalIdWithExtension) : "Resource was not created";
        Resource resource = resourceMap.get(goalIdWithExtension);
        assert MixsonCodecs.JSON_ELEMENT.deserialize(resource).equals(goalObject) : "Resource was not created correctly";
    }

    @Test
    public void testResourceDeletion() {
        ResourceLocation resourceId = ResourceLocation.parse("test:deletion");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");

        ResourceLocation otherId = ResourceLocation.parse("test:other_deletion");
        ResourceLocation otherIdWithExt = otherId.withSuffix(".json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "fileDeletionTest",
                ErrorPolciy.THROW,
                context -> context.markForDeletion(true)
        );
        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
            put(resourceIdWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        System.out.println(resourceMap);
        assert !resourceMap.containsKey(resourceIdWithExt) : "Resource was not deleted";
        assert resourceMap.containsKey(otherIdWithExt) : "Too many resources were deleted";
    }

    @Test
    public void testEventCancellation() {
        ResourceLocation resourceId = ResourceLocation.parse("test:canceler");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");

        ResourceLocation otherId = ResourceLocation.parse("test:canceled");
        ResourceLocation otherIdWithExt = otherId.withSuffix(".json");

        AtomicBoolean cancelled = new AtomicBoolean(true);

        UUID eventId = Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY + 100,
                Lifecycle.PERSISTENT,
                otherId::equals,
                "eventToBeCancelled",
                ErrorPolciy.THROW,
                context -> cancelled.set(false)
        );

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "fileCancellationTest",
                ErrorPolciy.THROW,
                context -> context.cancelFutureEvent(eventId)
        );
        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
            put(resourceIdWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert cancelled.get() : "Event was not cancelled";
    }

}
