package net.rasanovum.runeweaver.tests;

import com.google.gson.JsonElement;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.ResourceReference;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.util.Index;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class RegistrationTests {

    @Test
    public void testEventRegistration() {
        UUID eventId = Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "testEventRegistration",
                ignored -> false,
                ignored -> {}
        );
        assert Runeweaver.hasEvent(eventId) : "Event was not registered";
    }

    @Test
    public void testReferenceRegistration() {
        ResourceReference<JsonElement> ref = Runeweaver.registerReference(
                Runeweaver.DEFAULT_PRIORITY,
                new Index("test:test"),
                "testReferenceRegistration"
        );
        assert Runeweaver.hasReference(ref.getUuid()) : "Reference was not registered";
    }



}
