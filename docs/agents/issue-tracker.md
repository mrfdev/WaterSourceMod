# Issue tracker guidance

## System

GitHub Issues is the project issue tracker:

- Repository: `https://github.com/mrfdev/WaterSourceMod`
- Issues: `https://github.com/mrfdev/WaterSourceMod/issues`
- Current public test release: `v1.0.0 Beta 1`, build `012`, tested on
  Minecraft 26.2

Use the `gh` CLI for issue operations when it is available and authenticated.
The issue tracker is for implementation work, bugs, accessibility feedback,
compatibility reports, and release follow-up.

## Workflow

1. Search existing open issues before filing a duplicate.
2. For a bug, include Minecraft version, Fabric Loader version, Fabric API
   version, mod version/build, relevant configuration, reproduction steps, and
   logs with private paths or account information removed.
3. For accessibility feedback, describe the visual problem and the desired
   adjustment without attaching private world data unless it is essential.
4. Link a pull request to the issue and close it only when the behavior is
   verified.

Pull requests are the review surface. GitHub Projects, labels, and milestones
are optional organization aids, not a second source of truth.
