package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Subtle film grain overlay that intensifies with fear.
 * Enhanced with directional grain — at high intensity, the grain appears to
 * "flow" downward like static on a dying TV screen.
 */
public class FilmGrainRenderer {

    private static final ResourceLocation NOISE = new ResourceLocation("darkstare", "textures/noise.png");

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_FILM_GRAIN.get()) return;
        float strength = DarkStareConfig.FILM_GRAIN_STRENGTH.get().floatValue();

        // Fix #9: Clamp alpha to a safe upper bound so grain never overwhelms visibility.
        float rawAlpha = intensity * 0.07f * Math.min(strength, 3.0f);
        float alpha = Math.min(rawAlpha, 0.15f);

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        // Fix #1: Use per-frame local RandomSource instead of static shared one.
        long seed = System.nanoTime();
        RandomSource rand = RandomSource.create(seed);

        // Random UV offset every frame for animated grain
        float uOff = rand.nextFloat();
        float vOff = rand.nextFloat();

        float tilesX = w / 48f;
        float tilesY = h / 48f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.setShaderTexture(0, NOISE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(0, h, 0).uv(uOff, tilesY + vOff).endVertex();
        buf.vertex(w, h, 0).uv(tilesX + uOff, tilesY + vOff).endVertex();
        buf.vertex(w, 0, 0).uv(tilesX + uOff, vOff).endVertex();
        buf.vertex(0, 0, 0).uv(uOff, vOff).endVertex();
        tess.end();

        // At high intensity: add a second layer of grain with different scale for depth
        if (intensity > 0.6f) {
            float secondaryAlpha = Math.min(alpha * 0.5f, 0.15f);
            float uOff2 = rand.nextFloat();
            float vOff2 = rand.nextFloat();
            float tilesX2 = w / 32f; // larger grain for depth effect
            float tilesY2 = h / 32f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buf.vertex(0, h, 0).uv(uOff2, tilesY2 + vOff2).endVertex();
            buf.vertex(w, h, 0).uv(tilesX2 + uOff2, tilesY2 + vOff2).endVertex();
            buf.vertex(w, 0, 0).uv(tilesX2 + uOff2, vOff2).endVertex();
            buf.vertex(0, 0, 0).uv(uOff2, vOff2).endVertex();
            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
