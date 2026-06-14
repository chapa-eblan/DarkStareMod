package com.darkstare.mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.darkstare.mod.config.DarkStareConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Afterimage Effect — ghostly visual echoes that linger on the screen, as if your eyes
 * are retaining impressions of what was seen in darkness. Creates faint, distorted shapes
 * that slowly fade and shift position, like looking at a bright light then closing your eyes.
 * The afterimages take on an eerie quality — they're not quite what you saw, but something
 * darker and more unsettling.
 */
public class AfterimageRenderer {

    private static final int MAX_AFTERIMAGES = 8;
    private static float[] afterimageX = new float[MAX_AFTERIMAGES];
    private static float[] afterimageY = new float[MAX_AFTERIMAGES];
    private static float[] afterimageAlpha = new float[MAX_AFTERIMAGES];
    private static float[] afterimageSize = new float[MAX_AFTERIMAGES];
    private static long[] afterimageBirthTime = new long[MAX_AFTERIMAGES];

    public static void render(float intensity, Window window) {
        if (!DarkStareConfig.ENABLE_AFTERIMAGE.get()) return;
        if (intensity < 0.5f) return;

        float strength = DarkStareConfig.AFTERIMAGE_STRENGTH.get().floatValue();

        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        long now = System.currentTimeMillis();

        // Spawn new afterimages based on intensity — higher intensity = more frequent spawns
        float spawnChance = (intensity - 0.5f) / 0.5f * 0.03f * Math.min(strength, 3.0f);

        for (int i = 0; i < MAX_AFTERIMAGES; i++) {
            // Fade existing afterimages over time
            if (afterimageAlpha[i] > 0) {
                long age = now - afterimageBirthTime[i];
                float fadeRate = 0.001f + intensity * 0.002f; // faster fade at high intensity
                afterimageAlpha[i] -= fadeRate;

                // Slow drift — afterimages shift position slightly over time
                afterimageX[i] += (float)(Math.sin(now * 0.001f + i) * 0.3f);
                afterimageY[i] += (float)(Math.cos(now * 0.0008f + i * 2f) * 0.2f);

                if (afterimageAlpha[i] < 0) {
                    afterimageAlpha[i] = 0;
                }
            }

            // Spawn new afterimage in empty slot
            if (afterimageAlpha[i] <= 0 && Math.random() < spawnChance) {
                afterimageX[i] = (float)(Math.random() * w);
                afterimageY[i] = (float)(Math.random() * h);
                afterimageSize[i] = 20f + (float)(Math.random() * 60f);
                afterimageAlpha[i] = 0.05f + intensity * 0.1f;
                afterimageBirthTime[i] = now;
            }
        }

        // Check if there's anything to render
        boolean hasVisibleAfterimages = false;
        for (int i = 0; i < MAX_AFTERIMAGES; i++) {
            if (afterimageAlpha[i] > 0.001f) {
                hasVisibleAfterimages = true;
                break;
            }
        }

        if (!hasVisibleAfterimages) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        for (int i = 0; i < MAX_AFTERIMAGES; i++) {
            if (afterimageAlpha[i] <= 0.001f) continue;

            float alpha = afterimageAlpha[i];
            float size = afterimageSize[i];

            // Afterimages have a sickly color — dark with slight red/purple tint
            float r = 0.15f + (float)(Math.random() * 0.1f);
            float g = 0.05f;
            float b = 0.12f + (float)(Math.random() * 0.08f);

            // Draw as a soft, distorted circle using concentric rings
            int rings = 4;
            for (int ring = rings - 1; ring >= 0; ring--) {
                float ringRadius = size * ((ring + 1) / (float)rings);
                float ringAlpha = alpha * ((rings - ring) / (float)rings) * 0.5f;

                if (ringAlpha < 0.002f) continue;

                int segments = 8;
                for (int s = 0; s < segments; s++) {
                    double angle1 = (s / (double)segments) * Math.PI * 2f;
                    double angle2 = ((s + 1) / (double)segments) * Math.PI * 2f;

                    // Add distortion — afterimages are warped, not perfect circles
                    float distort1 = 0.85f + (float)(Math.sin(now * 0.003f + s * 0.7f + i * 3f) * 0.15f);
                    float distort2 = 0.85f + (float)(Math.cos(now * 0.0025f + s * 0.5f + i * 2f) * 0.15f);

                    buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                    buf.vertex(afterimageX[i], afterimageY[i], 0).color(r, g, b, ringAlpha * 1.3f).endVertex();
                    buf.vertex(
                        afterimageX[i] + (float)(Math.cos(angle1) * ringRadius * distort1),
                        afterimageY[i] + (float)(Math.sin(angle1) * ringRadius * distort1),
                        0
                    ).color(r, g, b, ringAlpha).endVertex();
                    buf.vertex(
                        afterimageX[i] + (float)(Math.cos(angle2) * ringRadius * distort2),
                        afterimageY[i] + (float)(Math.sin(angle2) * ringRadius * distort2),
                        0
                    ).color(r, g, b, ringAlpha * 0.9f).endVertex();

                    tess.end();
                }
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    public static void reset() {
        for (int i = 0; i < MAX_AFTERIMAGES; i++) {
            afterimageAlpha[i] = 0;
        }
    }
}
