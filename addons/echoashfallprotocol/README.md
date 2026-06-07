# ECHO: Ashfall Protocol Addon

Ashfall lives at `addons/echoashfallprotocol` as a normal addon module. It is the large, source-backed example for building a full ECHO content pack from the same addon layout other developers use.

Use this folder as the large example of an addon that owns gameplay content, NeoForge wiring, native-loader metadata, resources, data packs, optional integrations, and AdapterCore-backed runtime bridges.

Canonical descriptor:

- `src/main/resources/META-INF/echo.mod.json` is the source-of-truth ECHO descriptor for this addon.
- `entrypoint`: `com.knoxhack.echoashfallprotocol.EchoAshfallProtocol` for NeoForge.
- `access.nativeEntrypoint`: `com.knoxhack.echoashfallprotocol.EchoAshfallNativeModule` for Native Loader.
- `access.nativeClasspath`: `echo.native:inferred-classpath` in source. Release packaging replaces this with explicit `lib/*.jar` entries.
- `access.nativeBootstrapProfile`: `com.knoxhack.echoashfallprotocol.EchoAshfallBootstrapProductProfile`.

Addon structure:

- `build.gradle` - normal addon build wiring for NeoForge, metadata generation, dependencies, and publishing.
- `gradle.properties` - addon identity and version coordinates.
- `src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallProtocol.java` - NeoForge entrypoint.
- `src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallNativeModule.java` - Native Loader entrypoint.
- `src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallBootstrapProductProfile.java` - Native Loader product bootstrap profile.
- `src/main/resources` - runtime resources, data, mixin declaration, and canonical descriptor.
- `src/main/templates/META-INF/neoforge.mods.toml` - generated NeoForge mod metadata template.
- `src/qa/java` - verifier and asset-generation mains that must stay out of production packaging.

Transform policy:

Ashfall declares two NeoForge client mixins in `src/main/resources/echoashfallprotocol.mixins.json`: `EchoButtonMixin` and `EchoLoadingOverlayMixin`. Native Loader release mode does not execute those mixins or any other Minecraft/addon bytecode mutation. The descriptor maps both mixins through `access.nativeTransformCompatibility.replacementMappings` to the supported native projection `native:client_surface_projection`.

Use `echo-native native transform-policy <product-root>` to inspect this story. Errors block validation and release packaging, warnings require release-owner review, and notices document Forge-style declarations that are replaced by native projections.

Compile just this addon from the workspace root:

```powershell
.\gradlew.bat :echoashfallprotocol:compileJava -PechoAddonSet=ashfall-runtime
```

Ashfall should be treated like any other addon module. Generic platform code should not hardcode special filesystem paths for Ashfall.

Cleanup inventory:

- `src/main/java/com/knoxhack/echoashfallprotocol/test/ModGameTests.java` currently lives in production source and registers host-smoke GameTests. Move it to a test or QA source set once the NeoForge GameTest packaging model can keep those classes out of release jars.
- `src/qa/java/com/knoxhack/echoashfallprotocol/*Verifier.java` and `src/qa/java/com/knoxhack/echoashfallprotocol/tools/*Generator.java` are QA-only mains. Keep them sidecar-only and excluded from release artifacts.
- Native platform QA mains under `echo-native-platform/*/src/qa/java` are sidecar verification tools. Public products should ship the public CLI/product/bootstrap mains only.
