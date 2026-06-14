package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a textured circular vignette that pulses with heartbeat.
 * Enhanced with color shift (darkness takes on a sickly hue at high intensity)
 * and dynamic pulse that syncs with the breathing rhythm.
 */
public class VignetteRenderer {

    private static final ResourceLocation VIGNETTE = new ResourceLocation("darkstare", "textures/vignette.png");

    public static void renderVignette(float intensity, Window window, float shake, float heartbeat) {
        if (intensity <= 0.01f || !DarkStareConfig.ENABLE_VIGNETTE.get()) return;

        float maxAlpha = DarkStareConfig.MAX_VIGNETTE_ALPHA.get().floatValue();
        // Vignette pulses with heartbeat — stronger at high intensity
        float pulseFactor = 1.0f + heartbeat * (0.25f + intensity * 0.3f);
        float alpha = intensity * maxAlpha * pulseFactor;
        if (alpha < 0.005f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        // Smooth, intensity-limited jitter offset
        float effectiveShake = Math.min(shake * intensity, 1.8f);
        float time = (float)(System.nanoTime() / 1_000_000_000.0);
        float sx = (float)(Math.sin(time * 0.7) * effectiveShake * w * 0.012);
        float sy = (float)(Math.cos(time * 0.9) * effectiveShake * h * 0.012);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        // Color shift: at high intensity, vignette takes on a sickly dark-red/purple hue
        float colorShift = Math.max(0f, (intensity - 0.5f) / 0.5f); // 0..1 above 50% intensity
        float r = 1.0f - colorShift * 0.3f;   // less red at high intensity (darker)
        float g = 1.0f - colorShift * 0.6f;   // much less green (sickly tone)
        float b = 1.0f - colorShift * 0.25f;  // slightly less blue

        RenderSystem.setShaderColor(r, g, b, Math.min(alpha, 1.0f));
        RenderSystem.setShaderTexture(0, VIGNETTE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Overscan to cover edges during shake
        float margin = 0.15f;
        float x1 = -w * margin + sx;
        float y1 = -h * margin + sy;
        float x2 = w * (1f + margin) + sx;
        float y2 = h * (1f + margin) + sy;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(x1, y2, 0).uv(0, 1).endVertex();
        buf.vertex(x2, y2, 0).uv(1, 1).endVertex();
        buf.vertex(x2, y1, 0).uv(1, 0).endVertex();
        buf.vertex(x1, y1, 0).uv(0, 0).endVertex();
        tess.end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
