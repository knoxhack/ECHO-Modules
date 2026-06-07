# CreatorCore Adapters

Adapters let ECHO addons expose creator-facing definitions, detail views, diagnostics, panels, form schemas, draft templates, preview actions, and export hooks without making CreatorCore hard-depend on them.

## Registering An Adapter

```java
CreatorCoreApi.get().adapters().register(new MyAddonCreatorAdapter());
```

Register during common setup after your own registries are ready.

## Adapter Capabilities

Common capability strings:

- `definitions`
- `diagnostics`
- `drafts`
- `export`
- `reload`
- `preview`
- `editor`
- `screen_provider`
- `terminal_entry`

Capabilities should describe only what the adapter can safely do in the current runtime.

## Required Behavior

Adapters must:

- avoid client-only class loading from common code
- treat optional integrations as optional
- return clear status text
- return diagnostics instead of throwing
- never write files unless CreatorCore config and permissions allow it
- avoid inventing duplicate runtime formats

## Example Skeleton

```java
public final class MyAddonCreatorAdapter implements CreatorAdapter {
    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath("myaddon", "creator");
    }

    @Override
    public String displayName() {
        return "My Addon";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String status() {
        return "My Addon creator bridge ready.";
    }

    @Override
    public Set<String> capabilities() {
        return Set.of("definitions", "diagnostics", "preview");
    }

    @Override
    public Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
        return Optional.empty();
    }
}
```

## 0.2.1 Built-In Adapters

Real:

- CreatorCore Internal
- ScriptCore bridge when `echoscriptcore` and `EchoScriptCoreApi` are present
- ScreenCore dashboard entry when `echoscreencore` is present on the client
- Terminal tab/addon card when `echoterminal` is present on the client
- MissionCore read-only mission previews when `echomissioncore` is present
- Lens provider and ScriptCore `lens_scan` previews when `echolens` is present
- HoloMap layer/marker/route previews when `echoholomap` is present

Safe stubs:

- WeatherCore
- TutorialCore
- ThemeCore
- TextureForge
- Wiki

## Hardening Coverage

CreatorCore includes lightweight GameTests that exercise adapter aggregation, missing optional-adapter behavior, ScriptCore export delegation results, and Mission Studio schema publication. Optional adapters should keep returning clear status/diagnostics when their backing addon or API is unavailable.
