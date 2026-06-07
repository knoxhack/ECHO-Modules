# ECHO Native Mod Author Guide

## Getting Started

1. Clone or download the ECHO Native SDK.
2. Apply the `echo-sdk-gradle-plugin` to your addon project.
3. Pick a [template](NATIVE_TEMPLATES.md) matching your addon type.
4. Write your addon descriptor, service registrations, and content.
5. Validate with `./gradlew validateAddon` and package with `./gradlew packageAddon`.

## Project Structure

```
my-addon/
  build.gradle
  src/main/java/.../MyAddon.java
  src/main/resources/META-INF/echo-native-addon.descriptor.json
  src/main/resources/data/myaddon/...
  src/test/java/.../MyAddonTest.java
```

## Descriptor

Every addon needs `echo-native-addon.descriptor.json`:

```json
{
  "id": "myaddon",
  "name": "My Addon",
  "version": "1.0.0",
  "nativePolicy": "NATIVE",
  "services": ["myaddon:registry_service"],
  "optionalIntegrations": ["echoindex", "echoterminal"],
  "side": "BOTH"
}
```

| Field | Required | Description |
|---|---|---|
| `id` | Yes | Unique modid. Lowercase, no spaces. |
| `name` | Yes | Human-readable name. |
| `version` | Yes | Semver string. |
| `nativePolicy` | Yes | `NATIVE`, `NEOFORGE_BRIDGE`, or `STANDALONE`. |
| `services` | No | List of service contract IDs this addon registers. |
| `optionalIntegrations` | No | Modids this addon optionally integrates with. |
| `side` | Yes | `CLIENT`, `SERVER`, or `BOTH`. |

## Service Registration

Use `EchoCoreServices` or `EchoNativeAddonRuntime` to register providers:

```java
public class MyAddon {
    public void onInitialize(EchoNativeAddonRuntime runtime) {
        runtime.registerService("myaddon:registry_service", new MyRegistryService());
    }
}
```

Keep registrations idempotent and safe to call multiple times during reloads.

## Optional Integration

Never hard-reference optional addons. Use service lookup:

```java
Optional<IndexService> index = EchoOptionalServices.index();
index.ifPresent(i -> i.registerProvider(myDocsProvider));
```

## Build & Package

```bash
# Compile and run tests
./gradlew build

# Validate descriptor and service contracts
./gradlew validateAddon

# Produce distribution jar
./gradlew packageAddon
```

Output lands in `build/libs/<id>-<version>-echo-native.jar`.

## Testing

Use the `echo-native-testkit` dependency for in-memory loader tests:

```groovy
testImplementation 'dev.echo.native:testkit:1.0.0-RC'
```

```java
@Test
public void testBootstrap() {
    EchoNativeTestLoader loader = new EchoNativeTestLoader();
    loader.loadAddon("myaddon");
    assertTrue(loader.isServiceRegistered("myaddon:registry_service"));
}
```

## Publishing

See [Release Packaging Guide](NATIVE_RELEASE_PACKAGING_GUIDE.md) for artifact naming, checksums, and metadata requirements.
