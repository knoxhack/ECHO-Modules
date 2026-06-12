# Openlands Protocol Validation

This file is the release validation runbook for `echoopenlandsprotocol`. It is
kept intentionally concrete: each command either regenerates current evidence,
validates a report, or prepares the handoff files needed to replace the current
blocked Public Alpha state with real proof.

## Current Release State

Public Alpha is not ready until these blocker classes are cleared:

- product/legal signoff
- public artifact hosting
- public download verification
- real Native, NeoForge, and Standalone runtime execution
- real launcher install, update, repair, and rollback execution
- Release Index patch approval
- final distribution approval

Local rehearsals, previews, and templates are useful evidence scaffolding, but
they are not substitutes for real public URLs, real runtime runs, real launcher
runs, or human approval memos.

## Source Data And Catalogs

Regenerate the MVP gameplay catalog after changing blocks, items, recipes, loot,
or first-hour progression:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-gameplay-catalog.mjs --module-root addons/echoopenlandsprotocol
```

Regenerate the production phase matrix after changing phase evidence, edition
reports, artifact names, or roadmap state:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-production-phase-matrix.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

Run the contract validator before packaging:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-contract.mjs --module-root addons/echoopenlandsprotocol
```

Compile and smoke-test the shared first-hour runtime core:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-core.mjs --module-root addons/echoopenlandsprotocol
```

## Runtime Evidence

Generate blocked runtime execution reports before real adapter execution exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Validate real runtime execution reports after edition owners run the actual
Native, NeoForge, and Standalone adapter scenarios:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition neoforge --edition-root C:/Development/Github/ECHO-Openlands-NeoForge-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition standalone --edition-root C:/Development/Github/ECHO-Openlands-Standalone-Edition
```

Run and validate local runtime rehearsals only as preflight scaffolding:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-local-runtime-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

The rehearsal maps all runtime scenarios to fixtures and pure runtime hooks, but
it keeps `rehearsalOnly: true` and cannot clear real runtime gates.

## Launcher Evidence

Generate blocked launcher execution reports before real launcher runs exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Validate real launcher execution reports after install, update, repair, rollback,
and world/config preservation have been run through the launcher:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition neoforge --edition-root C:/Development/Github/ECHO-Openlands-NeoForge-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition standalone --edition-root C:/Development/Github/ECHO-Openlands-Standalone-Edition
```

Run and validate local launcher rehearsals only as preflight scaffolding:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-local-launcher-rehearsal-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

The local launcher rehearsal verifies artifact cache mechanics and rollback
shape, but it cannot clear real launcher gates.

## Product, Asset, Audio, And Legal Review

Generate blocked final review reports before human signoff exists:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Validate final release review reports after human public identity, block/item
asset, audio source, generated-output, and legal review evidence exists:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition neoforge --edition-root C:/Development/Github/ECHO-Openlands-NeoForge-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition standalone --edition-root C:/Development/Github/ECHO-Openlands-Standalone-Edition
```

## Public Artifact Hosting

Generate the blocked publication manifest after local module release artifacts
exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
```

Generate the publication URL map template and validate it in template mode:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-publication-url-map-template.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
```

The URL map may use either of these shapes:

```json
{ "urls": { "native": "https://downloads.openlands.example-host.com/file.echo-addon" } }
```

```json
{
  "artifactUrls": [
    {
      "id": "native",
      "downloadUrl": "https://downloads.openlands.example-host.com/file.echo-addon"
    }
  ]
}
```

Before download verification, validate the filled URL map with URL requirements:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --url-map C:/path/to/openlands-publication-urls.json --require-urls
```

Publication URL map validation checks artifact ID coverage, duplicate IDs,
file/kind/runtime metadata, expected hash parity, expected byte-size parity,
Release Index parity, optional missing-url template state, and required public
HTTPS URL shape before network verification.

## Public Download Verification

After all four artifacts have public HTTPS URLs, run the verifier:

```text
node addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --url-map C:/path/to/openlands-publication-urls.json
```

The verifier enforces the same URL-map validation whenever `--url-map` is
supplied, then requires public HTTPS artifact URLs, downloads every artifact,
checks SHA-256 and byte size against `echo-release.json`, writes
`openlands-release-publication-manifest.verified.json`, and saves per-artifact
download evidence under
`dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-verification-artifacts`.

Validate the verified publication manifest:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --manifest dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.verified.json
```

Verified downloads do not approve or apply the Release Index patch. They only
prove that uploaded bytes match the release metadata.

## Release Index Patch Approval

Generate the publication approval draft template after verified downloads and
distribution approval evidence are ready:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-approval-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

Do not pass the template itself to the approval tool. Copy `approvalDraft` into a
real approval JSON, then fill patch ID, Release Index commit, checklist statuses,
distribution signoff, and distribution approval report entries for Native,
NeoForge, and Standalone.

Approve the publication and write a reviewed Release Index preview:

```text
node addons/echoopenlandsprotocol/scripts/approve-openlands-release-publication.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release --approval C:/path/to/openlands-publication-approval.json --release-index-out dist/echo-module-release/echoopenlandsprotocol/echo-release.approved.preview.json
```

Use `--apply-release-index` only when the approval evidence authorizes patching
the live `dist/echo-module-release/echo-release.json`.

## Distribution Approval

Generate blocked distribution approval reports before public artifacts and
release approval exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
```

Validate distribution approval reports after public artifact publication,
verified downloads, non-preview edition manifest indexing, dependency gates,
co-op public-alpha session evidence, rollback plan, and approval signature exist:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition native --edition-root C:/Development/Github/ECHO-Openlands-Native-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition neoforge --edition-root C:/Development/Github/ECHO-Openlands-NeoForge-Edition
node addons/echoopenlandsprotocol/scripts/validate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition standalone --edition-root C:/Development/Github/ECHO-Openlands-Standalone-Edition
```

## Public Alpha Handoff

Generate the aggregate readiness report:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

Validate the aggregate readiness report:

```text
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

Generate and validate the Public Alpha evidence intake:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

The generated JSON includes `phaseHandoff`, a phase-by-phase view of active
blockers, owner hints, proof requirements, handoff files, and validation
commands.

Generate and validate the Public Alpha approval packet template:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

The approval packet template writes draft saved-artifact templates for
`approval-input-report-index.json`, `dependency-gate-summary.json`,
`release-readiness-hash.txt`, `public-alpha-approval.md`,
`rollback-plan-snapshot.md`, `approved-readiness-report.json`, and
`approved-readiness-report-by-phase.md`. These files are template-only until real
evidence replaces them and the readiness report is blocker-free. The packet also
copies active evidence-intake requirements into `externalEvidenceRequirements`,
so final approval review has the owner hints, impacted phases, proof required,
target files, and validation commands for every current blocker. The generated
`approval-input-report-index.template.json` carries those requirements too, so
the required distribution approval draft remains self-contained.

## Edition Manifest Index Preview

Generate and validate the edition manifest index preview after local release
artifacts and edition manifest templates exist:

```text
node addons/echoopenlandsprotocol/scripts/generate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-edition-manifest-index-preview.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
```

The preview proves local edition manifest indexing and module requirement
resolution. It does not clear real launcher channel indexing or distribution
approval gates.

## Full Local Validation Sweep

Use this focused sweep after changing release-readiness scripts or generated
handoff files:

```text
node --check addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs
node --check addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs
node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release
node addons/echoopenlandsprotocol/scripts/validate-openlands-editions.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

The edition validator also invokes the publication URL-map validator against the
blank template, so the full sweep catches a missing, filled, or stale template
before public download verification starts.
