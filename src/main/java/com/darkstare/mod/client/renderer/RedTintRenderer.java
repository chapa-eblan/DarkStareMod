package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Subtle red tint that seeps in at very high intensity.
 * Enhanced with pulsing variation — the red breathes and shifts between
 * deep crimson and sickly orange, like blood pumping through veins.
 */
public class RedTintRenderer {

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_RED_TINT.get()) return;
        if (intensity < 0.5f) return;

        // Base alpha grows from 0.5 to 1.0 intensity
        float baseAlpha = (intensity - 0.5f) / 0.5f * 0.14f;
        if (baseAlpha < 0.001f) return;

        // Pulsing: the red breathes with a slow, irregular rhythm
        double now = System.currentTimeMillis();
        float pulse1 = (float)Math.sin(now * 0.003);
        float pulse2 = (float)Math.sin(now * 0.007 + 1.5); // second slower wave for irregularity
        float combinedPulse = (pulse1 * 0.6f + pulse2 * 0.4f) * 0.3f; // -0.3 to +0.3

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Color shifts between deep crimson and sickly orange based on pulse
        float rTop = 0.55f + combinedPulse * 0.3f;   // 0.25 - 0.85
        float gTop = 0.0f + Math.max(0, combinedPulse) * 0.15f; // slight green at peak (sickly)
        float bTop = 0.0f;

        float rBottom = 0.35f + combinedPulse * 0.2f;
        float gBottom = 0.0f;
        float bBottom = 0.0f;

        float alphaTop = Math.max(0, baseAlpha + combinedPulse);
        float alphaBottom = Math.max(0, baseAlpha * 1.3f + combinedPulse);

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(0, h, 0).color(rTop, gTop, bTop, alphaTop).endVertex();
        buf.vertex(w, h, 0).color(rTop, gTop, bTop, alphaTop * 1.1f).endVertex();
        buf.vertex(w, 0, 0).color(rBottom, gBottom, bBottom, alphaBottom).endVertex();
        buf.vertex(0, 0, 0).color(rBottom, gBottom, bBottom, alphaBottom * 0.9f).endVertex();
        tess.end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
