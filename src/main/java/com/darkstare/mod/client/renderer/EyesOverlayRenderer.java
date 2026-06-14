package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Renders ghostly textured eyes in screen periphery with blinking and fade-in.
 * Enhanced: eyes now occasionally "look" toward the player, some have a faint
 * red glow, and they appear/disappear more erratically for maximum unease.
 */
public class EyesOverlayRenderer {

    private static final ResourceLocation EYE_TEXTURE = new ResourceLocation("darkstare", "textures/eye.png");

    public static void renderEyes(float intensity, long gameTime, Window window, float shake) {
        if (intensity <= 0.1f || !DarkStareConfig.ENABLE_EYES_OVERLAY.get()) return;

        double baseChanceD = DarkStareConfig.EYE_BASE_CHANCE.get();
        float maxAlpha = DarkStareConfig.MAX_ALPHA.get().floatValue();
        int maxPairs = DarkStareConfig.MAX_EYE_PAIRS.get();
        if (maxAlpha < 0.01f || maxPairs <= 0) return;

        // Clamp alpha to avoid overwhelming visuals
        float alpha = Math.min(intensity * maxAlpha, 1.0f);
        if (alpha < 0.01f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // Fix #1: Thread-safe per-frame RandomSource instead of static shared one.
        long eyeSeed = gameTime / 35L;
        RandomSource rand = RandomSource.create(eyeSeed);

        // Consistent shake per frame
        float sx = (float)(Math.sin(gameTime * 0.7) * shake * 3);
        float sy = (float)(Math.cos(gameTime * 0.9) * shake * 3);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, EYE_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Fix #7: Batch all eyes into a single buffer instead of per-eye draw calls.
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        int drawn = 0;
        for (int i = 0; i < maxPairs * 4; i++) {
            float chance = (float)(baseChanceD * intensity * 2.0f);
            if (chance < 1.0f && rand.nextFloat() > chance) continue;

            long s = eyeSeed + (long)(i * 137 + rand.nextInt(10000));
            float angle = (s % 6283) / 1000f; // 0 - 2PI
            float dist = 0.42f + (s % 100) / 300f; // 0.42 - 0.75 of screen radius
            float nx = cx + (float)(Math.cos(angle) * dist * w) + sx;
            float ny = cy + (float)(Math.sin(angle) * dist * h * 0.65f) + sy;

            // Subtle "glance at you" effect: occasionally pull eye toward center briefly, then release.
            {
                double glancePhase = gameTime * 0.012 + i * 3.9;
                float glancePulse = (float)Math.max(0, Math.sin(glancePhase));
                float glanceStrength = 0.14f * glancePulse * intensity;
                float dxToCenter = cx - nx;
                float dyToCenter = cy - ny;
                double len = Math.hypot(dxToCenter, dyToCenter);
                if (len > 1) {
                    nx += (float)(dxToCenter / len * glanceStrength);
                    ny += (float)(dyToCenter / len * glanceStrength);
                }
            }

            // Avoid center
            if (Math.abs(nx - cx) < w * 0.22f && Math.abs(ny - cy) < h * 0.22f) continue;

            // Blink: occasionally close the eye — more erratic at high intensity
            float blinkSpeed = 0.25f + intensity * 0.15f;
            float blink = (float)Math.max(0, Math.sin(gameTime * blinkSpeed + i * 2.7) * 1.8 - 1.0);
            float scaleY = 1.0f - blink * 0.95f;
            if (scaleY < 0.05f) continue;

            // Fix #2: Deterministic lifeDuration from seed s instead of mutating shared RAND.nextInt().
            // Keeps variety across eyes without shared-state mutation per frame.
            float lifeDuration = 240 + (intensity > 0.7f ? ((s % 120) + 60) : 0);
            float life = (float)((gameTime + i * 53) % lifeDuration) / lifeDuration;
            // Defensive clamp: sin(0..PI) >= 0, but protect against future changes.
            float fade = (float)Math.sin(life * Math.PI);
            fade = Math.max(0f, Math.min(fade, 1f));
            float pairAlpha = alpha * fade * (0.4f + 0.6f * ((s & 7) / 7f));
            // Clamp final alpha to safe range
            pairAlpha = Math.max(0f, Math.min(pairAlpha, 1f));
            if (pairAlpha < 0.01f) continue;

            // Jitter individual eye position — more jitter at high intensity
            float jitterAmount = 1.5f + intensity * 2.0f;
            float jx = (float)(Math.sin(gameTime * 0.8 + i) * jitterAmount);
            float jy = (float)(Math.cos(gameTime * 0.6 + i) * jitterAmount);

            // Some eyes have a faint red glow at high intensity
            boolean isGlowingEye = (s & 3) == 0 && intensity > 0.6f;
            float r, g, b;
            if (isGlowingEye) {
                r = 1f; g = 0.2f; b = 0.15f;
            } else {
                r = 1f; g = 1f; b = 1f;
            }

            float size = 14f + (s % 7);
            float sizeW = size * (1.1f + 0.2f * (float)Math.sin(gameTime * 0.1 + i));
            float sizeH = size * scaleY;

            // Add vertices for this eye into the batch buffer with per-eye color.
            float ex = nx - sizeW / 2 + jx;
            float ey = ny - sizeH / 2 + jy;
            buf.vertex(ex, ey + sizeH, 0).uv(0, 1).color(r, g, b, Math.min(pairAlpha * (isGlowingEye ? 0.8f : 1.0f), 1.0f)).endVertex();
            buf.vertex(ex + sizeW, ey + sizeH, 0).uv(1, 1).color(r, g, b, Math.min(pairAlpha * (isGlowingEye ? 0.8f : 1.0f), 1.0f)).endVertex();
            buf.vertex(ex + sizeW, ey, 0).uv(1, 0).color(r, g, b, Math.min(pairAlpha * (isGlowingEye ? 0.8f : 1.0f), 1.0f)).endVertex();
            buf.vertex(ex, ey, 0).uv(0, 0).color(r, g, b, Math.min(pairAlpha * (isGlowingEye ? 0.8f : 1.0f), 1.0f)).endVertex();

            drawn++;
            if (drawn >= maxPairs) break;
        }

        // End batch once for all eyes
        tess.end();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
