<!-- CURSEFORGE_README_START -->
# SignalOS Example Addon by ECHO Labs

![SignalOS Example Addon by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/signalosexample/brand-sheet.png)

**Example SignalOS integration module with Java registration, JSON content, custom record apps, diagnostics, archives, missions, drive templates, and script-friendly patterns.**

![SignalOS Example Addon by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/signalosexample/features-portrait.png)

![SignalOS Example Addon by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/signalosexample/features-landscape.png)

## CurseForge Summary

Example SignalOS integration module with Java registration, JSON content, custom record apps, diagnostics, archives, missions, drive templates, and script-friendly patterns.

## Main Features

- Example SignalOS widgets.
- Sample addon integration.
- Reference workflow panels.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/signalosexample/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/signalosexample/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/signalosexample/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# SignalOS Example Addon

This module shows three SignalOS integration paths:

- Java registration in `SignalOsExample`.
- Datapack JSON under `data/signalosexample/signalos`.
- KubeJS-friendly usage through `SignalOSKubeBridge`.

## Included Content

- Java chapter: `signalosexample:java_ops`
- Java mission: `signalosexample:java_boot`
- Java record app: `signalosexample:java_records`
- Java data provider: `signalosexample:example_records`
- Java peripheral provider: `signalosexample:example_peripheral`
- Java app action: `signalosexample:actions/ping`
- Java diagnostics provider: `signalosexample:example_diagnostics`
- JSON chapter: `signalosexample:field_ops`
- JSON mission: `signalosexample:secure_cache`
- JSON archive: `signalosexample:field_ops_brief`
- JSON app: `signalosexample:field_records`
- JSON record: `signalosexample:field_cache`
- JSON drive template: `signalosexample:handoff_drive`

The JSON mission uses `minecraft:story/root` as its completion advancement so it can be completed quickly in a normal test world.

## Java API Shape

```java
SignalOsApi.registerChapter(TerminalChapter.builder("signalosexample:java_ops")
        .title("Java Ops")
        .section("system")
        .page("missions")
        .page("archives")
        .page("diagnostics")
        .build());
```

## KubeJS-Friendly Script Shape

This is a soft bridge loaded through `Java.loadClass`, not a native KubeJS plugin event.

```js
const SignalOSEvents = Java.loadClass('com.knoxhack.signalos.kubejs.SignalOSEvents')

ServerEvents.loaded(event => {
  SignalOSEvents.content(event => {
    event.clear()

    event.chapter('signalosexample:script_ops')
      .title('Script Ops')
      .section('progress')
      .page('missions')
      .register()

    event.archive('signalosexample:script_brief')
      .chapter('signalosexample:script_ops')
      .title('Script Brief')
      .line('This content was registered from a KubeJS script.')
      .register()
  })
})
```

For reloadable pack content, prefer placing equivalent JSON files under the KubeJS `data/` folder.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore, signalos.
2. Launch the game or tool and confirm the module appears in `metadata/modules/signalosexample.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/signalosexample.md`.
