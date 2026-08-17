package net.rasanovum.runeweaver.util.interfaces;

import net.minecraft.resources.Identifier;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ErrorMessageProvider {

    String getRuntimeErrorMessage(Identifier resourceId);

    String getRegistrationErrorMessage();

    ErrorPolicy getErrorPolicy();

    String getRegistrationMessage(int priority);
}
