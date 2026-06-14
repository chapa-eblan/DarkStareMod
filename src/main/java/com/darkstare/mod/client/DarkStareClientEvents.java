package com.darkstare.mod.client;

import com.mojang.blaze3d.platform.Window;

import com.darkstare.mod.config.DarkStareConfig;
import com.darkstare.mod.client.renderer.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.event.TickEvent;

/**
 * Client-only event handler for DarkStare.
 */
public class DarkStareClientEvents {

    private static float stareTimer = 0f;
    private static float intensity = 0f;
    private static boolean hasDarkTarget = false;
    private static float screenShake = 0f;
    private static float heartbeatPulse = 0f;

    private static final RandomSource RAND = RandomSource.create();

    // For safe respawn/dimension-change detection inside onPlayerTick.
    private static int lastDimensionId = Integer.MIN_VALUE;
    private static float lastHealth = Float.MAX_VALUE;

    public static void registerClientEvents() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new DarkStareClientEvents());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !event.player.equals(mc.player)) return;

        // Detect respawn: health jumps back up after death.
        float currentHealth = event.player.getHealth();
        boolean wasRecentlyDead = lastHealth <= 0f && currentHealth > 3f;

        // Detect dimension change via level dimension ID.
        int dimId = mc.level.dimension().toString().hashCode();
        boolean changedDimension = (lastDimensionId != Integer.MIN_VALUE) && (dimId != lastDimensionId);

        if (wasRecentlyDead || changedDimension) {
            resetAll();
        }

        lastHealth = currentHealth;
        lastDimensionId = dimId;

        if (!DarkStareConfig.ENABLED.get()) {
            resetIfActive();
            return;
        }

        double eyeX = event.player.getX();
        double eyeY = event.player.getY() + (float)event.player.getBbHeight() * 0.9f;
        double eyeZ = event.player.getZ();

        float pitchRad = (float)Math.toRadians(event.player.getXRot());
        float yawRad   = (float)Math.toRadians(event.player.getYRot() - 90.0f);

        double cosPitch = Math.cos(-pitchRad);
        double sinYaw   = Math.sin(yawRad);
        double cosYaw   = Math.cos(yawRad);
        double sinPitch = -Math.sin(-pitchRad);

        double dx = cosPitch * sinYaw;
        double dy = sinPitch;
        double dz = cosPitch * cosYaw;

        int rayLength          = DarkStareConfig.RAY_LENGTH.get();
        int minConsecutiveDark = DarkStareConfig.MIN_CONSECUTIVE_DARK_BLOCKS.get();
        int maxLightLevel      = DarkStareConfig.MAX_LIGHT_LEVEL.get();
        double tunnelBoost     = DarkStareConfig.DARK_TUNNEL_BOOST.get();

        hasDarkTarget = false;
        intensity     = Math.max(intensity - 0.012f, 0f); // base decay when not staring

        int consecutiveCount = 0;
        float maxDepthFactor = 0f;

        float threshold = maxLightLevel / 15f;

        for (double t = 1.0; t <= rayLength; t += 0.4) {
            double px = eyeX + dx * t;
            double py = eyeY + dy * t;
            double pz = eyeZ + dz * t;

            int bx = (int)Math.floor(px);
            int by = (int)Math.floor(py);
            int bz = (int)Math.floor(pz);

            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState state = mc.level.getBlockState(pos);

            float blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos);
            float skyLight   = mc.level.getBrightness(LightLayer.SKY, pos);

            boolean isDark = blockLight <= threshold && skyLight <= threshold + 0.25f;

            if (isDark) {
                consecutiveCount++;
                // Depth factor: deeper tunnels feel worse
                float currentDepth = (float)(t / rayLength);
                maxDepthFactor = Math.max(maxDepthFactor, currentDepth);
            } else {
                if (consecutiveCount >= minConsecutiveDark) {
                    hasDarkTarget = true;
                }
                consecutiveCount = 0;

                // solid block close to face blocks the effect
                if (state.isSolidRender(mc.level, pos) && t < 3.0) {
                    break;
                }
            }
        }

        if (consecutiveCount >= minConsecutiveDark) {
            hasDarkTarget = true;
        }

        // Apply tunnel boost: deeper tunnels accelerate intensity ramp-up
        float effectiveDepthBoost = (float)Math.min(tunnelBoost * maxDepthFactor, 2.0f);

        double startSeconds = DarkStareConfig.START_SECONDS.get();
        double fullSeconds  = DarkStareConfig.FULL_INTENSITY_SECONDS.get();
        double fadeSpeed    = DarkStareConfig.FADE_SPEED.get();

        if (hasDarkTarget) {
            stareTimer += 1f / 20f;
        } else {
            stareTimer -= (float)(fadeSpeed * (1f / 20f));
        }
        stareTimer = Math.max(0f, stareTimer);

        if (stareTimer < (float)startSeconds) {
            intensity = Math.max(intensity - 0.03f, 0f);
        } else {
            float denom = (float)(fullSeconds - startSeconds);
            if (denom <= 0.1f) denom = 5f; // safe fallback for bad config
            float baseTarget = (float)Math.min(
                (stareTimer - startSeconds) / denom, 1f
            );

            // Tunnel/depth bonus: slightly speeds reaching full intensity
            float boostedTarget = Math.min(baseTarget * (0.8f + effectiveDepthBoost * 0.25f), 1f);

            // Blend toward target smoothly but steadily
            float blendSpeed = 0.04f + 0.03f * maxDepthFactor;
            intensity += (boostedTarget - intensity) * blendSpeed;
        }

        intensity = Math.min(intensity, 1.0f);

        // Screen shake: grows quadratically with intensity, but gated by config
        if (DarkStareConfig.ENABLE_SCREEN_SHAKE.get()) {
            float mult = DarkStareConfig.SCREEN_SHAKE_MULTIPLIER.get().floatValue();
            screenShake = intensity * intensity * 2.5f * Math.min(mult, 5f);
        } else {
            screenShake = 0f;
        }

        // Heartbeat pulse: sin wave that accelerates with intensity
        double now = System.currentTimeMillis();
        float beatFreq = 0.4f + intensity * 1.9f;
        heartbeatPulse = (float)Math.max(0, Math.sin(now * 0.008 * beatFreq) * intensity);

        // Update sounds
        SoundManager.update(intensity, stareTimer > (float)startSeconds);
    }

    private void resetIfActive() {
        if (intensity > 0 || stareTimer > 0) {
            resetAll();
        }
    }

    public static void resetAll() {
        stareTimer = 0f;
        intensity = 0f;
        hasDarkTarget = false;
        screenShake = 0f;
        heartbeatPulse = 0f;
        SoundManager.reset();
        StrobeEffectRenderer.reset();
        AfterimageRenderer.reset();
    }

    // Public getters used by renderers / events.
    public static float getIntensity()       { return intensity; }
    public static boolean isStaringDeeply()  { return hasDarkTarget && stareTimer > 3f; }
    public static float getScreenShake()     { return screenShake; }
    public static float getHeartbeatPulse()  { return heartbeatPulse; }

    @SubscribeEvent
    public void onFOVCompute(ComputeFov event) {
        if (!DarkStareConfig.ENABLE_FOV_ZOOM.get() || intensity <= 0.05f) return;

        float baseFov = (float)event.getFOV();
        double maxReduction = DarkStareConfig.MAX_FOV_REDUCTION.get();

        // Smooth zoom-in: FOV is pulled toward max reduction by intensity^1.2 for subtlety then escalation.
        float effectiveIntensity = (float)Math.pow(intensity, 1.2);
        float zoomFactor = (float)(maxReduction + (1.0 - maxReduction) * (1f - effectiveIntensity));

        float newFov = baseFov * zoomFactor;

        // Micro-jitter for unease at high intensity.
        if (DarkStareConfig.ENABLE_JITTER.get() && intensity > 0.45f) {
            double jitter = Math.sin(System.nanoTime() / 1_300_000.0) * 0.0025 * intensity;
            newFov *= (float)(1 + jitter);

            // Additional high-intensity "pressure" wobble.
            if (intensity > 0.7f) {
                double heavyJitter = Math.sin(System.nanoTime() / 900_000.0) * 0.002 * intensity;
                newFov *= (float)(1 + heavyJitter);
            }
        }

        // Optional FOV-based "screen shake" instead of moving camera directly.
        if (DarkStareConfig.ENABLE_SCREEN_SHAKE.get() && intensity > 0.35f) {
            double shake = Math.sin(System.nanoTime() / 1_600_000.0) * 0.002 * screenShake;
            newFov *= (float)(1 + shake);
        }

        event.setFOV(newFov);
    }

    public void onRenderGuiOverlay(Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float intensity      = DarkStareClientEvents.intensity;
        float shake          = screenShake;
        float beat           = heartbeatPulse;
        long gameTime        = mc.level.getGameTime();
        Window window        = mc.getWindow();

        // Render order: background layers first, then foreground effects last.
        // 1) Breathing distortion — slow organic pulse (deepest layer)
        BreathingDistortionRenderer.render(intensity, window);

        // 1b) Heartbeat pulse overlay — expanding rings synced to heartbeat
        HeartbeatPulseOverlay.render(intensity, beat, gameTime, window);

        // 2) Vignette with color shift and heartbeat sync
        VignetteRenderer.renderVignette(intensity, window, shake, beat);

        // 3) Peripheral blur — tunnel vision effect
        PeripheralBlurRenderer.render(intensity, window);

        // 4) Chromatic aberration — RGB color splitting
        ChromaticAberrationRenderer.render(intensity, window);

        // 5) Red tint with pulsing variation
        RedTintRenderer.render(intensity, window);

        // 6) Film grain with dual-layer depth
        FilmGrainRenderer.render(intensity, window);

        // 7) Creeping darkness — dark patches advancing from screen edges
        CreepingDarknessRenderer.render(intensity, gameTime, window);

        // 8) Screen darkening — near-blackout at peak intensity
        ScreenDarkeningRenderer.render(intensity, window);

        // 9) Shadow figures — ghostly silhouettes in periphery
        ShadowFiguresRenderer.render(intensity, gameTime, window);

        // 10) Spatial distortion — reality-bending waves at high intensity
        SpatialDistortionRenderer.render(intensity, gameTime, window);
        // 10b) Screen tear / glitch — reality-breaking artifacts at extreme intensity
        ScreenTearRenderer.render(intensity, gameTime, window);

        // 10c) Reality fracture — crack-like fissures spreading from screen edges
        RealityFractureRenderer.render(intensity, gameTime, window);

        // 10d) Breathing fog — dark organic fog creeping across screen
        BreathingFogRenderer.render(intensity, gameTime, window);

        // 10e) Peripheral presence — ghostly shapes at screen edges (extreme intensity)
        PeripheralPresenceRenderer.render(intensity, gameTime, window);

        // 10f) Afterimage — ghostly visual echoes that linger on screen
        AfterimageRenderer.render(intensity, window);
        // 11) Strobe effect — sudden flashes of light
        StrobeEffectRenderer.render(intensity, window);

        // 12) Eyes overlay — ghostly eyes (rendered last as foreground)
        EyesOverlayRenderer.renderEyes(intensity, gameTime, window, shake);
    }
}