# AdapterCore Gameplay Mutation Spine Review Manifest

This review manifest scopes the current beta-line AdapterCore gameplay mutation spine change set. It is review evidence, not release catalog metadata.

## Intended Change Buckets

- AdapterCore runtime contract: `NativeResult` carries optional `NativeMutationReceipt` data, and release proof is separate from factual `MUTATED` status.
- AdapterCore dispatcher and ledger: gameplay actions are normalized, undeclared or missing handlers fail, release-proof receipts are attached, and failed proof attempts are recorded without satisfying release evidence.
- Ashfall proof consumer: first-spawn, early-event, and machine flows are the canonical proof paths and must route release-evidence mutations through `EchoRuntimeActionDispatcher` outcomes.
- Native Platform bridge: `EchoAdapterCoreGameplayMutationService` is the typed Native backend contract; reflection remains compatibility fallback only when typed receipt evidence can be recovered.
- SDK and Standalone docs: templates and parity docs describe receipt-backed mutation proof and explicitly exclude queued-only, diagnostic-only, and metadata-only claims.

## Evidence Policy

- Keep source, docs, smoke reports, and parity matrices that explain the receipt model or are consumed by validation.
- Keep evidence-shaped JSON/Markdown reports under `reports/` unless current validation contradicts them.
- Remove disposable generated logs and empty smoke artifacts after validation.
- Do not update Release Index channel/catalog metadata until compiled artifacts, checksums, and release evidence are intentionally produced.

## Review Classification

Inventory source: `git diff --name-status` plus `git status --short` in `ECHO-Modules`, `ECHO-Native-Platform`, `ECHO-SDK`, and `ECHO-Standalone-Runtime`.

Keep in the AdapterCore receipt-spine review:

- `ECHO-Modules`: AdapterCore runtime/dispatcher/ledger/Native Loader host sources, AdapterCore QA smokes, Ashfall AdapterCore evidence/machine bridge sources, AdapterCore receipt docs, `docs/NATIVE_ADAPTERCORE_GUIDE.md`, strict-port audit scripts, `reports/echo/adaptercore/echoadaptercore-registry-backend-parity-smoke.json`, and this manifest.
- `ECHO-Native-Platform`: `NativeLoaderAdapterCoreBackend.java` and `EchoAdapterCoreGameplayMutationService.java`.
- `ECHO-SDK`: AdapterCore receipt docs/template updates in `docs/NATIVE_ADAPTERCORE_GUIDE.md`, `docs/NATIVE_API_REFERENCE.md`, and `templates/native-module-template/README.md`.
- `ECHO-Standalone-Runtime`: AdapterCore receipt coverage notes in `docs/echo/standalone/ECHO_ADAPTERCORE_PARITY_MATRIX.md`.

Leave out of the AdapterCore receipt-spine review and do not stage here:

- Content-graph release plumbing in `ECHO-Modules`, including workflow changes, release artifact verifier/generator changes, `docs/content-graph.md`, `docs/module-artifact-contract.md`, `scripts/update-artifacts-content-graph.mjs`, and generated `addons/*/docs/artifacts.md` files whose only current diff is content-graph sidecar metadata.
- Content-graph evidence gates and contract shape changes in `ECHO-Native-Platform`, including the content graph planner, QA CLI, workflow/build changes, and `EchoNativeContentGraphEvidenceGateMain`.
- Content-graph schemas and schema fixtures in `ECHO-SDK`.
- Standalone content-graph load smoke, compat bridge, and workflow changes in `ECHO-Standalone-Runtime`.

## Required Validation

- `.\gradlew.bat :echoadaptercore:runAdapterCoreTruthLayerSmoke :echoadaptercore:runAdapterCoreNativeLoaderRuntimeHostSmoke :echoadaptercore:runAdapterCoreRegistryParitySmoke :echoadaptercore:runAdapterCoreSpinePublisherSmoke :echoadaptercore:runModuleTruthLayerBootstrapSmoke :echoashfallprotocol:compileJava testGenerateAdapterCoreStrictPortAudit --console=plain`
- `.\gradlew.bat :echo-native-contracts:compileJava :echo-native-loader:compileJava --console=plain`
- `python tools\validate_echo_sdk_templates.py`
- `node scripts\docs-audit.mjs`
- `node scripts\test-generate-standalone-strict-play-evidence.mjs`
- `git diff --check` in touched repos. Line-ending warnings are acceptable; whitespace errors are not.
