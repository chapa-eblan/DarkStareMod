package com.darkstare.mod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * DarkStare - A subtle horror mod for staring into darkness.
 */
@Mod(DarkStare.MOD_ID)
public class DarkStare {

    public static final String MOD_ID = "darkstare";

    public DarkStare() {
        // Register config on mod bus
        com.darkstare.mod.config.DarkStareConfig.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Client-only registration: safe on dedicated server.
            com.darkstare.mod.client.DarkStareClientEvents.registerClientEvents();
        }
    }
}
