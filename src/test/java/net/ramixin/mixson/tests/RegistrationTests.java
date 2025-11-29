package net.ramixin.mixson.tests;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.enums.Lifecycle;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.ResourceReference;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class RegistrationTests {

    @Test
    public void testEventRegistration() {
        ResourceLocation resourceId = ResourceLocation.parse("test:test");
        UUID eventId = Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifecycle.PERSISTENT,
                resourceId::equals,
                "testEventRegistration",
                ErrorPolciy.THROW,
                context -> {}
        );
        assert Mixson.has(eventId) : "Event was not registered";
    }

    @Test
    public void testReferenceRegistration() {
        ResourceReference<JsonElement> ref = Mixson.registerReference(
                Mixson.DEFAULT_PRIORITY,
                new Index("test:test"),
                "testReferenceRegistration"
        );
        assert Mixson.has(ref.getUuid()) : "Reference was not registered";
    }



}
