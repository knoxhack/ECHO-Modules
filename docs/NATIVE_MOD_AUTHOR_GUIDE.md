# ECHO Native Mod Author Guide

## Getting Started

1. Start from the SDK canonical Native addon template.
2. Compile against `echo-native-contracts`, `echoaddonapi`, and `echoadaptercore`.
3. Implement `EchoNativeModuleEntrypoint`.
4. Declare `META-INF/echo.mod.json`.
5. Validate with `./gradlew check` and package with `./gradlew packageEchoNativeAddon`.

## Project Structure

```text
my-addon/
  build.gradle
  src/main/java/.../MyAddon.java
  src/main/resources/META-INF/echo.mod.json
  src/test/java/.../MyAddonTest.java
```

## Descriptor

Every Native addon needs `META-INF/echo.mod.json`:

```json
{
  "schema": "echo.mod.v1",
  "id": "myaddon",
  "name": "My Addon",
  "version": "1.0.0-RC1",
  "entrypoint": "com.example.myaddon.MyAddon",
  "side": "common",
  "access": {
    "nativeClasspath": ["addon.jar"]
  },
  "apiStability": "beta"
}
```

## Service Registration

Use `EchoNativeModuleLoadContext` for addon services and typed Native host services for mutation proof:

```java
public final class MyAddon implements EchoNativeModuleEntrypoint {
    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService("myaddon:registry_service", new MyRegistryService(), "registry");
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
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
    }
}
```

Do not self-mint `MUTATED` receipts. Release mode accepts only typed host-returned `EchoNativeMutationReceipt` records.

## Build And Package

```bash
./gradlew clean check
./gradlew packageEchoNativeAddon
```

Output lands in `build/echo-native/addons/<id>-<version>.echo-addon`.

## Testing

Use `echo-native-testkit` for in-memory loader tests:

```groovy
testImplementation 'dev.echo.native:echo-native-testkit:1.0.0-RC1'
```

```java
@Test
void bootstrapUsesTypedReceipts() {
    EchoNativeSdkTestkit.Environment env = EchoNativeSdkTestkit.common("myaddon");
    EchoNativeModuleLoadContext context = env.loadEntrypoint(new MyAddon());
    assertTrue(context.serviceRegistry().hasService("myaddon:registry_service"));
    env.goldenParity().requireOnlyTypedReceipts();
}
```

## Publishing

See [Release Packaging Guide](NATIVE_RELEASE_PACKAGING_GUIDE.md) for artifact naming, checksums, and metadata requirements. Do not publish modules as player-ready if they rely on `local_build_output_classpath_fallback`, `source-packaged` outputs, or `--allow-missing-runtime`.
