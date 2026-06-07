# ECHO Presence Link

`echopresencelink` is the client-side Discord Rich Presence addon for the ECHO/Ashfall stack.

Presence Link publishes privacy-safe local activity over Discord RPC IPC. It does not post Discord bot messages, does not use Community Bridge bot tokens or REST relay code, and does not own gameplay state. Providers read public ECHO services and optional Ashfall client state, then choose one concise activity line by priority.

## Defaults

- `enabled=true`
- `privacyMode=true`
- `updateIntervalSeconds=15`
- `showButtons=true`
- `includeWorldName=false`
- `includeServerName=false`
- `includeCoordinates=false`
- No join, spectate, party, server address, world name, coordinate, player-name, or secret sharing in v1.

If `discord.applicationId` is blank, the addon stays quiet and records debug/diagnostic status. If Discord desktop is configured but rejects or cannot receive activity, Presence Link records the IPC response and logs the first configured failure at info level, then suppresses repeated noise.

## Troubleshooting

- Check the client log for `ECHO: Presence Link registered.` and `ECHO Presence Link client IPC controller online.` to confirm the addon loaded and the client ticker registered.
- Use `/echopresence status` in-game to see ticking state, masked application id, config path, selected provider, IPC endpoint, last Discord response, and last failure.
- Use `/echopresence test` to send a minimal no-asset Rich Presence payload before the Discord Developer Portal assets are uploaded.
- Use `/echopresence resend` after changing Discord settings or uploading assets to bypass duplicate suppression and send the current provider payload again.

## Priority

1. Live boss or Nexus warfront.
2. ECHO Terminal, archive, or mission-review screen.
3. Active environmental event or hazard.
4. Active mission phase.
5. World region or faction context.
6. Generic ECHO idle.

## ECHO Ecosystem Integration

- `echocore` is required for pack mode, diagnostics, mission, faction, world, hazard, and Nexus campaign service reads.
- `echoashfallprotocol` optionally registers richer Ashfall client context when Presence Link is loaded.
- `echoterminal` is observed as screen state only; Terminal continues to own presentation.
- `signalos` receives diagnostics for IPC state, current provider, last update age, and last failure.
- `echocommunitybridge` may contribute a public Discord invite button only. Presence Link never reads bot tokens or relay queues.

## Discord Assets

Generated source assets live under `../../../art_sources/echopresencelink/discord/`.

Release-ready copies live under `src/main/resources/assets/echopresencelink/textures/presence/`.

Upload the lowercase keys listed in `docs/discord-assets.md` to the Discord Developer Portal for the **ECHO Presence Link** application before release.

## Verification

```bash
gradlew.bat :echopresencelink:compileJava
gradlew.bat :echopresencelink:runGameTestServer
gradlew.bat buildEchoWorkspace -PechoAddonSet=all
```
