# Galactic Survey Artifacts

Galactic Survey Protocol follows the shared ECHO module artifact contract.

Required player-facing outputs after compiled runtime support exists:

```text
echogalacticsurveyprotocol-0.1.0-neoforge.jar
echogalacticsurveyprotocol-0.1.0.echo-addon
echogalacticsurveyprotocol-0.1.0-standalone.jar
echogalacticsurveyprotocol-0.1.0-sources.jar
# Sidecar: Release-Index catalogable sidecar containing the canonical .ECHO Content Graph.
echogalacticsurveyprotocol-0.1.0-content-graph.json
# Embedded graph tree: embedded in every runtime archive and also available via the content-graph sidecar.
.echo/content-graph/*
```

Do not promote source-packaged outputs beyond review visibility. Public alpha
requires the module validator, gameplay route smoke, runtime load evidence, and
Launcher install/update/repair/rollback evidence.
