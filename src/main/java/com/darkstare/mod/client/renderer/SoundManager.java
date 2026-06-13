package com.darkstare.mod.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.darkstare.mod.config.DarkStareConfig;

/**
 * Manages atmospheric sound when player stares into darkness.
 */
public class SoundManager {

    private static final String END_AMBIENT_ID  = "ambient.end";
    private static final String CAVE_AMBIENT_ID = "ambient.cave";

    public static void update(float intensity, boolean staringDeeply) {
        if (!DarkStareConfig.ENABLE_SOUNDS.get()) return;

        // Nothing to do at very low intensity.
        if (intensity <= 0.1f && lastIntensity <= 0.1f) {
            lastIntensity = intensity;
            return;
        }

        float volumeScale = Math.min(intensity * 1.4f, 1.0f);

        if (!staringDeeply) {
            lastIntensity = intensity;
            return;
        }

        double humDbl      = DarkStareConfig.HUM_VOLUME_SCALE.get();
        double whisperDbl   = DarkStareConfig.WHISPER_VOLUME_SCALE.get();
        double spikeDbl     = DarkStareConfig.SPIKE_CHANCE_AT_FULL.get();

        float humScale      = (float)humDbl;
        float whisperScale  = (float)whisperDbl;
        float spikeChance   = (float)spikeDbl;

        // High-frequency "hum" via end ambience.
        if (intensity > 0.25f && Math.random() < 0.018 * intensity) {
            float pitch = 0.9f + 0.35f * intensity;
            float vol   = volumeScale * humScale * (float)(0.4f + 0.6f * intensity);
            playEerieSound(END_AMBIENT_ID, clamp(pitch, 0.5f, 2.0f), Math.min(vol, 1.0f));
        }

        // Whisper-like "pressure" using cave ambience: occasional and faint.
        if (intensity > 0.35f && Math.random() < 0.014 * intensity) {
            float pitch = 0.75f + 0.35f * intensity;
            float vol   = volumeScale * whisperScale * (float)(0.2f + 0.4f * intensity);
            playEerieSound(CAVE_AMBIENT_ID, clamp(pitch, 0.5f, 2.0f), Math.min(vol, 1.0f));
        }

        // At very high intensity: add occasional "spikes" to feel like it's watching back.
        if (intensity > 0.65f && Math.random() < spikeChance) {
            float pitch = 1.2f + (float)Math.random() * 0.4f;
            float vol   = volumeScale * humScale * (float)(0.9f);
            playEerieSound(END_AMBIENT_ID, clamp(pitch, 0.5f, 2.0f), Math.min(vol, 1.0f));
        }

        // Ultra-high: second layer of whispers for maximum unease
        if (intensity > 0.85f && Math.random() < 0.01) {
            float pitch = 0.6f + (float)Math.random() * 0.3f;
            float vol   = volumeScale * whisperScale * (float)(0.7f);
            playEerieSound(CAVE_AMBIENT_ID, clamp(pitch, 0.5f, 2.0f), Math.min(vol, 1.0f));
        }

        lastIntensity = intensity;
    }

    private static SoundEvent getSound(String soundId) {
        String[] parts = soundId.split("\\.");
        String ns = (parts.length > 1 && !parts[0].isEmpty()) ? parts[0] : "minecraft";
        String path = (parts.length > 1)
                ? String.join(".", java.util.Arrays.copyOfRange(parts, 1, parts.length))
                : soundId;

        ResourceLocation rl = new ResourceLocation(ns, path);
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (event != null) {
            return event;
        }

        // Fallback to ambient.cave.
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", "ambient.cave"));
    }

    private static void playEerieSound(String soundId, float pitch, float volume) {
        SoundEvent event = getSound(soundId);
        if (event == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        // Use public constructor instead of factory methods (safer with official mappings).
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(0L);

        net.minecraft.client.resources.sounds.SimpleSoundInstance instance =
            new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                event,
                SoundSource.AMBIENT,
                volume,
                pitch,
                rand,
                x, y, z
            );

        mc.getSoundManager().play(instance);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static void reset() {
        lastIntensity = 0f;
    }

    private static float lastIntensity = 0f;
}
