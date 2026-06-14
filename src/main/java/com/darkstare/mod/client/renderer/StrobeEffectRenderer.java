package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;

/**
 * Strobe/Flash effect — brief, sudden flashes of light that startle the player.
 * Occurs randomly at high intensity levels, like a flickering light or
 * something briefly illuminating in the darkness.
 */
public class StrobeEffectRenderer {

    private static final RandomSource RAND = RandomSource.create();
    private static long lastFlashTime = 0;
    private static float flashAlpha = 0f;
    private static boolean isFlashing = false;

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_STROBE_EFFECT.get()) return;
        if (intensity < 0.6f) return;

        long now = System.currentTimeMillis();
        float flashChance = DarkStareConfig.STROBE_CHANCE.get().floatValue();

        // Flash chance increases with intensity, but stays rare enough to be startling
        float effectiveChance = (intensity - 0.6f) / 0.4f * flashChance;

        if (!isFlashing && RAND.nextFloat() < effectiveChance * 0.01f && now - lastFlashTime > 3000L) {
            isFlashing = true;
            flashAlpha = 0.25f + intensity * 0.15f; // 0.25-0.4 alpha
            lastFlashTime = now;
        }

        if (isFlashing) {
            int w = window.getGuiScaledWidth();
            int h = window.getGuiScaledHeight();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.getBuilder();

            // Flash color: slightly warm white (like a dying light)
            float r = 0.95f;
            float g = 0.85f;
            float b = 0.7f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(0, h, 0).color(r, g, b, flashAlpha).endVertex();
            buf.vertex(w, h, 0).color(r, g, b, flashAlpha * 1.1f).endVertex();
            buf.vertex(w, 0, 0).color(r, g, b, flashAlpha * 0.9f).endVertex();
            buf.vertex(0, 0, 0).color(r, g, b, flashAlpha).endVertex();
            tess.end();

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            // Fade out quickly — flash lasts only a moment
            flashAlpha *= 0.75f;
            if (flashAlpha < 0.01f) {
                isFlashing = false;
                flashAlpha = 0f;
            }
        }
    }

    public static void reset() {
        isFlashing = false;
        flashAlpha = 0f;
        lastFlashTime = 0;
    }
}
