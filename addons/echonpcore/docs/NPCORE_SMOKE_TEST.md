# NPCore Smoke Test

Build:

```bash
gradlew.bat :echonpcore:compileJava -PechoAddonSet=all --no-configuration-cache
gradlew.bat :echonpcore:build -PechoAddonSet=all --no-configuration-cache
gradlew.bat buildEchoWorkspace -PechoAddonSet=all --no-configuration-cache
```

Manual test flow:
1. Launch the dev client.
2. Confirm `ECHO: NPCore` appears in the mod list.
3. Use the ECHO: NPCore creative tab or `/give @p echonpcore:echo_npc_spawn_egg`.
4. With `useScreenCoreNpcScreens=true` and ScreenCore loaded, spawn an ECHO NPC and right-click it.
5. Verify the ScreenCore page `echonpcore:npc_interaction` opens with name, role, faction, portrait, dialogue text, and channel rows.
6. Click a dialogue option that changes node and verify the ScreenCore page refreshes from server sync.
7. Open Trade and execute `charcoal_for_filter_patch` with 4 charcoal.
8. Open Services and execute `basic_heal` with 1 bread.
9. Toggle `useScreenCoreNpcScreens=false` and verify the classic `EchoNpcScreen` opens instead.
10. Run `/echonpcore spawn echonpcore:reclaimer_farmer`.
11. Spawn a vanilla farmer villager and verify conversion if replacement config is enabled.
12. Spawn a wandering trader and verify conversion to `roaming_scavenger`.
13. Open Terminal's addon/info surface with `echoterminal` loaded and verify NPCore metrics are listed under chapter id `npcore`.
14. With `echodatacore` loaded, trade once, restart or reload the world, and verify reduced stock is still reported.
15. With `echodatacore` loaded, use a cooldown service, restart or reload the world, and verify the cooldown remains for that player.
16. With `echomissioncore` loaded, add `requiresMission` to a test trade and verify locked/unlocked mission states are enforced server-side.
17. Run without DataCore or MissionCore and verify NPCore still opens screens, trades, and services using the in-memory fallback.

Diagnostics:

```mcfunction
/echonpcore diagnose
/echonpcore list profiles
/echonpcore smoke all
/echonpcore smoke state echonpcore:roaming_scavenger
/echonpcore smoke integrations echonpcore:roaming_scavenger
/echonpcore smoke open echonpcore:roaming_scavenger
/echonpcore convert_nearby_villagers 16
```

ScreenCore hardening checks:
1. Run `/echonpcore smoke all` and verify every bundled profile reports dialogue options, trades, services, behavior, ambient lines, and service actions.
2. Run `/echonpcore smoke state echonpcore:roaming_scavenger` and verify profile, dialogue option count, trade count, restocking trade count, service count, cooldown count, home summary, ScreenCore loaded state, and storage mode are reported.
3. Run `/echonpcore smoke open echonpcore:roaming_scavenger`.
4. Verify Refresh returns `Screen state refreshed.` in the NPCore status/footer path.
5. Verify Close, Exit, and Escape all close the interaction and clear server-side dialogue state.
6. With MissionCore loaded, add a temporary `requiresMission` to a dialogue option, trade, or service and verify the row is disabled with the denial message.

Full functionality checks:
1. Run `/echonpcore smoke open echonpcore:<profile>` for every bundled profile.
2. Verify each NPC records a home position, wanders inside `wanderRadius`, returns home after being moved outside `returnRadius`, and pauses movement while the screen is open.
3. Wait for an ambient line near the NPC and confirm it appears no more often than the profile's `ambientCooldown`.
4. Buy a limited-stock trade, verify stock decreases, wait or advance time past `restockTime`, press Refresh, and verify the stock refills.
5. Use a cooldown service and verify the row disables until the cooldown expires.
6. Use a `world_intel` service and verify the status updates; with WorldCore loaded, the message should include local region or hazard context when available.
7. Use a `discover_contact` dialogue or service action, then open Terminal's discovery/addon surfaces and verify the NPC appears as a discovered contact.
8. Use Signal Analyst's `Reveal Signal Marker` service, then refresh HoloMap and verify discovered NPC contacts are exposed by the `echonpcore:provider/map_data` provider when available.
9. With MissionCore loaded, test `requiresMission` on dialogue, trade, and service rows and verify denial text is shown without trusting the client.
10. With EchoCore faction data available, verify opening an NPC records faction contact and the screen relationship label updates from faction standing.
