# 1MB Water Source

1MB Water Source is a client-only Fabric accessibility mod for Minecraft
26.2. It makes water source blocks easier to distinguish from flowing water
when the overlay is useful, while staying completely off during ordinary play
by default.

Repository: [github.com/mrfdev/WaterSourceMod](https://github.com/mrfdev/WaterSourceMod)  
Issues: [github.com/mrfdev/WaterSourceMod/issues](https://github.com/mrfdev/WaterSourceMod/issues)  
License: MIT

## Release status

**Public Beta 1** is version `1.0.0`, build `012`. This exact build was
user-tested and confirmed working on Minecraft 26.2 on August 28, 2026.

[Download Beta 1](https://github.com/mrfdev/WaterSourceMod/releases/tag/v1.0.0-beta.1) ·
[Beta 1 release notes](docs/releases/v1.0.0-beta.1.md) ·
[Changelog](CHANGELOG.md)

The repository currently contains unreleased Beta 2 reliability, performance,
settings, visualization, and accessibility work. Beta 1 remains the latest
published and user-tested JAR.

## Features

- Toggle the visualization with `F9` (rebindable in Minecraft Controls).
- Optionally show it only while holding `F8`, without giving up the normal
  toggle behavior.
- Force a fresh scan with `Shift+F9` (also rebindable through the two key
  mappings).
- Choose current-chunk, nearby 3x3, nearby 5x5, or custom scan radii, plus a
  current-Y, nearby-height, or full-build-height vertical range.
- Refresh automatically, after debounced client block updates, or only when
  manually requested. Incremental work obeys both block and millisecond budgets.
- Use separate discovery and per-frame visible marker limits.
- Use an RGB color picker, a saved custom palette, and default,
  color-blind-safe, high-contrast, or monochrome presets.
- Distinguish muted setting labels from bright values using the same visual
  language as 1MB Locator HUD, including green `ON` and red `OFF` states.
- Choose independent source, flowing, and waterlogged shapes, including box,
  pillar, beacon, hollow, stripe, and dot patterns, so marker meaning never
  depends on color alone.
- Add a cap or cross to waterlogged markers and represent flowing-water depth
  through marker height, a surface gauge, both cues, or a fixed shape.
- Optionally highlight the nearest eligible marker in the world and show its
  distance, compass direction, and vertical offset in the HUD.
- Optionally draw the current chunk or the complete scan area. Loaded chunks
  use solid frames while skipped or pending chunks use dashed frames; this
  visualization never requests a chunk.
- Configure opacity, outline thickness and color, pulse animation, maximum
  render distance, and distance fade.
- Anchor, offset, scale, adjust the background, or compact the optional status
  HUD. It reports visible and discovered markers, skipped chunks, and whether
  a retained snapshot is stale during a refresh.
- Opt into narrator scan-completion and discovery-limit messages, or a
  diagnostic HUD with scan timing, processed blocks, discarded markers, and
  emitted render vertices.
- Apply local accessibility, exploration, and low-performance profiles.
- Import or export a validated settings copy inside the normal client config directory.
- Include or exclude waterlogged source blocks.
- Scan incrementally, only in client-loaded chunks, across the full loaded
  vertical build range.
- Optional through-wall visibility, off by default with a fair-play warning.
- Optional Mod Menu integration. The mod remains configurable with `F10` even
  when Mod Menu is not installed.

## Compatibility and scope

| Item | Target |
| --- | --- |
| Release status | Public Beta 1, user-tested on Minecraft 26.2 |
| Minecraft | 26.2 |
| Mod loader | Fabric Loader 0.19.3 or newer in the 26.2 line |
| API | Fabric API 0.154.2+26.2 or newer compatible 26.2 build |
| Java | 25 or newer |
| Server | No server-side mod required; works with Paper, vanilla, or other compatible servers |
| Optional menu | Mod Menu 20.0.1 or newer compatible 26.2 build |

The core mod does not use 1MB Bridge. An optional integration may be proposed
later, but it will not be required for the source/flow overlay.

## Controls and settings

The defaults are intentionally conservative:

| Setting | Default | Purpose |
| --- | --- | --- |
| Toggle overlay | `F9` | Show or hide the overlay |
| Temporary show | `F8`, opt-in | Show the overlay only while the key is held |
| Rescan | `Shift+F9` | Restart the loaded-chunk scan immediately |
| Open settings | `F10` | Open the built-in settings screen |
| Chunk radius | `1` | Scan a 3x3 chunk area around the player |
| Vertical range | Full height | Scan the full build height; current Y and nearby bands are available |
| Refresh mode | Automatic | Refresh every 4 seconds; block-update and manual-only modes are available |
| Hard scan budget | 12,000 blocks/tick | Bound scan work even on very fast clients |
| Adaptive time budget | 3 ms/tick | Stop incremental work early to protect frame time |
| Show sources | On | Mark source water |
| Show flowing water | On | Mark non-source water |
| Include waterlogged sources | On | Include source fluid inside waterlogged blocks |
| Through walls | Off | Disable depth-aware rendering only when server rules allow it; tooltip includes `#fairplay` |
| Labels | Off | Show the optional source/flowing legend in the HUD |
| Pulse | On | Gently animate marker emphasis |
| Source / flowing / waterlogged shapes | Box / pillar / beacon | Keep the three categories visually distinct without color |
| Waterlogged cue | Top cap | Add a redundant shape cue above waterlogged markers |
| Flowing-water level | Marker height | Represent the client-observed fluid level; surface gauge and combined modes are available |
| Nearest marker | Off | Optionally add HUD direction/distance, a world highlight, or both |
| Scan boundary | Off | Optionally frame the current chunk or loaded, skipped, and pending scan chunks without requesting them |
| Palette | Default | Select presets or edit and retain a custom RGB palette |
| Opacity | 82% | Slide left or right to control marker transparency |
| Outline thickness | 2 | Slide left or right to control marker border thickness, from 1 to 6 |
| Render distance / fade | 192 blocks / 75% | Bound marker rendering and fade toward the configured limit |
| Discovery limit | 4096 | Retain between 512 and 16,384 markers per completed scan |
| Visible limit | 4096 | Draw an independently bounded subset per frame |
| HUD | Top center, 100% | Configure anchor, offsets, scale, background opacity, and compact mode; compact mode includes visible/found/skipped/stale state |
| Narration / diagnostics | Off / Off | Opt into scan-completion narration or detailed local performance counters |
| Local profile | Custom defaults | Apply accessibility, exploration, or low-performance presets |

The built-in screen separates Scanning, Markers, Features, HUD, and Profiles.
All visible settings include explanatory tooltips. Minecraft's Controls menu
can change the key bindings. The built-in screen is available without Mod
Menu; when Mod Menu is present, its config button opens the same screen.

The JSON fallback is stored in the normal client config directory as
`config/water-source-mod.json`. JSON is intentionally simple and has no
comments; the in-game tooltips and this README are the explanations for each
setting. If the file is malformed, the mod logs a concise warning, moves the
original alongside it as `water-source-mod.json.invalid-<unique>.bak`, and
starts with safe defaults.

Beta 1 configuration version 1 is migrated explicitly to version 2. Its single
marker shape is copied to all three category shapes and its marker limit is
copied to both new limits, preserving the existing appearance. The Profiles
page exports and imports `config/water-source-mod-export.json`; malformed
imports are rejected without moving or changing that file.

## Installation

1. Install Fabric Loader for Minecraft 26.2 and Java 25.
2. Install a compatible Fabric API build.
3. Optionally install Mod Menu for convenient menu access.
4. Download `1MB-WaterSource-v1.0.0-012.jar` from the
   [Beta 1 release](https://github.com/mrfdev/WaterSourceMod/releases/tag/v1.0.0-beta.1)
   and put it in the client `mods/` folder.
5. Do not install this JAR on the Paper server.
6. Start the client, press `F9` when needed, and use `F10` to adjust options.

For the author's local client, the test copy is placed in:
`/Users/floris/Library/Application Support/minecraft/mods/`.

## Build from source

This repository targets Java 25, Minecraft 26.2, and Fabric Loom 1.17. The
included Gradle wrapper is the reproducible entry point:

```sh
./gradlew build
```

The client JAR is written to:

```text
build/libs/1MB-WaterSource-v1.0.0-<build>.jar
```

To run the pure configuration tests:

```sh
./gradlew test
```

If more than one JDK is installed, set `JAVA25_HOME` to a Java 25 home before
building. The build also defaults to the standard Java 25 installation path
used on the author's Mac.

## Build numbering and local releases

The version has two parts in the filename:

`1MB-WaterSource-v<mod_version>-<build_number>.jar`

For a new build, first move the prior local JAR from `releases/` into
`releases/archive/`, increment `build_number` in `gradle.properties`, build,
and copy the new JAR to the client `mods/` folder after removing only the
older WaterSource JAR. Build numbers start at `001` and increase to `002`,
`003`, and so on.

Release JARs are ignored by Git intentionally and are never committed to the
source tree. Source, documentation, and build metadata are public. Explicitly
approved public binaries, beginning with Beta 1 build `012`, are attached to
GitHub Releases as downloadable assets.

## Privacy and security

- No telemetry, analytics, account identifiers, network requests, or external
  configuration downloads.
- No server-side component and no custom server protocol.
- No filesystem access outside the normal client config directory for settings
  and the normal renderer/client state.
- No chunk requests, hidden resource packs, or attempts to evade anti-cheat.
- Through-wall visibility is disabled by default. Check every server's rules
  before enabling it.
- Issue reports should remove usernames, absolute private paths, tokens,
  world seeds, and unrelated logs.

## Credits

Created and maintained by [mrfloris](https://github.com/mrfloris).  
Vibe coded with the help of OpenAI.

## Development notes

See [CONTEXT.md](CONTEXT.md), the [architecture decisions](docs/adr/), and
[issue-tracker guidance](docs/agents/issue-tracker.md). Bug reports and
accessibility feedback are welcome through GitHub Issues.
