package net.ramixin.mixson.util.interfaces;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.enums.ErrorPolciy;

public interface ErrorMessageProvider {

    String getRuntimeErrorMessage(ResourceLocation resourceId);

    String getRegistrationErrorMessage();

    ErrorPolciy getErrorPolicy();

    String getRegistrationMessage(int priority);
}
