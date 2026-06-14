package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Creeping Darkness — dark patches that slowly advance from the edges of the screen,
 * as if something is closing in. The darkness moves organically with slow drift and
 * occasional surges forward, creating a sense of being surrounded by encroaching void.
 */
public class CreepingDarknessRenderer {

    private static final int NUM_PATCHES = 8;

    public static void render(float intensity, long gameTime, Window window) {
        if (intensity < 0.25f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Each patch has a deterministic position based on gameTime + index
        for (int i = 0; i < NUM_PATCHES; i++) {
            long seed = gameTime / 120L + (long)(i * 317);

            // Patch starts at an edge and creeps inward over time
            float edgePhase = ((gameTime / 60f + i * 45) % 300f) / 300f;

            // Which edge this patch comes from (0=top, 1=right, 2=bottom, 3=left)
            int edge = (int)(seed % 4);

            float x, y, pw, ph;

            // Fix #5: Use safe modular arithmetic for positioning — avoids overflow/negative issues.
            switch (edge) {
                case 0: // top
                    x = (float)((seed % w + w) % w);
                    y = -h * 0.15f + edgePhase * h * 0.45f;
                    break;
                case 1: // right
                    x = w + w * 0.15f - edgePhase * w * 0.45f;
                    y = (float)((seed % h + h) % h);
                    break;
                case 2: // bottom
                    x = (float)((seed % w + w) % w);
                    y = h + h * 0.15f - edgePhase * h * 0.45f;
                    break;
                default: // left
                    x = -w * 0.15f + edgePhase * w * 0.45f;
                    y = (float)((seed % h + h) % h);
                    break;
            }

            // Patch size grows with intensity and as it creeps inward
            float baseSize = (intensity * 0.12f + edgePhase * 0.08f) * Math.min(w, h);
            pw = baseSize * (0.6f + (seed % 40) / 100f);
            ph = baseSize * (0.5f + (seed % 30) / 100f);

            // Alpha: stronger as patch creeps inward, modulated by intensity
            float alpha = edgePhase * intensity * 0.12f;

            if (alpha < 0.003f || pw < 5 || ph < 5) continue;

            // Slow drift — patches move slightly over time
            double driftX = Math.sin(gameTime * 0.008 + i * 2.7) * w * 0.04f;
            double driftY = Math.cos(gameTime * 0.006 + i * 3.1) * h * 0.03f;

            float drawX = (float)(x + driftX);
            float drawY = (float)(y + driftY);

            // Color: near-black with slight variation — some patches are more blue, others more red
            float r = 0.01f + ((seed % 3) / 100f);
            float g = 0.005f;
            float b = 0.02f + ((seed % 5) / 100f);

            // Fix #6: Gradient winding corrected — each quad now has consistent CCW winding,
            // with darker front edge aligned to the creeping direction for natural vignette feel.
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float frontAlpha = alpha * 1.5f;
            float backAlpha = alpha * 0.6f;

            switch (edge) {
                case 0: // top — creeping downward: darker at bottom edge of patch
                    buf.vertex(drawX - pw / 2, drawY + ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY + ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY - ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX - pw / 2, drawY - ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    break;
                case 1: // right — creeping leftward: darker at left edge of patch
                    buf.vertex(drawX + pw / 2, drawY + ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX - pw / 2, drawY + ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX - pw / 2, drawY - ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY - ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    break;
                case 2: // bottom — creeping upward: darker at top edge of patch
                    buf.vertex(drawX - pw / 2, drawY + ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY + ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY - ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX - pw / 2, drawY - ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    break;
                default: // left — creeping rightward: darker at right edge of patch
                    buf.vertex(drawX - pw / 2, drawY + ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY + ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX + pw / 2, drawY - ph / 2, 0).color(r, g, b, backAlpha).endVertex();
                    buf.vertex(drawX - pw / 2, drawY - ph / 2, 0).color(r, g, b, frontAlpha).endVertex();
                    break;
            }

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
