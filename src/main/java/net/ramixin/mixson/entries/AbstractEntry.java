package net.ramixin.mixson.entries;

public abstract class AbstractEntry {

    private final int priority;

    AbstractEntry(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

}
