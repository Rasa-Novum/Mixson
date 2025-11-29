package net.ramixin.mixson.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.ResourceReference;
import net.ramixin.mixson.enums.DebugOption;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.enums.Lifecycle;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.hooks.ListHook;
import net.ramixin.mixson.hooks.NamespaceHook;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ramixin.mixson.tests.Util.createDummyResource;
import static net.ramixin.mixson.tests.Util.generateRandomJsonObject;

public class RunTests {

    @Test
    public void testStandardEventRun() {
        ResourceLocation resourceId = ResourceLocation.parse("test:test_event_run");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");
        ResourceLocation badResourceId = ResourceLocation.parse("test:test_event_not_run");

        MutableBoolean didRun = new MutableBoolean(false);
        MutableBoolean wrongEventRan = new MutableBoolean(false);

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "eventThatShouldRun",
                ErrorPolciy.THROW,
                context -> didRun.setValue(true)
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                badResourceId::equals,
                "eventThatShouldNotRun",
                ErrorPolciy.THROW,
                context -> {
                    System.out.println("This event should not run");
                    wrongEventRan.setValue(true);
                }
        );
        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
                put(resourceIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert didRun.booleanValue() : "Event was not run";
        assert !wrongEventRan.booleanValue() : "Wrong event ran";
    }

    @Test
    public void testNamespaceEventRun() {
        Mixson.setDebugOption(DebugOption.PREVENT_CATCHING, true, false);
        ResourceLocation resourceId = ResourceLocation.parse("test:test_event_run");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");
        ResourceLocation badResourceId = ResourceLocation.parse("test:test_event_not_run");

        MutableBoolean didRun = new MutableBoolean(false);
        MutableBoolean wrongEventRan = new MutableBoolean(false);

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "eventThatShouldRun",
                ErrorPolciy.THROW,
                context -> didRun.setValue(true)
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                id -> id.equals(badResourceId),
                "eventThatShouldNotRun",
                ErrorPolciy.THROW,
                context -> {
                    System.out.println("This event should not run");
                    wrongEventRan.setValue(true);
                }
        );
        List< Resource> resourceList = new ArrayList<>(){{
                add(createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new NamespaceHook(resourceList, resourceIdWithExt));
        assert didRun.booleanValue() : "Event was not run";
        assert !wrongEventRan.booleanValue() : "Wrong event ran";
    }

    @Test
    public void testListEventRun() {
        Mixson.setDebugOption(DebugOption.PREVENT_CATCHING, true, false);
        ResourceLocation resourceId = ResourceLocation.parse("test:test_event_run");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");
        ResourceLocation badResourceId = ResourceLocation.parse("test:test_event_not_run");


        MutableBoolean correctEventRan = new MutableBoolean(false);
        MutableBoolean wrongEventRan = new MutableBoolean(false);

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "eventThatShouldRun",
                ErrorPolciy.THROW,
                context -> correctEventRan.setValue(true)
        );
        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                id -> id.equals(badResourceId),
                "eventThatShouldNotRun",
                ErrorPolciy.THROW,
                context -> wrongEventRan.setValue(true)
        );
        Map<ResourceLocation, List<Resource>> resourceMap = new HashMap<>(){{
            put(resourceIdWithExt, List.of(createDummyResource(generateRandomJsonObject())));
        }};
        Mixson.processHook(new ListHook(resourceMap));
        assert correctEventRan.booleanValue() : "Event was not run";
        assert !wrongEventRan.booleanValue() : "Wrong event ran";
    }

    @Test
    public void testReferenceRun() {
        ResourceLocation resourceId = ResourceLocation.parse("test:test_event_run");
        ResourceLocation resourceIdWithExt = resourceId.withSuffix(".json");

        JsonObject value = generateRandomJsonObject();

        ResourceReference<JsonElement> reference = Mixson.registerReference(
                Mixson.DEFAULT_PRIORITY,
                new Index(resourceId),
                "testReferenceRun"
        );

        Map<ResourceLocation, Resource> resourceMap = new HashMap<>(){{
            put(resourceIdWithExt, createDummyResource(value));
        }};
        Mixson.processHook(new StandardHook(resourceMap));

        assert reference.retrieve().isPresent() : "Reference was not run";
        JsonObject refObject = reference.consume().orElseThrow().getAsJsonObject();
        assert refObject.equals(value) : "Reference was not retrieved correctly";

    }

}
