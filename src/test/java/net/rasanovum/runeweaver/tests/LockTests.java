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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.rasanovum.runeweaver.tests.TestUtil.createDummyResource;
import static net.rasanovum.runeweaver.tests.TestUtil.generateRandomJsonObject;

public class LockTests {

    @Test
    public void testRuneweaverLock() throws InterruptedException {
        Index index = new Index("test:locked");
        Identifier idWithExt = index.id().withSuffix(".json");
        AtomicBoolean eventRan = new AtomicBoolean(false);

        Runnable releaseLock = Runeweaver.lockEventProcessing();

        Runeweaver.registerEvent(
              Runeweaver.DEFAULT_PRIORITY,
              Lifetime.PERSISTENT,
              ErrorPolicy.THROW,
              "testRuneweaverLockingEvent",
              ignored -> true,
              ignored -> eventRan.set(true)
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
        }};

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            Runeweaver.processHook(new StandardHook(resourceMap));
        });

        Thread.sleep(100);

        assert !eventRan.get() : "Event was run";
        assert !future.isDone() : "Future ran while lock was held";

        releaseLock.run();
        Thread.sleep(100);

        assert eventRan.get() : "Event was not run";
        assert future.isDone() : "Future did not run after lock was released";
    }

}
