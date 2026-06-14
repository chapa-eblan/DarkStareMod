# DarkStareMod - Bug Report (Horror Visual Effects Mod)

Context: This is a horror-atmosphere mod for Minecraft (darkness / fear visuals). 
All bugs below are in client-side rendering code that runs every frame.

Legend:
- [CRIT] = Crash risk, race condition, or visibly broken behavior.
- [HIGH] = Performance issue or noticeable visual bug.
- [MED]  = Subtle bug; affects stability/quality over time.
- [LOW]  = Minor / cosmetic / robustness improvements.

====================
1. THREAD SAFETY: Static RandomSource race conditions (CRIT)
====================

Affected files:
- EyesOverlayRenderer.java
- CreepingDarknessRenderer.java
- FilmGrainRenderer.java

Bug:
All three use a static, shared RandomSource instance:
  - private static final RandomSource RAND = RandomSource.create();

In Minecraft’s modern rendering pipeline (multi-threaded / parallel), 
multiple render threads can call RAND concurrently. RandomSource is NOT thread-safe,
so this creates data races that can lead to:
- corrupted internal state
- inconsistent visual noise/eye patterns
- potential crashes or silent failures in some environments

Why it matters for horror mod:
The whole effect relies on controlled randomness (eyes, grain, darkness edges).
A race condition makes these effects unstable, flickery, and unreliable.

Fix:
- Use thread-local instances OR
- Use a deterministic seed per frame instead of shared state.
Example pattern:
  - long seed = System.nanoTime();
  - RandomSource rand = RandomSource.create(seed); // local to render()

====================
2. EyesOverlayRenderer: RAND.nextInt() destroys eye animation stability (CRIT)
====================

File: EyesOverlayRenderer.java
Lines: ~86-90

Code in question:
  float lifeDuration = 240 + (intensity > 0.7f ? RAND.nextInt(120) : 0);

Bug:
Every frame, when intensity > 0.7, this line mutates the shared RNG state inside
the main eye loop. This means:
- Different eyes each frame get different lifeDuration values unpredictably.
- The sequence of "random" eye behaviors changes chaotically instead of smoothly.

Effect in-game (horror impact):
- Eyes will jump between patterns, flicker weirdly, or behave unnaturally during
  intense moments—exactly when the effect should feel controlled and creepy, not glitchy.
- This breaks the "fade lifecycle" design: appear/hold/disappear no longer looks like
  intentional horror pacing; it becomes random noise.

Fix:
Use a deterministic value derived from the seed `s` instead of calling RAND.nextInt():
Example:
  float lifeDuration = 240 + (intensity > 0.7f ? ((s % 120) + 60) : 0);

This keeps variety across eyes but no shared-state mutation per frame.

====================
3. PeripheralPresenceRenderer: Static mutable state without synchronization (CRIT)
====================

File: PeripheralPresenceRenderer.java
Lines: ~18-34

Bug:
Static fields store presence data:
  - private static float[] presenceX / Y / Size
  - private static boolean[] presenceActive
  - private static long lastPresenceUpdate

These are updated inside render() based on system time and random values, but there is
no synchronization. If rendering or config reading occurs in parallel threads, you get:
- torn reads (half-updated positions)
- presences appearing in wrong places
- potential crash if arrays/flags shift unexpectedly

Horror impact:
These are the "figures at edge of vision." Racy updates may cause them to teleport,
flicker unnaturally, or occasionally not appear—breaking immersion.

Fix:
Either:
- Mark fields as volatile (for simple primitives) and ensure atomic updates, OR
- Preferably, make instances non-static (one per render context), OR
- Only update from a single known-safe thread (e.g., main thread in RenderEvent).

====================
4. RealityFractureRenderer: Wrong primitive for glow circle using QUADS (CRIT)
====================

File: RealityFractureRenderer.java
Lines: ~172-189

Code pattern:
  buf.begin(VertexFormat.Mode.QUADS, ...);
  buf.vertex(center).color(0,0,0,0);
  buf.vertex(edge1).color(...);
  buf.vertex(edge2).color(...);
  buf.vertex(center).color(0,0,0,0);

Bug:
- QUADS expects a proper quadrilateral (4 distinct corners), but here two vertices are the
  same center point. This creates degenerate geometry: one or both triangles in each quad
  may fail to render properly depending on the GPU/driver.
- The intended effect is a radial glow (like a small red halo at crack origins). With this
  setup, you either get inconsistent rendering or missing glow segments entirely.

Horror impact:
At extreme intensity (>0.9), reality fractures should have glowing edges. Due to this bug,
that “red aura” may not render consistently, making the effect weaker and less scary.

Fix:
Use TRIANGLE_FAN instead of QUADS for a proper radial glow:
  buf.begin(VertexFormat.Mode.TRIANGLE_FAN, ...);
  buf.vertex(center).color(...);
  for each angle:
      buf.vertex(on circle edge).color(...);
  last vertex wraps back to first.
  tess.end();

====================
5. CreepingDarknessRenderer: Confusing / risky seed-based positioning (HIGH)
====================

File: CreepingDarknessRenderer.java
Lines: ~47-64, especially x = ((float)(seed % w)) / w * w;

Bug:
This expression simplifies to just `seed % w`, but is written in a misleading way.
Additionally, if `seed` grows very large or overflows (long/float cast issues), you may get:
- negative positions
- out-of-bounds positions
- unexpected wrapping behavior

Horror impact:
Darkness edges might occasionally appear outside the intended screen bounds or behave
strangely after long play sessions instead of smoothly creeping in from the dark.

Fix:
Simplify and stabilize:
  float x = (float)((seed % w + w) % w);
or use a controlled, bounded formula based on intensity and time rather than unbounded seeds.

====================
6. CreepingDarknessRenderer: Gradient winding / diagonal artifacts (HIGH)
====================

File: CreepingDarknessRenderer.java
Lines: ~95-120

Bug:
The code draws large screen quads with different alpha values at each corner to simulate
a gradient of creeping darkness. However, the vertex order and color placement can lead to:
- diagonal gradients instead of smooth radial or edge-inward gradients
- visible "bands" where two triangles (of one quad) blend differently

Horror impact:
Instead of a natural vignette/tunnel-vision creep, you get unnatural shading patterns that
look “off,” breaking immersion in dark scenes.

Fix:
- Use proper vertex winding and matching gradient direction.
- Consider using many small segments or a radial gradient approach for smoother edges.

====================
7. EyesOverlayRenderer: Excessive draw calls per frame (HIGH)
====================

File: EyesOverlayRenderer.java
Lines: ~52-112, plus drawTexturedQuad()

Bug:
Each eye is drawn using its own:
  buf.begin(...) -> ... -> Tesselator.getInstance().end();

For potentially many eyes (maxPairs + jitter), this creates a large number of individual
draw calls every frame. Additionally:
- RenderSystem.setShaderColor(...) is called inside the loop for each eye color variation.

Horror impact:
At high fear intensity, when there are lots of eyes on screen, you can get:
- noticeable FPS drops
- stuttering exactly during intense horror moments (bad UX)

Fix:
Batch all eyes into a single buffer instead of calling end() per eye:
  buf.begin(...) at start of render()
    add vertices for each eye
  tess.end() once at the end.
This reduces draw overhead significantly while preserving the effect.

====================
8. PeripheralBlurRenderer: Hard threshold causes pop-in/pop-out (MED)
====================

File: PeripheralBlurRenderer.java
Lines: ~16-43

Bug:
There is a hard cutoff:
  if (intensity < 0.25f) return;
and later:
  float bandWidth = blurAmount * 60f;
  if (bandWidth < 2f) return;

This means as intensity changes near these thresholds, the effect can suddenly turn on/off
instead of fading smoothly, especially when tuning config strength.

Horror impact:
Tunnel vision should feel like a gradual psychological pressure. A sudden snap-in of blur at
edges feels cheap and breaks immersion.

Fix:
- Remove or soften hard returns; let alpha/bandWidth fade continuously to zero instead of
  switching on/off.
- For example, only return if intensity < some very small epsilon, not abruptly at 0.25.

====================
9. FilmGrainRenderer: Alpha not clamped (LOW)
====================

File: FilmGrainRenderer.java
Lines: ~24

Bug:
Alpha is calculated as:
  float alpha = intensity * 0.07f * Math.min(strength, 3.0f);

If a user sets FILM_GRAIN_STRENGTH very high (e.g., through config editing), the grain can
become too visible and oppressive, ruining visibility in-game instead of adding subtle horror.

Horror impact:
Grain should feel like an analog/horror-film vibe; if it’s too strong, players just see noise
and may disable the mod.

Fix:
Clamp alpha to a safe upper bound:
  float alpha = Math.min(intensity * 0.07f * Math.min(strength, 3.0f), 0.15f);

====================
10. Time source inconsistency: System.currentTimeMillis() vs gameTime (LOW)
====================

Affected files:
- HeartbeatPulseOverlay.java
- RealityFractureRenderer.java
- PeripheralPresenceRenderer.java

Bug:
Some renderers use:
  double now = System.currentTimeMillis() / 1000.0;
instead of the provided `gameTime` parameter, which is used more consistently elsewhere.

This can cause:
- effects to run out-of-sync with other mod systems tied to gameTime
- weird behavior in menus, paused states, or when game time changes (e.g., slowed in horror packs)

Horror impact:
Heartbeat pulse and reality fractures may drift from the intended pacing of fear intensity.
At best this is subtle; at worst it desyncs audio/visual cues designed to scare players.

Fix:
Use gameTime consistently across all renderers instead of System.currentTimeMillis(), or
ensure any use of system time is fully independent and not expected to sync with gameplay events.

====================
11. Minor robustness / design notes (LOW)
====================

- CreepingDarknessRenderer / PeripheralPresenceRenderer: Static RandomSource initialized once
  can produce the same patterns for all players/instances. Consider seeding from world or player ID
  so different runs feel slightly unique, enhancing horror atmosphere.

- RealityFractureRenderer uses float crackSeeds[i] % 12 to choose maxSegments. For very large seeds,
  floating point precision loss can occur. Use modular integer math instead for safety:
    int maxSegments = 8 + (int)(crackSeedIndex % 12);

- EyesOverlayRenderer fade formula: currently safe because sin(0..PI) ≥ 0; but add a defensive clamp
  to avoid negative alpha if logic is changed later.

====================
