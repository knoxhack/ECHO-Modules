# Industrial Nexus 1.2.0 Release Notes

Industrial Nexus 1.2.0 promotes the Nexus Furnace Array from future-facing data into a playable late-game factory route. Players can now craft the dedicated controller and blueprint, form the array through MultiblockCore, stabilize Hybrid Thermal Cores, and forge Core Key Assemblies through data-driven robotic tasks.

The route remains optional-stack friendly. Industrial Nexus plays without Nexus Protocol; when Nexus Protocol is installed, the new `echoindustrialnexus:nexus_array_pressure` effect records extra Nexus thermal pressure through the existing soft compat hook. Logistics Network support is also optional and adds loadout mappings for both Nexus Array tasks when present.

This release belongs with the next public ECHO stack minor release. Do not publish Industrial Nexus as a standalone public 1.2.0 unless the stack release process is changed deliberately.

Validation target:

```powershell
python tools\validate_resources.py --addon-set all
.\gradlew.bat :echomultiblockcore:compileJava :echoindustrialnexus:compileJava :echologisticsnetwork:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echoindustrialnexus:build :echoindustrialnexus:runGameTestServer --no-daemon --no-configuration-cache
```
