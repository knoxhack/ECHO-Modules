# CreatorCore Codex Studio

Codex Studio connects CreatorCore to a local Echo Codex Bridge sidecar. The sidecar invokes the local Codex CLI from the Echo workspace, captures output, checks changed files with `git diff --name-only`, and can run focused validation profiles.

## Start The Bridge

```bash
python tools/echo_codex_bridge.py --workspace C:\Github\Echo --allow-repo-edits
```

Useful options:

- `--dry-run` starts jobs without invoking Codex.
- `--codex-path <path>` points at a specific Codex CLI binary.
- `--allow-repo-edits` is required before the bridge accepts jobs that may edit the repo.
- `--auth-token <token>` requires `Authorization: Bearer <token>` from CreatorCore.
- `--max-jobs <count>` bounds in-memory job history; default is `25`.
- `--command-template "<command>"` overrides the default invocation. Placeholders: `{codex}`, `{model}`, `{model_arg}`, `{prompt}`, `{workspace}`.
- `--model <model>` sets the default model preference reported to CreatorCore. Leave it empty to use the Codex CLI default.

The bridge refuses non-loopback bind hosts by default. Use `--allow-remote-bind` only on a trusted network.

## Unlock CreatorCore

CreatorCore defaults to safe/locked mode. Enable only for a local trusted workspace:

```toml
allow_codex_bridge=true
allow_codex_repo_edits=true
codex_bridge_url="http://127.0.0.1:47321"
codex_bridge_token=""
codex_workspace_root="C:/Github/Echo"
codex_model=""
```

Codex/OpenAI credentials stay in the local Codex CLI configuration. Do not place API keys in Minecraft config.
If the bridge was started with `--auth-token`, put the same token in `codex_bridge_token`; CreatorCore redacts it from status output.

Repo edits require both Minecraft config (`allow_codex_repo_edits=true`) and bridge startup (`--allow-repo-edits`). Codex Studio's ScreenCore panel is status/read-only for job launches; start, validate, and cancel jobs through the permission-gated `/echo creatorcore codex ...` commands.

## Commands

```text
/echo creatorcore codex status
/echo creatorcore codex run asset_repair repair broken RenderCore mob assets
/echo creatorcore codex vision capture relay_room
/echo creatorcore codex vision run asset_repair inspect the latest in-game capture
/echo creatorcore codex run mob_model create a themed RenderCore creature model
/echo creatorcore codex refresh <job>
/echo creatorcore codex validate <job>
/echo creatorcore codex cancel <job>
```

Profiles: `mob_model`, `entity_renderer`, `block_model`, `block_entity_model`, `multiblock_visual`, `rendercore_profile`, and `asset_repair`.

Validation profiles are selected by the bridge request and default to `focused`, which runs the mob asset validator plus CreatorCore and RenderCore compile checks.

## Codex Vision

Codex Vision is opt-in local screenshot context for debugging from a real client. Enable `allow_codex_visual_context=true`, then run `/echo creatorcore codex vision capture <label>` from the client. CreatorCore writes a PNG and adjacent JSON under `codex_capture_root`, then registers it with the local bridge when `allow_codex_bridge=true`.

Use `python tools/echo_codex_capture.py latest` from the workspace to print the latest capture path and metadata. Jobs started with `/echo creatorcore codex vision run <profile> [prompt]` ask the bridge to attach the latest indexed capture to the Codex prompt.

## Codex Pilot

Codex Pilot is an opt-in local dev bot. Enable only in trusted dev worlds:

```toml
allow_codex_pilot=true
allow_codex_pilot_autopilot=true
allow_codex_pilot_world_actions=false
codex_pilot_max_radius=32
codex_pilot_max_steps=20
codex_pilot_log_root="run/creatorcore/codex_pilot"
```

In-game controls:

```text
/echo creatorcore codex pilot spawn echonpcore:test_survivor helper
/echo creatorcore codex pilot status
/echo creatorcore codex pilot inspect
/echo creatorcore codex pilot capture
/echo creatorcore codex pilot interact
/echo creatorcore codex pilot follow
/echo creatorcore codex pilot goto <x> <y> <z>
/echo creatorcore codex pilot look <yaw> <pitch>
/echo creatorcore codex pilot say checking this flow
/echo creatorcore codex pilot task test this NPC trade flow
/echo creatorcore codex pilot place <x> <y> <z> <block>
/echo creatorcore codex pilot break [x] [y] [z]
/echo creatorcore codex pilot stop
```

Desktop CLI controls:

```bash
python tools/echo_codex_pilot.py status
python tools/echo_codex_pilot.py spawn --profile echonpcore:test_survivor
python tools/echo_codex_pilot.py inspect
python tools/echo_codex_pilot.py capture
python tools/echo_codex_pilot.py goto 10 65 -4
python tools/echo_codex_pilot.py say checking this flow
python tools/echo_codex_pilot.py task "test this NPC trade flow"
python tools/echo_codex_pilot.py events
python tools/echo_codex_pilot.py stop
```

When NPCore is loaded, CreatorCore uses `echonpcore:echo_npc` as the visible avatar. The executor is a hidden NeoForge FakePlayer, and all world-changing actions stay locked unless `allow_codex_pilot_world_actions=true`.
Every bridge action receives a result (`done`, `refused`, or `failed`) in `/pilot/events` and `/pilot/status`, so the CLI can show whether Minecraft actually executed the queued action. `/pilot/task` is a guarded V1 fallback plan: it can inspect, optionally request a client-side capture, and report status, but it does not perform world edits unless future planner support explicitly adds safe whitelisted steps and CreatorCore accepts them.
