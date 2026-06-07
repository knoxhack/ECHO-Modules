# ECHO: GalacticCore

ECHO: GalacticCore is a faithful ECHO Platform port and modernization of Galacticraft Legacy by TeamGalacticraft, licensed under MIT. It preserves the classic Galacticraft-style space exploration experience while integrating with ECHO Core, PackOS, Index, Lens, HoloMap, ScreenCore, and Ashfall systems.

Best honest label: Unofficial ECHO Platform port/fork of Galacticraft Legacy.

This project is not affiliated with, endorsed by, or presented as a replacement for the maintained Galacticraft project.

## Native SDK Direction

GalacticCore is being rebuilt as an ASDK-native ECHO addon:

- Production code compiles against `echo-native-contracts`, `echoaddonapi`, and `echoadaptercore`.
- `echo-native-testkit` is used only for tests.
- Production code must not depend on `echo-native-loader`, NeoForge, Forge, or legacy `activateNative(Map)` surfaces.
- Runtime changes must be represented by typed ASDK service calls and `EchoNativeMutationReceipt` evidence.

## Current Port Foundation

The original 1.12.2 Forge/Galacticraft Legacy source remains in this repository as reference material while the native ECHO implementation is built under `com.knoxhack.echogalacticcore`.

Implemented foundation pieces:

- ASDK-native build wiring.
- ECHO module descriptor.
- Native entrypoint.
- Typed service registration plan.
- Data-first content declaration scaffolds.
- Resource migration scaffolds under the `echogalacticcore` namespace.
- Parity, legal, and release-gate documentation.
- ASDK testkit coverage for typed mutation receipts.

## Attribution

See `CREDITS.md` and `LICENSE`.
