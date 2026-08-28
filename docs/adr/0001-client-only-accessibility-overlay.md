# ADR 0001: Client-only accessibility overlay

- Status: Accepted
- Date: 2026-08-28

## Decision

WaterSourceMod is a client-only Fabric mod. It observes water states in chunks
already loaded by the client and renders an optional local overlay. It does
not add a server plugin, send custom packets, request extra chunks, or depend
on 1MB Bridge in the core release.

## Rationale

The requested Paper 26.2 server should remain unchanged, and the feature is a
visual accessibility aid rather than a gameplay mechanic. Client-observed
state is enough to distinguish source and flowing water in visible loaded
chunks while preserving compatibility with vanilla, Paper, and other servers.

## Consequences

The overlay cannot identify water in chunks the client has not loaded. Scan
work is incremental to avoid a large frame-time spike, and the radius and
marker cap are configurable. Future Bridge support must remain optional and
must not become a hidden data channel.
