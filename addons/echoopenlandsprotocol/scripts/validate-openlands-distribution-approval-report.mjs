import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { validatePublicationManifest } from './validate-openlands-release-publication-manifest.mjs'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    runtimeTarget: 'echo_native',
    reportName: 'native-distribution-approval-report.json',
    artifactFile: 'echoopenlandsprotocol-0.1.0.echo-addon',
    releaseIndexEntry: 'modpacks/openlands-native.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    reportName: 'neoforge-distribution-approval-report.json',
    artifactFile: 'echoopenlandsprotocol-0.1.0-neoforge.jar',
    releaseIndexEntry: 'modpacks/openlands-neoforge.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    reportName: 'standalone-distribution-approval-report.json',
    artifactFile: 'echoopenlandsprotocol-0.1.0-standalone.jar',
    releaseIndexEntry: 'modpacks/openlands-standalone.json',
  },
}

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    edition: null,
    editionRoot: null,
    report: null,
    allowMissing: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
    else if (arg === '--allow-missing') args.allowMissing = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actualValues, expectedValues) {
  return JSON.stringify(sortedUnique(actualValues)) === JSON.stringify(sortedUnique(expectedValues))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function requireFields(errors, object, fields, label) {
  for (const field of fields) {
    assert(errors, object?.[field] !== undefined && object?.[field] !== null && object?.[field] !== '', `${label} missing ${field}`)
  }
}

function findModuleRoot(explicitRoot, editionRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = editionRoot ? path.resolve(editionRoot) : process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function stableDistributionApprovalReport(value) {
  if (Array.isArray(value)) return value.map(stableDistributionApprovalReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (key === 'generatedAt') continue
    stable[key] = stableDistributionApprovalReport(entry)
  }
  return stable
}

function runGeneratorJson({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-distribution-approval-report.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--module-root',
    moduleRoot,
    '--release-root',
    releaseRoot,
    '--edition',
    editionKey,
    '--edition-root',
    editionRoot,
    '--out',
    reportPath,
    '--dry-run',
    '--json',
  ], {
    cwd: moduleRoot,
    encoding: 'utf8',
    shell: false,
  })
  if (result.status !== 0) {
    return {
      error: `distribution approval report generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `distribution approval report generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function publicationById(publicationManifest, id) {
  return (publicationManifest?.artifactPublications ?? []).find((publication) => publication.id === id)
}

function samePath(left, right) {
  if (!left || !right) return false
  return path.resolve(left).toLowerCase() === path.resolve(right).toLowerCase()
}

function resolveSavedArtifactPath({ artifactName, editionRoot, reportPath, approvalArtifactRoot, releaseRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0) return null
  const candidates = path.isAbsolute(artifactName)
    ? [artifactName]
    : [
        path.join(approvalArtifactRoot, artifactName),
        path.join(path.dirname(reportPath), artifactName),
        path.join(editionRoot, artifactName),
        path.join(releaseRoot, MODULE_ID, artifactName),
      ]
  return candidates.map((candidate) => path.resolve(candidate)).find((candidate) => fileExists(candidate)) ?? null
}

function realApprovalSavedArtifactPath({ artifactName, approvalArtifactRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0 || path.isAbsolute(artifactName)) return null
  return path.resolve(approvalArtifactRoot, artifactName)
}

function readJsonIfPossible(filePath) {
  try {
    return readJson(filePath)
  } catch {
    return null
  }
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function textIncludes(text, needle) {
  return text.toLowerCase().includes(String(needle ?? '').toLowerCase())
}

function assertTextIncludes(errors, text, needle, label) {
  assert(errors, textIncludes(text, needle), `${label} must mention ${needle}`)
}

function assertNoUnresolvedApprovalMarkers(errors, text, label) {
  for (const marker of ['todo', 'tbd', 'placeholder', 'not executed', 'not_executed', 'preflight only', 'rehearsal only']) {
    assert(errors, !textIncludes(text, marker), `${label} must not contain unresolved marker ${marker}`)
  }
}

function normalizeHashText(value) {
  const match = String(value ?? '').match(/[a-f0-9]{64}/i)
  return match ? match[0].toLowerCase() : null
}

function looksLikePreviewEvidence(value) {
  if (value === null || typeof value !== 'object') return false
  const schema = typeof value.schema === 'string' ? value.schema : ''
  return schema.includes('_preview.')
    || value.previewOnly === true
    || value.status === 'preview_only'
    || value.indexedState === 'preview_only'
    || value.clearsDistributionGates === false
}

function looksLikePreflightEvidence(filePath, payload) {
  const normalizedPath = filePath.replace(/\\/g, '/').toLowerCase()
  if ([
    'local-runtime-rehearsal',
    'local-launcher-rehearsal',
    'release-publication-rehearsal',
    'edition-manifest-index-preview',
    'legal-content-audit',
    'launcher-flow-report',
  ].some((marker) => normalizedPath.includes(marker))) {
    return true
  }
  const schema = String(payload?.schema ?? payload?.schemaVersion ?? '').toLowerCase()
  if ([
    'local_runtime_rehearsal',
    'local_launcher_rehearsal',
    'release_publication_rehearsal',
    'edition_manifest_index_preview',
    'legal_content_audit',
    'launcher_flow',
  ].some((marker) => schema.includes(marker))) {
    return true
  }
  return payload?.rehearsalOnly === true
    || payload?.preflightOnly === true
    || payload?.previewOnly === true
    || payload?.clearsDistributionGates === false
    || payload?.clearsRuntimeGates === false
    || payload?.clearsLauncherGates === false
    || payload?.clearsReleasePublicationGates === false
    || payload?.publicAlphaReady === false
}

function fileTextLooksLikePreflight(filePath) {
  const extension = path.extname(filePath).toLowerCase()
  if (!['.txt', '.log', '.md'].includes(extension)) return false
  const text = fs.readFileSync(filePath, 'utf8').slice(0, 65536).toLowerCase()
  return text.includes('local runtime rehearsal')
    || text.includes('local launcher rehearsal')
    || text.includes('release publication rehearsal')
    || text.includes('local_runtime_rehearsal')
    || text.includes('local_launcher_rehearsal')
    || text.includes('rehearsalonly')
    || text.includes('preflightonly')
    || text.includes('clearsdistributiongates: false')
}

function validateRealSavedArtifact(errors, { artifactPath, expectedArtifactPath, artifactName, areaId }) {
  assert(errors, artifactPath === expectedArtifactPath, `passed distribution approval area ${areaId} saved artifact ${artifactName} must be saved under the real distribution approval artifact root`)
  const extension = path.extname(artifactPath).toLowerCase()
  if (extension === '.json') {
    const payload = readJsonIfPossible(artifactPath)
    assert(errors, payload !== null, `passed distribution approval area ${areaId} saved artifact ${artifactName} must be valid JSON`)
    if (payload) {
      assert(errors, !looksLikePreflightEvidence(artifactPath, payload), `passed distribution approval area ${areaId} saved artifact ${artifactName} must be real distribution approval evidence, not preflight or rehearsal evidence`)
    }
  } else {
    assert(errors, !looksLikePreflightEvidence(artifactPath, null), `passed distribution approval area ${areaId} saved artifact ${artifactName} must be real distribution approval evidence, not preflight or rehearsal evidence`)
    assert(errors, !fileTextLooksLikePreflight(artifactPath), `passed distribution approval area ${areaId} saved artifact ${artifactName} must not be copied preflight or rehearsal text evidence`)
  }
}

function assertNotPreviewArtifact(errors, artifactPath, label) {
  const json = readJsonIfPossible(artifactPath)
  if (!json) return
  assert(errors, !looksLikePreviewEvidence(json), `${label} must not be preview-only evidence`)
  for (const [index, entry] of (json.editionResults ?? []).entries()) {
    assert(errors, !looksLikePreviewEvidence(entry), `${label} editionResults[${index}] must not be preview-only evidence`)
  }
  for (const [index, entry] of (json.entries ?? []).entries()) {
    assert(errors, !looksLikePreviewEvidence(entry), `${label} entries[${index}] must not be preview-only evidence`)
    if (Object.prototype.hasOwnProperty.call(entry, 'publicListingAllowed')) {
      assert(errors, entry.publicListingAllowed === true, `${label} entries[${index}] must be publicly listed`)
    }
  }
}

function isPassingIndexStatus(value) {
  return ['passed', 'indexed', 'published', 'listed', 'active'].includes(String(value ?? '').toLowerCase())
}

function entriesById(entries) {
  return new Map((Array.isArray(entries) ? entries : []).map((entry) => [entry.id, entry]))
}

function requiredCoreModuleIds(contract) {
  return sortedUnique(contract.descriptor?.requires ?? [])
}

function expectedEditionEntries(contract) {
  return (contract.acceptance.editionReports ?? [])
    .map((entry) => ({
      ...entry,
      ...(EDITIONS[entry.edition] ?? {}),
    }))
}

function assertExactEditionIds(errors, entries, expectedEntries, label) {
  assert(errors, Array.isArray(entries), `${label} must include edition entries`)
  assert(errors, sameSet((entries ?? []).map((entry) => entry.id), expectedEntries.map((entry) => entry.edition)), `${label} must include exactly Native, NeoForge, and Standalone entries`)
}

function validateIndexedEditionEntry(errors, entry, expected, label, { requireIndexFields = true } = {}) {
  assert(errors, entry !== undefined, `${label} missing ${expected.edition}`)
  if (!entry) return
  assert(errors, entry.packId === expected.packId, `${label} ${expected.edition} packId mismatch`)
  assert(errors, entry.runtimeTarget === expected.runtimeTarget, `${label} ${expected.edition} runtimeTarget mismatch`)
  if (requireIndexFields || entry.releaseIndexEntry !== undefined) {
    assert(errors, entry.releaseIndexEntry === expected.releaseIndexEntry, `${label} ${expected.edition} releaseIndexEntry mismatch`)
  }
  const artifactFile = entry.artifact?.file ?? entry.moduleArtifact?.file ?? entry.artifactFile ?? entry.artifactPattern
  if (requireIndexFields || artifactFile !== undefined) {
    assert(errors, artifactFile === expected.artifactFile || artifactFile === expected.artifactPattern, `${label} ${expected.edition} artifact file mismatch`)
  }
  if (entry.moduleRequirementsResolved !== undefined) {
    assert(errors, entry.moduleRequirementsResolved === true, `${label} ${expected.edition} module requirements must resolve`)
  }
  if (entry.moduleRequirementResolutionStatus !== undefined) {
    assert(errors, entry.moduleRequirementResolutionStatus === 'passed', `${label} ${expected.edition} module requirement resolution must pass`)
  }
  if (Array.isArray(entry.missingCoreModuleIds)) {
    assert(errors, entry.missingCoreModuleIds.length === 0, `${label} ${expected.edition} missingCoreModuleIds must be empty`)
  }
  if (entry.openlandsRequirement !== undefined) {
    assert(errors, entry.openlandsRequirement?.id === MODULE_ID, `${label} ${expected.edition} openlands requirement id mismatch`)
    assert(errors, entry.openlandsRequirement?.version === VERSION, `${label} ${expected.edition} openlands requirement version mismatch`)
  }
  if (entry.requiredPublicAlphaEvidenceMatches !== undefined) {
    assert(errors, entry.requiredPublicAlphaEvidenceMatches === true, `${label} ${expected.edition} public alpha evidence must match`)
  }
  if (entry.releaseIndexEntryResolved !== undefined) {
    assert(errors, entry.releaseIndexEntryResolved === true, `${label} ${expected.edition} release index entry must resolve`)
  }
  if (entry.publicListingAllowed !== undefined) {
    assert(errors, entry.publicListingAllowed === true, `${label} ${expected.edition} must be publicly listed`)
  }
  if (entry.indexedState !== undefined) {
    assert(errors, isPassingIndexStatus(entry.indexedState), `${label} ${expected.edition} indexedState must be real indexed state`)
  }
}

function validateIndexedModuleArtifact(errors, artifact, expected, label) {
  assert(errors, artifact && typeof artifact === 'object', `${label} ${expected.edition} requires moduleArtifact`)
  if (!artifact || typeof artifact !== 'object') return
  assert(errors, artifact.moduleId === undefined || artifact.moduleId === MODULE_ID, `${label} ${expected.edition} moduleArtifact moduleId mismatch`)
  assert(errors, artifact.version === undefined || artifact.version === VERSION, `${label} ${expected.edition} moduleArtifact version mismatch`)
  assert(errors, artifact.file === expected.artifactFile || artifact.file === expected.artifactPattern, `${label} ${expected.edition} moduleArtifact file mismatch`)
  assert(errors, typeof artifact.sha256 === 'string' && /^[a-f0-9]{64}$/i.test(artifact.sha256), `${label} ${expected.edition} moduleArtifact must include sha256`)
  assert(errors, Number.isInteger(artifact.size) && artifact.size > 0, `${label} ${expected.edition} moduleArtifact must include positive size`)
  assert(errors, isPublicHttpsUrl(artifact.downloadUrl), `${label} ${expected.edition} moduleArtifact downloadUrl must use a public https URL`)
}

function validatePassedEditionManifestIndexingArea(errors, { artifactPaths, contract }) {
  const label = 'passed distribution approval edition manifest indexing'
  const indexReport = readRequiredArtifactJson(errors, artifactPaths, 'edition-manifest-index-report.json', label)
  const requirementResolution = readRequiredArtifactJson(errors, artifactPaths, 'module-requirement-resolution.json', label)
  const launcherListing = readRequiredArtifactJson(errors, artifactPaths, 'launcher-channel-listing.json', label)
  const expectedEntries = expectedEditionEntries(contract)

  if (indexReport) {
    assert(errors, indexReport.moduleId === MODULE_ID, `${label} report moduleId mismatch`)
    assert(errors, indexReport.moduleVersion === VERSION, `${label} report moduleVersion mismatch`)
    assert(errors, isPassingIndexStatus(indexReport.status), `${label} report status must be passed/indexed`)
    const entries = indexReport.editionResults ?? indexReport.entries
    assertExactEditionIds(errors, entries, expectedEntries, `${label} report`)
    const byId = entriesById(entries)
    for (const expected of expectedEntries) {
      validateIndexedEditionEntry(errors, byId.get(expected.edition), expected, `${label} report`)
    }
  }

  if (requirementResolution) {
    assert(errors, requirementResolution.moduleId === MODULE_ID, `${label} requirement resolution moduleId mismatch`)
    assert(errors, requirementResolution.moduleVersion === VERSION, `${label} requirement resolution moduleVersion mismatch`)
    assert(errors, requirementResolution.status === 'passed', `${label} requirement resolution status must be passed`)
    const requiredIds = requiredCoreModuleIds(contract)
    assert(errors, requiredIds.length > 0, `${label} must know required core modules`)
    assert(errors, sameSet(requirementResolution.requiredCoreModules, requiredIds), `${label} requirement resolution requiredCoreModules mismatch`)
    assert(errors, (requirementResolution.requiredModuleIds ?? []).includes(MODULE_ID), `${label} requirement resolution must include Openlands module id`)
    assertExactEditionIds(errors, requirementResolution.editionResolutions, expectedEntries, `${label} requirement resolution`)
    const byId = entriesById(requirementResolution.editionResolutions)
    for (const expected of expectedEntries) {
      const entry = byId.get(expected.edition)
      validateIndexedEditionEntry(errors, entry, expected, `${label} requirement resolution`, { requireIndexFields: false })
      assert(errors, entry?.passed === true, `${label} requirement resolution ${expected.edition} must pass`)
      assert(errors, sameSet(entry?.requiredCoreModules, requiredIds), `${label} requirement resolution ${expected.edition} required core modules mismatch`)
      assert(errors, entry?.releaseIndexRequiresMatchDescriptor === true, `${label} requirement resolution ${expected.edition} release index requires must match descriptor`)
    }
    if (requirementResolution.summary) {
      assert(errors, requirementResolution.summary.editionCount === expectedEntries.length, `${label} requirement resolution summary editionCount mismatch`)
      assert(errors, requirementResolution.summary.passedCount === expectedEntries.length, `${label} requirement resolution summary passedCount mismatch`)
      assert(errors, requirementResolution.summary.missingRequirementCount === 0, `${label} requirement resolution summary missingRequirementCount must be zero`)
    }
  }

  if (launcherListing) {
    assert(errors, launcherListing.channelId === 'openlands', `${label} launcher channel id mismatch`)
    assert(errors, launcherListing.moduleId === MODULE_ID, `${label} launcher listing moduleId mismatch`)
    assert(errors, launcherListing.moduleVersion === VERSION, `${label} launcher listing moduleVersion mismatch`)
    assert(errors, isPassingIndexStatus(launcherListing.status), `${label} launcher listing status must be passed/indexed`)
    assert(errors, launcherListing.editionCount === expectedEntries.length, `${label} launcher listing editionCount mismatch`)
    assertExactEditionIds(errors, launcherListing.entries, expectedEntries, `${label} launcher listing`)
    const byId = entriesById(launcherListing.entries)
    for (const expected of expectedEntries) {
      const entry = byId.get(expected.edition)
      validateIndexedEditionEntry(errors, entry, expected, `${label} launcher listing`)
      validateIndexedModuleArtifact(errors, entry?.moduleArtifact, expected, `${label} launcher listing`)
    }
  }
}

function firstDefined(...values) {
  return values.find((value) => value !== undefined && value !== null)
}

function truthyField(source, ...keys) {
  return keys.some((key) => source?.[key] === true)
}

function readRequiredArtifactJson(errors, artifactPaths, artifactName, label) {
  const artifactPath = artifactPaths.get(artifactName)
  assert(errors, artifactPath !== undefined && fileExists(artifactPath), `${label} requires ${artifactName}`)
  if (!artifactPath || !fileExists(artifactPath)) return null
  const payload = readJsonIfPossible(artifactPath)
  assert(errors, payload !== null, `${label} ${artifactName} must be valid JSON`)
  return payload
}

function validatePassedCoopPublicAlphaSessionArea(errors, { artifactPaths, edition }) {
  const label = 'passed distribution approval co-op session'
  const session = readRequiredArtifactJson(errors, artifactPaths, 'coop-session-report.json', label)
  const waystone = readRequiredArtifactJson(errors, artifactPaths, 'coop-waystone-state-log.json', label)
  const permissions = readRequiredArtifactJson(errors, artifactPaths, 'coop-permission-transaction-log.json', label)

  if (session) {
    assert(errors, session.hostRuntimeTarget === edition.runtimeTarget, `${label} hostRuntimeTarget must match ${edition.runtimeTarget}`)
    assert(errors, Number.isInteger(session.playerCount) && session.playerCount >= 1 && session.playerCount <= 8, `${label} playerCount must be 1-8`)
    assert(errors, Number.isFinite(session.sessionDurationMinutes) && session.sessionDurationMinutes > 0, `${label} sessionDurationMinutes must be positive`)
    const reconnect = firstDefined(session.disconnectReconnectResult, session.reconnectResult)
    assert(errors, reconnect && typeof reconnect === 'object', `${label} disconnectReconnectResult is required`)
    if (reconnect && typeof reconnect === 'object') {
      assert(errors, truthyField(reconnect, 'statePreserved', 'playerStatePreserved', 'success'), `${label} disconnect/reconnect must preserve player state`)
    }
    const desync = firstDefined(session.authorityDesyncLog, session.authorityDesync)
    if (Array.isArray(desync)) {
      assert(errors, desync.length === 0, `${label} authorityDesyncLog must be empty`)
    } else if (desync && typeof desync === 'object') {
      assert(errors, truthyField(desync, 'noAuthorityDesync', 'empty', 'passed') || desync.desyncDetected === false || desync.count === 0, `${label} authorityDesyncLog must report no desync`)
    } else {
      assert(errors, session.authorityDesyncDetected === false || session.noAuthorityDesync === true, `${label} must report no authority desync`)
    }
  }

  if (waystone) {
    const entries = firstDefined(waystone.entries, waystone.stateSamples, waystone.events)
    if (entries !== undefined) {
      assert(errors, Array.isArray(entries) && entries.length > 0, `${label} waystone log must include state entries`)
    }
    assert(errors,
      truthyField(waystone, 'sharedWaystoneStateStaysConsistent', 'stateConsistent', 'consistent')
        || waystone.desyncDetected === false,
      `${label} waystone state must stay consistent`)
  }

  if (permissions) {
    const transactions = firstDefined(permissions.transactions, permissions.permissionTransactions, permissions.entries)
    if (transactions !== undefined) {
      assert(errors, Array.isArray(transactions) && transactions.length > 0, `${label} permission log must include transactions`)
    }
    assert(errors,
      truthyField(permissions, 'storagePermissionsEnforced', 'permissionsEnforced', 'passed')
        || permissions.unauthorizedWriteAllowed === false,
      `${label} storage permissions must be enforced`)
    assert(errors,
      permissions.duplicationDetected === false
        || permissions.itemDesyncDetected === false
        || permissions.authorityDesyncDetected === false
        || truthyField(permissions, 'noAuthorityDesync', 'noItemDesync'),
      `${label} permission transactions must report no item or authority desync`)
  }
}

function collectReportIndexEntries(value, entries = []) {
  if (Array.isArray(value)) {
    for (const entry of value) collectReportIndexEntries(entry, entries)
    return entries
  }
  if (value === null || typeof value !== 'object') return entries
  const declaredPath = value.path ?? value.reportPath ?? value.file ?? value.artifact
  const declaredHash = value.sha256 ?? value.hash ?? value.reportSha256
  if (typeof declaredPath === 'string' && typeof declaredHash === 'string') {
    entries.push({
      edition: value.edition,
      kind: value.kind ?? value.reportKind ?? value.type,
      path: declaredPath,
      sha256: declaredHash.toLowerCase(),
    })
  }
  for (const child of Object.values(value)) collectReportIndexEntries(child, entries)
  return entries
}

function resolveIndexedPath(declaredPath, workspaceRoot, releaseRoot, editionRoot) {
  if (typeof declaredPath !== 'string' || declaredPath.length === 0) return null
  if (path.isAbsolute(declaredPath)) return path.resolve(declaredPath)
  const candidates = [
    path.resolve(workspaceRoot, declaredPath),
    path.resolve(releaseRoot, declaredPath),
    path.resolve(editionRoot, declaredPath),
  ]
  return candidates.find((candidate) => fileExists(candidate)) ?? candidates[0]
}

function indexHasReport({ indexEntries, expectedPath, expectedHash, edition, workspaceRoot, releaseRoot, editionRoot }) {
  return indexEntries.some((entry) => {
    if (entry.edition && edition && entry.edition !== edition) return false
    const indexedPath = resolveIndexedPath(entry.path, workspaceRoot, releaseRoot, editionRoot)
    return samePath(indexedPath, expectedPath) && entry.sha256 === expectedHash.toLowerCase()
  })
}

function assertReportIndexEntry(errors, { indexEntries, expectedPath, expectedHash, edition, label, workspaceRoot, releaseRoot, editionRoot }) {
  assert(errors, indexHasReport({
    indexEntries,
    expectedPath,
    expectedHash,
    edition,
    workspaceRoot,
    releaseRoot,
    editionRoot,
  }), `passed distribution approval dependency index missing ${label} path/hash`)
}

function reportPathFromContract(workspaceRoot, reportDef) {
  return path.resolve(workspaceRoot, reportDef.repo, reportDef.requiredReport)
}

function validatePassedExecutionReport(errors, { reportPath, expected, reportKind, gateField, readyField, workspaceRoot, releaseRoot, editionRoot, indexEntries }) {
  assert(errors, fileExists(reportPath), `passed distribution approval dependency ${reportKind} report missing: ${reportPath}`)
  if (!fileExists(reportPath)) return
  const reportHash = sha256(reportPath)
  const report = readJsonIfPossible(reportPath)
  assert(errors, report !== null, `passed distribution approval dependency ${reportKind} report must be valid JSON: ${reportPath}`)
  if (!report) return
  assert(errors, report.edition === expected.edition, `passed distribution approval dependency ${reportKind} report edition mismatch for ${expected.edition}`)
  assert(errors, report.runtimeTarget === expected.runtimeTarget, `passed distribution approval dependency ${reportKind} report runtime target mismatch for ${expected.edition}`)
  assert(errors, report.moduleId === MODULE_ID, `passed distribution approval dependency ${reportKind} report module id mismatch for ${expected.edition}`)
  assert(errors, report.moduleVersion === VERSION, `passed distribution approval dependency ${reportKind} report module version mismatch for ${expected.edition}`)
  assert(errors, report.status === 'passed', `passed distribution approval dependency ${reportKind} report must be passed for ${expected.edition}`)
  assert(errors, report[readyField] === true, `passed distribution approval dependency ${reportKind} report must set ${readyField} true for ${expected.edition}`)
  assert(errors, Array.isArray(report[gateField]) && report[gateField].length === 0, `passed distribution approval dependency ${reportKind} report must have no ${gateField} for ${expected.edition}`)
  assertReportIndexEntry(errors, {
    indexEntries,
    expectedPath: reportPath,
    expectedHash: reportHash,
    edition: expected.edition,
    label: `${reportKind} ${expected.edition}`,
    workspaceRoot,
    releaseRoot,
    editionRoot,
  })
}

function validateDependencyGateSummary(errors, summaryPath) {
  const summary = readJsonIfPossible(summaryPath)
  assert(errors, summary !== null, 'passed distribution approval dependency-gate-summary.json must be valid JSON')
  if (!summary) return
  for (const [field, label] of [
    ['runtimeExecutionReportsPassed', 'runtime execution reports'],
    ['launcherExecutionReportsPassed', 'launcher execution reports'],
    ['finalReleaseReviewReportsPassed', 'final release review reports'],
    ['releaseReadinessReportHasNoBlockers', 'release readiness no blockers'],
    ['approvedReleaseIndexState', 'approved release index state'],
  ]) {
    if (Object.prototype.hasOwnProperty.call(summary, field)) {
      assert(errors, summary[field] === true, `passed distribution approval dependency summary must confirm ${label}`)
    }
  }
  for (const field of ['remainingReadinessBlockers', 'readinessBlockers']) {
    if (Array.isArray(summary[field])) {
      assert(errors, summary[field].length === 0, `passed distribution approval dependency summary ${field} must be empty`)
    }
  }
}

function validatePassedDependencyArea(errors, { artifactPaths, contract, workspaceRoot, releaseRoot, editionRoot }) {
  const readinessPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const readinessHashPath = artifactPaths.get('release-readiness-hash.txt')
  const reportIndexPath = artifactPaths.get('approval-input-report-index.json')
  const summaryPath = artifactPaths.get('dependency-gate-summary.json')

  validateDependencyGateSummary(errors, summaryPath)
  const reportIndex = readJsonIfPossible(reportIndexPath)
  assert(errors, reportIndex !== null, 'passed distribution approval approval-input-report-index.json must be valid JSON')
  const indexEntries = reportIndex ? collectReportIndexEntries(reportIndex) : []
  assert(errors, indexEntries.length > 0, 'passed distribution approval approval-input-report-index.json must list dependency report paths and hashes')

  assert(errors, fileExists(readinessPath), `passed distribution approval release readiness report missing: ${readinessPath}`)
  if (fileExists(readinessPath)) {
    const readinessHash = sha256(readinessPath)
    assert(errors, readinessHashPath !== undefined && fileExists(readinessHashPath), 'passed distribution approval release-readiness-hash.txt is required')
    if (readinessHashPath && fileExists(readinessHashPath)) {
      assert(errors, normalizeHashText(readText(readinessHashPath)) === readinessHash, 'passed distribution approval release-readiness-hash.txt must match current readiness report')
    }
    const readiness = readJsonIfPossible(readinessPath)
    assert(errors, readiness !== null, 'passed distribution approval release readiness report must be valid JSON')
    if (readiness) {
      assert(errors, readiness.publicAlphaReady === true, 'passed distribution approval dependency requires release readiness publicAlphaReady true')
      assert(errors, Array.isArray(readiness.blockers) && readiness.blockers.length === 0, 'passed distribution approval dependency requires release readiness blockers to be empty')
      assertReportIndexEntry(errors, {
        indexEntries,
        expectedPath: readinessPath,
        expectedHash: readinessHash,
        label: 'release readiness',
        workspaceRoot,
        releaseRoot,
        editionRoot,
      })
    }
  }

  for (const expected of contract.runtimeExecution.editionReports ?? []) {
    validatePassedExecutionReport(errors, {
      reportPath: reportPathFromContract(workspaceRoot, expected),
      expected,
      reportKind: 'runtime execution',
      gateField: 'remainingRuntimeGates',
      readyField: 'publicAlphaReady',
      workspaceRoot,
      releaseRoot,
      editionRoot,
      indexEntries,
    })
  }
  for (const expected of contract.launcherExecution.editionReports ?? []) {
    validatePassedExecutionReport(errors, {
      reportPath: reportPathFromContract(workspaceRoot, expected),
      expected,
      reportKind: 'launcher execution',
      gateField: 'remainingLauncherGates',
      readyField: 'publicAlphaReady',
      workspaceRoot,
      releaseRoot,
      editionRoot,
      indexEntries,
    })
  }
  for (const expected of contract.finalReleaseReview.editionReports ?? []) {
    validatePassedExecutionReport(errors, {
      reportPath: reportPathFromContract(workspaceRoot, expected),
      expected,
      reportKind: 'final release review',
      gateField: 'remainingFinalReviewGates',
      readyField: 'publicReleaseReady',
      workspaceRoot,
      releaseRoot,
      editionRoot,
      indexEntries,
    })
  }
}

function validatePassedPublicAlphaApprovalArea(errors, { artifactPaths, contract, releaseRoot, report }) {
  const approvalMemoPath = artifactPaths.get('public-alpha-approval.md')
  const rollbackPlanPath = artifactPaths.get('rollback-plan-snapshot.md')
  const approvedReadinessPath = artifactPaths.get('approved-readiness-report.json')
  const approvedPhaseReadinessPath = artifactPaths.get('approved-readiness-report-by-phase.md')
  const currentReadinessPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const currentPhaseReadinessPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report-by-phase.md')
  const approvedReadiness = readJsonIfPossible(approvedReadinessPath)
  assert(errors, approvedReadiness !== null, 'passed distribution approval approved-readiness-report.json must be valid JSON')
  if (!approvedReadiness) return
  assert(errors, approvedReadiness.publicAlphaReady === true, 'passed distribution approval approved readiness report must be publicAlphaReady')
  assert(errors, Array.isArray(approvedReadiness.blockers) && approvedReadiness.blockers.length === 0, 'passed distribution approval approved readiness report blockers must be empty')
  assert(errors, approvedReadiness.phaseReadiness?.schema === 'echo.openlands.release_phase_readiness.v1', 'passed distribution approval approved readiness report phaseReadiness schema mismatch')
  assert(errors, Number.isInteger(approvedReadiness.phaseReadiness?.phaseCount) && approvedReadiness.phaseReadiness.phaseCount > 0, 'passed distribution approval approved readiness report phaseReadiness must list phases')
  assert(errors, (approvedReadiness.phaseReadiness?.phases ?? []).length === approvedReadiness.phaseReadiness?.phaseCount, 'passed distribution approval approved readiness report phaseReadiness phase count mismatch')
  assert(errors, approvedReadiness.phaseReadiness?.blockedPhaseCount === 0, 'passed distribution approval approved readiness report phaseReadiness must have no blocked phases')
  assert(errors, approvedReadiness.phaseReadiness?.blockerCount === 0, 'passed distribution approval approved readiness report phaseReadiness must have no blockers')
  assert(errors, Array.isArray(approvedReadiness.phaseReadiness?.unmappedBlockers) && approvedReadiness.phaseReadiness.unmappedBlockers.length === 0, 'passed distribution approval approved readiness report phaseReadiness must have no unmapped blockers')
  assert(errors, (approvedReadiness.phaseReadiness?.phases ?? []).every((phase) => phase.readyForPublicAlpha === true && phase.status === 'ready' && (phase.activeBlockers ?? []).length === 0), 'passed distribution approval approved readiness report phaseReadiness must mark every phase ready')
  assert(errors, fileExists(currentReadinessPath), `passed distribution approval current readiness report missing: ${currentReadinessPath}`)
  assert(errors, fileExists(currentPhaseReadinessPath), `passed distribution approval current phase readiness report missing: ${currentPhaseReadinessPath}`)
  assert(errors, approvedPhaseReadinessPath !== undefined && fileExists(approvedPhaseReadinessPath), 'passed distribution approval approved-readiness-report-by-phase.md is required')
  const approvedReadinessHash = sha256(approvedReadinessPath)
  const approvedPhaseReadinessHash = approvedPhaseReadinessPath && fileExists(approvedPhaseReadinessPath) ? sha256(approvedPhaseReadinessPath) : null
  if (fileExists(currentReadinessPath)) {
    assert(errors, approvedReadinessHash === sha256(currentReadinessPath), 'passed distribution approval approved-readiness-report.json must match current readiness report')
  }
  if (approvedPhaseReadinessPath && fileExists(approvedPhaseReadinessPath) && fileExists(currentPhaseReadinessPath)) {
    assert(errors, approvedPhaseReadinessHash === sha256(currentPhaseReadinessPath), 'passed distribution approval approved-readiness-report-by-phase.md must match current phase readiness report')
  }

  const approver = typeof report.approvalRun?.approver === 'string' ? report.approvalRun.approver : ''
  const approvalDate = typeof report.approvalRun?.approvalDate === 'string' ? report.approvalRun.approvalDate : ''
  assert(errors, approver.length > 0, 'passed public alpha approval area requires approvalRun.approver')
  assert(errors, approvalDate.length > 0, 'passed public alpha approval area requires approvalRun.approvalDate')

  if (approvalMemoPath && fileExists(approvalMemoPath)) {
    const approvalMemo = readText(approvalMemoPath)
    assertNoUnresolvedApprovalMarkers(errors, approvalMemo, 'passed distribution approval public-alpha-approval.md')
    for (const needle of ['Openlands', 'Public Alpha', 'approval', 'readiness', 'phase readiness', 'launch roadmap', 'production phase matrix', 'Hardlands', 'optional', approvedReadinessHash, approvedPhaseReadinessHash].filter(Boolean)) {
      assertTextIncludes(errors, approvalMemo, needle, 'passed distribution approval public-alpha-approval.md')
    }
    if (approver) assertTextIncludes(errors, approvalMemo, approver, 'passed distribution approval public-alpha-approval.md')
    if (approvalDate) assertTextIncludes(errors, approvalMemo, approvalDate.split('T')[0], 'passed distribution approval public-alpha-approval.md')

    const requiredEvidenceIds = sortedUnique((contract.acceptance.distributionGates ?? [])
      .flatMap((gate) => gate.clearsEvidence ?? []))
    for (const evidenceId of requiredEvidenceIds) {
      assertTextIncludes(errors, approvalMemo, evidenceId, 'passed distribution approval public-alpha-approval.md')
    }
  }

  if (rollbackPlanPath && fileExists(rollbackPlanPath)) {
    const rollbackPlan = readText(rollbackPlanPath)
    assertNoUnresolvedApprovalMarkers(errors, rollbackPlan, 'passed distribution approval rollback-plan-snapshot.md')
    for (const needle of ['rollback', 'launcher', 'manifest', 'Release Index', 'download', 'world']) {
      assertTextIncludes(errors, rollbackPlan, needle, 'passed distribution approval rollback-plan-snapshot.md')
    }
    for (const target of contract.releasePublication.artifactTargets ?? []) {
      assertTextIncludes(errors, rollbackPlan, target.file, 'passed distribution approval rollback-plan-snapshot.md')
    }
  }
}

function jsonTextIncludes(value, needle) {
  return JSON.stringify(value).includes(String(needle ?? ''))
}

function validateArtifactPublicationCoverageArtifact(errors, { artifactPaths, artifactName, contract, releaseIndex, releaseModule, requireHashAndSize }) {
  const artifactPath = artifactPaths.get(artifactName)
  const payload = readJsonIfPossible(artifactPath)
  assert(errors, payload !== null, `passed artifact publication ${artifactName} must be valid JSON`)
  if (!payload) return
  if (Object.prototype.hasOwnProperty.call(payload, 'moduleId')) {
    assert(errors, payload.moduleId === MODULE_ID, `passed artifact publication ${artifactName} module id mismatch`)
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'moduleVersion')) {
    assert(errors, payload.moduleVersion === VERSION, `passed artifact publication ${artifactName} module version mismatch`)
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'releaseId')) {
    assert(errors, payload.releaseId === releaseIndex?.releaseId, `passed artifact publication ${artifactName} release id mismatch`)
  }
  for (const target of contract.releasePublication.artifactTargets ?? []) {
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(errors, releaseArtifact !== undefined, `passed artifact publication ${artifactName} Release Index missing ${target.file}`)
    if (!releaseArtifact) continue
    assert(errors, jsonTextIncludes(payload, target.file), `passed artifact publication ${artifactName} must cover ${target.file}`)
    assert(errors, jsonTextIncludes(payload, releaseArtifact.downloadUrl), `passed artifact publication ${artifactName} must record ${target.file} downloadUrl`)
    if (requireHashAndSize) {
      assert(errors, jsonTextIncludes(payload, releaseArtifact.sha256), `passed artifact publication ${artifactName} must record ${target.file} sha256`)
      assert(errors, jsonTextIncludes(payload, releaseArtifact.size), `passed artifact publication ${artifactName} must record ${target.file} size`)
    }
  }
}

function validateArtifactPublicationDownloadVerificationReport(errors, { artifactPaths, contract, releaseIndex, releaseModule }) {
  const artifactPath = artifactPaths.get('download-verification-report.json')
  const payload = readJsonIfPossible(artifactPath)
  assert(errors, payload !== null, 'passed artifact publication download-verification-report.json must be valid JSON')
  if (!payload) return
  assert(errors, payload.schema === 'echo.openlands.release_publication_download_verification_summary.v1', 'passed artifact publication download-verification-report.json schema mismatch')
  assert(errors, payload.moduleId === MODULE_ID, 'passed artifact publication download-verification-report.json module id mismatch')
  assert(errors, payload.moduleVersion === VERSION, 'passed artifact publication download-verification-report.json module version mismatch')
  assert(errors, payload.releaseId === releaseIndex?.releaseId, 'passed artifact publication download-verification-report.json release id mismatch')
  const results = Array.isArray(payload.verificationResults) ? payload.verificationResults : []
  const targets = contract.releasePublication.artifactTargets ?? []
  assert(errors, payload.artifactCount === targets.length, 'passed artifact publication download-verification-report.json artifact count mismatch')
  assert(errors, sameSet(results.map((entry) => entry.id), targets.map((target) => target.id)), 'passed artifact publication download-verification-report.json artifact ids mismatch')
  for (const target of targets) {
    const result = results.find((entry) => entry.id === target.id)
    assert(errors, result !== undefined, `passed artifact publication download-verification-report.json missing ${target.id}`)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(errors, releaseArtifact !== undefined, `passed artifact publication download-verification-report.json Release Index missing ${target.file}`)
    if (!result || !releaseArtifact) continue
    assert(errors, result.file === target.file, `passed artifact publication download-verification-report.json ${target.id} file mismatch`)
    assert(errors, isPublicHttpsUrl(result.downloadUrl), `passed artifact publication download-verification-report.json ${target.id} downloadUrl must use a public https URL`)
    assert(errors, result.downloadUrl === releaseArtifact.downloadUrl, `passed artifact publication download-verification-report.json ${target.id} downloadUrl mismatch`)
    assert(errors, isPublicHttpsUrl(result.finalUrl), `passed artifact publication download-verification-report.json ${target.id} finalUrl must use a public https URL`)
    assert(errors, result.statusCode === 200, `passed artifact publication download-verification-report.json ${target.id} statusCode must be 200`)
    assert(errors, result.expectedSha256 === releaseArtifact.sha256, `passed artifact publication download-verification-report.json ${target.id} expected sha mismatch`)
    assert(errors, result.downloadedSha256 === releaseArtifact.sha256, `passed artifact publication download-verification-report.json ${target.id} downloaded sha mismatch`)
    assert(errors, result.expectedSize === releaseArtifact.size, `passed artifact publication download-verification-report.json ${target.id} expected size mismatch`)
    assert(errors, result.downloadedSize === releaseArtifact.size, `passed artifact publication download-verification-report.json ${target.id} downloaded size mismatch`)
    assert(errors, result.sha256Matches === true, `passed artifact publication download-verification-report.json ${target.id} must confirm sha match`)
    assert(errors, result.sizeMatches === true, `passed artifact publication download-verification-report.json ${target.id} must confirm size match`)
    if (result.contentLength !== null && result.contentLength !== undefined) {
      assert(errors, Number.parseInt(result.contentLength, 10) === releaseArtifact.size, `passed artifact publication download-verification-report.json ${target.id} content length mismatch`)
    }
  }
}

function validatePassedArtifactPublicationArtifacts(errors, { artifactPaths, contract, releaseIndex, releaseModule }) {
  validateArtifactPublicationCoverageArtifact(errors, {
    artifactPaths,
    artifactName: 'artifact-publication-report.json',
    contract,
    releaseIndex,
    releaseModule,
    requireHashAndSize: true,
  })
  validateArtifactPublicationDownloadVerificationReport(errors, {
    artifactPaths,
    contract,
    releaseIndex,
    releaseModule,
  })
  validateArtifactPublicationCoverageArtifact(errors, {
    artifactPaths,
    artifactName: 'release-index-diff.json',
    contract,
    releaseIndex,
    releaseModule,
    requireHashAndSize: false,
  })
}

function validateVerifiedPublicationManifestDependency(errors, { moduleRoot, workspaceRoot, releaseRoot, contract, releaseIndex, releaseModule }) {
  const manifestPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json')
  assert(errors, fileExists(manifestPath), `passed artifact publication requires verified publication manifest: ${manifestPath}`)
  if (!fileExists(manifestPath)) return
  const validation = validatePublicationManifest({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    manifestPath,
  })
  assert(errors, validation.status === 'passed', `passed artifact publication verified publication manifest failed validation: ${validation.errors.join('; ')}`)
  assert(errors, validation.manifestStatus === 'verified', 'passed artifact publication requires publication manifest status verified')
  const manifest = readJsonIfPossible(manifestPath)
  assert(errors, manifest !== null, 'passed artifact publication verified manifest must be valid JSON')
  if (!manifest) return
  assert(errors, manifest.releaseId === releaseIndex?.releaseId, 'passed artifact publication verified manifest releaseId mismatch')
  assert(errors, manifest.summary?.downloadVerifiedCount === (contract.releasePublication.artifactTargets ?? []).length, 'passed artifact publication verified manifest must verify every artifact download')
  assert(errors, manifest.summary?.releaseIndexPatchAllowedCount === 0, 'passed artifact publication verified manifest must not allow Release Index patch before approval')
  for (const target of contract.releasePublication.artifactTargets ?? []) {
    const publication = publicationById(manifest, target.id)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(errors, publication !== undefined, `passed artifact publication verified manifest missing ${target.id}`)
    assert(errors, releaseArtifact !== undefined, `passed artifact publication verified manifest Release Index missing ${target.file}`)
    if (!publication || !releaseArtifact) continue
    assert(errors, publication.file === target.file, `passed artifact publication verified manifest ${target.id} file mismatch`)
    assert(errors, publication.urlStatus === 'download_verified', `passed artifact publication verified manifest ${target.id} must be download_verified`)
    assert(errors, isPublicHttpsUrl(publication.downloadUrl), `passed artifact publication verified manifest ${target.id} downloadUrl must use a public https URL`)
    assert(errors, publication.sha256 === releaseArtifact.sha256, `passed artifact publication verified manifest ${target.id} sha mismatch`)
    assert(errors, publication.size === releaseArtifact.size, `passed artifact publication verified manifest ${target.id} size mismatch`)
    assert(errors, publication.downloadVerification?.downloadedSha256 === releaseArtifact.sha256, `passed artifact publication verified manifest ${target.id} downloaded sha mismatch`)
    assert(errors, publication.downloadVerification?.downloadedSize === releaseArtifact.size, `passed artifact publication verified manifest ${target.id} downloaded size mismatch`)
    assert(errors, publication.downloadVerification?.sha256Matches === true, `passed artifact publication verified manifest ${target.id} must confirm sha match`)
    assert(errors, publication.downloadVerification?.sizeMatches === true, `passed artifact publication verified manifest ${target.id} must confirm size match`)
    assert(errors, publication.releaseIndexPatch?.patchAllowed === false, `passed artifact publication verified manifest ${target.id} must not allow patch before approval`)
    assert(errors, publication.releaseIndexPatch?.patchApplied === false, `passed artifact publication verified manifest ${target.id} must not apply patch before approval`)
  }
}

function buildContract(moduleRoot) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const harness = readJson(path.join(dataRoot, 'systems', 'distribution_approval_harness_plan.json'))
  const releasePublication = readJson(path.join(dataRoot, 'systems', 'release_publication_manifest_contract.json'))
  const runtimeExecution = readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json'))
  const launcherExecution = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const finalReleaseReview = readJson(path.join(dataRoot, 'systems', 'final_release_review_acceptance.json'))
  const descriptor = readJson(path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json'))
  return {
    acceptance,
    harness,
    releasePublication,
    runtimeExecution,
    launcherExecution,
    finalReleaseReview,
    descriptor,
    expectedAreaIds: sortedUnique((acceptance.approvalAreas ?? []).map((area) => area.id)),
    expectedGateIds: sortedUnique((acceptance.distributionGates ?? []).map((gate) => gate.id)),
    areaById: new Map((acceptance.approvalAreas ?? []).map((area) => [area.id, area])),
    gateById: new Map((acceptance.distributionGates ?? []).map((gate) => [gate.id, gate])),
    harnessBindingById: new Map((harness.approvalAreaBindings ?? []).map((area) => [area.id, area])),
    editionHarnessById: new Map((harness.editionHarnesses ?? []).map((entry) => [entry.edition, entry])),
  }
}

function validateReport({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath, allowMissing }) {
  const errors = []
  const warnings = []
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)

  const contract = buildContract(moduleRoot)
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const expectedEditionReport = (contract.acceptance.editionReports ?? []).find((entry) => entry.edition === editionKey)
  const expectedEditionHarness = (contract.harness.editionHarnesses ?? []).find((entry) => entry.edition === editionKey)
  assert(errors, expectedEditionReport !== undefined, `distribution approval acceptance missing edition ${editionKey}`)
  assert(errors, expectedEditionReport?.runtimeTarget === edition.runtimeTarget, `distribution approval runtime target mismatch for ${editionKey}`)
  assert(errors, contract.harness.schema === 'echo.openlands.systems.distribution_approval_harness_plan.v1', 'distribution approval harness schema mismatch')
  assert(errors, contract.harness.sourceContracts?.distributionApprovalAcceptance === 'systems/distribution_approval_acceptance.json', 'distribution approval harness source contract mismatch')
  assert(errors, sameSet((contract.harness.approvalAreaBindings ?? []).map((area) => area.id), contract.expectedAreaIds), 'distribution approval harness must cover every approval area')
  assert(errors, expectedEditionHarness !== undefined, `distribution approval harness missing edition ${editionKey}`)
  assert(errors, expectedEditionHarness?.runtimeTarget === edition.runtimeTarget, `distribution approval harness runtime target mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.requiredReport === expectedEditionReport?.requiredReport, `distribution approval harness report path mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.artifactPattern === expectedEditionReport?.artifactPattern, `distribution approval harness artifact pattern mismatch for ${editionKey}`)
  for (const expectedArea of contract.acceptance.approvalAreas ?? []) {
    const binding = contract.harnessBindingById.get(expectedArea.id)
    assert(errors, binding !== undefined, `distribution approval harness missing binding ${expectedArea.id}`)
    if (!binding) continue
    assert(errors, sameSet(binding.gateIds, expectedArea.gateIds), `distribution approval harness binding ${expectedArea.id} gateIds mismatch`)
    assert(errors, sameSet(binding.inputFixtureRefs, expectedArea.inputFixtureRefs), `distribution approval harness binding ${expectedArea.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(binding.checklist, expectedArea.checklist), `distribution approval harness binding ${expectedArea.id} checklist mismatch`)
    assert(errors, sameSet(binding.requiredSavedArtifacts, expectedArea.requiredSavedArtifacts), `distribution approval harness binding ${expectedArea.id} requiredSavedArtifacts mismatch`)
  }

  const resolvedReportPath = reportPath
    ? path.resolve(reportPath)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const approvalArtifactRoot = path.resolve(editionRoot, expectedEditionHarness?.approvalArtifactRoot ?? path.join('evidence', 'distribution-approval', editionKey))

  if (!fileExists(resolvedReportPath)) {
    return {
      status: allowMissing ? 'missing' : 'failed',
      edition: editionKey,
      runtimeTarget: edition.runtimeTarget,
      reportPath: resolvedReportPath,
      expectedApprovalAreaCount: contract.expectedAreaIds.length,
      expectedDistributionGateCount: contract.expectedGateIds.length,
      expectedHarnessDriverCount: contract.harness.driverSurfaces?.length ?? 0,
      missingReason: 'distribution approval report has not been produced by the real release approval process yet',
      errors: allowMissing ? [] : [`distribution approval report missing: ${resolvedReportPath}`],
      warnings,
    }
  }

  const report = readJson(resolvedReportPath)
  const reportContract = contract.acceptance.reportContract ?? {}
  const approvalResults = report.approvalResults ?? []
  const approvalResultById = new Map(approvalResults.map((area) => [area.id, area]))
  const allowedReportStatus = reportContract.allowedReportStatus ?? ['passed', 'failed', 'blocked']
  const allowedApprovalStatus = reportContract.allowedApprovalStatus ?? ['passed', 'failed', 'blocked', 'skipped']
  const allowedChecklistStatus = reportContract.allowedChecklistStatus ?? ['passed', 'failed', 'blocked', 'skipped']

  for (const field of reportContract.requiredReportFields ?? []) {
    requireFields(errors, report, [field], 'distribution approval report')
  }
  assert(errors, report.schema === reportContract.schema, 'distribution approval report schema mismatch')
  assert(errors, allowedReportStatus.includes(report.status), `distribution approval status must be one of ${allowedReportStatus.join(', ')}`)
  assert(errors, report.edition === editionKey, `distribution approval edition must be ${editionKey}`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `distribution approval runtimeTarget must be ${edition.runtimeTarget}`)
  assert(errors, report.moduleId === MODULE_ID, `distribution approval moduleId must be ${MODULE_ID}`)
  assert(errors, report.moduleVersion === VERSION, `distribution approval moduleVersion must be ${VERSION}`)

  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  if (fileExists(releaseIndexPath)) {
    assert(errors, report.releaseId === releaseIndex.releaseId, 'distribution approval releaseId mismatch')
    if (report.releaseIndex?.hash) {
      assert(errors, report.releaseIndex.hash === sha256(releaseIndexPath), 'distribution approval releaseIndex hash mismatch')
    }
  } else {
    warnings.push('release index file missing; report releaseId was checked only for presence')
  }

  assert(errors, sameSet(approvalResults.map((area) => area.id), contract.expectedAreaIds), 'distribution approval report must contain exactly the approval areas')
  for (const area of approvalResults) {
    const expected = contract.areaById.get(area.id)
    assert(errors, expected !== undefined, `distribution approval contains unknown area ${area.id}`)
    if (!expected) continue
    for (const field of reportContract.requiredApprovalFields ?? []) {
      requireFields(errors, area, [field], `distribution approval area ${area.id}`)
    }
    assert(errors, sameSet(area.gateIds, expected.gateIds), `distribution approval area ${area.id} gateIds mismatch`)
    assert(errors, allowedApprovalStatus.includes(area.status), `distribution approval area ${area.id} status must be allowed`)
    assert(errors, sameSet(area.evidenceRefs, expected.inputFixtureRefs), `distribution approval area ${area.id} evidenceRefs mismatch`)
    const checklist = area.checklist ?? []
    assert(errors, sameSet(checklist.map((item) => item.id), expected.checklist), `distribution approval area ${area.id} checklist mismatch`)
    for (const item of checklist) {
      for (const field of reportContract.requiredChecklistFields ?? []) {
        requireFields(errors, item, [field], `distribution approval checklist ${area.id}/${item.id}`)
      }
      assert(errors, allowedChecklistStatus.includes(item.status), `distribution approval checklist ${area.id}/${item.id} status must be allowed`)
      if (area.status === 'passed') {
        assert(errors, item.status === 'passed', `passed distribution approval area ${area.id} has non-passed checklist item ${item.id}`)
      }
    }
    if (area.status === 'passed') {
      assert(errors, sameSet(area.savedArtifacts, expected.requiredSavedArtifacts), `passed distribution approval area ${area.id} savedArtifacts mismatch`)
      const savedArtifactPaths = new Map()
      for (const artifactName of expected.requiredSavedArtifacts ?? []) {
        const artifactPath = resolveSavedArtifactPath({
          artifactName,
          editionRoot,
          reportPath: resolvedReportPath,
          approvalArtifactRoot,
          releaseRoot,
        })
        assert(errors, artifactPath !== null, `passed distribution approval area ${area.id} missing saved artifact file ${artifactName}`)
        if (artifactPath) {
          savedArtifactPaths.set(artifactName, artifactPath)
          assert(errors, fs.statSync(artifactPath).size > 0, `passed distribution approval area ${area.id} saved artifact ${artifactName} must not be empty`)
          validateRealSavedArtifact(errors, {
            artifactPath,
            expectedArtifactPath: realApprovalSavedArtifactPath({
              artifactName,
              approvalArtifactRoot,
            }),
            artifactName,
            areaId: area.id,
          })
          if (area.id === 'edition_manifest_indexing') {
            assertNotPreviewArtifact(errors, artifactPath, `passed distribution approval area ${area.id} saved artifact ${artifactName}`)
          }
        }
      }
      if (area.id === 'runtime_launcher_final_review_dependency') {
        validatePassedDependencyArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          workspaceRoot,
          releaseRoot,
          editionRoot,
        })
      }
      if (area.id === 'coop_public_alpha_session') {
        validatePassedCoopPublicAlphaSessionArea(errors, {
          artifactPaths: savedArtifactPaths,
          edition,
        })
      }
      if (area.id === 'edition_manifest_indexing') {
        validatePassedEditionManifestIndexingArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
        })
      }
      if (area.id === 'public_alpha_approval') {
        validatePassedPublicAlphaApprovalArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          releaseRoot,
          report,
        })
      }
      if (area.id === 'artifact_publication') {
        assert(errors, report.releaseIndex?.artifactDownloadUrlsPresent === true, 'passed artifact publication requires releaseIndex.artifactDownloadUrlsPresent true')
        assert(errors, report.releaseIndex?.approvedState === true, 'passed artifact publication requires releaseIndex.approvedState true')
        assert(errors, releaseModule !== undefined, 'passed artifact publication requires Openlands module in Release Index')
        validatePassedArtifactPublicationArtifacts(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          releaseIndex,
          releaseModule,
        })
        validateVerifiedPublicationManifestDependency(errors, {
          moduleRoot,
          workspaceRoot,
          releaseRoot,
          contract,
          releaseIndex,
          releaseModule,
        })
        for (const target of contract.releasePublication.artifactTargets ?? []) {
          const releaseArtifact = artifactByFile(releaseModule, target.file)
          assert(errors, releaseArtifact !== undefined, `passed artifact publication release index missing ${target.file}`)
          assert(errors, isPublicHttpsUrl(releaseArtifact?.downloadUrl), `passed artifact publication ${target.file} downloadUrl must use a public https URL`)
          assert(errors, releaseArtifact?.sha256 && /^[a-f0-9]{64}$/i.test(releaseArtifact.sha256), `passed artifact publication ${target.file} must have sha256`)
          assert(errors, Number.isInteger(releaseArtifact?.size) && releaseArtifact.size > 0, `passed artifact publication ${target.file} must have positive size`)
        }
      }
    }
  }

  const gatesClearedByPassingAreas = contract.expectedGateIds.filter((gateId) =>
    (contract.acceptance.approvalAreas ?? [])
      .filter((area) => (area.gateIds ?? []).includes(gateId))
      .every((area) => approvalResultById.get(area.id)?.status === 'passed'))
  const clearedDistributionGates = sortedUnique(report.clearedDistributionGates ?? [])
  const remainingDistributionGates = sortedUnique(report.remainingDistributionGates ?? [])

  assert(errors, sameSet([...clearedDistributionGates, ...remainingDistributionGates], contract.expectedGateIds), 'distribution approval cleared and remaining gates must cover every distribution gate')
  for (const gateId of clearedDistributionGates) {
    assert(errors, gatesClearedByPassingAreas.includes(gateId), `distribution approval clears ${gateId} without all approval areas passing`)
  }
  for (const gateId of remainingDistributionGates) {
    assert(errors, contract.expectedGateIds.includes(gateId), `distribution approval remainingDistributionGates contains unknown gate ${gateId}`)
  }
  for (const gateId of clearedDistributionGates) {
    assert(errors, !remainingDistributionGates.includes(gateId), `distribution approval gate ${gateId} cannot be both cleared and remaining`)
  }

  if (report.status === 'passed') {
    assert(errors, approvalResults.every((area) => area.status === 'passed'), 'passed distribution approval requires every area to pass')
    assert(errors, sameSet(clearedDistributionGates, contract.expectedGateIds), 'passed distribution approval must clear every distribution gate')
    assert(errors, remainingDistributionGates.length === 0, 'passed distribution approval must have no remaining distribution gates')
    assert(errors, report.approvalRun?.approver, 'passed distribution approval requires approver')
    assert(errors, report.approvalRun?.approvalDate, 'passed distribution approval requires approvalDate')
  } else {
    assert(errors, report.publicAlphaReady === false, 'non-passed distribution approval must not mark publicAlphaReady true')
  }
  if (report.publicAlphaReady === true) {
    assert(errors, report.status === 'passed', 'publicAlphaReady true requires passed status')
    assert(errors, sameSet(clearedDistributionGates, contract.expectedGateIds), 'publicAlphaReady true requires every distribution gate cleared')
  }
  if (report.status === 'blocked' && report.generatedBy === 'generate-openlands-distribution-approval-report.mjs') {
    const generated = runGeneratorJson({
      moduleRoot,
      releaseRoot,
      editionRoot,
      editionKey,
      reportPath: resolvedReportPath,
    })
    if (generated.error) {
      errors.push(generated.error)
    } else if (generated.json?.status === 'blocked') {
      assert(
        errors,
        sameJson(stableDistributionApprovalReport(report), stableDistributionApprovalReport(generated.json)),
        'distribution approval report stale against generator dry-run',
      )
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    edition: editionKey,
    runtimeTarget: edition.runtimeTarget,
    reportPath: resolvedReportPath,
    approvalAreaCount: approvalResults.length,
    reportStatus: report.status,
    distributionGateCount: contract.expectedGateIds.length,
    harnessDriverCount: contract.harness.driverSurfaces?.length ?? 0,
    clearedDistributionGates: clearedDistributionGates.length,
    remainingDistributionGates: remainingDistributionGates.length,
    publicAlphaReady: report.publicAlphaReady === true,
    errors,
    warnings,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const moduleRoot = findModuleRoot(args.moduleRoot, editionRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const result = validateReport({
    moduleRoot,
    releaseRoot,
    editionRoot,
    editionKey: args.edition,
    reportPath: args.report,
    allowMissing: args.allowMissing,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands ${result.edition} distribution approval report validated (${result.reportStatus}): ${result.approvalAreaCount} areas, ${result.clearedDistributionGates}/${result.distributionGateCount} gates cleared.`)
    for (const warning of result.warnings) console.warn(`warning: ${warning}`)
  } else if (result.status === 'missing') {
    console.log(`Openlands ${result.edition} distribution approval report missing: ${result.reportPath}`)
  } else {
    console.error(`Openlands ${result.edition} distribution approval report failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-distribution-approval-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Distribution approval report path. Defaults to evidence/<edition>-distribution-approval-report.json.
  --allow-missing         Return status "missing" instead of failing when the report does not exist.
  --json                  Print JSON output.
  --help                  Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
