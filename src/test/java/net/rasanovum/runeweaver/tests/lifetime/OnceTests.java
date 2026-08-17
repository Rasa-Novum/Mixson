package net.rasanovum.runeweaver.tests.lifetime;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.hooks.StandardHook;
import net.rasanovum.runeweaver.util.Index;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static net.rasanovum.runeweaver.tests.TestUtil.createDummyResource;
import static net.rasanovum.runeweaver.tests.TestUtil.generateRandomJsonObject;

public class OnceTests {

    @Test
    public void testStandardOnceLifetime() {
        Index index = new Index("test:standard_once");
        Identifier indexExt = index.id().withSuffix(".json");

        MutableInt runCount = new MutableInt();

        Runeweaver.registerEvent(
                Runeweaver.DEFAULT_PRIORITY,
                Lifetime.ONCE,
                ErrorPolicy.THROW,
                "onceLifetimeEvent",
                index::equals,
                ignored -> runCount.increment()
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(indexExt, createDummyResource(generateRandomJsonObject()));
        }};
        Runeweaver.processHook(new StandardHook(resourceMap));
        Runeweaver.processHook(new StandardHook(resourceMap));

        assert (int) runCount.getValue() == 1 : "Event ran " + runCount.getValue() + " times";
    }

}
