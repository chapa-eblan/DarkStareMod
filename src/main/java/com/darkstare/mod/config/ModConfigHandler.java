package com.darkstare.mod.config;

import com.darkstare.mod.DarkStare;
import com.darkstare.mod.client.DarkStareClientEvents;
import com.darkstare.mod.client.renderer.SoundManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Listens for mod config changes and resets active effects so new settings apply immediately.
 */
@Mod.EventBusSubscriber(modid = DarkStare.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfigHandler {

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(DarkStare.MOD_ID)) {
            DarkStareClientEvents.resetAll();
            SoundManager.reset();
        }
    }
}
