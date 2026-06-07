# Porting Guide from NeoForge to ECHO Native

## Why Port?

ECHO Native gives you service-based optional integrations, policy-driven runtime lanes, and built-in RuntimeGuard budgets. Porting is incremental: you can keep NeoForge compatibility while adding Native features.

## Porting Checklist

### 1. Project Setup

- Apply `echo-sdk-gradle-plugin` in `build.gradle`.
- Add `echo-native-contracts` as `implementation`.
- Move NeoForge-only dependencies to `compileOnly` or `neoforge` configuration.

### 2. Descriptor

Create `META-INF/echo-native-addon.descriptor.json` with:
- `nativePolicy`: Start with `NEOFORGE_BRIDGE` if you want gradual migration.
- `optionalIntegrations`: List every cross-mod integration.
- `side`: Declare `CLIENT`/`SERVER`/`BOTH` accurately.

### 3. Entry Point Migration

Replace `@Mod` main class with an `EchoNativeAddon` entry point:

```java
public class MyNeoForgeMod {
    // NeoForge entry point — keep it for NEOFORGE_BRIDGE lane
}

public class MyNativeAddon implements EchoNativeAddon {
    @Override
    public void onInitialize(EchoNativeAddonRuntime runtime) {
        // Register services here
    }
}
```

Register the native entry point in `echo-native-addon.descriptor.json`:

```json
{
  "entryPoints": {
    "native": "com.example.MyNativeAddon"
  }
}
```

### 4. Event Handling

Replace direct NeoForge event bus subscriptions with ECHO Native event contracts where available. For events without a Native wrapper, keep NeoForge subscriptions—they still work in the `NEOFORGE_BRIDGE` lane.

| NeoForge Event | ECHO Native Equivalent |
|---|---|
| `FMLCommonSetupEvent` | `EchoNativeAddonRuntime.onInitialize()` |
| `RegisterEvent` | `EchoCoreServices.registry().register(...)` |
| `ServerStartingEvent` | `EchoNativeServerLifecycle.STARTING` |
| `PlayerEvent.PlayerLoggedInEvent` | `EchoPlayerService.joinEvent()` |

### 5. Registry Porting

Use `EchoCoreServices.contentRegistry()` for blocks, items, and entities. If you need raw NeoForge registries, access them through the `EchoNeoForgeBridge` service.

### 6. Config

ECHO Native supports datapack-driven config and TOML files in `config/<modid>/`. For per-world config, use `DataCore` persistence services.

### 7. Networking

Replace raw `SimpleChannel` with `echonetcore` packet contracts. Define packet descriptors in JSON or Java records, then register with `EchoNetService`.

### 8. Testing

Add `echo-native-testkit` tests alongside NeoForge `GameTest` fixtures. Run both until you switch fully to `NATIVE` policy.

### 9. Validation

```bash
./gradlew validateAddon
./gradlew parityReport
```

Fix any `ParityMismatch` warnings before switching from `NEOFORGE_BRIDGE` to `NATIVE`.

## Common Pitfalls

- **Direct `ModList` checks in hot paths**: Move them to setup or use `EchoOptionalServices`.
- **Accessing another addon's saved data**: Use `DataCore` service contracts.
- **Assuming NeoForge registries exist in Standalone lane**: Guard with `EchoNativePolicy.current().hasNeoForgeBackend()`.
- **Missing `side` in descriptor**: Client-only code loaded on a server will crash.

## Gradual Migration Path

| Stage | Policy | Goal |
|---|---|---|
| 1 | `NEOFORGE_BRIDGE` | Addon loads via NeoForge; services registered to ECHO. |
| 2 | `NEOFORGE_BRIDGE` | Replace direct cross-mod calls with service lookups. |
| 3 | `NATIVE` | Switch policy; verify with testkit + parity report. |
| 4 | `STANDALONE` (optional) | Strip NeoForge-only code for lightweight deployments. |

See [AdapterCore Guide](NATIVE_ADAPTERCORE_GUIDE.md) for bridge helpers.
