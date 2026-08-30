# ADR 0004: Isolated client block-update rescan hook

- Status: Accepted
- Date: 2026-08-28

## Decision

The opt-in block-update rescan mode uses one client mixin targeting the exact
Minecraft 26.2 `ClientLevel.setBlock(BlockPos, BlockState, int, int)` method.
The hook only reports successful updates to the scanner. The scanner ignores
them unless the overlay, level, mode, scan area, and vertical range match, then
debounces them with a fixed maximum delay before requesting a replacement scan.

## Rationale

Fabric API 0.154.2+26.2 exposes client level and chunk lifecycle events but no
general event for server-originated client block changes. Polling more often
would waste client time, while omitting removal updates would leave block-driven
snapshots stale. One narrow, version-pinned hook provides the missing signal
without using raw rendering internals, network traffic, or extra chunk requests.

## Consequences

The mixin descriptor must be revalidated against official mappings and the
exact client JAR on every Minecraft update. Its required injection deliberately
fails startup instead of silently breaking block-driven refresh after a target
change. Automatic and manual modes pay only a short rejected callback, and
update bursts produce at most one delayed replacement scan per debounce window.
