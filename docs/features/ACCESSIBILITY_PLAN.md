# Accessibility (a11y) Plan

This document defines a plan to make **Defender of Egril** more accessible to
players with vision, hearing, motor, and cognitive impairments. It is the
response to the GitHub issue *"accessibility (a11y)"* and is meant as a living
roadmap — items should be implemented in small, focused follow‑up PRs.

## 1. Goals

- Reach **WCAG 2.1 Level AA** for all UI screens (menus, settings, info pages,
  level editor, gameplay HUD).
- Cover the **Basic** tier and as many **Intermediate** items as practical from
  the [Game Accessibility Guidelines](https://gameaccessibilityguidelines.com/).
- Cover the [IGDA‑GASIG SIG Top Ten](https://igda-gasig.org/how/sig-top-ten/)
  recommendations.
- Follow the JetBrains
  [Compose Multiplatform accessibility guide](https://kotlinlang.org/docs/multiplatform/compose-accessibility.html)
  so the same accessibility metadata works on Android (TalkBack), iOS
  (VoiceOver), Desktop (NVDA/JAWS/Orca/Narrator), and the web (browser screen
  readers).
- Make accessibility settings **discoverable, persistent, and per‑profile** via
  the existing `SettingsDialog` / multiplatform‑settings stack.

## 2. Reference Material

- [Compose Multiplatform a11y](https://kotlinlang.org/docs/multiplatform/compose-accessibility.html)
- [Game Accessibility Guidelines](https://gameaccessibilityguidelines.com/)
  (Basic / Intermediate / Advanced tiers, grouped by impairment)
- [IGDA‑GASIG SIG Top Ten](https://igda-gasig.org/how/sig-top-ten/)
- WCAG 2.1 (<https://www.w3.org/WAI/WCAG21/quickref/>)

## 3. Current State (Audit)

The codebase already implements a number of accessibility‑friendly features.
The plan builds on top of these — it does **not** replace them.

Already in place:

- **Semantic descriptions** on key buttons and icons in menus, settings and
  Android‑TV layouts (`Modifier.semantics { contentDescription = ... }`,
  `Image(contentDescription = ...)`).
- **Full localization** for English, German, Spanish, French and Italian using
  `compose-multiplatform-localize`; all strings live in `strings.xml` and have
  a `TranslationCoverageTest` enforcing parity.
- **Keyboard navigation** for the hexagonal map (arrow keys / WASD pan, mouse
  wheel zoom). See `docs/implementation/KEYBOARD_NAVIGATION.md`.
- **Keyboard shortcuts** for actions (`docs/guides/KEYBOARD_SHORTCUTS.md`).
- **Text uses `sp`** everywhere, so it scales with the user's system text
  scale factor; the app additionally exposes a manual *Text Scale* setting.
- **Touch targets** ≥ 48dp on mobile and Android TV (`SETTINGS_UI_GUIDE.md`,
  `CONTROL_PAD_IMPLEMENTATION.md`).
- **Visual focus indicators** on Android TV (yellow 4dp border) and Compose’s
  default focus indication elsewhere.
- **Dark mode** toggle and **Enhanced dig outcome images** toggle.
- **No Unicode emojis in code** (enforced by `UnicodeEmojiValidationTest`) so
  screen readers don’t announce raw codepoints — icons are rendered from
  drawables with explicit content descriptions.
- **Text selection** support in info screens and dialogs (`SelectionContainer`
  / `SelectableText`).
- **Pan and zoom** (0.5×–3×) on all platforms plus a **minimap** for
  orientation when zoomed in.
- **Tutorials and tower info dialogs** that can be re‑opened on demand instead
  of only appearing once.

Known gaps (addressed below):

- No formal **screen‑reader pass** across all screens — many composables still
  rely on visual‑only cues (icons without `contentDescription`, decorative
  text in `Row`s, color‑coded health/coins).
- **Color is sometimes the only channel** of information (e.g. tile states,
  enemy/defender status overlays, river flow).
- No **high‑contrast theme** option (only light/dark).
- No **color‑blind‑friendly palettes** (Deuteranopia/Protanopia/Tritanopia).
- An **Animations** toggle already exists in `SettingsDialog`
  (`animations` / `animations_on` / `animations_off` strings), but it needs
  to be audited to confirm that turning it **OFF disables every animation**
  in the app (tile transitions, attack flashes, banner fades, icon idle
  loops, dialog open/close). Any animation that still runs when the toggle
  is OFF is a bug that this plan covers.
- No **subtitles / captions** or visual indicators for sound effects (the game
  has sound effects and music but no captions); no separate volume sliders
  per channel verified across all screens.
- The game is **fully turn‑based with no real‑time pressure** (the player
  can leave at any time and resume with the same state), so no "extend /
  disable timer" assist is needed — but the audit must make sure no
  future feature introduces real‑time pressure.
- **Difficulty / assist** options beyond the existing difficulty setting are
  limited; no "skip puzzle" / "auto‑play turn" assist for cognitive load.
- No documented **focus order** and no guaranteed **tab‑navigation** through
  all interactive UI on desktop / web.
- No **a11y CI test** beyond `TranslationCoverageTest` and
  `UnicodeEmojiValidationTest` — no checks for missing `contentDescription`
  on `Image` / `Icon` / `IconButton`.

## 4. Roadmap

The work is grouped by impairment area, mirroring the structure of the Game
Accessibility Guidelines. Each item lists a **priority** (P0 = must, P1 =
should, P2 = nice‑to‑have) and a rough **scope hint**.

### 4.1 Vision

| # | Item | Priority | Scope hint |
|---|------|----------|------------|
| V1 | Screen‑reader pass: ensure every `Image`, `Icon`, `IconButton`, status indicator and tile has either a meaningful `contentDescription` or `contentDescription = null` for decorative content. | P0 | All `ui/` packages |
| V2 | Add `Modifier.semantics { ... }` summaries for composite widgets (tower card, enemy card, level tile, world‑map location) so a single swipe reads the whole entity. | P0 | `ui/gameplay/`, `ui/worldmap/`, `ui/loadgame/` |
| V3 | Add a **high‑contrast theme** variant (extends `MaterialTheme.colorScheme`) and expose toggle in `SettingsDialog`. | P1 | `ui/theme/`, `ui/settings/SettingsDialog.kt` |
| V4 | Add **color‑blind palettes** (Deuteranopia / Protanopia / Tritanopia) for game elements that currently rely on color (tile types, health bars, river flow, attack/defender team colors). Use shape + pattern + color, not color alone. | P1 | `ui/gameplay/GameMap.kt`, `model/Level.kt`, theme |
| V5 | Provide **scalable UI**: ensure the existing *Text Scale* setting also affects icons in the HUD; verify nothing clips at 1.5× scale on mobile and 2× on desktop. | P1 | HUD/header components |
| V6 | Audit color contrast of all text against backgrounds at AA level (`4.5:1`, `3:1` for ≥ 18sp). | P1 | All screens |
| V7 | Verify minimap, range previews, and selection highlights have a non‑color cue (border, hatching). | P2 | `ui/gameplay/GameMap.kt` |

### 4.2 Hearing

| # | Item | Priority | Scope hint |
|---|------|----------|------------|
| H1 | Audit the sound system: confirm independent volume sliders for **music**, **SFX** and **UI** in settings; add any missing. | P0 | `ui/settings/SettingsDialog.kt`, sound code |
| H2 | Add **visual indicators** for important sounds (e.g. "enemy spawned", "tower built", "wave warning") — flash, toast, or HUD badge. | P1 | `ui/gameplay/` |
| H3 | If/when narrated content is added (tutorials, dragon roars, etc.), provide **subtitles / captions** with adjustable size and background opacity. | P1 | New `CaptionOverlay` composable |
| H4 | Ensure no information is conveyed by sound only. | P0 | Cross‑cutting |

### 4.3 Motor

| # | Item | Priority | Scope hint |
|---|------|----------|------------|
| M1 | Full **keyboard‑only playability**: tab through all interactive UI in a logical order, every action reachable without a mouse. Add `Modifier.focusable()` / `focusRequester` where needed, define focus order with `focusGroup` and `focusProperties { next = ... }`. | P0 | All screens, esp. gameplay |
| M2 | Confirm minimum **48dp touch targets** everywhere on mobile / Android TV; add `Modifier.minimumInteractiveComponentSize()` where missing. | P0 | `ui/` |
| M3 | Add a **"hold to confirm" alternative** to double‑click / long‑press destructive actions (sell tower, end game) so they don't require timing precision. | P1 | Game dialogs / sell flow |
| M4 | Add a **pan/zoom assist** mode: snap to next defender / spawn on key press, dedicated key to centre on selected tower. | P2 | `ui/gameplay/GameMap.kt` |
| M5 | Allow **remapping keyboard shortcuts** (settings page). | P2 | `ui/settings/` + shortcuts model |
| M6 | Ensure all gestures have a **single‑pointer alternative** (panning via on‑screen control pad already exists — verify zoom buttons too on touch). | P1 | Control pad |

### 4.4 Cognitive

| # | Item | Priority | Scope hint |
|---|------|----------|------------|
| C1 | Audit the existing **Animations** toggle in `SettingsDialog` (`animations` / `animations_on` / `animations_off` strings): when OFF, **no** animations should play anywhere (tile transitions, attack flashes, banner fades, icon idle loops, dialog open/close). Add tests that assert animation‑producing composables short‑circuit when the toggle is OFF. Surface the toggle in the *Accessibility* settings section as well. | P0 | `ui/settings/SettingsDialog.kt`, animation call sites |
| C2 | Re‑opening of tutorials is already supported — surface a **Tutorials & Help** entry in the main menu and in‑game pause so players can re‑read them at any time. | P1 | `ui/MenuScreens.kt`, info pages |
| C3 | Provide a **simple language / plain text** review of all tutorial strings (≤ Flesch reading ease guidance). | P2 | `strings.xml` |
| C4 | Add an **objective indicator** that is always visible (current objective, lives, turn) in a fixed HUD spot. (Mostly present — verify on all layouts.) | P1 | `ui/gameplay/GameHeader.kt` |
| C5 | Ensure **autosave** is on by default and visible; the existing save system already supports this — confirm in settings. | P1 | `save/` |

> **Note — no time pressure.** Defender of Egril is fully turn‑based. The
> player can leave the game at any time and resume with the exact same
> state, so there is no real‑time pressure on decisions. Accordingly this
> plan does **not** propose any "extend / disable timers" assist — there
> are no timers to extend. The audit must, however, confirm that no
> future feature accidentally introduces real‑time pressure (e.g.
> auto‑advancing turns, real‑time countdowns).

### 4.5 Speech

No speech input is currently required. We will not actively add voice control
but must keep this in mind for any future feature (e.g. don't make voice the
only input method).

## 5. Cross‑Cutting Engineering Work

- **Add a `de.egril.defender.ui.a11y` package** with helpers:
  - `Modifier.a11ySemantics(role, label, stateDescription)` — wraps
    `Modifier.semantics { ... }` with project conventions (consistent
    `Role` mapping, localized `stateDescription`, and `liveRegion`
    handling for game events).
  - `Modifier.requireContentDescription()` — debug‑only assertion to flag
    decorative images that should be `null`.
  - `AccessibilityPreferences` (data class) wired into existing settings.
- **Settings UI**: add an *Accessibility* section to `SettingsDialog`
  collecting: high‑contrast theme, color‑blind palette, reduce motion,
  text scale, captions, hold‑to‑confirm, and the existing **Animations**
  toggle (mirrored here for discoverability).
- **Localization**: every new user‑facing string lands in `values/strings.xml`
  and in each `values-{lang}/strings.xml`, validated by
  `TranslationCoverageTest`.
- **Persistence**: store accessibility prefs via `multiplatform-settings`
  (already a dependency) so they survive restarts and are per‑profile.

## 6. Testing & Validation

- **New automated tests** (desktop test source set, alongside existing
  `TranslationCoverageTest` / `UnicodeEmojiValidationTest`):
  - `ContentDescriptionCoverageTest` — scans `*.kt` for `Image(` /
    `Icon(` / `IconButton(` and fails if no `contentDescription` parameter
    is present (allowing an explicit `null` for decorative content).
  - `TouchTargetSizeTest` (where feasible) — verifies that interactive
    composables in test snapshots are ≥ 48dp.
- **Compose UI tests** using `SemanticsNodeInteraction` to assert that
  selected screens expose semantic nodes with the expected labels.
- **Manual matrix** (document in `docs/guides/A11Y_TESTING_GUIDE.md`):
  - TalkBack (Android), VoiceOver (iOS), NVDA (Windows desktop & web),
    Orca (Linux desktop), VoiceOver (macOS desktop & web).
  - Keyboard‑only run‑through of a full level.
  - High‑contrast + 200% text scale smoke test on every screen.
  - Color‑blind simulator pass (Chrome DevTools / macOS sim).
- **CI**: extend the existing `:composeApp:desktopTest` job to run the new
  a11y tests on every PR.

## 7. Phased Delivery

Each phase is intended as one or more small PRs so reviews stay focused.

1. **Phase 1 — Foundations (P0)**
   - Audit + add `contentDescription` everywhere (V1).
   - Add `ContentDescriptionCoverageTest` (test for V1).
   - Composite‑widget semantics (V2).
   - Keyboard‑only navigation pass (M1).
   - Animations‑toggle audit (C1): verify the existing toggle disables
     every animation; fix any animation that ignores it.
   - Hearing audit & visual indicators for critical sounds (H1, H4).
2. **Phase 2 — Visual variety (P1)**
   - High‑contrast theme (V3).
   - Color‑blind palettes (V4).
   - Text/icon scale verification (V5, V6).
3. **Phase 3 — Motor & cognitive comfort (P1)**
   - Hold‑to‑confirm (M3).
   - Tutorials & Help entry point (C2).
4. **Phase 4 — Polish (P2)**
   - Pan/zoom assists (M4).
   - Remappable shortcuts (M5).
   - Plain‑language tutorial review (C3).

## 8. Definition of Done

Accessibility is "done enough to ship a release" when:

- All Phase 1 items are merged and covered by automated tests.
- Every screen has been manually tested with at least one screen reader on at
  least one platform.
- The `Accessibility` settings section is documented in
  `docs/guides/SETTINGS_UI_GUIDE.md` and translated to all supported
  languages.
- A short user‑facing accessibility statement is added to the in‑game info
  pages and to the README.

## 9. Out of Scope (for now)

- Voice‑control input.
- Switch‑access device certification.
- Haptic feedback (could be a future P2 item on Android / iOS).

---

*This document is a plan, not an implementation. Subsequent PRs should
reference the item IDs (e.g. **V1**, **M3**) so progress can be tracked
against this roadmap.*
