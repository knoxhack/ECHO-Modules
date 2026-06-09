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
requireText('subject-checksums: dist/echo-module-release/checksums.sha256', 'checksum attestation subject')
requireText('node scripts/generate-module-release.mjs', 'module release generator invocation')
requireText('node scripts/verify-module-release.mjs --release-dir dist/echo-module-release', 'release verifier invocation')

requireText('MODULE="${{ github.event.inputs.module || \'\' }}"', 'selected module input handling')
requireText('find addons -mindepth 2 -maxdepth 2 -name gradlew -perm -111', 'all-module wrapper discovery')
requireText('player_ready=true requires compiled runtime jars or module-local Gradle wrappers.', 'player-ready fail-fast')
requireText('ARGS+=(--package-from-source)', 'development visibility source-packaging mode')
requireText('gh release create "$TAG"', 'GitHub release creation')
requireText('gh release upload "$TAG"', 'GitHub release upload fallback')
requireText('! -name checksums.txt', 'checksum compatibility copy excluded from release upload')

if (errors.length) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Release workflow audit passed.')
