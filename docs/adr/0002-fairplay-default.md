# ADR 0002: Fair-play visibility default

- Status: Accepted
- Date: 2026-08-28

## Decision

Through-wall marker rendering is disabled by default. A setting may enable it,
but its tooltip explicitly warns the player to make sure the server allows
that visibility, with the `#fairplay` reminder.

## Rationale

Showing markers through terrain can resemble an x-ray aid and may be banned by
server rules. The mod cannot reliably determine a server's policy, so the
safe default is depth-aware rendering while still allowing an informed player
to opt in where it is permitted.

## Consequences

The player is responsible for checking the rules of each server before
enabling the option. The mod does not attempt to bypass anti-cheat systems,
hide the setting, or infer permission from server responses.
