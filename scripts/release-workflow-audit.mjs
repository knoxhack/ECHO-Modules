import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const workflowPath = path.join(root, '.github', 'workflows', 'release-modules.yml')
const workflow = fs.readFileSync(workflowPath, 'utf8')
const errors = []

function requireText(text, label) {
  if (!workflow.includes(text)) errors.push(`release-modules.yml missing ${label}`)
}

function forbidText(text, label) {
  if (workflow.includes(text)) errors.push(`release-modules.yml contains forbidden ${label}`)
}

requireText('contents: write', 'contents write permission')
requireText('id-token: write', 'id-token write permission for attestations')
requireText('attestations: write', 'attestations write permission')
forbidText('artifact-metadata:', 'unsupported artifact-metadata permission')

requireText('actions/attest@v4', 'actions/attest@v4 step')
requireText('Prepare release archive', 'release archive preparation before attestation')
requireText('sha256sum echo-module-release.tar.gz > echo-module-release.tar.gz.sha256', 'release archive checksum')
requireText('subject-checksums: ECHO-Modules/echo-module-release.tar.gz.sha256', 'archive checksum attestation subject')
requireText('sdk_ref:', 'optional SDK ref input for branch validation')
requireText("ref: ${{ github.event_name == 'workflow_dispatch' && inputs.sdk_ref || 'main' }}", 'ECHO-SDK branch-aware checkout')
requireText('node scripts/generate-module-release.mjs', 'module release generator invocation')
requireText('node scripts/verify-module-release.mjs --release-dir dist/echo-module-release', 'release verifier invocation')
requireText('node scripts/validate-content-graph.mjs --strict --sdk-root ../ECHO-SDK', 'strict content graph SDK schema validation')

requireText('MODULE="${{ github.event.inputs.module || \'\' }}"', 'selected module input handling')
requireText('find addons -mindepth 2 -maxdepth 2 -name gradlew -perm -111', 'all-module wrapper discovery')
requireText('player_ready=true requires compiled runtime jars or module-local Gradle wrappers.', 'player-ready fail-fast')
requireText('ARGS+=(--package-from-source)', 'development visibility source-packaging mode')
requireText('gh release create "$TAG"', 'GitHub release creation')
requireText('gh release upload "$TAG"', 'GitHub release upload fallback')
requireText("-o -name '*-content-graph.json'", 'content graph sidecar release upload')
requireText('dist/echo-module-release/content-graph-evidence.json', 'content graph evidence release upload')

if (errors.length) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Release workflow audit passed.')
