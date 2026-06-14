package com.darkstare.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**
 * Forge config for DarkStare.
 */
public final class DarkStareConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // General
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    // Detection
    public static final ForgeConfigSpec.IntValue RAY_LENGTH;
    public static final ForgeConfigSpec.IntValue MIN_CONSECUTIVE_DARK_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_LIGHT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DARK_TUNNEL_BOOST;

    // Intensity curve
    public static final ForgeConfigSpec.DoubleValue START_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FULL_INTENSITY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FADE_SPEED;

    // FOV zoom
    public static final ForgeConfigSpec.BooleanValue ENABLE_FOV_ZOOM;
    public static final ForgeConfigSpec.DoubleValue MAX_FOV_REDUCTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JITTER;

    // Eyes overlay
    public static final ForgeConfigSpec.BooleanValue ENABLE_EYES_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue EYE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MAX_ALPHA;
    public static final ForgeConfigSpec.IntValue MAX_EYE_PAIRS;

    // Sounds
    public static final ForgeConfigSpec.BooleanValue ENABLE_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue HUM_VOLUME_SCALE;
    public static final ForgeConfigSpec.DoubleValue WHISPER_VOLUME_SCALE;
    public static final ForgeConfigSpec.DoubleValue SPIKE_CHANCE_AT_FULL;
    public static final ForgeConfigSpec.DoubleValue HEARTBEAT_VOLUME_SCALE;

    // Vignette
    public static final ForgeConfigSpec.BooleanValue ENABLE_VIGNETTE;
    public static final ForgeConfigSpec.DoubleValue MAX_VIGNETTE_ALPHA;

    // Film grain
    public static final ForgeConfigSpec.BooleanValue ENABLE_FILM_GRAIN;
    public static final ForgeConfigSpec.DoubleValue FILM_GRAIN_STRENGTH;

    // Red tint
    public static final ForgeConfigSpec.BooleanValue ENABLE_RED_TINT;

    // Screen shake
    public static final ForgeConfigSpec.BooleanValue ENABLE_SCREEN_SHAKE;
    public static final ForgeConfigSpec.DoubleValue SCREEN_SHAKE_MULTIPLIER;

    // Chromatic aberration (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHROMATIC_ABERRATION;
    public static final ForgeConfigSpec.DoubleValue CHROMATIC_ABERRATION_STRENGTH;

    // Breathing distortion (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_BREATHING_DISTORTION;
    public static final ForgeConfigSpec.DoubleValue BREATHING_DISTORTION_STRENGTH;

    // Shadow figures (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHADOW_FIGURES;
    public static final ForgeConfigSpec.DoubleValue SHADOW_FIGURE_CHANCE;
    public static final ForgeConfigSpec.IntValue MAX_SHADOW_FIGURES;

    // Strobe effect (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_STROBE_EFFECT;
    public static final ForgeConfigSpec.DoubleValue STROBE_CHANCE;

    // Peripheral blur (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_PERIPHERAL_BLUR;
    public static final ForgeConfigSpec.DoubleValue PERIPHERAL_BLUR_STRENGTH;

    // Peripheral presence (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_PERIPHERAL_PRESENCE;
    public static final ForgeConfigSpec.DoubleValue PERIPHERAL_PRESENCE_STRENGTH;

    // Afterimage effect (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_AFTERIMAGE;
    public static final ForgeConfigSpec.DoubleValue AFTERIMAGE_STRENGTH;

    // Breathing fog (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_BREATHING_FOG;
    public static final ForgeConfigSpec.DoubleValue BREATHING_FOG_STRENGTH;

    // Reality fracture (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_REALITY_FRACTURE;
    public static final ForgeConfigSpec.DoubleValue REALITY_FRACTURE_STRENGTH;

    // Screen tear (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_SCREEN_TEAR;
    public static final ForgeConfigSpec.DoubleValue SCREEN_TEAR_STRENGTH;

    // Spatial distortion (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPATIAL_DISTORTION;
    public static final ForgeConfigSpec.DoubleValue SPATIAL_DISTORTION_STRENGTH;

    // Creeping darkness (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_CREEPING_DARKNESS;
    public static final ForgeConfigSpec.DoubleValue CREEPING_DARKNESS_STRENGTH;

    // Heartbeat pulse overlay (NEW)
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEARTBEAT_PULSE_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue HEARTBEAT_PULSE_OVERLAY_STRENGTH;
    private static final ForgeConfigSpec SPEC;
    static {
        BUILDER.comment("DarkStare - A subtle horror mod for staring into darkness")
               .push("general");

        ENABLED = BUILDER
                .comment("Enable all DarkStare effects.")
                .define("enabled", true);

        BUILDER.pop();

        // Detection
        BUILDER.comment("Settings for detecting dark passages/tunnels").push("darkness_detection");

        RAY_LENGTH = BUILDER
                .comment("Max distance (blocks) to raycast into darkness. Default 32.")
                .defineInRange("rayLength", 32, 4, 128);

        MIN_CONSECUTIVE_DARK_BLOCKS = BUILDER
                .comment("Minimum consecutive dark samples along the ray before effects trigger.")
                .defineInRange("minConsecutiveDarkBlocks", 3, 1, 15);

        MAX_LIGHT_LEVEL = BUILDER
                .comment("Max light level (0-15) to consider a block 'dark'. " +
                         "0 = pitch black only; higher = dim areas count too.")
                .defineInRange("maxLightLevel", 2, 0, 7);

        DARK_TUNNEL_BOOST = BUILDER
                .comment("Extra intensity multiplier for deep tunnels (1.0 = no boost).")
                .defineInRange("darkTunnelBoost", 1.4, 1.0, 3.0);

        BUILDER.pop();

        // Intensity curve
        BUILDER.comment("How fast effects build up and fade").push("intensity");

        START_SECONDS = BUILDER
                .comment("Seconds staring before any effect begins.")
                .defineInRange("startSeconds", 2.5, 1.0, 30.0);

        FULL_INTENSITY_SECONDS = BUILDER
                .comment("Seconds staring to reach maximum intensity.")
                .defineInRange("fullIntensitySeconds", 16.0, 4.0, 90.0);

        FADE_SPEED = BUILDER
                .comment("How quickly effects fade when you look away (higher = faster).")
                .defineInRange("fadeSpeed", 3.5, 1.0, 12.0);

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
                .defineInRange("baseChance", 0.03, 0.001, 0.25);

        MAX_ALPHA = BUILDER
                .comment("Maximum alpha of eyes effect.")
                .defineInRange("maxAlpha", 0.16, 0.02, 0.7);

        MAX_EYE_PAIRS = BUILDER
                .comment("Max number of eye pairs visible on screen at once (for atmosphere).")
                .defineInRange("maxEyePairs", 3, 0, 8);

        BUILDER.pop();

        // Sounds
        BUILDER.comment("Eerie sound effects").push("sounds");

        ENABLE_SOUNDS = BUILDER
                .comment("Enable atmospheric sounds when staring into darkness.")
                .define("enabled", true);

        HUM_VOLUME_SCALE = BUILDER
                .comment("Volume scale for the high-frequency hum (ambient.end).")
                .defineInRange("humVolumeScale", 0.9, 0.0, 3.0);

        WHISPER_VOLUME_SCALE = BUILDER
                .comment("Volume scale for whisper-like cave sounds.")
                .defineInRange("whisperVolumeScale", 0.6, 0.0, 2.0);

        SPIKE_CHANCE_AT_FULL = BUILDER
                .comment("Chance per tick of an eerie spike at full intensity.")
                .defineInRange("spikeChanceAtFull", 0.012, 0.0, 0.15);

        HEARTBEAT_VOLUME_SCALE = BUILDER
                .comment("Volume scale for heartbeat that fades in at high intensity (entity.warden.heartbeat).")
                .defineInRange("heartbeatVolumeScale", 0.7, 0.0, 3.0);

        BUILDER.pop();

        // Vignette
        BUILDER.comment("Dark vignette effect around screen edges").push("vignette");

        ENABLE_VIGNETTE = BUILDER
                .comment("Enable a subtle dark vignette when staring into darkness.")
                .define("enabled", true);

        MAX_VIGNETTE_ALPHA = BUILDER
                .comment("Maximum alpha of the vignette.")
                .defineInRange("maxVignetteAlpha", 0.5, 0.01, 0.95);

        BUILDER.pop();

        // Film grain
        BUILDER.comment("Film grain overlay for analog horror feel").push("film_grain");

        ENABLE_FILM_GRAIN = BUILDER
                .comment("Enable subtle film grain at high intensity.")
                .define("enabled", true);

        FILM_GRAIN_STRENGTH = BUILDER
                .comment("Strength of the film grain (0 to disable).")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Red tint
        BUILDER.comment("Red color tint at very high intensity").push("red_tint");

        ENABLE_RED_TINT = BUILDER
                .comment("Enable red tint when staring long enough.")
                .define("enabled", true);

        BUILDER.pop();

        // Screen shake
        BUILDER.comment("Subtle screen shake / camera jitter at high intensity").push("screen_shake");

        ENABLE_SCREEN_SHAKE = BUILDER
                .comment("Enable subtle screen shake (FOV-based, not full camera).")
                .define("enabled", true);

        SCREEN_SHAKE_MULTIPLIER = BUILDER
                .comment("Multiplier for screen shake intensity. 0 = disabled.")
                .defineInRange("multiplier", 1.0, 0.0, 5.0);

        BUILDER.pop();

        // === NEW EFFECTS ===

        // Chromatic aberration
        BUILDER.comment("Chromatic aberration — RGB color splitting at screen edges").push("chromatic_aberration");

        ENABLE_CHROMATIC_ABERRATION = BUILDER
                .comment("Enable chromatic aberration effect (color fringing).")
                .define("enabled", true);

        CHROMATIC_ABERRATION_STRENGTH = BUILDER
                .comment("Strength of the chromatic aberration. Higher = more color splitting.")
                .defineInRange("strength", 1.5, 0.0, 4.0);

        BUILDER.pop();

        // Breathing distortion
        BUILDER.comment("Breathing distortion — slow organic pulse on screen edges").push("breathing_distortion");

        ENABLE_BREATHING_DISTORTION = BUILDER
                .comment("Enable breathing distortion effect (organic pulsing darkness).")
                .define("enabled", true);

        BREATHING_DISTORTION_STRENGTH = BUILDER
                .comment("Strength of the breathing distortion. Higher = more visible pulse.")
                .defineInRange("strength", 1.5, 0.0, 3.0);

        BUILDER.pop();

        // Shadow figures
        BUILDER.comment("Shadow Figures — ghostly silhouettes in peripheral vision").push("shadow_figures");

        ENABLE_SHADOW_FIGURES = BUILDER
                .comment("Enable shadow figure effect (ghostly humanoid shapes).")
                .define("enabled", true);

        SHADOW_FIGURE_CHANCE = BUILDER
                .comment("Base chance for a shadow figure to appear per render call.")
                .defineInRange("chance", 0.15, 0.0, 0.8);

        MAX_SHADOW_FIGURES = BUILDER
                .comment("Maximum number of shadow figures visible at once.")
                .defineInRange("maxFigures", 2, 0, 6);

        BUILDER.pop();

        // Strobe effect
        BUILDER.comment("Strobe/Flash — sudden brief flashes of light").push("strobe_effect");

        ENABLE_STROBE_EFFECT = BUILDER
                .comment("Enable strobe flash effect (sudden bright flashes).")
                .define("enabled", true);

        STROBE_CHANCE = BUILDER
                .comment("Chance per tick for a strobe flash at high intensity.")
                .defineInRange("chance", 0.5, 0.0, 3.0);

        BUILDER.pop();

        // Peripheral blur
        BUILDER.comment("Peripheral Vision Blur — tunnel vision effect").push("peripheral_blur");

        ENABLE_PERIPHERAL_BLUR = BUILDER
                .comment("Enable peripheral blur (tunnel vision darkening).")
                .define("enabled", true);

        PERIPHERAL_BLUR_STRENGTH = BUILDER
                .comment("Strength of the peripheral blur. Higher = more tunnel vision.")
                .defineInRange("strength", 1.5, 0.0, 3.0);

        BUILDER.pop();

        // Peripheral presence
        BUILDER.comment("Peripheral Presence — ghostly shapes in the periphery of vision").push("peripheral_presence");

        ENABLE_PERIPHERAL_PRESENCE = BUILDER
                .comment("Enable peripheral presence effect (ghostly silhouettes at screen edges).")
                .define("enabled", true);

        PERIPHERAL_PRESENCE_STRENGTH = BUILDER
                .comment("Strength of the peripheral presence. Higher = more visible shapes.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Afterimage effect
        BUILDER.comment("Afterimage — ghostly afterimages that linger when looking away from darkness").push("afterimage");

        ENABLE_AFTERIMAGE = BUILDER
                .comment("Enable afterimage effect (lingering visual artifacts).")
                .define("enabled", true);

        AFTERIMAGE_STRENGTH = BUILDER
                .comment("Strength of the afterimage. Higher = more persistent ghosts.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Breathing fog
        BUILDER.comment("Breathing Fog — dark organic fog creeping across the screen").push("breathing_fog");

        ENABLE_BREATHING_FOG = BUILDER
                .comment("Enable breathing fog effect.")
                .define("enabled", true);

        BREATHING_FOG_STRENGTH = BUILDER
                .comment("Strength of the breathing fog. Higher = denser fog patches.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Reality fracture
        BUILDER.comment("Reality Fracture — crack-like fissures spreading from screen edges").push("reality_fracture");

        ENABLE_REALITY_FRACTURE = BUILDER
                .comment("Enable reality fracture effect.")
                .define("enabled", true);

        REALITY_FRACTURE_STRENGTH = BUILDER
                .comment("Strength of the reality fracture. Higher = more cracks and glow.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Screen tear
        BUILDER.comment("Screen Tear — glitch/tear effects at extreme intensity").push("screen_tear");

        ENABLE_SCREEN_TEAR = BUILDER
                .comment("Enable screen tear effect.")
                .define("enabled", true);

        SCREEN_TEAR_STRENGTH = BUILDER
                .comment("Strength of the screen tear. Higher = more glitchy.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Spatial distortion
        BUILDER.comment("Spatial Distortion — reality-bending waves at high intensity").push("spatial_distortion");

        ENABLE_SPATIAL_DISTORTION = BUILDER
                .comment("Enable spatial distortion effect.")
                .define("enabled", true);

        SPATIAL_DISTORTION_STRENGTH = BUILDER
                .comment("Strength of the spatial distortion. Higher = more warped reality.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Creeping darkness
        BUILDER.comment("Creeping Darkness — dark patches advancing from screen edges").push("creeping_darkness");

        ENABLE_CREEPING_DARKNESS = BUILDER
                .comment("Enable creeping darkness effect.")
                .define("enabled", true);

        CREEPING_DARKNESS_STRENGTH = BUILDER
                .comment("Strength of the creeping darkness. Higher = darker patches.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

        BUILDER.pop();

        // Heartbeat pulse overlay
        BUILDER.comment("Heartbeat Pulse Overlay — expanding pulse rings synced to heartbeat").push("heartbeat_pulse_overlay");

        ENABLE_HEARTBEAT_PULSE_OVERLAY = BUILDER
                .comment("Enable heartbeat pulse overlay effect.")
                .define("enabled", true);

        HEARTBEAT_PULSE_OVERLAY_STRENGTH = BUILDER
                .comment("Strength of the heartbeat pulse. Higher = more visible rings.")
                .defineInRange("strength", 1.0, 0.0, 3.0);

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
        ModLoadingContext.get().registerConfig(Type.CLIENT, SPEC, "darkstare-client.toml");
    }
}
