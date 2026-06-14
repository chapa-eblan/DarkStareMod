package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import com.darkstare.mod.config.DarkStareConfig;

/**
 * Breathing Fog Effect — at high intensity, a dark fog slowly creeps across the screen
 * in organic, breathing patterns. The fog appears to have depth and moves like something
 * alive is breathing behind it. Creates an oppressive atmosphere of suffocation.
 */
public class BreathingFogRenderer {

    private static final int FOG_PATCHES = 16;
    private static final float[] fogSeedsX = new float[FOG_PATCHES];
    private static final float[] fogSeedsY = new float[FOG_PATCHES];
    private static final float[] fogSizes = new float[FOG_PATCHES];

    static {
        for (int i = 0; i < FOG_PATCHES; i++) {
            fogSeedsX[i] = (float)(Math.random() * 1000f);
            fogSeedsY[i] = (float)(Math.random() * 1000f);
            fogSizes[i] = 50f + (float)(Math.random() * 200f);
        }
    }

    public static void render(float intensity, long gameTime, Window window) {
        if (!DarkStareConfig.ENABLE_BREATHING_FOG.get()) return;
        if (intensity < 0.6f) return;

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

        // Breathing cycle — slow in/out like something is breathing
        double breathPhase = Math.sin(now * 0.3f) * 0.5 + 0.5; // 0 to 1, slow oscillation
        float fogAlpha = (intensity - 0.6f) * 0.4f * (float)(breathPhase * 0.7 + 0.3);

        if (fogAlpha < 0.01f) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            return;
        }

        // Draw fog patches — soft, organic shapes that drift slowly
        for (int i = 0; i < FOG_PATCHES; i++) {
            double driftX = Math.sin(now * 0.15f + fogSeedsX[i] * 0.01f) * w * 0.3f;
            double driftY = Math.cos(now * 0.12f + fogSeedsY[i] * 0.01f) * h * 0.3f;

            float centerX = (float)(fogSeedsX[i] % w + driftX);
            float centerY = (float)(fogSeedsY[i] % h + driftY);
            float size = fogSizes[i] * (0.8f + (float)breathPhase * 0.4f); // expands with breath

            // Draw as a soft circle using multiple concentric rings
            int rings = 5;
            for (int ringIdx = rings - 1; ringIdx >= 0; ringIdx--) {
                float ringRadius = size * ((ringIdx + 1) / (float)rings);
                float ringAlpha = fogAlpha * ((rings - ringIdx) / (float)rings) * 0.3f;

                if (ringAlpha < 0.005f) continue;

                int segments = 12;
                for (int s = 0; s < segments; s++) {
                    double angle1 = (s / (double)segments) * Math.PI * 2f;
                    double angle2 = ((s + 1) / (double)segments) * Math.PI * 2f;

                    // Add slight wobble to make it look organic, not perfectly circular
                    float wobble1 = 0.9f + (float)Math.sin(now * 2f + s * 0.5f + i) * 0.1f;
                    float wobble2 = 0.9f + (float)Math.cos(now * 1.7f + s * 0.3f + i) * 0.1f;

                    buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                    // Fog color — very dark grey with slight blue tint
                    float r = 0.02f;
                    float g = 0.025f;
                    float b = 0.04f;

                    buf.vertex(centerX, centerY, 0).color(r, g, b, ringAlpha * 1.5f).endVertex();
                    buf.vertex(
                        centerX + (float)Math.cos(angle1) * ringRadius * wobble1,
                        centerY + (float)Math.sin(angle1) * ringRadius * wobble1,
                        0
                    ).color(r, g, b, ringAlpha).endVertex();
                    buf.vertex(
                        centerX + (float)Math.cos(angle2) * ringRadius * wobble2,
                        centerY + (float)Math.sin(angle2) * ringRadius * wobble2,
                        0
                    ).color(r, g, b, ringAlpha * 0.9f).endVertex();

                    tess.end();
                }
            }
        }

        // Draw creeping tendrils — thin dark lines that extend from fog patches toward center
        if (intensity > 0.8f) {
            float tendrilAlpha = (intensity - 0.8f) * 1.5f;
            for (int i = 0; i < FOG_PATCHES / 2; i++) {
                double driftX = Math.sin(now * 0.15f + fogSeedsX[i] * 0.01f) * w * 0.3f;
                double driftY = Math.cos(now * 0.12f + fogSeedsY[i] * 0.01f) * h * 0.3f;

                float startX = (float)(fogSeedsX[i] % w + driftX);
                float startY = (float)(fogSeedsY[i] % h + driftY);

                // Tendril extends toward center of screen with organic curve
                double angleToCenter = Math.atan2(h / 2f - startY, w / 2f - startX);
                float tendrilLength = 80f + (float)(Math.random() * 120f) * intensity;

                int segments = 6;
                float currentX = startX;
                float currentY = startY;

                for (int seg = 0; seg < segments; seg++) {
                    double segAngle = angleToCenter + Math.sin(now * 3f + i + seg) * 0.5f;
                    float segLen = tendrilLength / segments;

                    float nextX = currentX + (float)Math.cos(segAngle) * segLen;
                    float nextY = currentY + (float)Math.sin(segAngle) * segLen;

                    float distFromStart = (float)Math.sqrt(Math.pow(currentX - startX, 2) + Math.pow(currentY - startY, 2));
                    float maxDist = tendrilLength;
                    float segAlpha = tendrilAlpha * Math.max(0, 1f - distFromStart / maxDist);

                    if (segAlpha < 0.01f) break;

                    // Draw as thin line using two triangles
                    double perpAngle = segAngle + Math.PI / 2f;
                    float halfW = 1f + (float)(Math.random() * 2f);

                    buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                    float r = 0.03f;
                    float g = 0.02f;
                    float b = 0.04f;

                    buf.vertex(
                        currentX + (float)Math.cos(perpAngle) * halfW,
                        currentY + (float)Math.sin(perpAngle) * halfW,
                        0
                    ).color(r, g, b, segAlpha).endVertex();
                    buf.vertex(
                        nextX + (float)Math.cos(perpAngle) * halfW,
                        nextY + (float)Math.sin(perpAngle) * halfW,
                        0
                    ).color(r, g, b, segAlpha * 1.1f).endVertex();
                    buf.vertex(
                        currentX - (float)Math.cos(perpAngle) * halfW,
                        currentY - (float)Math.sin(perpAngle) * halfW,
                        0
                    ).color(r, g, b, segAlpha * 0.9f).endVertex();
                    buf.vertex(
                        nextX - (float)Math.cos(perpAngle) * halfW,
                        nextY - (float)Math.sin(perpAngle) * halfW,
                        0
                    ).color(r, g, b, segAlpha * 1.05f).endVertex();

                    tess.end();

                    currentX = nextX;
                    currentY = nextY;
                }
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
