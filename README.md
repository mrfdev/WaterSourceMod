# 1MB Water Source

1MB Water Source is a client-only Fabric accessibility mod for Minecraft
26.2. It makes water source blocks easier to distinguish from flowing water
when the overlay is useful, while staying completely off during ordinary play
by default.

Repository: [github.com/mrfdev/WaterSourceMod](https://github.com/mrfdev/WaterSourceMod)  
Issues: [github.com/mrfdev/WaterSourceMod/issues](https://github.com/mrfdev/WaterSourceMod/issues)  
License: MIT

## Features

- Toggle the visualization with `F9` (rebindable in Minecraft Controls).
- Force a fresh scan with `Shift+F9` (also rebindable through the two key
  mappings).
- Default scan radius is 1 chunk, covering the current chunk plus one chunk
  in every direction, or 3x3 chunks.
- Configure a larger radius, including radius 2 for 5x5 chunks.
- Use distinct high-contrast source and flowing-water marker colors.
- Choose a marker style, slider-controlled opacity and outline thickness, pulse
  animation, and optional HUD labels/legend.
- Keep the optional status HUD at the top center so it avoids common corner overlays.
- Include or exclude waterlogged source blocks.
- Scan incrementally, only in client-loaded chunks, across the full loaded
  vertical build range.
- Optional through-wall visibility, off by default with a fair-play warning.
- Optional Mod Menu integration. The mod remains configurable with `F10` even
  when Mod Menu is not installed.

## Compatibility and scope

| Item | Target |
| --- | --- |
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
| Rescan | `Shift+F9` | Restart the loaded-chunk scan immediately |
| Open settings | `F10` | Open the built-in settings screen |
| Chunk radius | `1` | Scan a 3x3 chunk area around the player |
| Show sources | On | Mark source water |
| Show flowing water | On | Mark non-source water |
| Include waterlogged sources | On | Include source fluid inside waterlogged blocks |
| Through walls | Off | Disable depth-aware rendering only when server rules allow it; tooltip includes `#fairplay` |
| Labels | Off | Show the optional source/flowing legend in the HUD |
| Pulse | On | Gently animate marker emphasis |
| Opacity | 82% | Slide left or right to control marker transparency |
| Outline thickness | 2 | Slide left or right to control marker border thickness, from 1 to 6 |
| Marker limit | 4096 | Slide left or right to retain between 512 and 16,384 markers per scan |

All visible settings include explanatory tooltips. Minecraft's Controls menu
can change the key bindings. The built-in screen is available without Mod
Menu; when Mod Menu is present, its config button opens the same screen.

The JSON fallback is stored in the normal client config directory as
`config/water-source-mod.json`. JSON is intentionally simple and has no
comments; the in-game tooltips and this README are the explanations for each
setting.

## Installation

1. Install Fabric Loader for Minecraft 26.2 and Java 25.
2. Install a compatible Fabric API build.
3. Optionally install Mod Menu for convenient menu access.
4. Put the latest `1MB-WaterSource-v1.0.0-<build>.jar` in the client `mods/` folder.
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

Release JARs are ignored by Git intentionally. Source, documentation, and
build metadata are public; local binaries are kept out of the repository until
the maintainer explicitly decides to publish a beta/release asset.

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
