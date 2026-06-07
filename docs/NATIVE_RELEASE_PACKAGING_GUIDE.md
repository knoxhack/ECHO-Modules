# ECHO Native Release Packaging Guide

## Artifact Naming

| Type | Filename Pattern | Example |
|---|---|---|
| Addon jar | `<id>-<version>-echo-native.jar` | `echoexample-1.0.0-echo-native.jar` |
| Sources jar | `<id>-<version>-sources.jar` | `echoexample-1.0.0-sources.jar` |
| API jar | `<id>-<version>-api.jar` | `echoexample-1.0.0-api.jar` |

## Required Files Inside the Addon Jar

```
META-INF/
  echo-native-addon.descriptor.json   # validated addon metadata
  neoforge.mods.toml                  # if nativePolicy is NEOFORGE_BRIDGE
  LICENSE                             # license file
data/<id>/                            # datapack content
assets/<id>/                          # resources, textures, lang
```

## Packaging Steps

1. **Build** `./gradlew build`
2. **Validate** `./gradlew validateAddon` — must pass before release.
3. **Test** `./gradlew test` and `./gradlew parityReport`.
4. **Package** `./gradlew packageAddon` — produces the distribution jar.
5. **Checksum** Generate SHA-256:
   ```bash
   sha256sum build/libs/<id>-<version>-echo-native.jar > build/libs/<id>-<version>-echo-native.jar.sha256
   ```
6. **Metadata** Ensure `echo-native-addon.descriptor.json` has correct `version`, `services`, and `optionalIntegrations`.
7. **SBOM** Include `build/reports/echo-native-sbom.json` if publishing to a curated registry.

## Distribution Checklist

- [ ] Descriptor validates with no warnings.
- [ ] All declared services have implementations.
- [ ] All optional integrations are truly optional (no hard imports).
- [ ] `side` matches actual client/server boundary.
- [ ] Sources jar published alongside binary jar.
- [ ] Changelog included or linked.
- [ ] License file present in jar root.
- [ ] No secrets or credentials in jar contents.
- [ ] Parity report shows zero unexpected mismatches.

## Maven / Gradle Publishing

```groovy
publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'dev.echo.native'
            artifactId = 'echoexample'
            version = '1.0.0'
            from components.java
            artifact sourcesJar
        }
    }
}
```

Publish to a Maven repository (Maven Central, Modrinth Maven, or self-hosted) so other addons can depend on your API jar with `compileOnly`.

## Rollback Preparation

Always keep the previous version jar and checksums. See [Rollback Guide](NATIVE_ROLLBACK_GUIDE.md).
