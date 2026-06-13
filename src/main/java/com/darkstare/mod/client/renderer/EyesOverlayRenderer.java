package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Renders ghostly eyes appearing in darkness (screen-space overlay via RenderLevelStageEvent).
 */
public class EyesOverlayRenderer {

    public static void renderEyes(float intensity, long gameTime) {
        if (intensity <= 0f || !com.darkstare.mod.config.DarkStareConfig.ENABLE_EYES_OVERLAY.get()) return;

        float alpha = (float)(intensity * com.darkstare.mod.config.DarkStareConfig.MAX_ALPHA.get());
        int count = com.darkstare.mod.config.DarkStareConfig.MAX_EYE_PAIRS.get();
        long eyeSeed = gameTime / 60L;

        if (alpha < 0.01f || count <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int w = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        for (int i = 0; i < count && alpha > 0.01f; i++) {
            long s = eyeSeed + (long)(i * 73);
            float nx = ((s >> 8) % 1000 / 1000f - 0.5f) * w;
            float ny = (((s >> 16) % 1000 / 1000f - 0.42f)) * h;

            // Only place eyes near screen edges (creepy periphery effect)
            if ((nx > -w*0.3 && nx < w*0.3 && ny > -h*0.3 && ny < h*0.3)) continue;

            float pairAlpha = alpha * (0.4f + 0.6f * ((s & 7) / 7f));

            float baseSize = 8f;
            float stretchX = baseSize * (float)(1.4f + 0.7f * Math.abs(Math.sin(eyeSeed * 0.8)));

            drawEyePair(nx - stretchX/2f, ny, stretchX, baseSize, pairAlpha);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawEyePair(float cx, float cy, float spread, float size, float alpha) {
        addPupil(cx - spread/2f, cy - (float)(size * 0.15),
                 (float)(size * 0.7f), alpha);
        addPupil(cx + spread/2f, cy - (float)(size * 0.1f),
                 (float)(size * 0.6f), alpha * 0.85f);
    }

    private static void addPupil(float px, float py, float radius, float alpha) {
        int segments = 12;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Use TRIANGLE_FAN via VertexFormat.Mode (official mappings).
        buf.begin(
            VertexFormat.Mode.TRIANGLE_FAN,
            DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i < segments; i++) {
            float angle = (float)(i * 2.0 / segments * Math.PI);

            if (i == 0) {
                // Center glow vertex at first position
                buf.vertex(px, py, 0f)
                   .color(0.95f, 0.78f, 0.1f, alpha);
                buf.endVertex();
            } else {
                float x = px + radius * (float)Math.cos(angle);
                float y = py - radius * (float)Math.sin(angle);

                // Outer rim (darker)
                buf.vertex(x, y, 0f)
                   .color(0.6f, 0.45f, 0.05f, (float)(alpha * 0.3));
                buf.endVertex();
            }
        }

        tess.end();
    }
}
