# Porting Guide From NeoForge To ECHO Native

## Why Port?

ECHO Native gives modules service-based optional integrations, policy-driven runtime lanes, release-mode descriptor validation, and RuntimeGuard budgets. Porting can be incremental: keep a NeoForge bridge lane while adding Native-first entrypoints and typed host service calls.

## Porting Checklist

### 1. Project Setup

- Apply `dev.echo.native.echo-sdk-gradle-plugin`.
- Compile against `echo-native-contracts`, `echoaddonapi`, and `echoadaptercore`.
- Keep `echo-native-loader` and NeoForge runtime classes out of Native-first source sets.
- Keep NeoForge `@Mod` classes in a bridge/compat source set or edition lane.

### 2. Descriptor

Native-first addons use `META-INF/echo.mod.json`:

```json
{
  "schema": "echo.mod.v1",
  "id": "myaddon",
  "name": "My Addon",
  "version": "1.0.0-RC1",
  "entrypoint": "com.example.MyNativeAddon",
  "side": "common",
  "access": {
    "nativeClasspath": ["addon.jar"]
  },
  "apiStability": "beta"
}
```

### 3. Entry Point Migration

Replace Native-lane `@Mod` assumptions with `EchoNativeModuleEntrypoint`:

```java
public final class MyNativeAddon implements EchoNativeModuleEntrypoint {
    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService("myaddon:service", new MyService(), "registry");
    }
}
```

Do not reference `EchoNativeAddon` or `EchoNativeAddonRuntime`; those are not the RC1 public entrypoint model.

### 4. Mutation Proof

Release mode counts a module as `MUTATED` only when a typed host service returns an `EchoNativeMutationReceipt` with status `MUTATED`.

```java
EchoNativeServiceMutation mutation = EchoNativeServiceMutation.of(
        "myaddon",
        "registry",
        "declare_content",
        "myaddon:example",
        EchoNativeRuntimeSide.COMMON
);

context.serviceRegistry()
        .service("echo.native.registry", EchoNativeRegistryService.class)
        .map(registry -> registry.register(mutation))
        .ifPresent(context::recordMutation);
```

Descriptor metadata, diagnostic maps, and legacy `activateNative(Map)` claims are not mutation proof.

### 5. Event, Registry, Config, And Network Porting

- Replace direct NeoForge event bus subscriptions with Native event services where available.
- Replace raw registry writes with typed Native registry services.
- Keep config reloads non-mutating unless the host changes active state or persists data.
- Replace raw `SimpleChannel` assumptions with `echonetcore` packet contracts or a typed Native network host service.

### 6. Testing

Add `echo-native-testkit` tests alongside any NeoForge GameTest fixtures:

```groovy
testImplementation 'dev.echo.native:echo-native-testkit:1.0.0-RC1'
```

Require typed receipts in tests before marking a module Native-ready.

### 7. Validation

```bash
./gradlew clean check packageEchoNativeAddon
```

The resulting `.echo-addon` must load in ECHO Native release mode without inferred classpath tokens, dev classpath fallback, or loader-internal imports.

## Common Pitfalls

- Direct `ModList` checks in Native-first code.
- Accessing another addon's saved data instead of using service contracts.
- Assuming NeoForge registries exist in Native or Standalone lanes.
- Missing `side` declarations in `META-INF/echo.mod.json`.
- Self-minting `MUTATED` receipts instead of recording host-returned receipts.

## Gradual Migration Path

| Stage | Lane | Goal |
|---|---|---|
| 1 | NeoForge bridge | Keep `@Mod` compatibility and expose ECHO services. |
| 2 | Dual lane | Add `EchoNativeModuleEntrypoint` and typed host receipts. |
| 3 | Native RC | Package `.echo-addon` and pass release-mode loader gates. |
| 4 | Stable Native | Attach published artifacts, provenance, launcher, rollback, and gameplay evidence. |

See [AdapterCore Guide](NATIVE_ADAPTERCORE_GUIDE.md) for bridge helpers.
