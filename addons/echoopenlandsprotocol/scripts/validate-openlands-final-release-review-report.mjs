import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    runtimeTarget: 'echo_native',
    reportName: 'native-final-release-review-report.json',
    legalReportName: 'native-legal-content-audit.json',
    artifactKind: 'echo-addon',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    reportName: 'neoforge-final-release-review-report.json',
    legalReportName: 'neoforge-legal-content-audit.json',
    artifactKind: 'neoforge',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    reportName: 'standalone-final-release-review-report.json',
    legalReportName: 'standalone-legal-content-audit.json',
    artifactKind: 'standalone',
  },
}

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
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

function firstDefined(...values) {
  return values.find((value) => value !== undefined && value !== null)
}

function truthyField(source, ...keys) {
  return keys.some((key) => source?.[key] === true)
}

function textIncludes(text, needle) {
  return text.toLowerCase().includes(String(needle ?? '').toLowerCase())
}

function assertTextIncludes(errors, text, needle, label) {
  assert(errors, textIncludes(text, needle), `${label} must mention ${needle}`)
}

function assertNoUnresolvedReviewMarkers(errors, text, label) {
  for (const marker of ['todo', 'tbd', 'placeholder', 'not reviewed', 'not_reviewed', 'preflight only', 'rehearsal only']) {
    assert(errors, !textIncludes(text, marker), `${label} must not contain unresolved marker ${marker}`)
  }
}

function stableFinalReleaseReviewReport(value) {
  if (Array.isArray(value)) return value.map(stableFinalReleaseReviewReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (key === 'generatedAt') continue
    stable[key] = stableFinalReleaseReviewReport(entry)
  }
  return stable
}

function runGeneratorJson({ moduleRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-final-release-review-report.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--module-root',
    moduleRoot,
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
      error: `final release review report generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `final release review report generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function sameResolvedPath(actual, expected) {
  if (typeof actual !== 'string' || typeof expected !== 'string') return false
  return path.resolve(actual) === path.resolve(expected)
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

function resolveMaybeFile(value, editionRoot, moduleRoot) {
  if (typeof value !== 'string' || value.length === 0) return null
  const candidates = [
    path.resolve(editionRoot, value),
    path.resolve(moduleRoot, value),
    path.resolve(moduleRoot, '..', '..', value),
    path.resolve(value),
  ]
  return candidates.find((candidate) => fileExists(candidate)) ?? null
}

function resolveSavedArtifactPath({ artifactName, editionRoot, reportPath, reviewArtifactRoot, moduleRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0) return null
  const candidates = path.isAbsolute(artifactName)
    ? [artifactName]
    : [
        path.join(reviewArtifactRoot, artifactName),
        path.join(path.dirname(reportPath), artifactName),
        path.join(editionRoot, artifactName),
        path.join(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID, artifactName),
      ]
  return candidates.map((candidate) => path.resolve(candidate)).find((candidate) => fileExists(candidate)) ?? null
}

function realReviewSavedArtifactPath({ artifactName, reviewArtifactRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0 || path.isAbsolute(artifactName)) return null
  return path.resolve(reviewArtifactRoot, artifactName)
}

function looksLikePreflightEvidence(filePath, payload) {
  const normalizedPath = filePath.replace(/\\/g, '/').toLowerCase()
  if ([
    'local-runtime-rehearsal',
    'local-launcher-rehearsal',
    'release-publication-rehearsal',
    'edition-manifest-index-preview',
    'legal-content-audit',
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
  ].some((marker) => schema.includes(marker))) {
    return true
  }
  return payload?.rehearsalOnly === true
    || payload?.preflightOnly === true
    || payload?.previewOnly === true
    || payload?.clearsFinalReviewGates === false
    || payload?.clearsRuntimeGates === false
    || payload?.clearsLauncherGates === false
    || payload?.publicReleaseReady === false
}

function fileTextLooksLikePreflight(filePath) {
  const extension = path.extname(filePath).toLowerCase()
  if (!['.txt', '.log', '.md'].includes(extension)) return false
  const text = fs.readFileSync(filePath, 'utf8').slice(0, 65536).toLowerCase()
  return text.includes('local runtime rehearsal')
    || text.includes('local launcher rehearsal')
    || text.includes('local_runtime_rehearsal')
    || text.includes('local_launcher_rehearsal')
    || text.includes('rehearsalonly')
    || text.includes('preflightonly')
    || text.includes('clearsfinalreviewgates: false')
}

function validateRealSavedArtifact(errors, { artifactPath, expectedArtifactPath, artifactName, reviewId }) {
  assert(errors, artifactPath === expectedArtifactPath, `passed final review area ${reviewId} saved artifact ${artifactName} must be saved under the real final review artifact root`)
  const extension = path.extname(artifactPath).toLowerCase()
  if (extension === '.json') {
    const payload = readJsonIfPossible(artifactPath)
    assert(errors, payload !== null, `passed final review area ${reviewId} saved artifact ${artifactName} must be valid JSON`)
    if (payload) {
      assert(errors, !looksLikePreflightEvidence(artifactPath, payload), `passed final review area ${reviewId} saved artifact ${artifactName} must be real final review evidence, not preflight or rehearsal evidence`)
    }
  } else {
    assert(errors, !looksLikePreflightEvidence(artifactPath, null), `passed final review area ${reviewId} saved artifact ${artifactName} must be real final review evidence, not preflight or rehearsal evidence`)
    assert(errors, !fileTextLooksLikePreflight(artifactPath), `passed final review area ${reviewId} saved artifact ${artifactName} must not be copied preflight or rehearsal text evidence`)
  }
}

function validateLegalAuditDependency(errors, { edition, editionRoot, report, artifactPath }) {
  const legalReportPath = path.join(editionRoot, 'evidence', edition.legalReportName)
  assert(errors, fileExists(legalReportPath), `passed final release review requires legal audit report: ${legalReportPath}`)
  if (!fileExists(legalReportPath)) return
  const legalReport = readJsonIfPossible(legalReportPath)
  assert(errors, legalReport !== null, `passed final release review legal audit report must be valid JSON: ${legalReportPath}`)
  if (!legalReport) return

  assert(errors, legalReport.schema === 'echo.openlands.edition.legal_content_audit_report.v1', 'passed final release review legal audit report schema mismatch')
  assert(errors, legalReport.status === 'preflight_passed', 'passed final release review requires preflight-passed legal audit report')
  assert(errors, legalReport.publicReleaseAllowed === false, 'passed final release review legal audit report must remain preflight-only public release evidence')
  assert(errors, legalReport.packId === edition.packId, 'passed final release review legal audit packId mismatch')
  assert(errors, legalReport.runtimeTarget === edition.runtimeTarget, 'passed final release review legal audit runtimeTarget mismatch')
  assert(errors, legalReport.moduleId === MODULE_ID, 'passed final release review legal audit moduleId mismatch')
  assert(errors, legalReport.moduleVersion === VERSION, 'passed final release review legal audit moduleVersion mismatch')
  assert(errors, legalReport.legalAuditContract === 'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json', 'passed final release review legal audit contract path mismatch')
  assert(errors, legalReport.contentPolicy === 'data/echoopenlandsprotocol/openlands/config/content_policy.json', 'passed final release review legal audit content policy path mismatch')
  assert(errors, legalReport.assetManifest === 'assets/echoopenlandsprotocol/asset_manifest.json', 'passed final release review legal audit asset manifest path mismatch')
  assert(errors, legalReport.artifact?.file === path.basename(report.moduleArtifact), 'passed final release review legal audit artifact file mismatch')
  assert(errors, legalReport.artifact?.kind === edition.artifactKind, 'passed final release review legal audit artifact kind mismatch')
  if (artifactPath && typeof legalReport.artifact?.path === 'string') {
    assert(errors, sameResolvedPath(legalReport.artifact.path, artifactPath), 'passed final release review legal audit artifact path mismatch')
  }
  for (const entry of [
    'data/echoopenlandsprotocol/openlands/config/content_policy.json',
    'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json',
    'data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json',
    'assets/echoopenlandsprotocol/asset_manifest.json',
    'assets/echoopenlandsprotocol/lang/en_us.json',
  ]) {
    assert(errors, (legalReport.artifact?.runtimeEntriesChecked ?? []).includes(entry), `passed final release review legal audit report missing runtime entry ${entry}`)
  }
  assert(errors, Number.isInteger(legalReport.scanSummary?.publicIdentityValues) && legalReport.scanSummary.publicIdentityValues > 0, 'passed final release review legal audit report must scan public identity values')
  assert(errors, Number.isInteger(legalReport.scanSummary?.assetPaths) && legalReport.scanSummary.assetPaths > 0, 'passed final release review legal audit report must scan asset paths')
  assert(errors, Number.isInteger(legalReport.scanSummary?.forbiddenPublicTerms) && legalReport.scanSummary.forbiddenPublicTerms >= 20, 'passed final release review legal audit report must include forbidden public terms')
  assert(errors, Number.isInteger(legalReport.scanSummary?.blockAssetsChecked) && legalReport.scanSummary.blockAssetsChecked > 0, 'passed final release review legal audit report must check block assets')
  assert(errors, Number.isInteger(legalReport.scanSummary?.itemAssetsChecked) && legalReport.scanSummary.itemAssetsChecked > 0, 'passed final release review legal audit report must check item assets')
  assert(errors, Number.isInteger(legalReport.scanSummary?.recipesChecked) && legalReport.scanSummary.recipesChecked > 0, 'passed final release review legal audit report must check recipes')
  assert(errors, legalReport.policyResults?.noForbiddenPublicTerms === true, 'passed final release review legal audit must pass forbidden public term scan')
  assert(errors, legalReport.policyResults?.canonicalEchoIdsRetained === true, 'passed final release review legal audit must retain canonical Echo IDs')
  assert(errors, legalReport.policyResults?.borrowedAssetPathsDetected === false, 'passed final release review legal audit must not detect borrowed asset paths')
  assert(errors, legalReport.policyResults?.placeholderCoverageComplete === true, 'passed final release review legal audit must prove placeholder coverage before final replacement review')
  assert(errors, legalReport.policyResults?.publicReleaseAllowedWithPlaceholders === false, 'passed final release review legal audit must block placeholder public release')
  assert(errors, legalReport.policyResults?.requiresHumanArtLegalReview === true, 'passed final release review legal audit must require human art/legal review')
  for (const proof of [
    'legal_content_audit_contract_loaded',
    'content_policy_loaded',
    'no_forbidden_public_terms_in_public_identity',
    'canonical_echo_ids_retained',
    'asset_manifest_placeholder_policy_applied',
    'generated_artifact_paths_audited',
    'public_release_blocked_until_final_asset_human_review',
  ]) {
    assert(errors, (legalReport.proofs ?? []).includes(proof), `passed final release review legal audit report missing proof ${proof}`)
  }
}

function readPlaceholderCount(payload, artifactName) {
  const directKeys = ['remainingPlaceholderCount']
  if (artifactName.startsWith('block-')) directKeys.push('blockPlaceholderCount')
  if (artifactName.startsWith('item-')) directKeys.push('itemPlaceholderCount')
  for (const key of directKeys) {
    if (Number.isInteger(payload?.[key])) return payload[key]
  }
  for (const container of [payload?.summary, payload?.counts, payload?.placeholderCounts]) {
    for (const key of directKeys) {
      if (Number.isInteger(container?.[key])) return container[key]
    }
  }
  return null
}

function arrayFromFirst(...values) {
  for (const value of values) {
    if (Array.isArray(value)) return value
  }
  return []
}

function countFromFirst(...values) {
  for (const value of values) {
    if (Number.isInteger(value)) return value
  }
  return null
}

function forbiddenScanHits(payload) {
  return arrayFromFirst(
    payload?.hits,
    payload?.matches,
    payload?.violations,
    payload?.findings,
    payload?.forbiddenTermHits,
    payload?.summary?.hits,
    payload?.summary?.matches,
    payload?.summary?.violations,
  )
}

function forbiddenScanTerms(payload) {
  return arrayFromFirst(
    payload?.forbiddenPublicTerms,
    payload?.terms,
    payload?.scannedTerms,
    payload?.summary?.forbiddenPublicTerms,
  )
}

function forbiddenScanSourceRefs(payload) {
  return arrayFromFirst(
    payload?.publicTextSources,
    payload?.checkedSources,
    payload?.sources,
    payload?.summary?.publicTextSources,
    payload?.summary?.checkedSources,
  )
}

function forbiddenScanCount(payload, field) {
  return countFromFirst(
    payload?.[field],
    payload?.summary?.[field],
    payload?.scanSummary?.[field],
    payload?.counts?.[field],
  )
}

function exceptionEntries(payload) {
  return arrayFromFirst(
    payload?.exceptions,
    payload?.approvedExceptions,
    payload?.adapterMetadataExceptions,
    payload?.entries,
  )
}

function exceptionTerm(entry) {
  return firstDefined(entry.term, entry.name, entry.value, entry.targetName, entry.compatibilityTarget, entry.brand)
}

function exceptionScope(entry) {
  return String(firstDefined(entry.scope, entry.usage, entry.context, entry.location, entry.reason) ?? '').toLowerCase()
}

function exceptionApproved(entry) {
  return truthyField(entry, 'approved', 'allowed', 'internalOnly', 'policyExceptionApproved')
}

function textHasForbiddenTerm(value, forbiddenTerms) {
  const text = String(value ?? '').toLowerCase()
  return forbiddenTerms.some((term) => text.includes(String(term).toLowerCase()))
}

function validatePassedPublicIdentityReviewArea(errors, { artifactPaths, contract, report }) {
  const label = 'passed final review public identity'
  const reviewPath = artifactPaths.get('public-identity-review.md')
  const scanPath = artifactPaths.get('forbidden-term-scan.json')
  const exceptionsPath = artifactPaths.get('branding-exceptions.json')
  const forbiddenTerms = contract.legalAudit.forbiddenPublicTerms ?? []
  const requiredInternalTerms = contract.legalAudit.auditScope?.internalPolicyDocsMayMention ?? []

  if (reviewPath && fileExists(reviewPath)) {
    const reviewText = readText(reviewPath)
    assertNoUnresolvedReviewMarkers(errors, reviewText, `${label} public-identity-review.md`)
    for (const needle of ['Openlands', MODULE_ID, 'forbidden', 'branding', 'public text', 'launcher', report.reviewer]) {
      if (needle) assertTextIncludes(errors, reviewText, needle, `${label} public-identity-review.md`)
    }
    if (report.reviewDate) assertTextIncludes(errors, reviewText, String(report.reviewDate).split('T')[0], `${label} public-identity-review.md`)
  }

  const scan = readJsonIfPossible(scanPath)
  assert(errors, scan !== null, `${label} forbidden-term-scan.json must be valid JSON`)
  if (scan) {
    assert(errors,
      scan.status === 'passed'
        || scan.passed === true
        || scan.noForbiddenPublicTerms === true
        || scan.policyResults?.noForbiddenPublicTerms === true,
      `${label} forbidden-term-scan.json must report a passing forbidden term scan`)
    assert(errors, sameSet(forbiddenScanTerms(scan), forbiddenTerms), `${label} forbidden-term-scan.json forbidden term list mismatch`)
    assert(errors, forbiddenScanHits(scan).length === 0, `${label} forbidden-term-scan.json must have no forbidden term hits`)
    const publicIdentityCount = forbiddenScanCount(scan, 'publicIdentityValues')
    const forbiddenTermCount = forbiddenScanCount(scan, 'forbiddenPublicTerms')
    assert(errors, publicIdentityCount === null || publicIdentityCount >= Object.keys(contract.lang).length, `${label} forbidden-term-scan.json publicIdentityValues count is too low`)
    assert(errors, forbiddenTermCount === null || forbiddenTermCount === forbiddenTerms.length, `${label} forbidden-term-scan.json forbiddenPublicTerms count mismatch`)
    const sourceRefs = forbiddenScanSourceRefs(scan).map((value) => String(value))
    for (const source of contract.legalAudit.publicTextSources ?? []) {
      assert(errors, sourceRefs.length === 0 || sourceRefs.includes(source), `${label} forbidden-term-scan.json missing source ${source}`)
    }
  }

  const exceptions = readJsonIfPossible(exceptionsPath)
  assert(errors, exceptions !== null, `${label} branding-exceptions.json must be valid JSON`)
  if (exceptions) {
    assert(errors,
      exceptions.status === undefined
        || ['passed', 'approved', 'no_exceptions'].includes(String(exceptions.status).toLowerCase()),
      `${label} branding-exceptions.json status must be passed/approved`)
    const entries = exceptionEntries(exceptions)
    if (entries.length === 0) {
      assert(errors,
        exceptions.noBrandingExceptions === true
          || exceptions.noPublicBrandingExceptions === true
          || exceptions.exceptionsAllowed === 0
          || exceptions.summary?.exceptionCount === 0
          || exceptions.status === 'no_exceptions',
        `${label} branding-exceptions.json must explicitly report no branding exceptions when empty`)
    }
    for (const [index, entry] of entries.entries()) {
      const term = exceptionTerm(entry)
      const scope = exceptionScope(entry)
      assert(errors, exceptionApproved(entry), `${label} branding exception ${index} must be approved`)
      if (term && requiredInternalTerms.map((value) => value.toLowerCase()).includes(String(term).toLowerCase())) {
        assert(errors, scope.includes('internal') || scope.includes('adapter') || scope.includes('doc') || scope.includes('compatibility'), `${label} branding exception ${term} must be internal-only`)
      }
      assert(errors, entry.publicTextAllowed !== true && entry.launcherCopyAllowed !== true && entry.marketingAllowed !== true, `${label} branding exception ${index} must not allow public launcher or marketing copy`)
      if (textHasForbiddenTerm(firstDefined(entry.publicText, entry.launcherCopy, entry.marketingCopy), forbiddenTerms)) {
        assert(errors, false, `${label} branding exception ${index} must not include forbidden terms in public text`)
      }
    }
  }
}

function normalizeId(value) {
  return String(value ?? '').replace(/^echoopenlandsprotocol:/, '').replace(/^openlands:/, '')
}

function normalizeTexture(value, kind) {
  return String(value ?? '').replace(new RegExp(`^${kind}/`), '')
}

function assetReviewEntries(payload, kind) {
  for (const key of [
    `${kind}Assets`,
    `${kind}RenderSamples`,
    `${kind}Samples`,
    `${kind}s`,
    'assets',
    'renderSamples',
    'samples',
    'entries',
  ]) {
    const value = payload?.[key]
    if (Array.isArray(value)) return value
    const entries = objectEntriesAsArray(value, 'id')
    if (entries.length > 0) return entries
  }
  return []
}

function expectedAssetEntries(contract, kind) {
  const sourceEntries = kind === 'block' ? contract.blockContract.blocks : contract.itemContract.items
  return (sourceEntries ?? []).map((entry) => ({
    id: normalizeId(entry.id),
    model: entry.model,
    texture: normalizeTexture(entry.texture, kind),
  }))
}

function assetReviewEntryId(entry) {
  return normalizeId(firstDefined(entry.id, entry.assetId, entry.blockId, entry.itemId, entry.name))
}

function assetReviewEntryModel(entry) {
  return firstDefined(entry.model, entry.modelId, entry.expectedModel, entry.reviewedModel)
}

function assetReviewEntryTexture(entry, kind) {
  return normalizeTexture(firstDefined(entry.texture, entry.textureId, entry.expectedTexture, entry.reviewedTexture), kind)
}

function assetReviewSamplePath(entry) {
  return firstDefined(entry.samplePath, entry.renderSample, entry.renderPath, entry.screenshot, entry.preview, entry.file, entry.path)
}

function assetReviewEntryPassed(entry) {
  return entry.status === 'passed'
    || entry.reviewStatus === 'passed'
    || truthyField(entry, 'passed', 'approved', 'publicReleaseApproved')
}

function assetReviewEntryOriginalOrLicensed(entry) {
  const sourceType = String(firstDefined(entry.sourceType, entry.provenance, entry.licenseType, entry.origin) ?? '').toLowerCase()
  return truthyField(entry, 'original', 'licensed', 'ownedOrLicensed', 'publicReleaseApproved', 'licenseApproved')
    || ['original', 'owned', 'licensed', 'commissioned'].includes(sourceType)
}

function assetReviewEntryNoCopiedSource(entry) {
  const noCopiedTexture = entry.copiedTexture === false || truthyField(entry, 'noCopiedTexture')
  const noCopiedModel = entry.copiedModel === false || truthyField(entry, 'noCopiedModel')
  const noCopiedSilhouette = entry.copiedSilhouette === false || truthyField(entry, 'noCopiedSilhouette')
  const noBorrowedAsset = entry.borrowedAsset === false || truthyField(entry, 'noBorrowedAsset')
  return noCopiedTexture && noCopiedModel && noCopiedSilhouette && noBorrowedAsset
}

function validatePassedAssetReviewArea(errors, { artifactPaths, contract, report, kind }) {
  const label = `passed final review ${kind} assets`
  const reviewPath = artifactPaths.get(`${kind}-asset-review.md`)
  const indexPath = artifactPaths.get(`${kind}-render-sample-index.json`)
  const placeholderPath = artifactPaths.get(`${kind}-placeholder-count.json`)
  const expectedEntries = expectedAssetEntries(contract, kind)
  const expectedIds = expectedEntries.map((entry) => entry.id)
  const assetManifestIds = kind === 'block'
    ? contract.assetManifest.mvpCoverage?.blockIds
    : contract.assetManifest.mvpCoverage?.itemIds

  assert(errors, sameSet(assetManifestIds, expectedIds), `${label} asset manifest ${kind} coverage mismatch`)

  if (reviewPath && fileExists(reviewPath)) {
    const reviewText = readText(reviewPath)
    assertNoUnresolvedReviewMarkers(errors, reviewText, `${label} ${kind}-asset-review.md`)
    for (const needle of ['Openlands', kind, 'original', 'licensed', 'render', report.reviewer]) {
      if (needle) assertTextIncludes(errors, reviewText, needle, `${label} ${kind}-asset-review.md`)
    }
    if (report.reviewDate) assertTextIncludes(errors, reviewText, String(report.reviewDate).split('T')[0], `${label} ${kind}-asset-review.md`)
  }

  if (placeholderPath && fileExists(placeholderPath)) {
    const placeholder = readJsonIfPossible(placeholderPath)
    assert(errors, placeholder !== null, `${label} ${kind}-placeholder-count.json must be valid JSON`)
    if (placeholder) {
      assert(errors, readPlaceholderCount(placeholder, `${kind}-placeholder-count.json`) === 0, `${label} ${kind}-placeholder-count.json must report zero placeholders`)
      const reviewedIds = arrayFromFirst(
        placeholder.reviewedIds,
        placeholder[`${kind}Ids`],
        placeholder.summary?.reviewedIds,
        placeholder.summary?.[`${kind}Ids`],
      )
      assert(errors, reviewedIds.length === 0 || sameSet(reviewedIds.map(normalizeId), expectedIds), `${label} ${kind}-placeholder-count.json reviewed ids mismatch`)
    }
  }

  const index = readJsonIfPossible(indexPath)
  assert(errors, index !== null, `${label} ${kind}-render-sample-index.json must be valid JSON`)
  const entries = index ? assetReviewEntries(index, kind) : []
  assert(errors, entries.length > 0, `${label} ${kind}-render-sample-index.json must list render samples`)
  const entryById = new Map(entries.map((entry) => [assetReviewEntryId(entry), entry]).filter(([id]) => id))
  assert(errors, sameSet([...entryById.keys()], expectedIds), `${label} ${kind}-render-sample-index.json must cover every MVP ${kind}`)

  for (const expected of expectedEntries) {
    const entry = entryById.get(expected.id)
    assert(errors, entry !== undefined, `${label} missing render sample for ${expected.id}`)
    if (!entry) continue
    assert(errors, assetReviewEntryPassed(entry), `${label} ${expected.id} review must pass`)
    assert(errors, assetReviewEntryOriginalOrLicensed(entry), `${label} ${expected.id} must be original or licensed`)
    assert(errors, assetReviewEntryNoCopiedSource(entry), `${label} ${expected.id} must report no copied texture/model/silhouette`)
    assert(errors, firstDefined(entry.remainingPlaceholderCount, entry.placeholderCount) === 0 || entry.placeholder === false || entry.placeholderUsed === false, `${label} ${expected.id} must report no placeholder asset`)
    assert(errors, typeof assetReviewSamplePath(entry) === 'string' && assetReviewSamplePath(entry).length > 0, `${label} ${expected.id} requires render sample path`)
    if (assetReviewEntryModel(entry) !== undefined) {
      assert(errors, assetReviewEntryModel(entry) === expected.model, `${label} ${expected.id} model mismatch`)
    }
    if (assetReviewEntryTexture(entry, kind) !== '') {
      assert(errors, assetReviewEntryTexture(entry, kind) === expected.texture, `${label} ${expected.id} texture mismatch`)
    }
  }

  if (index?.summary) {
    assert(errors, index.summary.reviewedCount === undefined || index.summary.reviewedCount === expectedEntries.length, `${label} ${kind}-render-sample-index summary reviewedCount mismatch`)
    assert(errors, index.summary.remainingPlaceholderCount === undefined || index.summary.remainingPlaceholderCount === 0, `${label} ${kind}-render-sample-index summary remainingPlaceholderCount must be zero`)
    assert(errors, index.summary.copiedAssetCount === undefined || index.summary.copiedAssetCount === 0, `${label} ${kind}-render-sample-index summary copiedAssetCount must be zero`)
  }
}

function normalizeSoundKey(value) {
  return typeof value === 'string' ? value.replace(':', '.') : null
}

function expectedSoundEventKeys(contract) {
  const keys = [
    ...Object.keys(contract.soundsManifest ?? {}),
    ...(contract.soundContract.soundFamilies ?? []).map((family) => normalizeSoundKey(family.assetKey)),
  ]
  for (const creature of contract.creatureContract.creatures ?? []) {
    for (const soundKey of Object.values(creature.sounds ?? {})) {
      keys.push(normalizeSoundKey(soundKey))
    }
  }
  return sortedUnique(keys)
}

function objectEntriesAsArray(value, keyName = 'id') {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return []
  return Object.entries(value).map(([key, entry]) =>
    entry && typeof entry === 'object'
      ? { [keyName]: key, ...entry }
      : { [keyName]: key, value: entry })
}

function soundSourceEntries(payload) {
  for (const key of ['soundEvents', 'events', 'entries', 'sources']) {
    const value = payload?.[key]
    if (Array.isArray(value)) return value
    const entries = objectEntriesAsArray(value, key === 'sources' ? 'soundEvent' : 'id')
    if (entries.length > 0) return entries
  }
  return []
}

function entrySoundKey(entry) {
  return normalizeSoundKey(firstDefined(entry.id, entry.key, entry.event, entry.soundEvent, entry.assetKey))
}

function soundEventBinding(contract, key) {
  return contract.soundsManifest?.[key]
    ?? contract.soundsManifest?.[String(key).replace(/\./g, ':')]
}

function soundBindingEntries(binding) {
  return Array.isArray(binding?.sounds) ? binding.sounds : []
}

function soundBindingName(entry) {
  if (typeof entry === 'string') return entry
  return firstDefined(entry?.name, entry?.sound, entry?.path, entry?.file)
}

function entryLicenseId(entry) {
  const license = firstDefined(entry.licenseId, entry.license, entry.sourceLicense, entry.licenseRef)
  if (license && typeof license === 'object') return firstDefined(license.id, license.licenseId, license.name)
  return typeof license === 'string' ? license : null
}

function entrySourcePath(entry) {
  return firstDefined(entry.sourceFile, entry.sourcePath, entry.audioFile, entry.file, entry.path, entry.source)
}

function entryHasOwnedOrLicensedSource(entry) {
  const sourceType = String(firstDefined(entry.sourceType, entry.origin, entry.provenance, entry.licenseType) ?? '').toLowerCase()
  return truthyField(entry, 'ownedOrLicensed', 'original', 'licensed', 'publicSourceVerified', 'sourceVerified')
    || ['original', 'owned', 'licensed', 'commissioned', 'cc0', 'public-domain'].includes(sourceType)
}

function entryReportsNoBorrowedAudio(entry) {
  return entry.borrowedVanillaAudio === false
    || entry.borrowedAudio === false
    || entry.borrowed === false
    || truthyField(entry, 'noBorrowedVanillaAudio', 'noBorrowedAudio')
}

function licenseEntries(payload) {
  for (const key of ['licenses', 'licenseEntries', 'entries', 'bundle']) {
    const value = payload?.[key]
    if (Array.isArray(value)) return value
    const entries = objectEntriesAsArray(value, 'id')
    if (entries.length > 0) return entries
  }
  return []
}

function licenseEntryId(entry) {
  return firstDefined(entry.id, entry.licenseId, entry.name)
}

function licenseIsAllowed(entry) {
  const type = String(firstDefined(entry.type, entry.licenseType, entry.kind, entry.sourceType) ?? '').toLowerCase()
  return truthyField(entry, 'approvedForPublicRelease', 'approved', 'publicUseAllowed', 'ownedOrLicensed', 'original', 'licensed')
    || ['original', 'owned', 'licensed', 'commissioned', 'cc0', 'public-domain'].includes(type)
}

function licenseReportsNoBorrowedAudio(entry) {
  return entry.borrowedVanillaAudio === false
    || entry.borrowedAudio === false
    || entry.borrowed === false
    || truthyField(entry, 'noBorrowedVanillaAudio', 'noBorrowedAudio')
}

function validatePassedAudioReviewArea(errors, { artifactPaths, contract, report }) {
  const label = 'passed final review audio sources'
  const reviewPath = artifactPaths.get('audio-source-review.md')
  const sourceManifestPath = artifactPaths.get('sound-event-source-manifest.json')
  const licenseBundlePath = artifactPaths.get('audio-license-bundle.json')
  const expectedKeys = expectedSoundEventKeys(contract)

  if (reviewPath && fileExists(reviewPath)) {
    const reviewText = readText(reviewPath)
    assertNoUnresolvedReviewMarkers(errors, reviewText, `${label} audio-source-review.md`)
    for (const needle of ['Openlands', 'audio', 'licensed', 'original', 'sound event', 'creature', 'ambience', report.reviewer]) {
      if (needle) assertTextIncludes(errors, reviewText, needle, `${label} audio-source-review.md`)
    }
    if (report.reviewDate) assertTextIncludes(errors, reviewText, String(report.reviewDate).split('T')[0], `${label} audio-source-review.md`)
  }

  const sourceManifest = readJsonIfPossible(sourceManifestPath)
  assert(errors, sourceManifest !== null, `${label} sound-event-source-manifest.json must be valid JSON`)
  const sourceEntries = sourceManifest ? soundSourceEntries(sourceManifest) : []
  assert(errors, sourceEntries.length > 0, `${label} sound-event-source-manifest.json must list sound event sources`)
  const sourceByKey = new Map(sourceEntries.map((entry) => [entrySoundKey(entry), entry]).filter(([key]) => key))
  assert(errors, sameSet([...sourceByKey.keys()], expectedKeys), `${label} sound-event-source-manifest.json must cover every Openlands sound event`)

  const licenseIds = new Set()
  for (const key of expectedKeys) {
    const entry = sourceByKey.get(key)
    assert(errors, entry !== undefined, `${label} missing source entry for ${key}`)
    if (!entry) continue
    const binding = soundEventBinding(contract, key)
    const boundSounds = soundBindingEntries(binding)
    assert(errors, binding !== undefined, `${label} sounds.json missing ${key}`)
    assert(errors, boundSounds.length > 0, `${label} sounds.json ${key} must bind at least one public sound asset`)
    for (const boundSound of boundSounds) {
      const boundName = soundBindingName(boundSound)
      assert(errors, typeof boundName === 'string' && boundName.length > 0, `${label} sounds.json ${key} has an empty sound binding`)
      assert(errors, !String(boundName ?? '').startsWith('minecraft:'), `${label} sounds.json ${key} must not bind borrowed vanilla audio`)
    }
    assert(errors, typeof entrySourcePath(entry) === 'string' && entrySourcePath(entry).length > 0, `${label} ${key} requires a source file`)
    assert(errors, entryHasOwnedOrLicensedSource(entry), `${label} ${key} must be original or licensed`)
    assert(errors, entryReportsNoBorrowedAudio(entry), `${label} ${key} must report no borrowed vanilla audio`)
    const licenseId = entryLicenseId(entry)
    assert(errors, typeof licenseId === 'string' && licenseId.length > 0, `${label} ${key} requires licenseId`)
    if (licenseId) licenseIds.add(licenseId)
  }

  const creatureRequiredEvents = contract.soundContract.creatureSoundTemplate?.requiredEvents ?? []
  for (const creature of contract.creatureContract.creatures ?? []) {
    for (const eventId of creatureRequiredEvents) {
      const key = normalizeSoundKey(creature.sounds?.[eventId])
      assert(errors, key && sourceByKey.has(key), `${label} missing required creature ${creature.id}.${eventId} source`)
    }
  }
  for (const family of contract.soundContract.soundFamilies ?? []) {
    if (family.category === 'ambience') {
      const key = normalizeSoundKey(family.assetKey)
      assert(errors, key && sourceByKey.has(key), `${label} missing ambience source for ${family.id}`)
    }
  }

  const licenseBundle = readJsonIfPossible(licenseBundlePath)
  assert(errors, licenseBundle !== null, `${label} audio-license-bundle.json must be valid JSON`)
  const licenses = licenseBundle ? licenseEntries(licenseBundle) : []
  assert(errors, licenses.length > 0, `${label} audio-license-bundle.json must list licenses`)
  const licenseById = new Map(licenses.map((entry) => [licenseEntryId(entry), entry]).filter(([id]) => typeof id === 'string' && id.length > 0))
  for (const licenseId of licenseIds) {
    const license = licenseById.get(licenseId)
    assert(errors, license !== undefined, `${label} audio-license-bundle.json missing license ${licenseId}`)
    if (!license) continue
    assert(errors, licenseIsAllowed(license), `${label} license ${licenseId} must be approved for public release`)
    assert(errors, licenseReportsNoBorrowedAudio(license), `${label} license ${licenseId} must report no borrowed vanilla audio`)
  }
  if (licenseBundle?.summary) {
    assert(errors, licenseBundle.summary.borrowedVanillaAudioDetected === false || licenseBundle.summary.borrowedAudioDetected === false || licenseBundle.summary.noBorrowedVanillaAudio === true, `${label} audio-license-bundle summary must report no borrowed audio`)
  }
}

function buildContract(moduleRoot) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const assetsRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'assets', MODULE_ID)
  const acceptance = readJson(path.join(dataRoot, 'systems', 'final_release_review_acceptance.json'))
  const harness = readJson(path.join(dataRoot, 'systems', 'final_release_review_harness_plan.json'))
  return {
    acceptance,
    harness,
    expectedReviewIds: sortedUnique((acceptance.reviewAreas ?? []).map((area) => area.id)),
    expectedGateIds: sortedUnique((acceptance.finalReviewGates ?? []).map((gate) => gate.id)),
    reviewById: new Map((acceptance.reviewAreas ?? []).map((area) => [area.id, area])),
    gateById: new Map((acceptance.finalReviewGates ?? []).map((gate) => [gate.id, gate])),
    harnessBindingById: new Map((harness.reviewAreaBindings ?? []).map((area) => [area.id, area])),
    assetManifestPath: path.join(assetsRoot, 'asset_manifest.json'),
    assetManifest: readJson(path.join(assetsRoot, 'asset_manifest.json')),
    legalAudit: readJson(path.join(dataRoot, 'systems', 'legal_content_audit.json')),
    contentPolicy: readJson(path.join(dataRoot, 'config', 'content_policy.json')),
    lang: readJson(path.join(assetsRoot, 'lang', 'en_us.json')),
    blockContract: readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')),
    itemContract: readJson(path.join(dataRoot, 'items', 'mvp_items.json')),
    soundsManifest: readJson(path.join(assetsRoot, 'sounds.json')),
    soundContract: readJson(path.join(dataRoot, 'sounds', 'mvp_sound_contract.json')),
    creatureContract: readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json')),
    legalAuditPath: path.join(dataRoot, 'systems', 'legal_content_audit.json'),
  }
}

function validateReport({ moduleRoot, editionRoot, editionKey, reportPath, allowMissing }) {
  const errors = []
  const warnings = []
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)

  const contract = buildContract(moduleRoot)
  const expectedEditionReport = (contract.acceptance.editionReports ?? []).find((entry) => entry.edition === editionKey)
  const expectedEditionHarness = (contract.harness.editionHarnesses ?? []).find((entry) => entry.edition === editionKey)
  assert(errors, expectedEditionReport !== undefined, `final release review acceptance missing edition ${editionKey}`)
  assert(errors, expectedEditionReport?.runtimeTarget === edition.runtimeTarget, `final release review runtime target mismatch for ${editionKey}`)
  assert(errors, contract.harness.schema === 'echo.openlands.systems.final_release_review_harness_plan.v1', 'final release review harness schema mismatch')
  assert(errors, contract.harness.sourceContracts?.finalReleaseReviewAcceptance === 'systems/final_release_review_acceptance.json', 'final release review harness source contract mismatch')
  assert(errors, sameSet((contract.harness.reviewAreaBindings ?? []).map((area) => area.id), contract.expectedReviewIds), 'final release review harness must cover every review area')
  assert(errors, expectedEditionHarness !== undefined, `final release review harness missing edition ${editionKey}`)
  assert(errors, expectedEditionHarness?.runtimeTarget === edition.runtimeTarget, `final release review harness runtime target mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.requiredReport === expectedEditionReport?.requiredReport, `final release review harness report path mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.artifactPattern === expectedEditionReport?.artifactPattern, `final release review harness artifact pattern mismatch for ${editionKey}`)
  assert(errors, typeof expectedEditionHarness?.reviewArtifactRoot === 'string' && expectedEditionHarness.reviewArtifactRoot.startsWith(`evidence/final-review/${editionKey}`), `final release review harness review artifact root mismatch for ${editionKey}`)
  for (const expectedArea of contract.acceptance.reviewAreas ?? []) {
    const binding = contract.harnessBindingById.get(expectedArea.id)
    assert(errors, binding !== undefined, `final release review harness missing binding ${expectedArea.id}`)
    if (!binding) continue
    assert(errors, sameSet(binding.gateIds, expectedArea.gateIds), `final release review harness binding ${expectedArea.id} gateIds mismatch`)
    assert(errors, sameSet(binding.inputFixtureRefs, expectedArea.inputFixtureRefs), `final release review harness binding ${expectedArea.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(binding.checklist, expectedArea.checklist), `final release review harness binding ${expectedArea.id} checklist mismatch`)
    assert(errors, sameSet(binding.requiredSavedArtifacts, expectedArea.requiredSavedArtifacts), `final release review harness binding ${expectedArea.id} requiredSavedArtifacts mismatch`)
  }

  const resolvedReportPath = reportPath
    ? path.resolve(reportPath)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const reviewArtifactRoot = path.resolve(editionRoot, expectedEditionHarness?.reviewArtifactRoot ?? path.join('evidence', 'final-review', editionKey))

  if (!fileExists(resolvedReportPath)) {
    return {
      status: allowMissing ? 'missing' : 'failed',
      edition: editionKey,
      runtimeTarget: edition.runtimeTarget,
      reportPath: resolvedReportPath,
      expectedReviewAreaCount: contract.expectedReviewIds.length,
      expectedFinalReviewGateCount: contract.expectedGateIds.length,
      expectedHarnessDriverCount: contract.harness.driverSurfaces?.length ?? 0,
      missingReason: 'final release review report has not been produced by the human review process yet',
      errors: allowMissing ? [] : [`final release review report missing: ${resolvedReportPath}`],
      warnings,
    }
  }

  const report = readJson(resolvedReportPath)
  const reportContract = contract.acceptance.reportContract ?? {}
  const reviewResults = report.reviewResults ?? []
  const reviewResultById = new Map(reviewResults.map((review) => [review.id, review]))
  const allowedReportStatus = reportContract.allowedReportStatus ?? ['passed', 'failed', 'blocked']
  const allowedReviewStatus = reportContract.allowedReviewStatus ?? ['passed', 'failed', 'blocked', 'skipped']
  const allowedChecklistStatus = reportContract.allowedChecklistStatus ?? ['passed', 'failed', 'blocked', 'skipped']

  for (const field of reportContract.requiredReportFields ?? []) {
    if (report.status === 'blocked' && (field === 'reviewer' || field === 'reviewDate')) {
      assert(errors, report[field] === null || report[field] === 'blocked', `blocked final release review report ${field} must remain null or blocked`)
    } else {
      requireFields(errors, report, [field], 'final release review report')
    }
  }
  assert(errors, report.schema === reportContract.schema, 'final release review report schema mismatch')
  assert(errors, allowedReportStatus.includes(report.status), `final release review status must be one of ${allowedReportStatus.join(', ')}`)
  assert(errors, report.edition === editionKey, `final release review edition must be ${editionKey}`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `final release review runtimeTarget must be ${edition.runtimeTarget}`)
  assert(errors, report.moduleId === MODULE_ID, `final release review moduleId must be ${MODULE_ID}`)
  assert(errors, report.moduleVersion === VERSION, `final release review moduleVersion must be ${VERSION}`)
  assert(errors, /^[a-f0-9]{64}$/i.test(String(report.moduleArtifactSha256 ?? '')), 'final release review moduleArtifactSha256 must be a 64-character hex string')
  assert(errors, report.assetManifestHash === sha256(contract.assetManifestPath), 'final release review assetManifestHash mismatch')
  assert(errors, report.legalAuditHash === sha256(contract.legalAuditPath), 'final release review legalAuditHash mismatch')

  const artifactPath = resolveMaybeFile(report.moduleArtifact, editionRoot, moduleRoot)
  if (artifactPath) {
    const actualHash = sha256(artifactPath)
    assert(errors, actualHash.toLowerCase() === String(report.moduleArtifactSha256).toLowerCase(), 'final release review moduleArtifactSha256 does not match local moduleArtifact')
  } else {
    warnings.push('moduleArtifact was not a local file; sha256 format was checked but file bytes were not rehashed')
  }

  assert(errors, sameSet(reviewResults.map((review) => review.id), contract.expectedReviewIds), 'final release review report must contain exactly the review areas')
  for (const review of reviewResults) {
    const expected = contract.reviewById.get(review.id)
    assert(errors, expected !== undefined, `final release review contains unknown review area ${review.id}`)
    if (!expected) continue
    for (const field of reportContract.requiredReviewFields ?? []) {
      requireFields(errors, review, [field], `final release review area ${review.id}`)
    }
    assert(errors, sameSet(review.gateIds, expected.gateIds), `final release review area ${review.id} gateIds mismatch`)
    assert(errors, allowedReviewStatus.includes(review.status), `final release review area ${review.id} status must be allowed`)
    const checklist = review.checklist ?? []
    assert(errors, sameSet(checklist.map((item) => item.id), expected.checklist), `final release review area ${review.id} checklist mismatch`)
    for (const item of checklist) {
      for (const field of reportContract.requiredChecklistFields ?? []) {
        requireFields(errors, item, [field], `final release checklist ${review.id}/${item.id}`)
      }
      assert(errors, allowedChecklistStatus.includes(item.status), `final release checklist ${review.id}/${item.id} status must be allowed`)
      if (review.status === 'passed') {
        assert(errors, item.status === 'passed', `passed final review area ${review.id} has non-passed checklist item ${item.id}`)
      }
    }
    if (review.status === 'passed') {
      assert(errors, sameSet(review.savedArtifacts, expected.requiredSavedArtifacts), `passed final review area ${review.id} savedArtifacts mismatch`)
      const savedArtifactPaths = new Map()
      for (const artifactName of expected.requiredSavedArtifacts ?? []) {
        const artifactPath = resolveSavedArtifactPath({
          artifactName,
          editionRoot,
          reportPath: resolvedReportPath,
          reviewArtifactRoot,
          moduleRoot,
        })
        assert(errors, artifactPath !== null, `passed final review area ${review.id} missing saved artifact file ${artifactName}`)
        if (artifactPath) {
          savedArtifactPaths.set(artifactName, artifactPath)
          const stats = fs.statSync(artifactPath)
          assert(errors, stats.isFile(), `passed final review area ${review.id} saved artifact ${artifactName} must be a file`)
          assert(errors, stats.size > 0, `passed final review area ${review.id} saved artifact ${artifactName} must not be empty`)
          validateRealSavedArtifact(errors, {
            artifactPath,
            expectedArtifactPath: realReviewSavedArtifactPath({
              artifactName,
              reviewArtifactRoot,
            }),
            artifactName,
            reviewId: review.id,
          })
          if (artifactName.endsWith('-placeholder-count.json')) {
            const placeholderArtifact = readJsonIfPossible(artifactPath)
            const placeholderCount = readPlaceholderCount(placeholderArtifact, artifactName)
            assert(errors, placeholderCount !== null, `passed final review area ${review.id} saved artifact ${artifactName} missing remainingPlaceholderCount`)
            assert(errors, placeholderCount === 0, `passed final review area ${review.id} saved artifact ${artifactName} must report zero remaining placeholders`)
          }
        }
      }
      if (review.id === 'public_identity_and_branding') {
        validatePassedPublicIdentityReviewArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          report,
        })
      }
      if (review.id === 'block_textures_models_and_blockstates') {
        validatePassedAssetReviewArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          report,
          kind: 'block',
        })
      }
      if (review.id === 'item_icons_models_and_tools') {
        validatePassedAssetReviewArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          report,
          kind: 'item',
        })
      }
      if (review.id === 'audio_sources_and_sound_events') {
        validatePassedAudioReviewArea(errors, {
          artifactPaths: savedArtifactPaths,
          contract,
          report,
        })
      }
    }
  }

  const gatesClearedByPassingReviews = contract.expectedGateIds.filter((gateId) =>
    (contract.acceptance.reviewAreas ?? [])
      .filter((area) => (area.gateIds ?? []).includes(gateId))
      .every((area) => reviewResultById.get(area.id)?.status === 'passed'))
  const clearedFinalReviewGates = sortedUnique(report.clearedFinalReviewGates ?? [])
  const remainingFinalReviewGates = sortedUnique(report.remainingFinalReviewGates ?? [])

  assert(errors, sameSet([...clearedFinalReviewGates, ...remainingFinalReviewGates], contract.expectedGateIds), 'final release review cleared and remaining gates must cover every final review gate')
  for (const gateId of clearedFinalReviewGates) {
    assert(errors, gatesClearedByPassingReviews.includes(gateId), `final release review clears ${gateId} without all review areas passing`)
  }
  for (const gateId of remainingFinalReviewGates) {
    assert(errors, contract.expectedGateIds.includes(gateId), `final release review remainingFinalReviewGates contains unknown gate ${gateId}`)
  }
  for (const gateId of clearedFinalReviewGates) {
    assert(errors, !remainingFinalReviewGates.includes(gateId), `final release review gate ${gateId} cannot be both cleared and remaining`)
  }

  const assetManifestStillPlaceholderOnly = contract.assetManifest.status === 'owned_placeholder_coverage'
    && contract.assetManifest.publicReleaseAllowedWithPlaceholders === false

  if (report.status === 'passed') {
    assert(errors, typeof report.reviewer === 'string' && report.reviewer.length > 0, 'passed final release review requires reviewer')
    assert(errors, typeof report.reviewDate === 'string' && report.reviewDate.length > 0, 'passed final release review requires reviewDate')
    assert(errors, reviewResults.every((review) => review.status === 'passed'), 'passed final release review requires every area to pass')
    assert(errors, sameSet(clearedFinalReviewGates, contract.expectedGateIds), 'passed final release review must clear every final review gate')
    assert(errors, remainingFinalReviewGates.length === 0, 'passed final release review must have no remaining final review gates')
    assert(errors, !assetManifestStillPlaceholderOnly, 'passed final release review requires asset manifest to be updated beyond owned placeholder coverage')
    validateLegalAuditDependency(errors, {
      edition,
      editionRoot,
      report,
      artifactPath,
    })
  } else {
    assert(errors, report.publicReleaseReady === false, 'non-passed final release review must not mark publicReleaseReady true')
  }
  if (report.publicReleaseReady === true) {
    assert(errors, report.status === 'passed', 'publicReleaseReady true requires passed status')
    assert(errors, sameSet(clearedFinalReviewGates, contract.expectedGateIds), 'publicReleaseReady true requires every final review gate cleared')
    assert(errors, !assetManifestStillPlaceholderOnly, 'publicReleaseReady true requires final assets beyond owned placeholder coverage')
  }
  if (report.status === 'blocked' && report.generatedBy === 'generate-openlands-final-release-review-report.mjs') {
    const generated = runGeneratorJson({
      moduleRoot,
      editionRoot,
      editionKey,
      reportPath: resolvedReportPath,
    })
    if (generated.error) {
      errors.push(generated.error)
    } else if (generated.json?.status === 'blocked') {
      assert(
        errors,
        sameJson(stableFinalReleaseReviewReport(report), stableFinalReleaseReviewReport(generated.json)),
        'final release review report stale against generator dry-run',
      )
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    edition: editionKey,
    runtimeTarget: edition.runtimeTarget,
    reportPath: resolvedReportPath,
    reviewAreaCount: reviewResults.length,
    reportStatus: report.status,
    finalReviewGateCount: contract.expectedGateIds.length,
    harnessDriverCount: contract.harness.driverSurfaces?.length ?? 0,
    clearedFinalReviewGates: clearedFinalReviewGates.length,
    remainingFinalReviewGates: remainingFinalReviewGates.length,
    publicReleaseReady: report.publicReleaseReady === true,
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
  const result = validateReport({
    moduleRoot,
    editionRoot,
    editionKey: args.edition,
    reportPath: args.report,
    allowMissing: args.allowMissing,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands ${result.edition} final release review report validated (${result.reportStatus}): ${result.reviewAreaCount} areas, ${result.clearedFinalReviewGates}/${result.finalReviewGateCount} gates cleared.`)
    for (const warning of result.warnings) console.warn(`warning: ${warning}`)
  } else if (result.status === 'missing') {
    console.log(`Openlands ${result.edition} final release review report missing: ${result.reportPath}`)
  } else {
    console.error(`Openlands ${result.edition} final release review report failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-final-release-review-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --report <path>         Final review report path. Defaults to evidence/<edition>-final-release-review-report.json.
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
