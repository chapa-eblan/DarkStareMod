package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Peripheral Vision Blur — simulates tunnel vision by darkening and blurring
 * the edges of the screen. Creates a claustrophobic feeling as if the world
 * is closing in around you.
 */
public class PeripheralBlurRenderer {

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_PERIPHERAL_BLUR.get()) return;

        // Fix #8: Soften hard threshold so effect fades continuously instead of snapping on/off at 0.25.
        float strength = DarkStareConfig.PERIPHERAL_BLUR_STRENGTH.get().floatValue();

        // Map intensity from [0..1] into a smooth blurAmount starting faintly well before 0.25.
        float blurAmount;
        if (intensity < 0.1f) {
            blurAmount = 0f; // effectively off near zero
        } else {
            // Gradual ramp: at 0.1 → tiny, at 0.25 → moderate, at 1.0 → strong.
            float base = (intensity - 0.1f) / 0.9f; // [0..1] over [0.1..1.0]
            blurAmount = base * Math.min(strength, 3.0f);
        }

        if (blurAmount < 0.005f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Draw darkening bands on each side — simulating tunnel vision
        float bandWidth = blurAmount * 60f; // pixels of blur per side

        // Remove hard cutoff at <2; let it fade smoothly. If tiny, alpha will also be tiny.
        float alpha = intensity * 0.15f * Math.min(blurAmount, 2.0f);
        if (alpha < 0.002f || bandWidth < 0.5f) return;

        // Left band
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(0, h, 0).color(0f, 0f, 0f, alpha * 1.2f).endVertex();
        buf.vertex(bandWidth, h, 0).color(0f, 0f, 0f, alpha * 0.3f).endVertex();
        buf.vertex(bandWidth, 0, 0).color(0f, 0f, 0f, alpha * 0.3f).endVertex();
        buf.vertex(0, 0, 0).color(0f, 0f, 0f, alpha * 1.2f).endVertex();
        tess.end();

        // Right band
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(w - bandWidth, h, 0).color(0f, 0f, 0f, alpha * 0.3f).endVertex();
        buf.vertex(w, h, 0).color(0f, 0f, 0f, alpha * 1.2f).endVertex();
        buf.vertex(w, 0, 0).color(0f, 0f, 0f, alpha * 1.2f).endVertex();
        buf.vertex(w - bandWidth, 0, 0).color(0f, 0f, 0f, alpha * 0.3f).endVertex();
        tess.end();

        // Top band (narrower — less tunnel effect from above)
        float topBand = bandWidth * 0.5f;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(0, h - topBand, 0).color(0f, 0f, 0f, alpha * 0.4f).endVertex();
        buf.vertex(w, h - topBand, 0).color(0f, 0f, 0f, alpha * 0.4f).endVertex();
        buf.vertex(w, h, 0).color(0f, 0f, 0f, alpha * 1.0f).endVertex();
        buf.vertex(0, h, 0).color(0f, 0f, 0f, alpha * 1.0f).endVertex();
        tess.end();

        // Bottom band (wider — as if something is below)
        float bottomBand = bandWidth * 0.7f;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(0, bottomBand, 0).color(0f, 0f, 0f, alpha * 0.5f).endVertex();
        buf.vertex(w, bottomBand, 0).color(0f, 0f, 0f, alpha * 0.5f).endVertex();
        buf.vertex(w, 0, 0).color(0f, 0f, 0f, alpha * 1.1f).endVertex();
        buf.vertex(0, 0, 0).color(0f, 0f, 0f, alpha * 1.1f).endVertex();
        tess.end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
