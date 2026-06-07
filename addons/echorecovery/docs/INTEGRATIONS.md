# ECHO Recovery Integrations

## Standalone (ECHO Core + NetCore)

- Grave creation on death
- `/graves` command with list/locate/recover/delete
- Grave Key and Recovery Compass items
- Safe placement logic
- Death history tracking
- Built-in dark UI fallback

## ThemeCore

When ThemeCore is installed:
- Grave UI uses theme tokens
- Grave blocks can be reskinned per theme
- Supported themes: Default Dark, Vanilla, Tech, Magic, Skyblock, RPG, Horror, Ashfall

## Terminal

When Terminal is installed:
- Registers Graves page
- Registers Recovery page
- Registers Death History page
- Admin recovery page for operators

## Ashfall Protocol

When Ashfall is installed:
- Graves become Field Recovery Caches
- Recovery Compass becomes Field Locator
- Adds Signal Integrity and Contamination mechanics
- Storms can jam signals
- Toxic/radiation zones can contaminate caches
- Convoy and Logistics can extract field caches

## Optional Module Hooks

| Module | Behavior |
|--------|----------|
| HoloMap | Grave markers, death markers, recovery routes |
| MissionCore | recover_first_grave, recover_team_grave, etc. |
| TutorialCore | "You died. Your items are safe in a grave." |
| SoundCore | Grave created/opened/recovered sounds |
| WorldCore | Hazard-aware placement, danger-zone warnings |
| WeatherCore | Storms hide markers, toxic rain contaminates |
| Lens | Scan graves for condition, contamination, risk |
| Armory | Preserve weapon modules, ammo, augments |
| Logistics | Remote grave delivery to depot |
| Convoy | Convoy recovery missions |
| PowerGrid | Powered recovery beacons |
| RelicTech | Soul-bound relic handling |
| Nexus | Optional corruption/false markers |
| Blackbox | Death evidence records |
