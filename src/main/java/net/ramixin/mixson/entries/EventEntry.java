package net.ramixin.mixson.entries;

import net.ramixin.mixson.MixsonEvent;

public class EventEntry<T> extends AbstractEntry {

    private final MixsonEvent<T> event;

    public EventEntry(int priority, MixsonEvent<T> event) {
        super(priority);
        this.event = event;
    }

    public MixsonEvent<T> event() {
        return event;
    }

}
