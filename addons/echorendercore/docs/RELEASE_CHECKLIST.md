# RenderCore V21 Release Checklist

- `.\gradlew.bat :echorendercore:compileJava`
- `.\gradlew.bat :echorendercore:check`
- `.\gradlew.bat :echorendercore:runGameTestServer`
- `.\gradlew.bat validateEchoResources`
- `.\gradlew.bat :echoterminal:compileJava :echosignalos:compileJava :echoholomap:compileJava :echoindex:compileJava :echolens:compileJava`
- `.\gradlew.bat :echoconvoyprotocol:compileJava :echoindustrialnexus:compileJava :echoblockworks:compileJava :echomultiblockcore:compileJava`
- `.\gradlew.bat :echoagriculturereclamation:compileJava :echoblackboxprotocol:compileJava :echoorbitalremnants:compileJava :echoarmory:compileJava :echorecovery:compileJava :echoworldcore:compileJava`
- `.\gradlew.bat buildEchoWorkspace -PechoAddonSet=all`

Manual QA should inspect entity and block visuals, particle anchors, advanced-FX fallback modes, world-surface evidence, the Creator Workbench, and all shared screen chrome consumers with clean no-scanline glass. Repeat Terminal in reduced-motion mode.
