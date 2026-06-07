# ECHO PowerGrid Smoke Test

## Build
1. Run `gradlew.bat :echopowergrid:compileJava :echomultiblockcore:compileJava :echoindustrialnexus:compileJava`.
2. Run `gradlew.bat :echopowergrid:runGameTestServer` when the dev runtime has no optional-mod ordering cycle.
3. Finish with `gradlew.bat buildEchoWorkspace -PechoAddonSet=all` before release promotion.

## Dev Client
1. Launch dev client.
2. Open the **ECHO: PowerGrid** creative tab.
3. Verify all blocks and items are present with names and icons, including Medium Battery Bank, Industrial Battery Bank, Relay Substation, and Nexus Stabilizer Coupler.

## Basic Grid
1. Place a **Scrap Burner Generator**.
2. Place a **Small Battery Bank** adjacent or connect with **Low Voltage Cable**.
3. Place a **Test Power Consumer** on the network.
4. Right-click the generator to open the PowerGrid screen.
5. Put coal, charcoal, planks, or sticks into the generator fuel slot.
6. Confirm the generator screen shows burn time and buffered EP.
7. Wait a few seconds.
8. Right-click the battery and confirm stored EP increases.
9. Right-click the consumer and confirm it reports powered status.

## Network Diagnostics
1. Place a **Power Meter** on the network.
2. Right-click it to open the PowerGrid dashboard with generation, demand, stored EP, state, quality, nodes, and transfer limit.
3. Use the **Grid Diagnostic Tool** on the generator (local node info).
4. Sneak-use the diagnostic tool on the generator (network info).
5. Use the **Refresh** button on a PowerGrid screen and confirm the screen remains live.
6. Run `/echo_power networks`, `/echo_power alerts`, and `/echo_power route <from> <to>` to confirm route/loss diagnostics.

## Substation Policy
1. Place an **Outpost Substation** or **Relay Substation** on a loaded network.
2. Right-click to open the substation screen and confirm policy/state/node data is visible.
3. Sneak-right-click to cycle policy and confirm the selected policy persists after save/load.

## Breaker Screen
1. Create an overload/tripped breaker scenario or use commands/dev setup to place a tripped **Emergency Breaker**.
2. Right-click the breaker to open its PowerGrid screen.
3. Confirm the screen reports the tripped state.
4. Click **Reset** and confirm the breaker returns to nominal.

## Cable Disconnect
1. Break a cable between generator and battery.
2. Confirm battery stops charging.
3. Replace the cable.
4. Confirm battery resumes charging.

## Persistence
1. Save and quit the world.
2. Re-enter the world.
3. Confirm generator fuel slot contents, generator buffer, and battery energy persisted.

## Commands
1. Run `/echo_power inspect` while standing on the battery.
2. Run `/echo_power status`.
3. Run `/echo_power give_test_kit` (creative/op).
4. Run `/echo_power reset_network`.
5. Run `/echo_power set_energy <amount>` while targeting a battery and confirm it can both raise and lower stored EP.

## Terminal And HoloMap
1. Install Terminal alongside PowerGrid.
2. Open the Power Grid tab.
3. Confirm loaded networks, selected-network detail, state/quality colors, and Refresh sync.
4. Install HoloMap alongside PowerGrid.
5. Confirm the `Power Networks` layer shows one marker per loaded network and alert markers when a breaker trips or a brownout/overload occurs.

## Multiblock Integration
1. Install MultiblockCore and Industrial Nexus alongside PowerGrid.
2. Build a Circuit Fabricator, Recipe Matrix Core, or Nexus Furnace Array with an `Industrial Power Bus`.
3. Queue a powered task without grid reserve and confirm it blocks with a `Power-starved` reason.
4. Connect a battery/generator network and confirm the same task advances while drawing EP.

## Dedicated Server
1. Launch dedicated server.
2. Join with a client.
3. Repeat basic grid steps.
4. Verify no client-only class crashes.

## Acceptance
- All blocks place and break correctly.
- No missing textures (purple/black squares).
- Energy flows from generator -> cable -> battery -> consumer.
- PowerGrid block screens show meaningful live data.
- Substations expose persistent policy state.
- Route diagnostics expose path transfer and loss.
- Scrap Burner fuel slot consumes fuel and generates buffered EP.
- Emergency Breaker reset works from the screen.
- Commands execute without errors.
- Save/load preserves energy.
- Terminal and HoloMap surfaces sync loaded networks.
- Powered multiblock tasks block clearly when EP is unavailable and advance when supplied.
