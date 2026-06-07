# Agriculture Reclamation 1.3.1

## Completion Update

- Set module metadata to `1.3.1` and public branding to Reclamation by ECHO Labs.
- Added standalone `/reclamation` and `/agriculturereclamation` command aliases.
- Added common config for growth, hydroponics, greenhouse scoring, pollinators, restoration thresholds, WeatherCore penalties, and PowerGrid acceleration.
- Added Hydroponic Tray menu and screen with seed, nutrient, output, growth, greenhouse, stability, contamination, and status readouts.
- Added direct MissionCore registration for Field Reclamation / Ecology Recovery objectives.
- Added Logistics external endpoint support and Reclamation nutrient/seed stock categories.
- Kept Ashfall, Terminal, ThemeCore, WeatherCore, PowerGrid, SoundCore, HoloMap, TutorialCore, Index, and Logistics optional.
- Added docs for setup, config, integrations, and 1.3.1 release notes.
- Added GPT Image 2 prompt manifest for release-critical texture refresh targets.

## Verification Targets

- `gradlew.bat :echoagriculturereclamation:compileJava -PechoAddonSet=all`
- `gradlew.bat :echoagriculturereclamation:build -PechoAddonSet=beta`
- Full `-PechoAddonSet=all` build after unrelated Index compile blockers are cleared.
