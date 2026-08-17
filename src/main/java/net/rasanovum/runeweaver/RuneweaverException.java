package net.rasanovum.runeweaver;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class RuneweaverException extends RuntimeException {

    public RuneweaverException(String message, Exception e) {
        super(message, e);
    }

    public RuneweaverException(String message, Object... args) {
        super(String.format(message, args));
    }
}
