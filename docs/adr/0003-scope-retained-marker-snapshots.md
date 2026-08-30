# ADR 0003: Scope retained marker snapshots

- Status: Accepted
- Date: 2026-08-28

## Decision

A completed marker snapshot may remain visible while another scan refreshes
the same client level, scan-center chunk, horizontal radius, and vertical
range. Every scan
captures the snapshot generation in which it starts, and its result is
published only if that generation is still current. Changing level, scan
center, horizontal radius, or vertical range advances the generation and
clears the snapshot. Unloading
an in-range client chunk advances the generation and immediately removes that
chunk's markers before another scan begins.

## Rationale

Clearing markers at the start of every incremental refresh causes visible
blinking, but retaining results without a validity boundary can render marker
positions from a previous world or from chunks the client no longer holds.
Generation-scoped publication preserves refresh continuity while enforcing the
client-only, currently-loaded-state boundary from ADR 0001.

## Consequences

Manual and automatic refreshes do not blank an otherwise valid overlay.
Invalidated in-flight work cannot republish stale markers even if cancellation
timing changes later. Snapshot invalidation must remain connected to client
level changes, scan-area changes, and in-range chunk unloads.
