package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Breathing distortion — a slow, organic pulse that subtly darkens the screen in waves.
 * Simulates the feeling of being watched by something breathing nearby.
 * Renders as concentric gradient overlays that expand and contract slowly from center.
 */
public class BreathingDistortionRenderer {

    private static final int NUM_LAYERS = 4;

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_BREATHING_DISTORTION.get()) return;
        if (intensity < 0.15f) return;

        float strength = DarkStareConfig.BREATHING_DISTORTION_STRENGTH.get().floatValue();
        double now = System.currentTimeMillis() / 1000.0;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Breathing cycle: ~5s at low intensity, down to ~3s at high
        float breathPeriod = 5.0f - intensity * 2.0f;

        for (int layer = 0; layer < NUM_LAYERS; layer++) {
            // Each layer has its own phase offset — creates a wave-like propagation
            float layerOffset = layer * 1.2f;
            float layerPhase = (float)Math.sin((now + layerOffset) / breathPeriod * Math.PI * 2);
            float layerPulse = (layerPhase + 1f) / 2f; // 0..1

            // Each layer covers a different "distance" from center — outer layers are larger quads
            float scale = 0.3f + layer * 0.175f; // 0.3, 0.475, 0.65, 0.825
            int insetX = (int)(w * (1f - scale) / 2f);
            int insetY = (int)(h * (1f - scale) / 2f);

            // Alpha: strongest at medium-high intensity, modulated by the breathing pulse
            float distFactor = 1f - (layer / (float)NUM_LAYERS);
            float alpha = intensity * 0.04f * Math.min(strength, 3.0f) * layerPulse * distFactor;

            if (alpha < 0.002f) continue;

            // Color: dark purplish-black that shifts slightly with the breath
            float r = 0.12f + layerPulse * 0.03f;
            float g = 0.06f;
            float b = 0.14f + layerPulse * 0.02f;

            // Draw a centered quad with gradient alpha (corners darker than edges)
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(insetX, h - insetY, 0).color(r, g, b, alpha * 1.3f).endVertex();
            buf.vertex(w - insetX, h - insetY, 0).color(r, g, b, alpha * 1.2f).endVertex();
            buf.vertex(w - insetX, insetY, 0).color(r, g, b, alpha * 1.4f).endVertex();
            buf.vertex(insetX, insetY, 0).color(r, g, b, alpha).endVertex();
            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
