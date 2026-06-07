# SignalOS 1.2.x Full Integration Notes

SignalOS now targets the complete ECHO-stack computer OS role: terminals and workstations anchor operator networks, racks and relays form the local machine graph, drives carry bounded portable records, and Echo Link mirrors optional ECHO Core services without replacing ECHO Terminal.

## Highlights

- Expands built-in SignalOS datapack content from a single starter mission into an onboarding chain for booting the shell, writing notes, bringing rack storage online, applying templates, copying records, and verifying Echo Link.
- Adds archive and data records that document the terminal/workstation, server rack, relay, drive-template, pack-authoring, and Echo Link workflows in field-canon voice.
- Adds multiple shipped drive templates: blank field drive, diagnostics drive, field drive, and operator handoff drive.
- Exposes first-class network peripherals for terminals, workstations, server racks, and relays in the server-owned network snapshot.
- Extends Echo Link records to include profile, hazards, chapter capability, discovery, faction, ThemeCore, and SoundCore status when those providers are available.
- Adds additive Java APIs for server-side app actions with `SignalOsAppContext` and public drive-template lookup.
- Adds additive provider health metadata through `SignalOsProviderStatus` and `SignalOsApi.providerStatuses(player)`.
- Hardens rack actions with payload, identifier, template, slot, menu, position, and selected-drive validation.
- Invalidates network snapshots after note, preference, mission-claim, archive-read, rack, relay, terminal, and reload mutations.
- Adds `needs_iron_tool` block tag coverage for SignalOS machine blocks.
- Adds shared JSON schemas for SignalOS app, record, drive-template, chapter, mission, and archive content.

## Verification Status

- `.\gradlew.bat :echosignalos:compileJava --warning-mode all` passes.
- `.\gradlew.bat :echosignalos:build --warning-mode all` passes.
- `.\gradlew.bat :echosignalos:runGameTestServer --warning-mode all` passes.
- `.\gradlew.bat :signalosexample:build --warning-mode all` passes.
- `python tools\validate_resources.py --addon-set beta` passes.

## Manual Smoke Checklist

1. Place `signalos:terminal` and `signalos:workstation`; open the desktop shell from each.
2. Open every built-in app: Home, Files, Notes, Logs, Network Monitor, Settings, Data Vault, Echo Link, Missions, Archives, Rewards, and Diagnostics.
3. Create, edit, delete, and clear a note; reopen SignalOS and confirm persistence.
4. Place a server rack, network relay, and data drive; insert the drive, open the rack screen, apply a template, copy a network record, rename, clear, remove records, and eject the drive.
5. Confirm Network Monitor shows rack and relay peripherals.
6. Confirm Echo Link still works when optional providers are absent and shows richer records when ECHO providers are present.
7. Run `/reload` and confirm JSON apps, data records, drive templates, chapters, missions, and archives remain merged.

## Notes

- SignalOS still uses one active app at a time rather than draggable window management.
- SignalOS remains separate from `echoterminal`; it integrates through Echo Core and shared service contracts.
- KubeJS support remains a soft `Java.loadClass` bridge with `kubejs.classfilter.txt`, not a hard KubeJS plugin.
