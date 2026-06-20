# ECHO Runtime Parity Fix Backlog

Generated: 2026-06-19T22:04:54.529Z

## P0

No items.

## P1

### ECHO-Modules

#### RPA-001 - Regenerate module docs index from the full descriptor inventory

- Subsystem: docs index
- Summary: Docs index drift detected: 1 missing id(s), 1 missing directorie(s), 0 extra entrie(s).
- Modules (1): echodeepreachprotocol
- Recommended fix: Update the docs index generator/source data so every descriptor appears exactly once.

## P2

### ECHO-Modules

#### RPA-002 - Promote runtime parity audit into release workflow documentation

- Subsystem: audit polish
- Summary: The generator is intentionally separate from release mutation until the first backlog is triaged.
- Recommended fix: After the P0/P1 items are understood, decide whether --strict should become a release workflow gate.

