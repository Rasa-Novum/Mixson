package net.rasanovum.runeweaver.entries;

import net.rasanovum.runeweaver.RuneweaverEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class EventEntry<T> extends AbstractEntry {

    private final RuneweaverEvent<T> event;

    public EventEntry(int priority, RuneweaverEvent<T> event) {
        super(priority);
        this.event = event;
    }

    public RuneweaverEvent<T> event() {
        return event;
    }

}
