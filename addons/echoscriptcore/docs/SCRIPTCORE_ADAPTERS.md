# ScriptCore Adapters

Adapters let optional addons consume ScriptCore definitions or execute ScriptCore actions.

An adapter implements:

```java
EchoScriptAdapter
```

It declares:
- `supportedDefinitionTypes`
- `supportedActions`
- `supportedConditions`
- `isAvailable`
- `registerDefinitions`
- `executeAction`
- `evaluateCondition`

## Built-In Adapters

Real or partially real in 1.0.0:
- Internal fallback: `noop`, boolean condition groups, and safe `give_item`.
- MissionCore: registers ScriptCore missions through ECHO Core mission content when available, and executes basic mission actions.
- Lens: registers ScriptCore scan types through the Lens service.
- HoloMap: publishes ScriptCore map layers and markers through the shared map provider service.
- SoundCore: routes `play_sound` through the shared sound service.
- Terminal: publishes archive entries and exposes a ScreenCore terminal browser page when Terminal is available.
- DataCore: backs world states, faction reputation, custom metrics, branch markers, and runtime migration snapshots.

Stubbed or diagnostic-only in 1.0.0:
- WeatherCore
- TutorialCore
- Index
- WorldCore

Stubs report unavailable or unsupported actions without crashing.

## Custom Adapter Example

```java
EchoScriptCoreApi.get().registerAdapter(new MyPackAdapter());
```

Use `custom` condition/action types for pack-specific behavior, then implement those IDs in your adapter. Never execute class names, scripts, or reflection targets from JSON.
