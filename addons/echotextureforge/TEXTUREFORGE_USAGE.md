# ECHO: TextureForge Usage

TextureForge is a dev-only asset workflow tool for the ECHO ecosystem. It audits resource folders, builds exact texture specs, exports copy/paste prompts for Codex/internal image skill, creates sheet cut maps, stages generated sheet crops, tracks review state, and applies staged textures safely.

TextureForge does not generate images, call external APIs, add gameplay content, delete files, or overwrite source textures by default.

## Run A Scan

From the repo root:

```bash
.\gradlew.bat :echotextureforge:textureForgeScan --console=plain
```

For one addon:

```bash
.\gradlew.bat :echotextureforge:textureForgeScan -PtextureForgeMod=echorelictech --console=plain
```

Primary reports are written under:

```text
build/textureforge/reports/
```

Compatibility reports are still written as `texture_audit.md`, `texture_audit.json`, and `textureforge_summary.md`.

## Export Prompts

```bash
.\gradlew.bat :echotextureforge:textureForgePrompts --console=plain
.\gradlew.bat :echotextureforge:textureForgeSheets --console=plain
```

Prompt output is organized as:

```text
build/textureforge/prompts/
  master_codex_texture_prompts.md
  by_addon/<modid>_prompts.md
  by_type/item_textures.md
  by_type/block_textures.md
  by_type/machine_textures.md
  sheets/<modid>_item_sheet.md
  sheets/<modid>_item_sheet.cut_map.json
```

Each single prompt includes the exact mod ID, asset ID, kind, texture type, output path, resolution, style family, transparency rule, and hard requirements such as no mockups, no labels, no scene background, and no fake 3D render.

## Generate Image Sheets With Codex/Internal Image Skill

Open a sheet prompt from:

```text
build/textureforge/prompts/sheets/
```

Paste the entire prompt into Codex/internal image skill. Save the returned transparent PNG and the matching cut map into:

```text
build/textureforge/import/incoming/
```

Use matching names:

```text
echorelictech_item_sheet.png
echorelictech_item_sheet.cut_map.json
```

The cut map contains row, column, crop rectangle, expected size, and target output path for each cell.

## Plan, Preview, And Stage Imports

Plan only:

```bash
.\gradlew.bat :echotextureforge:textureForgeImportPlan -PtextureForgeSheet=echorelictech_item_sheet --console=plain
```

In-game commands:

```text
/echo textureforge import plan echorelictech_item_sheet
/echo textureforge import preview echorelictech_item_sheet
/echo textureforge import stage echorelictech_item_sheet
```

Import planning checks that the PNG and cut map exist, validates sheet dimensions, computes crop rectangles, detects target conflicts, and writes:

```text
build/textureforge/import/import_plan.json
build/textureforge/import/import_report.md
build/textureforge/review/import_plan.json
```

Staging crops cells into:

```text
build/textureforge/import/staged/<modid>/textures/...
```

Staging never writes to `src/main/resources`.

## Review Generated Assets

Review state is stored at:

```text
build/textureforge/review/review_state.json
build/textureforge/review/review_state.md
```

Commands:

```text
/echo textureforge review list
/echo textureforge review approve echorelictech:fractured_relay_core
/echo textureforge review reject echorelictech:fractured_relay_core needs clearer silhouette
/echo textureforge review mark needs_regen echorelictech:fractured_relay_core
/echo textureforge review export
```

Review statuses are `pending`, `approved`, `rejected`, `needs_regen`, and `applied`.

## Apply Safely

Dry-run staged files:

```bash
.\gradlew.bat :echotextureforge:textureForgeApplyDryRun --console=plain
```

In-game:

```text
/echo textureforge apply dryrun
/echo textureforge apply staged
/echo textureforge apply staged --modid echorelictech
/echo textureforge apply staged --overwrite-approved
```

Default apply behavior:

- copies only missing files
- skips existing target files as conflicts
- never deletes anything
- never overwrites unless `--overwrite-approved` is used and the review state approves the asset
- writes every copied, skipped, and conflict action to `build/textureforge/import/apply_report.md`

Approved overwrites create backup copies under:

```text
build/textureforge/import/backups/
```

## Manual Texture Specs

Manual specs live under:

```text
assets/<modid>/textureforge/specs/*.json
```

Example:

```json
{
  "styleFamily": "RELICTECH",
  "defaultResolution": "32x32",
  "assets": [
    {
      "id": "fractured_relay_core",
      "kind": "item",
      "textureType": "component",
      "outputPath": "textures/item/fractured_relay_core.png",
      "notes": "dark metal relic core with cyan cracks",
      "promptPriority": 100,
      "colorPaletteHints": ["dark metal", "cyan cracks"],
      "silhouetteNotes": "small broken core with a strong outer ring"
    }
  ]
}
```

Manual specs override generated specs for the same `namespace:assetId:kind`.

## Reports To Read First

Start with:

```text
build/textureforge/reports/summary.md
build/textureforge/reports/texture_audit.md
build/textureforge/reports/by_addon/<modid>_audit.md
```

Severity files:

```text
build/textureforge/reports/issues/critical.json
build/textureforge/reports/issues/warnings.json
build/textureforge/reports/issues/info.json
```

Specialized files:

```text
missing_assets.json
wrong_size_textures.json
model_reference_errors.json
naming_errors.json
style_families.md
```

## Example Production Workflow

1. Run `textureForgeScan`.
2. Open `build/textureforge/reports/texture_audit.md`.
3. Run `textureForgePrompts` and `textureForgeSheets`.
4. Paste a sheet prompt into Codex/internal image skill.
5. Save the generated transparent PNG into `build/textureforge/import/incoming`.
6. Run `textureForgeImportPlan`.
7. Run `/echo textureforge import stage <sheet_name>` if the plan is valid.
8. Review staged crops and approve good assets.
9. Run `/echo textureforge apply staged` for missing-only copy.
10. Re-run `textureForgeScan`.

## Known Limitations

- Gradle/CLI scans are filesystem-only; live registry totals require in-game commands.
- ScreenCore dashboard remains optional and deferred; reports and commands are the primary workflow.
- TextureForge validates PNG dimensions and alpha but does not judge artistic quality automatically.
- Apply uses staged PNG files only; model/lang/blockstate source edits remain suggested in reports and are not auto-written.
