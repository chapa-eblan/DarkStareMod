package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import com.darkstare.mod.config.DarkStareConfig;
/**
 * Reality Fracture Effect — at extreme intensity, thin crack-like lines appear on screen,
 * as if reality is breaking apart. The cracks grow and branch over time, creating a web of
 * dark fissures that pulse with faint red light.
 */
public class RealityFractureRenderer {

    private static final int MAX_CRACKS = 12;
    // Fix #11: Use long seeds instead of float to avoid precision loss on large values.
    private static final long[] crackSeeds = new long[MAX_CRACKS];
    private static long lastCrackUpdate = 0;
    private static final long CRACK_UPDATE_INTERVAL = 3000L; // update cracks every 3 seconds

    static {
        for (int i = 0; i < MAX_CRACKS; i++) {
            crackSeeds[i] = (long)(Math.random() * 1000f);
        }
    }

    public static void render(float intensity, long gameTime, Window window) {
        if (!DarkStareConfig.ENABLE_REALITY_FRACTURE.get()) return;
        if (intensity < 0.8f) return;
        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Fix #10: Use gameTime-based time instead of System.currentTimeMillis().
        double now = (gameTime / 1000.0);

        // Update crack seeds periodically for dynamic feel (still using gameTime)
        if (gameTime - lastCrackUpdate > CRACK_UPDATE_INTERVAL) {
            for (int i = 0; i < MAX_CRACKS; i++) {
                crackSeeds[i] += (long)(Math.random() * 50f);
            }
            lastCrackUpdate = gameTime;
        }

        // Draw cracks — thin, branching lines that appear from edges and spread inward
        float fractureAlpha = (intensity - 0.8f) * 0.6f;
        if (fractureAlpha < 0.01f) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            return;
        }

        // Batch all crack segments into one draw call to reduce overhead and artifacts.
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean anyCrack = false;

        for (int i = 0; i < MAX_CRACKS; i++) {
            double phase = now * 0.5f + crackSeeds[i] * 0.1f;
            float visibility = (float)Math.max(0, Math.sin(phase) * fractureAlpha);

            if (visibility < 0.02f) continue;

            long baseSeed = crackSeeds[i] + (long)(i * 7919);
            int edge = i % 4;
            float startX, startY;

            switch (edge) {
                case 0: // top
                    startX = (float)((baseSeed & 0x3FFFFFFF) / (double)0x3FFFFFFF * w);
                    startY = 0f;
                    break;
                case 1: // right
                    startX = w;
                    startY = (float)(((baseSeed >> 20) & 0x3FFFFF) / (double)0x3FFFFF * h);
                    break;
                case 2: // bottom
                    startX = (float)(((baseSeed >> 10) & 0x3FFFFF) / (double)0x3FFFFF * w);
                    startY = h;
                    break;
                default: // left
                    startX = 0f;
                    startY = (float)((baseSeed & 0x3FFFFF) / (double)0x3FFFFF * h);
                    break;
            }

            float currentX = startX;
            float currentY = startY;

            int maxSegments = 8 + (int)(crackSeeds[i] % 12);
            float crackWidth = 0.5f + (float)((baseSeed & 0x3FFFFF) / (double)0x3FFFFF * 1.5f);

            for (int seg = 0; seg < maxSegments; seg++) {
                long segSeed = baseSeed ^ (long)(seg * 268435459L + now * 7.3);
                double angle = Math.atan2(h / 2f - currentY, w / 2f - currentX);
                float randAngleOffset = (float)((segSeed & 0x3FFFFFFF) / (double)0x3FFFFFFF * 1.2f - 0.6f);
                angle += randAngleOffset;

                float segLength = 10f + (float)((segSeed >> 10 & 0x3FFFFF) / (double)0x3FFFFF * 40f) * intensity;
                float nextX = currentX + (float)Math.cos(angle) * segLength;
                float nextY = currentY + (float)Math.sin(angle) * segLength;

                nextX = Math.max(0, Math.min(w, nextX));
                nextY = Math.max(0, Math.min(h, nextY));

                float distFromEdge = (float)Math.sqrt(Math.pow(currentX - startX, 2) + Math.pow(currentY - startY, 2));
                float maxDist = Math.max(w, h) * 0.6f;
                float segAlpha = visibility * Math.max(0, 1f - distFromEdge / maxDist);

                if (segAlpha < 0.01f) break;

                double perpAngle = angle + Math.PI / 2f;
                float halfW = crackWidth / 2f;

                // Crack color — dark with faint red glow that pulses.
                float pulsePhase = (float)(now * 3f + i * 0.7f);
                float redPulse = 0.15f + (float)Math.sin(pulsePhase) * 0.1f;
                float r = redPulse;
                float g = 0.02f;
                float b = 0.03f;

                float cx1 = (float)(currentX + Math.cos(perpAngle) * halfW);
                float cy1 = (float)(currentY + Math.sin(perpAngle) * halfW);
                float cx2 = (float)(nextX + Math.cos(perpAngle) * halfW);
                float cy2 = (float)(nextY + Math.sin(perpAngle) * halfW);
                float cx3 = (float)(nextX - Math.cos(perpAngle) * halfW);
                float cy3 = (float)(nextY - Math.sin(perpAngle) * halfW);
                float cx4 = (float)(currentX - Math.cos(perpAngle) * halfW);
                float cy4 = (float)(currentY - Math.sin(perpAngle) * halfW);

                buf.vertex(cx1, cy1, 0).color(r, g, b, segAlpha).endVertex();
                buf.vertex(cx2, cy2, 0).color(r, g, b, segAlpha * 1.1f).endVertex();
                buf.vertex(cx3, cy3, 0).color(r, g, b, segAlpha * 0.9f).endVertex();
                buf.vertex(cx4, cy4, 0).color(r, g, b, segAlpha * 1.05f).endVertex();

                anyCrack = true;

                currentX = nextX;
                currentY = nextY;
            }
        }

        if (anyCrack) {
            tess.end();
        }

        // Fracture glow: stable pseudo-random positions/radii so glows drift smoothly instead of flickering.
        if (intensity > 0.9f) {
            float glowAlpha = (intensity - 0.9f) * 2f;
            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < MAX_CRACKS / 3; i++) {
                double phase = now * 0.5f + crackSeeds[i] * 0.1f;
                float glowVis = (float)Math.max(0, Math.sin(phase) * glowAlpha);

                if (glowVis < 0.02f) continue;

                int edge = i % 4;
                long gSeed = (long)(now * 3.1f + i * 7919L);
                float gx, gy;

                switch (edge) {
                    case 0: gx = (float)((gSeed & 0x3FFFFFFF) / (double)0x3FFFFFFF * w); gy = 0f; break;
                    case 1: gx = w; gy = (float)(((gSeed >> 20) & 0x3FFFFF) / (double)0x3FFFFF * h); break;
                    case 2: gx = (float)(((gSeed >> 10) & 0x3FFFFF) / (double)0x3FFFFF * w); gy = h; break;
                    default: gx = 0f; gy = (float)((gSeed & 0x3FFFFF) / (double)0x3FFFFF * h); break;
                }

                float glowRadius = 15f + (float)(((gSeed >> 30) & 0x3FFFFFFF) / (double)0x3FFFFFFF * 25f);
                int segments = 8;

                // Center vertex with full red color and strong alpha.
                float rCenter = 0.3f + (float)Math.sin(now * 4f + i) * 0.15f;
                buf.vertex(gx, gy, 0).color(rCenter, 0.05f, 0.02f, glowVis).endVertex();

                // Edge vertices fading outward with decreasing alpha.
                for (int s = 0; s <= segments; s++) {
                    double angle = (s / (double)segments) * Math.PI * 2f;
                    float ex = gx + (float)(Math.cos(angle) * glowRadius);
                    float ey = gy + (float)(Math.sin(angle) * glowRadius);
                    float rEdge = rCenter * 0.7f;
                    float aEdge = glowVis * 0.3f;
                    buf.vertex(ex, ey, 0).color(rEdge, 0.05f, 0.02f, aEdge).endVertex();
                }
            }

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
