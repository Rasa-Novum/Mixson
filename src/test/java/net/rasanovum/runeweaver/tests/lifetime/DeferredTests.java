package net.rasanovum.runeweaver.tests.lifetime;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.rasanovum.runeweaver.tests.TestUtil.createDummyResource;
import static net.rasanovum.runeweaver.tests.TestUtil.generateRandomJsonObject;

public class DeferredTests {

    private static final Logger log = LoggerFactory.getLogger(DeferredTests.class);

    @Test
    public void testEventPulling() {
        Index index = new Index("test:pulling");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:pulled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean pulled = new AtomicBoolean(false);

        UUID eventId = Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.DEFERRED,
                ErrorPolicy.THROW,
                "eventToBePulled",
                otherIndex::idEquals,
                ignored -> pulled.set(true)
        );
        log.info("Registered event with id {}", eventId);

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullTest",
                index::idEquals,
                context -> {
                    log.info("pulling event with id {}", eventId);
                    context.pullIntoRuntime(eventId);
                }
        );

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY + 100,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullErrorTest",
                index::idEquals,
                context -> context.pullIntoRuntime(eventId)
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, () -> Runeweaver.processHook(new StandardHook(resourceMap)));
        log.error("Caught exception", e);
        assert e.getMessage().contains("failed to locate event or reference with uuid of ") : "Caught wrong exception";
        assert pulled.get() : "Event was not pulled";
    }

    @Test
    public void testConcurrentPulling() {
        Index index = new Index("test:pulling");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:pulled");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");

        AtomicBoolean pulled = new AtomicBoolean(false);

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "eventPullTest",
                index::idEquals,
                context -> {

                    UUID eventId = Runeweaver.registerEvent(
                            Runeweaver.DEFAULT_PRIORITY,
                            Lifetime.DEFERRED,
                            ErrorPolicy.THROW,
                            "eventToBePulled",
                            otherIndex::idEquals,
                            ignored -> pulled.set(true)
                    );

                    log.info("pulling concurrent event with id {}", eventId);
                    context.pullIntoRuntime(eventId);
                }
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));
        assert pulled.get() : "Event was not pulled";
    }
}
