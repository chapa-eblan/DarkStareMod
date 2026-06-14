package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Near-blackout overlay at maximum intensity.
 * Enhanced with gradual fade pattern — the darkness creeps in from the edges,
 * like being swallowed by something alive. At peak intensity it pulses slightly.
 */
public class ScreenDarkeningRenderer {

    public static void render(float intensity, Window window) {
        if (intensity < 0.8f) return;

        // Base alpha grows from 0.8 to 1.0 intensity
        float baseAlpha = (intensity - 0.8f) / 0.2f * 0.45f;
        if (baseAlpha < 0.001f) return;

        // Pulsing at very high intensity — the darkness breathes
        double now = System.currentTimeMillis();
        float pulse = (float)Math.sin(now * 0.004);
        float alpha = Math.max(0, baseAlpha + pulse * 0.03f);

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Darkness is stronger at edges, slightly weaker in center (tunnel effect)
        float edgeAlpha = alpha * 1.3f;
        float centerAlpha = alpha * 0.7f;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(0, h, 0).color(0.02f, 0.01f, 0.03f, edgeAlpha).endVertex();
        buf.vertex(w, h, 0).color(0.02f, 0.01f, 0.03f, edgeAlpha * 1.1f).endVertex();
        buf.vertex(w, 0, 0).color(0.01f, 0.01f, 0.02f, centerAlpha).endVertex();
        buf.vertex(0, 0, 0).color(0.01f, 0.01f, 0.02f, edgeAlpha * 0.9f).endVertex();
        tess.end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
