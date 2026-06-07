# ECHO Industrial Nexus Multiblock Smoke Test

Run from the repository root with Java 25:

```powershell
$env:JAVA_HOME='C:\Users\knox\.jdks\temurin-25'
python tools\validate_resources.py --addon-set all
.\gradlew.bat :echomultiblockcore:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echorendercore:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echoterminal:compileJava :echolens:compileJava :echoholomap:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echoindustrialnexus:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echologisticsnetwork:compileJava --no-daemon --no-configuration-cache
.\gradlew.bat :echoindustrialnexus:build --no-daemon --no-configuration-cache
.\gradlew.bat :echoindustrialnexus:runGameTestServer --no-daemon --no-configuration-cache
.\gradlew.bat :echoindustrialnexus:runIndustrialClient --no-daemon --no-configuration-cache
```

Manual client checklist:

- Verify the `ECHO: Industrial Nexus` creative tab includes controllers, buses, casings, crates, robotics, tool heads, blueprints, upgrade chips, and Industrial materials.
- Place each controller and sneak-use or use the Factory Diagnostic Tool to confirm incomplete diagnostics are readable.
- Build the Industrial Assembly Line from `data/echoindustrialnexus/echo_multiblocks/industrial_assembly_line.json`.
- Install an Industrial Welder Head into the Robotic Arm Mount.
- Put 4 Refined Plates, 1 Servo Motor, and 1 Industrial Circuit in the Input Depot Crate.
- Right-click the formed Industrial Assembly Line Controller and confirm the Factory Command GUI opens.
- Press 1 on Weld Reinforced Machine Frame and confirm inputs are consumed, the arm animates, progress updates, and a Reinforced Machine Frame appears in the Output Depot Crate.
- Restock enough ingredients for three runs, press 3, and confirm the task queue advances tasks in order.
- Restock enough ingredients for five runs, press 5, and confirm the queue caps at the visible capacity instead of overfilling.
- Press CLEAR with a queued task and confirm the queue clears.
- Create a blocked task, press RETRY, and confirm the blocked reason clears while already-consumed inputs are not duplicated.
- Press REVALIDATE and confirm the structure status remains online.
- Open ECHO Terminal, select Industrial Nexus, confirm Factory Command appears before Missions, press REFRESH, select the Assembly Line, queue a task remotely, toggle Logistics auto-restock, set the target to x1/x3/x5, then clear/retry/revalidate from Terminal.
- With ECHO Lens installed, Deep Scan the Assembly Line Controller, Robotic Arm Mount, Input Depot Crate, and Output Depot Crate; verify status, alert, tool, heat, queue, and safe inventory summaries.
- With ECHO HoloMap installed, sync the map and verify the Assembly Line marker appears on the Multiblocks layer with alert, integrity, and restock summary text.
- With ECHO Logistics Network installed, connect a Logistics network, Drone Delivery Dock, stocked storage, and Smart Storage Label to an Input Depot Crate, then press REQ for a recipe and confirm a courier dispatches to the depot.
- Enable AUTO on the controller, set target x3, select the matching Industrial loadout on an Auto-Restock Station, and confirm Logistics dispatches only when the Input Depot Crate falls below the minimum run threshold.
- Build the Recipe Matrix Core, encode a Recipe Matrix Shard, and confirm the `recipe_matrix_encoding` Terminal mission completes.
- Craft the Nexus Furnace Array Controller and Nexus Furnace Array Blueprint.
- Build the Nexus Furnace Array from `data/echoindustrialnexus/echo_multiblocks/nexus_furnace_array.json`.
- Queue Stabilize Hybrid Thermal Core with the required inputs and no tool head installed; confirm the task blocks and inputs remain in the Input Depot Crate.
- Install a Coolant Injector Head or Inspection Scanner Head, retry Stabilize Hybrid Thermal Core, and confirm the Output Depot Crate receives a Hybrid Thermal Core and Rad Slag.
- Queue Forge Core Key Assembly and confirm the Output Depot Crate receives a Core Key Assembly and Protocol Extractor Coil.
- Confirm the `nexus_furnace_array` Terminal mission becomes claimable after Forge Core Key Assembly completes.
- With ECHO Nexus Protocol absent, repeat the two Nexus Furnace Array tasks and confirm the soft pressure effect does not fail the queue.
- With ECHO Logistics Network present, verify the Nexus Array recipes expose their matching Logistics loadout requests.
- Try the same task with the tool head removed and confirm the diagnostic says a robotic tool is missing.
- Remove a required Reinforced Machine Casing and confirm the structure becomes incomplete or damaged.
- Replace the casing, revalidate the controller, and confirm the facility returns online.
- Save and reload the world; formed state should safely persist or revalidate.
- Launch a dedicated GameTest server to catch client-only classloading mistakes.

Known 1.3.0 validation notes:

- Industrial multiblock upgrades are installed on formed controllers through Industrial upgrade items. Speed, efficiency, cooling, overclock, emergency shutdown, and Factory Link effects are active through MultiblockCore runtime modifiers.
- Logistics Network request and auto-restock routing are optional and only appear when ECHO Logistics Network is present; Factory Command shows controls and status, while Logistics owns route discovery, stock checks, in-flight caps, and delivery.
- Publish this as part of the next public stack minor release, not as an addon-only public 1.3.0.
