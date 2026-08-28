# Changelog

Notable user-facing changes are recorded here. Build numbers are never reused,
including when a build is rejected before publication.

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
