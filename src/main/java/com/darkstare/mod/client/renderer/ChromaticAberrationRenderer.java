package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Chromatic aberration — RGB channel separation at screen edges,
 * intensifying with fear. Creates a sickly color-split effect like
 * looking through cracked glass. Renders as colored fringe bands along
 * the screen perimeter that grow wider and more saturated with intensity.
 */
public class ChromaticAberrationRenderer {

    private static final int FRINGE_STEPS = 8; // gradient steps per band

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_CHROMATIC_ABERRATION.get()) return;
        if (intensity < 0.2f) return;

        float strength = DarkStareConfig.CHROMATIC_ABERRATION_STRENGTH.get().floatValue();
        // Aberration grows non-linearly: barely visible at 0.2, strong at 1.0
        float aberrationAmount = (intensity - 0.2f) / 0.8f;
        aberrationAmount = aberrationAmount * aberrationAmount * Math.min(strength, 4.0f);

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        // Band width in pixels — grows with intensity
        float bandWidth = aberrationAmount * 12f;
        if (bandWidth < 1f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // --- Red fringe on the right and bottom edges ---
        float redAlpha = aberrationAmount * 0.25f;
        if (redAlpha > 0.01f) {
            // Right edge — gradient from pure red at edge to transparent inward
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS; step++) {
                float t = step / (float)FRINGE_STEPS; // 0 = outermost edge, 1 = innermost
                float x = w - bandWidth * (1f - t);
                float a = redAlpha * (1f - t); // fades inward

                buf.vertex(x, h, 0).color(1f, 0.15f, 0.1f, a).endVertex();
                buf.vertex(x, 0, 0).color(1f, 0.15f, 0.1f, a).endVertex();
            }
            tess.end();

            // Bottom edge — gradient from pure red at edge to transparent upward
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS; step++) {
                float t = step / (float)FRINGE_STEPS;
                float y = bandWidth * (1f - t);
                float a = redAlpha * (1f - t);

                buf.vertex(0, y, 0).color(1f, 0.15f, 0.1f, a).endVertex();
                buf.vertex(w, y, 0).color(1f, 0.15f, 0.1f, a).endVertex();
            }
            tess.end();
        }

        // --- Cyan fringe on the left and top edges (opposite of red) ---
        float cyanAlpha = aberrationAmount * 0.22f;
        if (cyanAlpha > 0.01f) {
            // Left edge — gradient from pure cyan at edge to transparent inward
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS; step++) {
                float t = step / (float)FRINGE_STEPS;
                float x = bandWidth * (1f - t);
                float a = cyanAlpha * (1f - t);

                buf.vertex(x, h, 0).color(0.1f, 0.95f, 1f, a).endVertex();
                buf.vertex(x, 0, 0).color(0.1f, 0.95f, 1f, a).endVertex();
            }
            tess.end();

            // Top edge — gradient from pure cyan at edge to transparent downward
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS; step++) {
                float t = step / (float)FRINGE_STEPS;
                float y = h - bandWidth * (1f - t);
                float a = cyanAlpha * (1f - t);

                buf.vertex(0, y, 0).color(0.1f, 0.95f, 1f, a).endVertex();
                buf.vertex(w, y, 0).color(0.1f, 0.95f, 1f, a).endVertex();
            }
            tess.end();
        }

        // --- Magenta fringe on top-right and bottom-left corners (diagonal aberration) ---
        float magAlpha = aberrationAmount * 0.12f;
        if (magAlpha > 0.01f && bandWidth > 3f) {
            float cornerSize = bandWidth * 1.5f;

            // Top-right corner
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS / 2; step++) {
                float t = step / ((float)FRINGE_STEPS / 2);
                float x = w - cornerSize * (1f - t);
                float y = h - cornerSize * (1f - t);
                float a = magAlpha * (1f - t);

                buf.vertex(x, y, 0).color(1f, 0.2f, 0.8f, a).endVertex();
                buf.vertex(w, y, 0).color(1f, 0.2f, 0.8f, a * 0.5f).endVertex();
            }
            tess.end();

            // Bottom-left corner
            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int step = 0; step <= FRINGE_STEPS / 2; step++) {
                float t = step / ((float)FRINGE_STEPS / 2);
                float x = cornerSize * (1f - t);
                float y = cornerSize * (1f - t);
                float a = magAlpha * (1f - t);

                buf.vertex(0, y, 0).color(1f, 0.2f, 0.8f, a * 0.5f).endVertex();
                buf.vertex(x, y, 0).color(1f, 0.2f, 0.8f, a).endVertex();
            }
            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
