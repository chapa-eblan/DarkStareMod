package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Spatial Distortion — at high intensity, the screen appears to warp and distort,
 * as if reality itself is bending. Creates subtle wave-like displacement patterns
 * that make the world feel unstable and wrong.
 */
public class SpatialDistortionRenderer {

    private static final int NUM_WAVES = 5;

    public static void render(float intensity, long gameTime, Window window) {
        if (intensity < 0.6f) return;

        // Distortion strength grows sharply after 0.7
        float distortionStrength = Math.max(0f, (intensity - 0.6f)) * 2.5f;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        double now = System.currentTimeMillis() / 1000.0;

        // Draw horizontal wave bands — dark strips that ripple across the screen
        for (int wave = 0; wave < NUM_WAVES; wave++) {
            float waveSpeed = 0.3f + wave * 0.15f;
            double wavePhase = now * waveSpeed + wave * 1.7;

            // Vertical position oscillates across the screen
            float yPos = (float)(Math.sin(wavePhase) * h * 0.4f + h / 2f);

            // Band height grows with distortion strength
            float bandHeight = 3f + distortionStrength * 8f;

            // Alpha: subtle but noticeable at high intensity
            float alpha = distortionStrength * 0.015f;

            if (alpha < 0.002f) continue;

            // Color: very dark with slight color variation per wave
            float r = 0.02f + (wave % 3) * 0.01f;
            float g = 0.01f;
            float b = 0.04f + (wave % 2) * 0.02f;

            // Horizontal ripple — the band curves slightly
            float curveAmount = distortionStrength * 15f;
            double curvePhase = now * 0.5f + wave * 3.1;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float topY = yPos - bandHeight / 2f;
            float bottomY = yPos + bandHeight / 2f;

            // Add slight curve to the band edges
            float leftCurve = (float)(Math.sin(curvePhase) * curveAmount);
            float rightCurve = (float)(Math.sin(curvePhase + Math.PI) * curveAmount);

            buf.vertex(0, topY + leftCurve, 0).color(r, g, b, alpha).endVertex();
            buf.vertex(w, topY + rightCurve, 0).color(r, g, b, alpha * 1.2f).endVertex();
            buf.vertex(w, bottomY + rightCurve, 0).color(r, g, b, alpha * 0.8f).endVertex();
            buf.vertex(0, bottomY + leftCurve, 0).color(r, g, b, alpha * 0.9f).endVertex();

            tess.end();
        }

        // Vertical distortion streaks — thin dark lines that appear and fade
        for (int streak = 0; streak < 3; streak++) {
            double streakPhase = now * 0.2f + streak * 4.5;
            float xPos = (float)(Math.sin(streakPhase) * w * 0.3f + w / 2f);

            // Streaks appear briefly and fade — like tears in reality
            double flickerPhase = now * 1.5f + streak * 2.3;
            float flickerAlpha = (float)Math.max(0, Math.sin(flickerPhase) * distortionStrength * 0.02f);

            if (flickerAlpha < 0.002f) continue;

            float streakWidth = 1f + distortionStrength * 3f;
            float r = 0.01f;
            float g = 0.005f;
            float b = 0.03f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            buf.vertex(xPos - streakWidth / 2, h, 0).color(r, g, b, flickerAlpha * 1.2f).endVertex();
            buf.vertex(xPos + streakWidth / 2, h, 0).color(r, g, b, flickerAlpha).endVertex();
            buf.vertex(xPos + streakWidth / 2, 0, 0).color(r, g, b, flickerAlpha * 0.8f).endVertex();
            buf.vertex(xPos - streakWidth / 2, 0, 0).color(r, g, b, flickerAlpha * 1.1f).endVertex();

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
