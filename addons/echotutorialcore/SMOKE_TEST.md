# ECHO: TutorialCore Smoke Test

Quick verification steps to confirm TutorialCore loads and operates correctly.

## 1. Module Load

- Start the game (client or dedicated server).
- Check logs for: `ECHO: TutorialCore online. Ashfall deep, but not confusing.`

## 2. Data Reload

- Run `/reload`.
- Check logs for: `TutorialCore reloaded: X cards, Y hints, Z flows, W tooltips.`
- Values should be non-zero (default content is bundled).

## 3. Commands

- Run `/echotutorialcore guide_mode normal`
- Expected: `Guide mode set to NORMAL.`
- Run `/echotutorialcore progress`
- Expected: progress summary with flags, cards, flows counts.
- Run `/echotutorialcore list_cards`
- Expected: list of registered tutorial cards.
- Run `/echotutorialcore list_hints`
- Expected: list of registered tutorial hints.
- Run `/echotutorialcore debug`
- Expected: counts for cards, hints, flows.

## 4. Player Progress

- Join the world.
- Expected log: progress flag `entered_world` is marked automatically.
- Run `/echotutorialcore progress`
- Expected: at least 1 progress flag.

## 5. Guide Mode Persistence

- Set guide mode to `assisted`.
- Disconnect and reconnect.
- Run `/echotutorialcore progress`
- Expected: guide mode is still `ASSISTED`.

## 6. Optional Integrations

If other ECHO addons are present, check logs for integration messages:
- `ECHO: TutorialCore integrated with Terminal.`
- `ECHO: TutorialCore integrated with PowerGrid.`
- `ECHO: TutorialCore integrated with Index.`
- `ECHO: TutorialCore integrated with HoloMap.`
- `ECHO: TutorialCore SoundCore bridge registered.`
- etc.

If an addon is absent, TutorialCore must not crash.

## 7. Client Display

- On client, trigger a hint with `/echotutorialcore hint @p echotutorialcore:no_power`
- Expected: chat message `[ECHO-7] Machine Offline: No EP input detected...`
- If toast hints are enabled, expected: a non-blocking tutorial toast appears.

## 8. Config

- Verify `echotutorialcore-common.toml` and `echotutorialcore-client.toml` are generated in the config folder.

## 9. Tooltips

- Hover a bundled tooltip target such as the purifier/filter item if present.
- Expected: ECHO-7 tooltip help appears, or asks for Shift when the tooltip requires it.

## 10. Guide Surfaces

- With Terminal present, open the Guide page.
- Expected: card categories, unread badges, guide mode controls, and What Now recommendations are visible.
- With Index present, check the Index provider snapshot for tutorial Guide entries.
- With HoloMap/Lens present, verify only real progress-context reminders and scan rows appear.
