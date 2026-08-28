# WaterSourceMod contributor notes

WaterSourceMod is a client-only Fabric mod for Minecraft 26.2. It must not
require installation on a Paper, Bukkit, or vanilla server. Keep the core
mod independent of 1MB Bridge; a future optional integration must be isolated
behind an optional dependency and must not change the core behavior.

Use Java 25, official Minecraft mappings, Fabric Loader 0.19.3, and the
project-pinned Fabric API version unless a version update is deliberately
reviewed. Prefer supported Fabric and Minecraft client APIs over internals.
Minecraft 26.2 rendering must use Blaze3D/Fabric extraction and rendering
hooks, never raw OpenGL calls.

The default overlay is off, client-side scanning is limited to currently
loaded chunks, through-wall rendering is off, and no telemetry or network
access is permitted. Preserve these privacy and fair-play defaults.

Do not commit JAR files, local configuration, credentials, or generated build
output. Before a later build, archive the prior local release JAR under
`releases/archive/`, increment `build_number`, and keep the exact artifact
naming convention documented in the README.

Track work in GitHub Issues. Keep the issue tracker and domain guidance in
`docs/agents/`, and record durable architectural decisions in `docs/adr/`.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues and are managed with the `gh` CLI. See
`docs/agents/issue-tracker.md`.

### Domain docs

This is a single-context repository using root `CONTEXT.md` and `docs/adr/`.
See `docs/agents/domain.md`.
