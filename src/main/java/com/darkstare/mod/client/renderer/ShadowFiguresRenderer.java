package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;

/**
 * Shadow Figures — ghostly humanoid silhouettes that appear briefly in the periphery.
 * They flicker into existence, stand for a moment, then dissolve.
 * Each figure is drawn as a detailed dark silhouette with head, torso, arms, and legs.
 */
public class ShadowFiguresRenderer {

    private static final RandomSource RAND = RandomSource.create();

    /** Draw a vertical quad (rectangle) centered at (x, y) with given width/height. */
    private static void drawQuad(BufferBuilder buf, float x, float y, float w, float h,
                                 float r, float g, float b, float a) {
        buf.vertex(x - w / 2f, y + h / 2f, 0).color(r, g, b, a).endVertex();
        buf.vertex(x + w / 2f, y + h / 2f, 0).color(r, g, b, a).endVertex();
        buf.vertex(x + w / 2f, y - h / 2f, 0).color(r, g, b, a * 0.95f).endVertex();
        buf.vertex(x - w / 2f, y - h / 2f, 0).color(r, g, b, a * 0.95f).endVertex();
    }

    public static void render(float intensity, long gameTime, Window window) {
        if (!DarkStareConfig.ENABLE_SHADOW_FIGURES.get()) return;
        if (intensity < 0.4f) return;

        int maxFigures = DarkStareConfig.MAX_SHADOW_FIGURES.get();
        float baseChance = DarkStareConfig.SHADOW_FIGURE_CHANCE.get().floatValue();

        // Chance scales with intensity — rare below 0.6, more common at peak
        float figureChance = (intensity - 0.4f) / 0.6f * baseChance;
        if (figureChance < 0.01f) return;

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        long figSeed = gameTime / 80L;
        RAND.setSeed(figSeed);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        int drawn = 0;
        for (int i = 0; i < maxFigures * 4 && drawn < maxFigures; i++) {
            if (RAND.nextFloat() > figureChance) continue;

            long s = figSeed + (long)(i * 251);

            // Position in periphery — far from center, near edges
            float angle = (s % 6283) / 1000f; // 0 - 2PI
            float dist = 0.55f + (s % 40) / 200f; // 0.55-0.75 of screen radius
            float cx = w / 2f;
            float cy = h / 2f;
            float fx = cx + (float)(Math.cos(angle) * dist * w);
            float fy = cy + (float)(Math.sin(angle) * dist * h * 0.65f);

            // Avoid center area
            if (Math.abs(fx - cx) < w * 0.18f && Math.abs(fy - cy) < h * 0.18f) continue;

            // Fade lifecycle: appear -> hold briefly -> dissolve
            float life = (float)((gameTime + i * 71) % 350) / 350f;
            float fade = (float)Math.sin(life * Math.PI);
            if (fade < 0.12f) continue;

            // Figure dimensions — tall, thin humanoid shape
            float figHeight = h * (0.14f + (s % 30) / 1000f);
            float figWidth = figHeight * 0.28f;

            // Alpha: very subtle, barely perceptible at best
            float alpha = fade * intensity * 0.1f;
            if (alpha < 0.003f) continue;

            // Slight flicker — figures aren't stable
            double flickerPhase = gameTime * 0.15 + i * 4.7;
            float flicker = 0.6f + 0.4f * (float)Math.max(0, Math.sin(flickerPhase));

            // Slight sway — figures gently rock back and forth
            double swayPhase = gameTime * 0.03 + i * 2.1;
            float swayAngle = (float)(Math.sin(swayPhase) * 0.05f);
            float swayX = (float)(Math.sin(swayAngle) * figHeight * 0.1);

            // Color: very dark with slight purple tint — not pure black for more eerie feel
            float r = 0.04f + fade * 0.02f;
            float g = 0.02f;
            float b = 0.06f + fade * 0.03f;

            // === HEAD (oval-ish shape) ===
            float headSize = figWidth * 1.1f;
            float headY = fy - figHeight * 0.45f + swayX * 0.8f;
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            drawQuad(buf, fx + swayX * 0.8f, headY, headSize, headSize * 1.2f, r, g, b, alpha * flicker * 0.95f);

            // === NECK (thin connection) ===
            float neckWidth = figWidth * 0.35f;
            drawQuad(buf, fx + swayX * 0.78f, headY + headSize * 0.6f, neckWidth, figHeight * 0.04f, r, g, b, alpha * flicker * 0.85f);

            // === TORSO (wider at shoulders, tapering to waist) — draw as two stacked quads ===
            float shoulderWidth = figWidth * 1.6f;
            float waistWidth = figWidth * 0.9f;
            float torsoTop = fy - figHeight * 0.32f + swayX * 0.7f;
            float midTorsoY = fy - figHeight * 0.15f + swayX * 0.55f;
            float torsoBottom = fy + figHeight * 0.05f + swayX * 0.4f;

            // Upper torso (shoulders)
            drawQuad(buf, fx + swayX * 0.7f, torsoTop + (midTorsoY - torsoTop) / 2f,
                     shoulderWidth, midTorsoY - torsoTop, r, g, b, alpha * flicker);

            // Lower torso (waist)
            drawQuad(buf, fx + swayX * 0.55f, midTorsoY + (torsoBottom - midTorsoY) / 2f,
                     waistWidth, torsoBottom - midTorsoY, r, g, b, alpha * flicker * 0.95f);

            // === LEFT ARM (hanging down from shoulder) ===
            float armLength = figHeight * 0.32f;
            float armWidth = figWidth * 0.28f;
            float leftArmX = fx - shoulderWidth / 2f + swayX * 0.7f;

            drawQuad(buf, leftArmX, torsoTop + armLength / 2f,
                     armWidth, armLength, r, g, b, alpha * flicker * 0.8f);

            // === RIGHT ARM (hanging down from shoulder) ===
            float rightArmX = fx + shoulderWidth / 2f + swayX * 0.7f;

            drawQuad(buf, rightArmX, torsoTop + armLength / 2f,
                     armWidth, armLength, r, g, b, alpha * flicker * 0.8f);

            // === LEFT LEG (from waist down) ===
            float legLength = figHeight * 0.42f;
            float legWidth = figWidth * 0.38f;
            float leftLegX = fx - waistWidth / 4f + swayX * 0.3f;

            drawQuad(buf, leftLegX, torsoBottom + legLength / 2f,
                     legWidth, legLength, r, g, b, alpha * flicker * 0.9f);

            // === RIGHT LEG (from waist down) ===
            float rightLegX = fx + waistWidth / 4f + swayX * 0.3f;

            drawQuad(buf, rightLegX, torsoBottom + legLength / 2f,
                     legWidth, legLength, r, g, b, alpha * flicker * 0.9f);

            tess.end();
            drawn++;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
