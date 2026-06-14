package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;

/**
 * Peripheral Presence Effect — at extreme intensity, subtle dark shapes appear in the
 * periphery of vision and vanish when you try to look directly at them. Creates the feeling
 * that something is watching from just outside your field of view. The presence manifests as
 * faint silhouettes, sudden shadows, and brief glimpses of movement at screen edges.
 */
public class PeripheralPresenceRenderer {

    private static final int MAX_PRESENCES = 6;
    private static final long PRESENCE_UPDATE_INTERVAL = 5000L; // logical interval in ticks/scaled time

    public static void render(float intensity, long gameTime, Window window) {
        if (!DarkStareConfig.ENABLE_PERIPHERAL_PRESENCE.get()) return;
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

        // Fix #3, #10: Deterministic per-frame state derived from gameTime (no static mutable arrays).
        long updateCycle = gameTime / PRESENCE_UPDATE_INTERVAL;
        RandomSource rand = RandomSource.create(updateCycle * 31337L);

        float[] presenceX = new float[MAX_PRESENCES];
        float[] presenceY = new float[MAX_PRESENCES];
        float[] presenceSize = new float[MAX_PRESENCES];
        boolean[] presenceActive = new boolean[MAX_PRESENCES];

        for (int i = 0; i < MAX_PRESENCES; i++) {
            // Move presences slowly toward screen edges (periphery) using deterministic RNG.
            float edgeBias = 0.85f + rand.nextFloat() * 0.15f;
            presenceX[i] = rand.nextFloat() * w;
            presenceY[i] = rand.nextFloat() * h;

            // Bias toward edges — presences appear in periphery, not center
            if (presenceX[i] < w * edgeBias && presenceX[i] > w * (1f - edgeBias)) {
                presenceX[i] = rand.nextFloat() > 0.5f
                    ? rand.nextFloat() * w * (1f - edgeBias)
                    : (float)(w * edgeBias + rand.nextFloat() * w * edgeBias);
            }
            if (presenceY[i] < h * edgeBias && presenceY[i] > h * (1f - edgeBias)) {
                presenceY[i] = rand.nextFloat() > 0.5f
                    ? rand.nextFloat() * h * (1f - edgeBias)
                    : (float)(h * edgeBias + rand.nextFloat() * h * edgeBias);
            }

            // Randomly activate/deactivate presences deterministically
            presenceActive[i] = rand.nextFloat() < 0.3f * intensity;
            presenceSize[i] = 30f + rand.nextFloat() * 80f;
        }

        float presenceAlpha = (intensity - 0.75f) * 0.8f;
        if (presenceAlpha < 0.01f) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            return;
        }

        // Draw each active presence as a dark, humanoid silhouette
        for (int i = 0; i < MAX_PRESENCES; i++) {
            if (!presenceActive[i]) continue;

            // Flickering visibility — presences appear and disappear rapidly
            double flickerPhase = (gameTime / 1000.0) * 4f + i * 2.3f;
            float flickerAlpha = (float)Math.max(0, Math.sin(flickerPhase) * presenceAlpha);

            if (flickerAlpha < 0.02f) continue;

            // Slow drift — presences move imperceptibly slowly (using gameTime instead of system time)
            float driftX = (float)(Math.sin((gameTime / 1000.0) * 0.1f + i * 1.7f) * 5f);
            float driftY = (float)(Math.cos((gameTime / 1000.0) * 0.08f + i * 2.1f) * 3f);

            float px = presenceX[i] + driftX;
            float py = presenceY[i] + driftY;
            float size = presenceSize[i];

            // Draw a humanoid silhouette — tall, thin shape with slight head bulge
            drawHumanoidSilhouette(buf, tess, px, py, size, flickerAlpha);

            // Draw faint red glow around the presence (like eyes in darkness)
            if (intensity > 0.9f && rand.nextFloat() < 0.3f) {
                float eyeGlow = (intensity - 0.9f) * 2f;
                drawEyes(buf, tess, px, py - size * 0.35f, size * 0.15f, eyeGlow);
            }
        }

        // Draw sudden shadow flashes — brief dark patches that appear and vanish instantly
        if (intensity > 0.85f && rand.nextFloat() < 0.02f) {
            float flashX = rand.nextFloat() * w;
            float flashY = rand.nextFloat() * h;
            float flashSize = 100f + rand.nextFloat() * 200f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float r = 0.01f;
            float g = 0.01f;
            float b = 0.02f;
            float flashAlpha = presenceAlpha * 0.5f;

            buf.vertex(flashX - flashSize, flashY - flashSize / 2f, 0).color(r, g, b, flashAlpha).endVertex();
            buf.vertex(flashX + flashSize, flashY - flashSize / 2f, 0).color(r, g, b, flashAlpha * 1.1f).endVertex();
            buf.vertex(flashX + flashSize, flashY + flashSize / 2f, 0).color(r, g, b, flashAlpha * 0.9f).endVertex();
            buf.vertex(flashX - flashSize, flashY + flashSize / 2f, 0).color(r, g, b, flashAlpha * 1.05f).endVertex();

            tess.end();
        }

        // Draw peripheral movement — brief streaks of darkness at screen edges
        if (intensity > 0.8f && rand.nextFloat() < 0.03f) {
            float streakX = rand.nextFloat() > 0.5f ? 0f : w;
            float streakY = rand.nextFloat() * h;
            float streakLength = 50f + rand.nextFloat() * 150f;

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float r = 0.02f;
            float g = 0.015f;
            float b = 0.03f;
            float streakAlpha = presenceAlpha * 0.4f;

            // Vertical streak from edge toward center
            buf.vertex(streakX, streakY - streakLength / 2f, 0).color(r, g, b, streakAlpha).endVertex();
            buf.vertex(streakX + (streakX == 0 ? 15f : -15f), streakY - streakLength / 2f, 0)
                .color(r, g, b, streakAlpha * 1.1f).endVertex();
            buf.vertex(streakX + (streakX == 0 ? 15f : -15f), streakY + streakLength / 2f, 0)
                .color(r, g, b, streakAlpha * 0.9f).endVertex();
            buf.vertex(streakX, streakY + streakLength / 2f, 0)
                .color(r, g, b, streakAlpha * 1.05f).endVertex();

            tess.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void drawHumanoidSilhouette(BufferBuilder buf, Tesselator tess, float x, float y, float size, float alpha) {
        // Draw as a tall, thin dark shape — body + head
        int segments = 8;

        // Body — elongated oval
        for (int s = 0; s < segments; s++) {
            double angle1 = (s / (double)segments) * Math.PI * 2f;
            double angle2 = ((s + 1) / (double)segments) * Math.PI * 2f;

            // Body is taller than wide — aspect ratio ~3:1
            float bodyWidth = size * 0.25f;
            float bodyHeight = size * 0.7f;

            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            float r = 0.01f;
            float g = 0.01f;
            float b = 0.02f;

            // Center of body is slightly below the given position
            float bodyCX = x;
            float bodyCY = y + size * 0.15f;

            buf.vertex(bodyCX, bodyCY, 0).color(r, g, b, alpha * 1.2f).endVertex();
            buf.vertex(
                bodyCX + (float)Math.cos(angle1) * bodyWidth,
                bodyCY + (float)Math.sin(angle1) * bodyHeight,
                0
            ).color(r, g, b, alpha).endVertex();
            buf.vertex(
                bodyCX + (float)Math.cos(angle2) * bodyWidth,
                bodyCY + (float)Math.sin(angle2) * bodyHeight,
                0
            ).color(r, g, b, alpha * 0.9f).endVertex();

            tess.end();
        }

        // Head — smaller circle above the body
        float headSize = size * 0.15f;
        float headY = y - size * 0.35f;

        for (int s = 0; s < segments; s++) {
            double angle1 = (s / (double)segments) * Math.PI * 2f;
            double angle2 = ((s + 1) / (double)segments) * Math.PI * 2f;

            buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            float r = 0.015f;
            float g = 0.01f;
            float b = 0.02f;

            buf.vertex(x, headY, 0).color(r, g, b, alpha * 1.3f).endVertex();
            buf.vertex(
                x + (float)Math.cos(angle1) * headSize,
                headY + (float)Math.sin(angle1) * headSize,
                0
            ).color(r, g, b, alpha * 1.1f).endVertex();
            buf.vertex(
                x + (float)Math.cos(angle2) * headSize,
                headY + (float)Math.sin(angle2) * headSize,
                0
            ).color(r, g, b, alpha).endVertex();

            tess.end();
        }
    }

    // Deterministic pseudo-random for eye glow to avoid frame-to-frame flicker.
    private static void drawEyes(BufferBuilder buf, Tesselator tess, float x, float y, float size, float alpha) {
        float eyeSpacing = size * 1.5f;

        // Use a stable per-eye pseudo-random derived from position and size so glow is consistent frame-to-frame.
        long eyeSeed = (long)(x * 7919 + y * 6271 + size * 3571);

        for (int eIdx = -1; eIdx <= 1; eIdx += 2) {
            float ex = x + eIdx * eyeSpacing / 2f;

            long seed = eyeSeed ^ (long)(eIdx * 268435459L);
            float r = 0.4f + (float)((seed & 0x3FFFFF) / (double)0x3FFFFF * 0.3f);
            float g = 0.05f;
            float b = 0.02f;

            int segments = 6;

            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

            buf.vertex(ex, y, 0).color(r, g, b, alpha * 1.5f).endVertex();

            for (int s = 0; s <= segments; s++) {
                double angle = (s / (double)segments) * Math.PI * 2f;
                float ex2 = ex + (float)Math.cos(angle) * size;
                float ey2 = y + (float)Math.sin(angle) * size;

                buf.vertex(ex2, ey2, 0).color(r * 0.9f, g * 0.5f, b * 0.3f, alpha * 0.8f).endVertex();
            }

            tess.end();
        }
    }
}
