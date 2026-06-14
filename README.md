# DarkStareMod

Horror visual-effects mod for Minecraft client-side.  
Designed for dark/horror packs: intensifies fear with screen distortions when it gets too dark.

All effects run on the client only (no server impact).  
Plays nice with any pack where “looking into darkness” should feel unsettling.

---

## Features

- **Vignette / Darkness:**
  - Creeping vignette that tightens in low light.
  - Screen darkening at peak intensity.

- **Visual Distortions:**
  - Chromatic aberration (color splitting).
  - Film grain and analog noise overlay.
  - Peripheral blur / tunnel vision.
  - Spatial distortion waves.
  - Strobe-like flashes and screen tears.

- **Horror Atmosphere:**
  - Ghostly eyes in the periphery that glance at you.
  - Shadow figures near the edges of your view.
  - Creeping darkness patches moving inward.
  - Reality fracture: crack lines + faint red glow.
  - Afterimages and breathing fog / distortion.

- **Heartbeat & Pulse:**
  - Pulsing heartbeat rings synced to intensity.
  - Red tint pulses when tension is high.

All intensities scale smoothly with how dark/unsettled the scene is — no hard toggles, just pressure building over time.

---

## Installation (Client)

- Requires: Minecraft [specify version] + Fabric / Quilt loader.
- Steps:
  - Download `.jar` from Releases.
  - Drop it into your `mods/` folder.
  - Launch and go somewhere dark.

No special configuration is required — the mod reacts automatically to environment darkness.

---

## Configuration (Optional)

If you tweak configs, note:

- Effects are intensity-based; they fade in/out gradually instead of snapping on/off.
- Avoid setting individual effect strengths too high; at extreme values:
  - visibility drops and horror turns into “I can’t see anything.”
- Recommended: keep most options near default unless you’re building a dedicated horror setup.

---

## Technical Notes (For Developers / Modpack Authors)

- All renderers are client-side overlays tied to darkness/intensity events.
- Safe for modpacks and multiplayer; no server logic, no packet spam.
- Thread-safe: race conditions in shared RandomSource have been removed.
- Performance-friendly: draw calls batched where possible (e.g., eyes overlay).

If you’re integrating this into a horror pack and want:
- custom intensity curves
- event-based triggers (sounds / footsteps / mobs)
let me know — I can prepare hooks/configs for smoother integration.

---

## Links

- Repo: https://github.com/chapa-eblan/DarkStareMod
- Issues/Bugs: Open an issue with screenshots/GIF if possible.

---

# Тёмный взгляд (DarkStareMod)

Мод визуальных эффектов для хоррор-атмосферы в Minecraft (только клиент).  
Создаёт ощущение, что темнота «живая»: всё нарастает постепенно и неприятно.

## Особенности

- Виньетка, которая сужается во тьме.
- Размытие по краям зрения — эффект тоннеля.
- Хроматическая аберрация, плёночный шум, искажения пространства.
- Призрачные глаза на периферии: иногда смотрят прямо на тебя.
- Тени по краям экрана; кратковременные вспышки и разломы реальности.
- Пульсация сердцебиения — красные кольца и лёгкое покачивание.

Всё настроено так, чтобы не превращать игру в «полностью чёрный экран», а именно усиливать страх.

## Установка (Клиент)

- Требуется: Minecraft [версия] + Fabric / Quilt.
- Положи `.jar` мода в папку `mods/`.
- Запусти и иди куда-нибудь без света.

Конфигурация не обязательна — мод сам реагирует на темноту.

## Технические детали (для разработчиков)

- Только клиент; сервер не затрагивается.
- Все эффекты плавные, без резких включений/выключений.
- Исправлены проблемы с многопоточностью и лишними вызовами отрисовки.
- Подходит для хоррор-сборок: можно тонко подкрутить интенсивность через конфиг.
