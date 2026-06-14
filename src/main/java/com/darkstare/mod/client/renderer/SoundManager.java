package com.darkstare.mod.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;
import com.darkstare.mod.config.DarkStareConfig;

/**
 * Manages atmospheric sound when player stares into darkness.
 * Enhanced with layered soundscape: low drones, metallic scraping, breath-like sounds,
 * and sudden silence at peak intensity for maximum unease.
 */
public class SoundManager {

    private static final String END_AMBIENT_ID  = "ambient.end";
    private static final String CAVE_AMBIENT_ID = "ambient.cave";
    private static final String HEARTBEAT_ID    = "entity.warden.heartbeat";
    // New sound sources for richer soundscape
    private static final String ANVIL_USE_ID     = "block.anvil.use";
    private static final String LAVA_AMBIENT_ID  = "ambient.cave.lava";
    private static final String PORTAL_TRAVEL_ID = "entity.portal.travel";
    // Additional horror sounds
    private static final String WOOD_BREAK_ID    = "block.wood.break";
    private static final String CHAIN_PLACE_ID   = "block.chain.place";
    private static final String GLASS_BREAK_ID   = "block.glass.break";

    private static float lastIntensity = 0f;
    private static final RandomSource RAND = RandomSource.create();
    private static long lastHeartbeatTick = 0;

    // Sudden silence tracking
    private static boolean isSilent = false;
    private static long silenceStartTime = 0;
    private static final long MAX_SILENCE_DURATION = 2500L; // max 2.5 seconds of silence

    public static void update(float intensity, boolean staringDeeply) {
        if (!DarkStareConfig.ENABLE_SOUNDS.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Quick exit: no meaningful sound at very low intensity
        if (intensity <= 0.1f && lastIntensity <= 0.1f) {
            isSilent = false;
            lastIntensity = intensity;
            return;
        }

        float volumeScale = Math.min(intensity * 1.4f, 1.0f);

        // Only full effects when truly staring into dark area
        if (!staringDeeply) {
            isSilent = false;
            lastIntensity = intensity;
            return;
        }

        // Read config values safely using floatValue()
        float humScale      = DarkStareConfig.HUM_VOLUME_SCALE.get().floatValue();
        float whisperScale  = DarkStareConfig.WHISPER_VOLUME_SCALE.get().floatValue();
        float spikeChance   = DarkStareConfig.SPIKE_CHANCE_AT_FULL.get().floatValue();
        float heartScale    = DarkStareConfig.HEARTBEAT_VOLUME_SCALE.get().floatValue();

        RandomSource rand = RAND;

        // === SUDDEN SILENCE at peak intensity ===
        // At very high intensity, occasionally cut all sound for a brief moment —
        // the absence of sound is more terrifying than any noise.
        if (intensity > 0.85f && !isSilent && rand.nextFloat() < 0.003f) {
            isSilent = true;
            silenceStartTime = System.currentTimeMillis();
        }

        // End silence if it's been too long
        if (isSilent && System.currentTimeMillis() - silenceStartTime > MAX_SILENCE_DURATION) {
            isSilent = false;
        }

        // If silent, skip all sound generation — the void speaks louder than words
        if (isSilent) {
            lastIntensity = intensity;
            return;
        }

        // Heartbeat: thuds that speed up with intensity (roughly every 0.8-2.5s, clamped)
        if (intensity > 0.4f) {
            long tick = mc.level.getGameTime();
            long rawInterval = (long)(40 - intensity * 25); // 40 -> 15 ticks as intensity grows
            long interval = Math.max(10, rawInterval);      // cap at 10 ticks (~0.5s) to avoid spam
            if (tick - lastHeartbeatTick >= interval) {
                lastHeartbeatTick = tick;
                float pitch = 0.7f + intensity * 0.3f;
                float vol = volumeScale * heartScale * (0.25f + 0.75f * intensity);
                playEerieSound(HEARTBEAT_ID, clamp(pitch, 0.5f, 1.8f), Math.min(vol, 0.9f));

                // Double-thump at very high intensity — like a real racing heartbeat
                if (intensity > 0.75f && rand.nextFloat() < 0.4f) {
                    try { Thread.sleep(120); } catch (InterruptedException ignored) {}
                    playEerieSound(HEARTBEAT_ID, clamp(pitch * 0.95f, 0.5f, 1.8f), Math.min(vol * 0.7f, 0.9f));
                }
            }
        }

        // High-frequency "hum" via end ambience (subtle pressure)
        if (intensity > 0.25f && rand.nextFloat() < 0.016f * intensity) {
            float pitch = 0.85f + 0.3f * intensity;
            float vol   = volumeScale * humScale * (0.3f + 0.7f * intensity);
            playEerieSound(END_AMBIENT_ID, clamp(pitch, 0.5f, 1.8f), Math.min(vol, 0.9f));
        }

        // Whisper-like "pressure" using cave ambience: occasional and faint
        if (intensity > 0.35f && rand.nextFloat() < 0.012f * intensity) {
            float pitch = 0.7f + 0.4f * intensity;
            float vol   = volumeScale * whisperScale * (0.18f + 0.5f * intensity);
            playEerieSound(CAVE_AMBIENT_ID, clamp(pitch, 0.6f, 1.9f), Math.min(vol, 0.85f));
        }

        // At very high intensity: add occasional "spikes" to feel like it's watching back
        if (intensity > 0.65f && rand.nextFloat() < spikeChance) {
            float pitch = 1.0f + rand.nextFloat() * 0.4f;
            float vol   = volumeScale * humScale * 0.8f;
            playEerieSound(END_AMBIENT_ID, clamp(pitch, 0.6f, 2.0f), Math.min(vol, 0.95f));
        }

        // Ultra-high: second layer of whispers for maximum unease
        if (intensity > 0.85f && rand.nextFloat() < 0.008f) {
            float pitch = 0.6f + rand.nextFloat() * 0.3f;
            float vol   = volumeScale * whisperScale * 0.7f;
            playEerieSound(CAVE_AMBIENT_ID, clamp(pitch, 0.5f, 1.8f), Math.min(vol, 0.9f));
        }

        // === NEW: Metallic scraping / anvil sounds at high intensity ===
        if (intensity > 0.6f && rand.nextFloat() < 0.004f * intensity) {
            float pitch = 1.2f + rand.nextFloat() * 0.8f; // high-pitched metallic
            float vol   = volumeScale * 0.15f;
            playEerieSound(ANVIL_USE_ID, clamp(pitch, 1.0f, 2.0f), Math.min(vol, 0.4f));
        }

        // === NEW: Low-frequency drone via portal travel sound at medium-high intensity ===
        if (intensity > 0.5f && rand.nextFloat() < 0.006f * intensity) {
            float pitch = 0.3f + rand.nextFloat() * 0.2f; // very low, sub-bass feel
            float vol   = volumeScale * humScale * 0.25f;
            playEerieSound(PORTAL_TRAVEL_ID, clamp(pitch, 0.2f, 1.0f), Math.min(vol, 0.35f));
        }

        // === Distant lava-like rumble at very high intensity (deep dread) ===
        if (intensity > 0.75f && rand.nextFloat() < 0.003f * intensity) {
            float pitch = 0.4f + rand.nextFloat() * 0.15f;
            float vol   = volumeScale * 0.2f;
            playEerieSound(LAVA_AMBIENT_ID, clamp(pitch, 0.3f, 0.8f), Math.min(vol, 0.3f));
        }

        // === Wood creaking — like something is moving in the dark ===
        if (intensity > 0.55f && rand.nextFloat() < 0.002f * intensity) {
            float pitch = 0.8f + rand.nextFloat() * 0.3f;
            float vol   = volumeScale * 0.12f;
            playEerieSound(WOOD_BREAK_ID, clamp(pitch, 0.6f, 1.5f), Math.min(vol, 0.25f));
        }

        // === Chain clinking — subtle metallic presence ===
        if (intensity > 0.7f && rand.nextFloat() < 0.003f * intensity) {
            float pitch = 1.5f + rand.nextFloat() * 0.5f;
            float vol   = volumeScale * 0.08f;
            playEerieSound(CHAIN_PLACE_ID, clamp(pitch, 1.2f, 2.0f), Math.min(vol, 0.2f));
        }

        // === Glass shatter — sudden sharp sound at peak intensity ===
        if (intensity > 0.9f && rand.nextFloat() < 0.001f) {
            float pitch = 1.8f + rand.nextFloat() * 0.3f;
            float vol   = volumeScale * 0.25f;
            playEerieSound(GLASS_BREAK_ID, clamp(pitch, 1.5f, 2.0f), Math.min(vol, 0.4f));
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
        // Fallback to cave ambience instead of returning null
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", "ambient.cave"));
    }

    private static void playEerieSound(String soundId, float pitch, float volume) {
        SoundEvent event = getSound(soundId);
        if (event == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        net.minecraft.client.resources.sounds.SimpleSoundInstance instance =
            new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                event,
                SoundSource.AMBIENT,
                volume,
                pitch,
                RAND,
                x, y, z
            );

        mc.getSoundManager().play(instance);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static void reset() {
        lastIntensity = 0f;
        lastHeartbeatTick = 0;
        isSilent = false;
        silenceStartTime = 0;
    }
}
