package net.rasanovum.runeweaver.tests;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.rasanovum.runeweaver.tests.TestUtil.createDummyResource;
import static net.rasanovum.runeweaver.tests.TestUtil.generateRandomJsonObject;

public class ContextTests {

    @Test
    public void testEventCancellation() {
        Index index = new Index("test:canceler");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:canceled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean cancelled = new AtomicBoolean(true);

        UUID eventId = Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY + 100,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventToBeCancelled",
                otherIndex::idEquals,
                ignored -> cancelled.set(false)
        );

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "fileCancellationTest",
                index::idEquals,
                context -> context.cancelFutureEvent(eventId)
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));
        assert cancelled.get() : "Event was not cancelled";
    }
}
