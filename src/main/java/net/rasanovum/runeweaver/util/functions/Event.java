package net.rasanovum.runeweaver.util.functions;

import net.rasanovum.runeweaver.EventContext;

@FunctionalInterface
public interface Event<T> {

    void runEvent(EventContext<T> context);
}
