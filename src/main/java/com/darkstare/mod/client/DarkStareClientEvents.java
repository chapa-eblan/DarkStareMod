package com.darkstare.mod.client;

import com.darkstare.mod.config.DarkStareConfig;
import com.darkstare.mod.client.renderer.SoundManager;
import com.darkstare.mod.client.renderer.EyesOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Client-only event handler for DarkStare.
 * Registered on MinecraftForge.EVENT_BUS (game/render events).
 */
public class DarkStareClientEvents {

    private static float stareTimer = 0f;
    private static float intensity = 0f;
    private static boolean hasDarkTarget = false;

    public static void registerClientEvents() {
        // Forge game/render events — use MinecraftForge.EVENT_BUS.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new DarkStareClientEvents());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !event.player.equals(mc.player)) return;

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

        hasDarkTarget = false;
        intensity     = Math.max(intensity - 0.012f, 0f); // decay when not staring

        int consecutiveCount = 0;

        for (double t = 1.0; t <= rayLength; t += 0.5) {
            double px = eyeX + dx * t;
            double py = eyeY + dy * t;
            double pz = eyeZ + dz * t;

            int bx = (int)Math.floor(px);
            int by = (int)Math.floor(py);
            int bz = (int)Math.floor(pz);

            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(bx, by, bz);
            float brightness = mc.level.getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK, pos
            );

            if (brightness <= maxLightLevel / 15f) {
                consecutiveCount++;
                if (consecutiveCount >= minConsecutiveDark) {
                    hasDarkTarget = true;
                    break;
                }
            } else {
                consecutiveCount = 0;
            }
        }

        double startSeconds = DarkStareConfig.START_SECONDS.get();
        double fadeSpeed    = DarkStareConfig.FADE_SPEED.get();

        if (hasDarkTarget) {
            stareTimer += 1f / 20f;
        } else {
            stareTimer -= (float)(fadeSpeed * (1f / 20f));
        }

        stareTimer = Math.max(0f, stareTimer);

        double fullSeconds = DarkStareConfig.FULL_INTENSITY_SECONDS.get();

        if (stareTimer < (float)startSeconds) {
            intensity = Math.max(intensity - 0.025f, 0f);
        } else {
            float targetIntensity = (float)Math.min(
                (stareTimer - startSeconds) / (fullSeconds - startSeconds), 1f
            );
            float blendSpeed = 0.05f;
            intensity += (targetIntensity - intensity) * blendSpeed;
        }

        SoundManager.update(intensity, stareTimer > (float)startSeconds);
    }

    private void resetIfActive() {
        if (intensity > 0 || stareTimer > 0) {
            stareTimer = 0f;
            intensity = 0f;
            hasDarkTarget = false;
            SoundManager.reset();
        }
    }

    @SubscribeEvent
    public void onFOVCompute(ComputeFov event) {
        if (!DarkStareConfig.ENABLE_FOV_ZOOM.get() || intensity <= 0.05f) return;

        float baseFov = (float) event.getFOV();
        double maxReduction = DarkStareConfig.MAX_FOV_REDUCTION.get();

        float zoomFactor = (float)(maxReduction + (1.0 - maxReduction) * (1f - intensity));
        float newFov = baseFov * zoomFactor;

        // Micro-shake for unease at high intensity.
        if (DarkStareConfig.ENABLE_JITTER.get() && intensity > 0.55f) {
            double jitter = Math.sin(System.nanoTime() / 1_200_000.0) * 0.003 * intensity;
            newFov *= (float)(1 + jitter);
        }

        event.setFOV(newFov);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!DarkStareConfig.ENABLE_EYES_OVERLAY.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Render eyes in LAST stage after the world is drawn.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        long gameTime = mc.level.getGameTime();
        EyesOverlayRenderer.renderEyes(intensity, gameTime);
    }

    public static float getIntensity() {
        return intensity;
    }
}
