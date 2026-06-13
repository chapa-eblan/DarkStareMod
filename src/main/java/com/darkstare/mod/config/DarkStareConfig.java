package com.darkstare.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**
 * Forge config for DarkStare - fully tunable horror effects.
 */
public class DarkStareConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // General
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    // Darkness Detection
    public static final ForgeConfigSpec.IntValue RAY_LENGTH;
    public static final ForgeConfigSpec.IntValue MIN_CONSECUTIVE_DARK_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_LIGHT_LEVEL;

    // Intensity Curve
    public static final ForgeConfigSpec.DoubleValue START_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FULL_INTENSITY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FADE_SPEED;

    // FOV Zoom
    public static final ForgeConfigSpec.BooleanValue ENABLE_FOV_ZOOM;
    public static final ForgeConfigSpec.DoubleValue MAX_FOV_REDUCTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JITTER;

    // Eyes Overlay
    public static final ForgeConfigSpec.BooleanValue ENABLE_EYES_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue EYE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MAX_ALPHA;
    public static final ForgeConfigSpec.IntValue MAX_EYE_PAIRS;

    // Sound Effects
    public static final ForgeConfigSpec.BooleanValue ENABLE_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue HUM_VOLUME_SCALE;
    public static final ForgeConfigSpec.DoubleValue WHISPER_VOLUME_SCALE;
    public static final ForgeConfigSpec.DoubleValue SPIKE_CHANCE_AT_FULL;

    // Vignette / Atmosphere
    public static final ForgeConfigSpec.BooleanValue ENABLE_VIGNETTE;
    public static final ForgeConfigSpec.DoubleValue MAX_VIGNETTE_ALPHA;

    private static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("DarkStare - A subtle horror mod for staring into darkness")
               .push("general");

        ENABLED = BUILDER
                .comment("Enable all DarkStare effects.")
                .define("enabled", true);

        BUILDER.pop();

        // Darkness detection
        BUILDER.comment("Settings for detecting dark passages/tunnels").push("darkness_detection");

        RAY_LENGTH = BUILDER
                .comment("Max distance (blocks) to raycast into darkness. Default 32.")
                .defineInRange("rayLength", 32, 4, 64);

        MIN_CONSECUTIVE_DARK_BLOCKS = BUILDER
                .comment("Minimum consecutive dark samples along the ray before effects trigger.")
                .defineInRange("minConsecutiveDarkBlocks", 3, 1, 10);

        MAX_LIGHT_LEVEL = BUILDER
                .comment("Max light level (0-15) to consider a block 'dark'. " +
                         "0 = pitch black only; higher = dim areas count too.")
                .defineInRange("maxLightLevel", 2, 0, 7);

        BUILDER.pop();

        // Intensity curve
        BUILDER.comment("How fast effects build up and fade").push("intensity");

        START_SECONDS = BUILDER
                .comment("Seconds staring before any effect begins.")
                .defineInRange("startSeconds", 3.0, 1.0, 20.0);

        FULL_INTENSITY_SECONDS = BUILDER
                .comment("Seconds staring to reach maximum intensity.")
                .defineInRange("fullIntensitySeconds", 18.0, 5.0, 60.0);

        FADE_SPEED = BUILDER
                .comment("How quickly effects fade when you look away (higher = faster).")
                .defineInRange("fadeSpeed", 3.5, 1.0, 10.0);

        BUILDER.pop();

        // FOV zoom
        BUILDER.comment("FOV zoom effect when staring into darkness").push("fov_zoom");

        ENABLE_FOV_ZOOM = BUILDER
                .comment("Enable the subtle FOV reduction (zoom-in) effect.")
                .define("enabled", true);

        MAX_FOV_REDUCTION = BUILDER
                .comment("Maximum FOV reduction as a multiplier. 0.74 = zoom in ~26% at full intensity.")
                .defineInRange("maxFovReduction", 0.74, 0.5, 1.0);

        ENABLE_JITTER = BUILDER
                .comment("Enable tiny micro-shake FOV jitter for unease at high intensity.")
                .define("enableJitter", true);

        BUILDER.pop();

        // Eyes overlay
        BUILDER.comment("Faint eyes that appear in the darkness").push("eyes_overlay");

        ENABLE_EYES_OVERLAY = BUILDER
                .comment("Enable rendering of faint eyes in dark areas.")
                .define("enabled", true);

        EYE_BASE_CHANCE = BUILDER
                .comment("Base chance per render call to show an eye (scaled by intensity).")
                .defineInRange("baseChance", 0.025, 0.001, 0.1);

        MAX_ALPHA = BUILDER
                .comment("Maximum alpha of eyes effect.")
                .defineInRange("maxAlpha", 0.14, 0.02, 0.5);

        MAX_EYE_PAIRS = BUILDER
                .comment("Max number of eye pairs visible on screen at once (for atmosphere).")
                .defineInRange("maxEyePairs", 2, 0, 6);

        BUILDER.pop();

        // Sounds
        BUILDER.comment("Eerie sound effects").push("sounds");

        ENABLE_SOUNDS = BUILDER
                .comment("Enable atmospheric sounds when staring into darkness.")
                .define("enabled", true);

        HUM_VOLUME_SCALE = BUILDER
                .comment("Volume scale for the high-frequency hum (ambient.end).")
                .defineInRange("humVolumeScale", 0.8, 0.0, 2.0);

        WHISPER_VOLUME_SCALE = BUILDER
                .comment("Volume scale for whisper-like cave sounds.")
                .defineInRange("whisperVolumeScale", 0.5, 0.0, 1.5);

        SPIKE_CHANCE_AT_FULL = BUILDER
                .comment("Chance per tick of an eerie spike at full intensity.")
                .defineInRange("spikeChanceAtFull", 0.006, 0.0, 0.05);

        BUILDER.pop();

        // Vignette
        BUILDER.comment("Dark vignette effect around screen edges").push("vignette");

        ENABLE_VIGNETTE = BUILDER
                .comment("Enable a subtle dark vignette when staring into darkness.")
                .define("enabled", true);

        MAX_VIGNETTE_ALPHA = BUILDER
                .comment("Maximum alpha of the vignette.")
                .defineInRange("maxVignetteAlpha", 0.45, 0.01, 1.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static ForgeConfigSpec spec() {
        return SPEC;
    }

    /**
     * Register config with FML (called from main mod constructor).
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
            DarkStareConfig.spec(),
            "darkstare.cfg"
        );
    }
}
