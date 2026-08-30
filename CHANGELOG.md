# Changelog

Notable user-facing changes are recorded here. Build numbers are never reused,
including when a build is rejected before publication.

## [Unreleased]

### Beta 2 settings and options

- Replace the single crowded settings view with Scanning, Markers, Features,
  HUD, and Profiles pages, while retaining built-in configuration without Mod
  Menu.
- Add current-chunk and nearby-chunk presets, current-Y and nearby vertical
  ranges, timed, block-update-driven, and manual refresh modes, and separately
  configurable hard block and adaptive millisecond scan budgets.
- Separate the completed-scan discovery limit from the per-frame visible limit.
- Add independent source, flowing, and waterlogged marker shapes, an RGB color
  picker, a persisted custom palette, and color-blind-safe, high-contrast, and
  monochrome palette presets.
- Add maximum render distance and configurable distance fading.
- Add HUD anchors, X/Y offsets, scaling, background opacity, and compact mode.
- Add an opt-in temporary hold key alongside the persistent overlay toggle.
- Add local accessibility, exploration, and low-performance profiles plus
  current-chunk, nearby 3x3, and nearby 5x5 area shortcuts.
- Add explicit version 1 to version 2 migration and validated local settings
  import/export without network access or filesystem access outside the normal
  client configuration directory.

### Beta 2 reliability and performance work

- Scope every scan to an invalidation generation so stale work cannot publish
  after a level, scan-center, radius, or loaded-chunk boundary; remove markers
  immediately as their client chunk unloads.
- Keep scan settings consistent for an entire pass and rescan only when a
  setting changes which markers belong in the result.
- Apply source, flowing, waterlogged, and marker-limit visibility changes to
  retained markers immediately instead of waiting for a rescan to finish.
- Report live scan progress and counts with the full 16x16 chunk area included
  in progress totals.
- Bound retained scan results to the configured marker limit while continuing
  to prioritize source-water markers over flowing-water markers.
- Hold at most one loaded chunk during an incremental scan and avoid rejected
  marker allocations after the configured limit is reached.
- Cull markers outside the active render sections, camera distance, and view
  frustum before generating their vertices.
- Own and close the staged GPU buffer with the Fabric client lifecycle, without
  a permanent renderer-close mixin.
- Save changed settings when the settings screen closes or the client stops,
  instead of writing the configuration file for every control update.
- Atomically publish copy-on-write configuration with an immutable render
  snapshot whose colors are decoded once, and cache static HUD text.
- Warn about malformed configuration files, preserve them as uniquely named
  backups, and restore safe defaults.
- Use the documented 512-marker minimum consistently in both configuration
  validation and the settings slider.

### Beta 2 visualization and features

- Add cap and cross cues for waterlogged markers plus height, surface-gauge,
  and combined fluid-level treatments for flowing water.
- Add optional nearest-marker HUD guidance with distance, compass direction,
  and vertical offset, plus a matching world-space highlight.
- Add current-chunk and full scan-area boundaries. Loaded chunks use solid
  frames while skipped and pending chunks use dashed frames, without retaining
  chunk objects or requesting additional chunks.
- Expand distance fading to every marker and scan-boundary treatment.
- Expand the compact HUD with visible, discovered, skipped-chunk, and stale
  snapshot state.
- Add hollow-box, stripe, and dot marker patterns plus accessible and classic
  pattern shortcuts.
- Add opt-in narrator messages for completed scans and discovery-limit losses.
- Add an opt-in diagnostic HUD for scan duration, block progress, discarded
  markers, visible markers, and emitted vertices.
- Bound generated geometry to 200,000 vertices per frame and report the exact
  count, including optional patterns, highlights, gauges, and scan boundaries.

## [v1.0.0 Beta 1] - 2026-08-28

Public Beta 1 uses the user-tested build `012` artifact for Minecraft 26.2.
Build `011` was rejected after its initial test run and was never published or
installed as the beta.

### Accessibility and visualization

- Toggle source and flowing-water visualization independently.
- Highlight source and flowing water with separate high-contrast colors.
- Preview each selected marker color directly inside its settings button.
- Choose box, pillar, or beacon marker styles.
- Adjust opacity, outline thickness, marker limit, pulsing, labels, and the
  top-center status HUD.
- Use the same muted-key, bright-value, green-`ON`, and red-`OFF` visual
  language as 1MB Locator HUD.

### Scanning and fair play

- Scan only chunks already loaded by the client, with configurable chunk
  radius and manual rescan support.
- Include or exclude waterlogged source blocks.
- Keep through-wall markers disabled by default with an explicit fair-play
  tooltip.

### Configuration and compatibility

- Provide rebindable `F9`, `Shift+F9`, and `F10` controls.
- Store settings locally in `config/water-source-mod.json`.
- Support configuration without Mod Menu and optional integration when Mod
  Menu is installed.
- Require Minecraft 26.2, Java 25, Fabric Loader 0.19.3 or newer in the 26.2
  line, and a compatible Fabric API.
- Require no server-side mod; user-tested against a Paper 26.2 server.

### Privacy and security

- No telemetry, analytics, account identifiers, or network requests.
- No custom server protocol or hidden chunk requests.
- No filesystem writes outside the normal local client configuration path.

[v1.0.0 Beta 1]: https://github.com/mrfdev/WaterSourceMod/releases/tag/v1.0.0-beta.1
