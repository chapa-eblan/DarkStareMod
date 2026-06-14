package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import com.darkstare.mod.config.DarkStareConfig;
/**
 * Screen Tear / Glitch Effect — at extreme intensity, the screen appears to tear and glitch,
 * as if reality is breaking apart. Creates horizontal displacement bands with color artifacts
 * that flash briefly and disappear.
 */
public class ScreenTearRenderer {

    private static final int MAX_TEARS = 8;
    private static long lastGlitchTime = 0;
    private static final long GLITCH_COOLDOWN = 200L; // minimum ms between glitch bursts

    public static void render(float intensity, long gameTime, Window window) {
        if (!DarkStareConfig.ENABLE_SCREEN_TEAR.get()) return;
        if (intensity < 0.75f) return;

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

        // Glitch burst — sudden intense flash of horizontal displacement bands
        if (intensity > 0.85f && System.currentTimeMillis() - lastGlitchTime > GLITCH_COOLDOWN) {
            if (Math.random() < 0.02f * intensity) {
                lastGlitchTime = System.currentTimeMillis();

                // Draw a burst of thick horizontal bands with color artifacts
                for (int i = 0; i < MAX_TEARS; i++) {
                    float yPos = (float)(Math.random() * h);
                    float bandHeight = 1f + (float)Math.random() * 4f;
                    float offset = (float)((Math.random() - 0.5) * 20f * intensity);

                    // Color artifact — red or blue shift depending on the tear
                    boolean isRedShift = Math.random() > 0.5f;
                    float r, g, b;
                    if (isRedShift) {
                        r = 0.15f + (float)Math.random() * 0.1f;
                        g = 0.02f;
                        b = 0.01f;
                    } else {
                        r = 0.01f;
                        g = 0.02f;
                        b = 0.15f + (float)Math.random() * 0.1f;
                    }

                    float alpha = 0.3f + (float)Math.random() * 0.4f;

                    buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                    // Offset the band horizontally to simulate displacement
                    buf.vertex(offset, yPos - bandHeight / 2f, 0).color(r, g, b, alpha).endVertex();
                    buf.vertex(w + offset, yPos - bandHeight / 2f, 0).color(r, g, b, alpha * 1.1f).endVertex();
                    buf.vertex(w + offset, yPos + bandHeight / 2f, 0).color(r, g, b, alpha * 0.9f).endVertex();
                    buf.vertex(offset, yPos + bandHeight / 2f, 0).color(r, g, b, alpha * 1.05f).endVertex();

                    tess.end();
                }
            }
        }

        // Continuous subtle tearing — thin horizontal lines that appear and disappear rapidly
        float tearAlpha = (intensity - 0.75f) * 0.08f;
        if (tearAlpha < 0.01f) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            return;
        }

        for (int i = 0; i < 4; i++) {
            double flickerPhase = now * 8f + i * 3.7;
            float flickerAlpha = (float)Math.max(0, Math.sin(flickerPhase) * tearAlpha);

            if (flickerAlpha < 0.005f) continue;

            float yPos = (float)(Math.sin(now * 2f + i * 1.3) * h * 0.4f + h / 2f);
            float bandHeight = 0.5f + (float)Math.random() * 1.5f;

            // Subtle color shift — mostly dark with slight red/blue tint
            float r = 0.05f + (i % 3) * 0.02f;
            float g = 0.01f;
            float b = 0.04f + (i % 2) * 0.03f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // Slight horizontal displacement to simulate tear
            float offset = (float)((Math.sin(now * 5f + i * 2.1) * 5f));

            buf.vertex(offset, yPos - bandHeight / 2f, 0).color(r, g, b, flickerAlpha).endVertex();
            buf.vertex(w + offset, yPos - bandHeight / 2f, 0).color(r, g, b, flickerAlpha * 1.2f).endVertex();
            buf.vertex(w + offset, yPos + bandHeight / 2f, 0).color(r, g, b, flickerAlpha * 0.8f).endVertex();
            buf.vertex(offset, yPos + bandHeight / 2f, 0).color(r, g, b, flickerAlpha * 1.1f).endVertex();

            tess.end();
        }

        // Vertical scan lines — very thin horizontal lines that scroll down the screen
        for (int i = 0; i < 3; i++) {
            double scanPhase = now * 3f + i * 2.5;
            float yPos = (float)((Math.sin(scanPhase) * h * 0.5f + h / 2f));

            // Scan lines are very thin and barely visible — just enough to feel wrong
            float scanAlpha = tearAlpha * 0.3f;
            if (scanAlpha < 0.003f) continue;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // White-ish scan line with slight color variation
            float brightness = 0.15f + i * 0.05f;
            buf.vertex(0, yPos - 0.25f, 0).color(brightness, brightness * 0.9f, brightness * 1.1f, scanAlpha).endVertex();
            buf.vertex(w, yPos - 0.25f, 0).color(brightness, brightness * 0.9f, brightness * 1.1f, scanAlpha).endVertex();
            buf.vertex(w, yPos + 0.25f, 0).color(brightness, brightness * 0.9f, brightness * 1.1f, scanAlpha).endVertex();
            buf.vertex(0, yPos + 0.25f, 0).color(brightness, brightness * 0.9f, brightness * 1.1f, scanAlpha).endVertex();

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
