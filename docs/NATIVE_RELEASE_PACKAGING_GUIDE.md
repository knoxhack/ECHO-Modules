# Native Release Packaging Guide

Native addon releases use `.echo-addon` packages generated from module descriptors.

## Required Outputs

- `<module>-<version>.echo-addon`
- `<module>-<version>-neoforge.jar` when the module supports the bridge lane
- `<module>-<version>-standalone.jar` when the module supports the standalone lane
- `<module>-<version>-sources.jar`
- `META-INF/echo.mod.json`
- `META-INF/neoforge.mods.toml` for NeoForge artifacts
- `echo-addon-package.json`
- `checksums.sha256` inside each `.echo-addon` package and at the release root

Publish module artifacts from `knoxhack/ECHO-Modules` and consume them through Native Edition `moduleRequirements`.

## Player-Ready Gate

Player-facing module releases must be generated from compiled runtime jars and verified before Release Index import:

```powershell
.\gradlew.bat generateGalacticSurveyModuleRelease --console=plain
node scripts\verify-module-release.mjs --release-dir dist\echo-module-release
```

The generator embeds `META-INF/echo.mod.json`, `echo-addon-package.json`, and package-local `checksums.sha256` into each `.echo-addon`; embeds `META-INF/echo.mod.json` into compiled standalone artifacts; and embeds both `META-INF/echo.mod.json` and `META-INF/neoforge.mods.toml` into compiled NeoForge artifacts before calculating release-root checksums. Do not promote artifacts marked `source-packaged`, generated with `--allow-missing-runtime`, or backed by `local_build_output_classpath_fallback`.
