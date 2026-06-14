package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Heartbeat Pulse Overlay — screen pulses with each heartbeat, creating a visceral
 * sense of dread. The pulse radiates from center as concentric rings that expand outward,
 * synchronized with the player's accelerating heartbeat.
 */
public class HeartbeatPulseOverlay {

    private static final int NUM_RINGS = 6;

    public static void render(float intensity, float heartbeatPulse, long gameTime, Window window) {
        if (intensity < 0.3f) return;
        if (heartbeatPulse < 0.1f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Fix #10: Use gameTime instead of System.currentTimeMillis() for consistent timing.
        double now = (gameTime / 1000.0);

        // Draw expanding pulse rings from center
        for (int ring = 0; ring < NUM_RINGS; ring++) {
            float ringPhase = (float)(((now * 2.5f + ring * 0.8f) % 3.0f) / 3.0f);

            // Ring expands outward — radius grows from center to edge
            float maxRadius = (float)Math.sqrt(w * w + h * h) / 2f;
            float currentRadius = ringPhase * maxRadius;

            // Alpha: strongest at the leading edge of each pulse, fades as it expands
            float alpha = heartbeatPulse * intensity * 0.06f * (1f - ringPhase);

            if (alpha < 0.002f) continue;

            // Draw a thin ring using two concentric quads with gradient
            float innerR = currentRadius - maxRadius * 0.03f;
            float outerR = currentRadius + maxRadius * 0.03f;

            if (innerR < 0) continue;

            // Ring color: deep red that fades to dark purple at edges
            float r = 0.25f - ringPhase * 0.15f;
            float g = 0.02f;
            float b = 0.08f + ringPhase * 0.04f;

            // Draw ring as a quad with varying alpha per corner (top/bottom darker)
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            int cx = w / 2;
            int cy = h / 2;

            // Top of ring
            float topY = cy - outerR;
            float bottomY = cy + outerR;
            float leftX = cx - innerR;
            float rightX = cx + innerR;

            buf.vertex(leftX, topY, 0).color(r, g, b, alpha * 1.2f).endVertex();
            buf.vertex(rightX, topY, 0).color(r, g, b, alpha * 1.2f).endVertex();
            buf.vertex(rightX, bottomY, 0).color(r, g, b, alpha * 0.8f).endVertex();
            buf.vertex(leftX, bottomY, 0).color(r, g, b, alpha * 0.8f).endVertex();

            tess.end();
        }

        // Central pulse — subtle red flash at center on each beat
        float centerPulse = heartbeatPulse * intensity * 0.03f;
        if (centerPulse > 0.002f) {
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            int pulseSize = Math.min(w, h) / 4;
            int cx = w / 2;
            int cy = h / 2;

            buf.vertex(cx - pulseSize, cy + pulseSize, 0).color(0.15f, 0.01f, 0.03f, centerPulse * 0.6f).endVertex();
            buf.vertex(cx + pulseSize, cy + pulseSize, 0).color(0.15f, 0.01f, 0.03f, centerPulse * 0.6f).endVertex();
            buf.vertex(cx + pulseSize, cy - pulseSize, 0).color(0.18f, 0.02f, 0.04f, centerPulse * 0.8f).endVertex();
            buf.vertex(cx - pulseSize, cy - pulseSize, 0).color(0.18f, 0.02f, 0.04f, centerPulse * 0.8f).endVertex();

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
