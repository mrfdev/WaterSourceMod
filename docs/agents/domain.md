# Domain documentation guidance

This repository uses one bounded context. The glossary lives in the root
`CONTEXT.md`; durable design decisions live in `docs/adr/`.

Add a glossary term only when it names a stable domain concept used by more
than one part of the project. Put implementation details in code comments or
the relevant ADR, not in the glossary.

ADRs are immutable historical decisions. If a decision changes, add a new
ADR that supersedes the earlier one instead of rewriting history.
