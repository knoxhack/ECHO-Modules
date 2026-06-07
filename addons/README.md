# ECHO Addons

This folder is the canonical home for ECHO addon modules. Native-loader examples should live here beside the production addon code, not in a separate root-level Ashfall copy.

Use `echoashfallprotocol` as the full product-pack example. Gradle discovers it through the same `addons/<module>` include path as every other addon; there is no separate root-level Ashfall module to copy from or maintain.

- `addons/echoashfallprotocol/src/main/resources/META-INF/echo.mod.json` declares the native-loader descriptor.
- `addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallNativeModule.java` is the native entrypoint.
- `addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallBootstrapProductProfile.java` owns the product bootstrap profile.
- `addons/echoashfallprotocol/build.gradle` shows the normal addon build wiring.

Developer-facing native addon shape:

- Keep the addon under `addons/<module>` and let `settings.gradle` include it through the shared addon discovery list.
- Declare both `access.nativeEntrypoint` and `access.nativeClasspath` in `META-INF/echo.mod.json`.
- Implement the direct native entrypoint contracts in production source; do not depend on legacy `activateNative(Map)` bridges.
- Put product-specific bootstrap or gameplay wiring inside the addon, with generic loader behavior staying in `echo-native-platform`.

The native product package task now scans the workspace root by default, so it discovers `core` and `addons` the same way other developer modules are laid out:

```powershell
.\gradlew.bat packageNativeProductLayout --console=plain --no-problems-report
```

The workspace root `echo.pack.json` is the canonical Ashfall runtime product profile for that package task. It names `echoashfallprotocol` as the root module and selects the runtime modules by descriptor id, including `signalos` from `addons/echosignalos`.

Use the packaged release launcher path when you want the clean product flow instead of a source-tree or tester launch. `startNativeClient` is the short alias for that same release-mode path:

```powershell
.\gradlew.bat startNativeClient -PechoAddonSet=ashfall-runtime --console=plain --no-problems-report
.\gradlew.bat runPackagedNativeProductLauncher --console=plain --no-problems-report
```

Override the source root only when packaging an explicit fixture or external product workspace:

```powershell
.\gradlew.bat packageNativeProductLayout -PechoNativeSourceRoot=C:\path\to\product --console=plain --no-problems-report
```
