<!-- CURSEFORGE_README_START -->
# Community Bridge by ECHO Labs

![Community Bridge by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/brand-sheet.png)

**echocommunitybridge runs inside the official Minecraft server process.**

![Community Bridge by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/features-portrait.png)

![Community Bridge by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/features-landscape.png)

## CurseForge Summary

echocommunitybridge runs inside the official Minecraft server process.

## Main Features

- Official Minecraft server status, Discord relay, and launcher bridge for the ECHO/Ashfall ecosystem.
- Community Bridge publishing-ready addon presentation.
- Built for the shared modular stack.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echocommunitybridge/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO Community Bridge

`echocommunitybridge` runs inside the official Minecraft server process. It exposes a small public status endpoint for ECHO Launcher, relays Minecraft chat/events to Discord and ECHO Chat, and can listen to Discord chat through the Discord Gateway for all-way official server chat.

## Operator Setup

1. Add the addon to the official server mod set with its required dependencies:
   - `echocore`
   - `echonetcore`
2. Optional integration:
   - `signalos`
3. Start the server once so NeoForge writes the config file.
4. Configure the bridge values, then run `/echobridge reload` from an operator account.
5. Point the launcher setting `officialServerStatusUrl` at the public status URL.

## Defaults

The release defaults are intentionally local-first:

| Setting | Default | Notes |
| --- | --- | --- |
| `general.enabled` | `true` | Master bridge switch. |
| `server.serverId` | `official-ashfall` | Stable public id. |
| `server.serverName` | `ECHO Ashfall Official` | Public display name returned in JSON. |
| `server.serverMotd` | `Survive. Adapt. Endure.` | Launcher tagline. |
| `public_status.enabled` | `true` | Enables the HTTP JSON endpoint. |
| `public_status.host` | `127.0.0.1` | Use `0.0.0.0` only when the endpoint should bind publicly. |
| `public_status.port` | `47870` | Internal mod bind port. Public players should use the forwarded status port, currently `16363`. |
| `public_status.path` | `/status.json` | V1 public JSON contract path. |
| `public_status.corsOrigin` | `*` | Allows browser preview and desktop launcher fetches. |
| `privacy.showPlayerNames` | `true` | Set to `false` to hide public player names. |
| `privacy.recentEventLimit` | `12` | Public recent event ring buffer size. |
| `discord.enabled` | `false` | Discord posting is opt-in. |
| `discord.inviteUrl` | blank | Exposed publicly only when configured. |
| `relay.minecraftChat` | `true` | Posts Minecraft chat when Discord is enabled and configured. |
| `relay.discordChat` | `false` | Listens for Discord chat and relays it into Minecraft and ECHO Chat. Requires Message Content Intent. |
| `relay.joinLeave` | `true` | Posts player join/leave events. |
| `relay.serverLifecycle` | `true` | Posts server start/stop events. |
| `launcher_chat.enabled` | `true` | Enables the mod-hosted launcher/Android official chat API. |
| `launcher_chat.channelId` | `server-ashfall` | Launcher community channel mapped to this server. |
| `launcher_chat.historyLimit` | `200` | In-memory official chat history retained for launcher/Android. |
| `launcher_chat.allowLauncher` | `true` | Allows launcher clients to post official chat. |
| `launcher_chat.allowAndroid` | `true` | Allows Android clients to post official chat. |
| `discord.postCooldownMs` | `1000` | Minimum delay between Discord REST posts. |
| `discord.maxQueueSize` | `256` | Queue limit before events are dropped. |

With defaults, the local status URL is:

```powershell
curl.exe http://127.0.0.1:47870/status.json
```

The deployed official player-facing base URL is `http://64.74.111.235:16363`. Launcher and Android chat must use that same public status base, not the internal bind port.

The health probe is:

```powershell
curl.exe http://127.0.0.1:47870/health
```

## Status JSON

`GET /status.json` returns schema version `1`. The response includes public server identity, online state, player count, optional player names, Discord invite metadata, version strings, recent sanitized public events, and `lastUpdated`.

The bridge sanitizes public chat/event text, truncates long values, strips unsafe player-name characters, and breaks Discord mass mentions. The status response never includes the Discord bot token or token config field names.

## Discord Setup

Discord posting uses REST API v10. Discord inbound chat uses the Discord Gateway when `relay.discordChat=true`.

1. Create a Discord application and bot.
2. Invite the bot to the official server with permission to post in the target channels.
3. Enable Message Content Intent in the Discord Developer Portal if inbound Discord chat should relay to Minecraft, launcher, and Android.
4. Set:
   - `discord.enabled=true`
   - `discord.statusChannelId=<channel id>` for join/leave and lifecycle messages
   - `discord.chatChannelId=<channel id>` for Minecraft chat relay
   - `discord.inviteUrl=https://discord.gg/...` if the launcher should show `Join Discord`
   - `relay.discordChat=true` only after any old Discord-to-Minecraft relay is disabled
5. Prefer setting the bot token through the process environment:

```powershell
$env:ECHO_DISCORD_BOT_TOKEN="your-bot-token"
```

`ECHO_DISCORD_BOT_TOKEN` takes precedence over `discord.botToken`. Do not commit tokens, paste them in public logs, or expose the generated config file.

Every Discord payload includes:

```json
{ "allowed_mentions": { "parse": [] } }
```

On HTTP 429, the queue respects Discord's `retry_after`. Other retryable failures use bounded retries. Failed posts are logged without the token.

The gateway listener ignores bot and webhook messages to prevent echo loops. If another Discord-to-Minecraft bot is still active, disable it before enabling `relay.discordChat`.

## Launcher Chat Setup

Launcher and Android chat are hosted by this mod on the same HTTP port as `/status.json`.

The public endpoints are:

- `GET /v1/community/bootstrap`
- `GET /v1/channels/server-ashfall/messages`
- `POST /v1/channels/server-ashfall/messages`
- `WS /v1/chat/socket`

Set `launcher_chat.enabled=true` and run `/echobridge reload`. Minecraft chat, Discord chat, join/leave, lifecycle, and advancement events are stored in the in-memory server chat history and pushed to launcher/Android WebSocket clients. Launcher and Android messages are broadcast in-game as `[Launcher] <nickname>: <message>` or `[Android] <nickname>: <message>`, and posted to the configured Discord chat channel. Slash-prefixed launcher/Android messages are stored for clients but are not relayed into Minecraft as commands.

## Commands

| Command | Purpose |
| --- | --- |
| `/echobridge status` | Shows current server summary, HTTP state, Discord queue depth, Discord gateway state, chat client count, and in-memory chat history size. |
| `/echobridge testdiscord` | Queues a test message to the status channel. Requires operator permission. |
| `/echobridge reload` | Restarts the public status HTTP service and Discord gateway, then refreshes the snapshot. Requires operator permission. |

## Release Checklist

1. Start the Minecraft server with `echocommunitybridge`, `echocore`, and `echonetcore` loaded.
2. Confirm `/echobridge status` reports HTTP running.
3. Fetch `http://127.0.0.1:47870/status.json` or the deployed public URL.
4. If Discord is enabled, run `/echobridge testdiscord` and confirm the message posts.
5. Join and leave the server with a test account and confirm player counts update.
6. Send one Minecraft chat message and confirm the Discord chat channel receives it when chat relay is enabled.
7. Stop the server and confirm ECHO Launcher moves to stale or unavailable without blocking the Home page.
