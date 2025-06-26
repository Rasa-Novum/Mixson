package net.ramixin.mixson.client;

import net.fabricmc.api.ClientModInitializer;

import static net.ramixin.mixson.util.MixsonUtil.loadATPMixsonEntries;

public class MixsonClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        loadATPMixsonEntries("mixsonClient");
    }
}
