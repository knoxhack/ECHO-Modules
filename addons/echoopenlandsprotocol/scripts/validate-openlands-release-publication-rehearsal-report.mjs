import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RELEASE_PUBLICATION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DISTRIBUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'
const REQUIRED_BLOCKERS = [
  'public_release_download_urls_missing',
  'public_download_verification_missing',
  'release_index_patch_approval_missing',
  'distribution_approval_missing',
  'local_rehearsal_does_not_publish_public_urls',
]
const REQUIRED_PROOFS = [
  'release_publication_contract_loaded',
  'release_index_loaded',
  'publication_manifest_loaded',
  'all_local_artifacts_present',
  'local_download_back_hashes_match',
  'release_index_patch_preview_generated',
  'public_alpha_stays_blocked_until_public_urls_verified',
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    report: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
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

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function sameResolvedPath(actual, expected) {
  return typeof actual === 'string' && path.resolve(actual) === path.resolve(expected)
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function stablePublicationRehearsalArtifact(artifact) {
  if (!artifact || typeof artifact !== 'object') return artifact
  const localDownloadBack = { ...(artifact.localDownloadBack ?? {}) }
  delete localDownloadBack.cachedArtifactPath
  return {
    ...artifact,
    localDownloadBack,
  }
}

function stablePublicationRehearsalReport(report) {
  if (!report || typeof report !== 'object') return report
  const { generatedAt, dryRun, savedArtifactRoot, ...stableReport } = report
  return {
    ...stableReport,
    artifactResults: (report.artifactResults ?? []).map(stablePublicationRehearsalArtifact),
  }
}

function readSavedJson(errors, filePath, label) {
  assert(errors, typeof filePath === 'string' && filePath.length > 0, `${label} path missing`)
  assert(errors, fileExists(filePath), `${label} missing`)
  if (!fileExists(filePath)) return null
  assert(errors, fs.statSync(filePath).size > 0, `${label} must be non-empty`)
  try {
    return readJson(filePath)
  } catch {
    errors.push(`${label} must be valid JSON`)
    return null
  }
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function publicationById(manifest, id) {
  return (manifest?.artifactPublications ?? []).find((publication) => publication.id === id)
}

function validate({ moduleRoot, releaseRoot, reportPath }) {
  const errors = []
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const contract = readJson(path.join(resourcesRoot, RELEASE_PUBLICATION_CONTRACT))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const report = readJson(reportPath)
  const expectedSavedArtifactRoot = path.join(path.dirname(reportPath), 'openlands-release-publication-rehearsal-artifacts')
  const generatorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-release-publication-rehearsal-report.mjs')
  let generatedRehearsalReport = null
  assert(errors, fileExists(generatorScript), `missing Openlands release publication rehearsal generator ${generatorScript}`)
  if (fileExists(generatorScript)) {
    const generatorArgs = [
      generatorScript,
      '--module-root',
      moduleRoot,
      '--release-root',
      releaseRoot,
      '--out',
      reportPath,
      '--dry-run',
      '--json',
    ]
    if (typeof report.releasePublicationManifestPath === 'string' && report.releasePublicationManifestPath.length > 0) {
      generatorArgs.push('--manifest', report.releasePublicationManifestPath)
    }
    const generated = spawnSync(process.execPath, generatorArgs, {
      cwd: path.resolve(moduleRoot, '..', '..'),
      encoding: 'utf8',
      windowsHide: true,
    })
    assert(errors, generated.status === 0, `Openlands release publication rehearsal generator dry-run failed: ${generated.stderr || generated.stdout}`)
    if (generated.status === 0) {
      try {
        generatedRehearsalReport = JSON.parse(generated.stdout)
      } catch (error) {
        errors.push(`Openlands release publication rehearsal generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  const publicationManifest = fileExists(report.releasePublicationManifestPath ?? '')
    ? readJson(report.releasePublicationManifestPath)
    : null

  assert(errors, report.schema === 'echo.openlands.release_publication_rehearsal_report.v1', 'publication rehearsal schema mismatch')
  assert(errors, report.status === 'preflight_passed', 'publication rehearsal status must be preflight_passed')
  assert(errors, report.publicAlphaReady === false, 'publication rehearsal must not mark publicAlphaReady true')
  assert(errors, report.rehearsalOnly === true, 'publication rehearsal must declare rehearsalOnly true')
  assert(errors, report.clearsDistributionGates === false, 'publication rehearsal must not clear distribution gates')
  assert(errors, report.clearsReleasePublicationGates === false, 'publication rehearsal must not clear release publication gates')
  assert(errors, report.moduleId === MODULE_ID, 'publication rehearsal module id mismatch')
  assert(errors, report.moduleVersion === VERSION, 'publication rehearsal version mismatch')
  assert(errors, report.releaseId === releaseIndex?.releaseId, 'publication rehearsal release id mismatch')
  assert(errors, report.releaseIndexPath === releaseIndexPath, 'publication rehearsal release index path mismatch')
  assert(errors, report.releasePublicationManifestContract === RELEASE_PUBLICATION_CONTRACT, 'publication rehearsal contract path mismatch')
  assert(errors, report.distributionContract === DISTRIBUTION_CONTRACT, 'publication rehearsal distribution contract path mismatch')
  assert(errors, report.savedArtifactRoot === expectedSavedArtifactRoot, 'publication rehearsal savedArtifactRoot path mismatch')
  assert(errors, typeof report.savedArtifactRoot === 'string' && fileExists(report.savedArtifactRoot), 'publication rehearsal savedArtifactRoot must exist')
  if (generatedRehearsalReport) {
    assert(errors, sameJson(
      stablePublicationRehearsalReport(report),
      stablePublicationRehearsalReport(generatedRehearsalReport),
    ), 'publication rehearsal report stale against generator dry-run')
  }
  assert(errors, publicationManifest?.schema === contract.reportContract?.schema, 'publication rehearsal manifest schema mismatch')
  assert(errors, publicationManifest?.status === 'template_blocked', 'publication rehearsal manifest must remain template_blocked')

  const expectedTargets = contract.artifactTargets ?? []
  const distributionIds = (distribution.artifactTargets ?? []).map((target) => target.id)
  assert(errors, sameSet(distributionIds, expectedTargets.map((target) => target.id)), 'publication rehearsal distribution artifact ids mismatch')
  assert(errors, sameSet((report.artifactResults ?? []).map((artifact) => artifact.id), expectedTargets.map((target) => target.id)), 'publication rehearsal artifact ids mismatch')
  assert(errors, report.summary?.artifactCount === expectedTargets.length, 'publication rehearsal summary artifact count mismatch')
  assert(errors, report.summary?.requiredForPublicAlphaCount === expectedTargets.filter((target) => target.requiredForPublicAlpha).length, 'publication rehearsal required artifact count mismatch')
  assert(errors, report.summary?.missingPublicDownloadUrlCount === expectedTargets.length, 'publication rehearsal must keep every public URL missing')
  assert(errors, report.summary?.localDownloadVerifiedCount === expectedTargets.length, 'publication rehearsal local download verification count mismatch')
  assert(errors, report.summary?.patchPreviewCount === expectedTargets.length, 'publication rehearsal patch preview count mismatch')
  assert(errors, report.summary?.releaseIndexPatchAppliedCount === 0, 'publication rehearsal must not apply release index patches')
  assert(errors, report.patchPreview?.status === 'preview_only', 'publication rehearsal patch preview status mismatch')
  assert(errors, report.patchPreview?.patchApplied === false, 'publication rehearsal aggregate patch preview must not apply')
  assert(errors, report.patchPreview?.patchAllowed === false, 'publication rehearsal aggregate patch preview must not be allowed')
  assert(errors, report.patchPreview?.entryCount === expectedTargets.length, 'publication rehearsal aggregate patch preview entry count mismatch')

  const savedRoot = report.savedArtifactRoot ?? ''
  const aggregatePatchPreviewPath = path.join(savedRoot, report.patchPreview?.artifact ?? '')
  const aggregatePatchPreview = readSavedJson(errors, aggregatePatchPreviewPath, 'publication rehearsal aggregate patch preview artifact')
  const releaseIndexSnapshot = readSavedJson(errors, path.join(savedRoot, 'release-index-snapshot.json'), 'publication rehearsal release index snapshot')
  const publicationManifestSnapshot = readSavedJson(errors, path.join(savedRoot, 'publication-manifest-template-snapshot.json'), 'publication rehearsal publication manifest snapshot')
  if (releaseIndexSnapshot) {
    assert(errors, sameJson(releaseIndexSnapshot, releaseIndex), 'publication rehearsal release index snapshot must match current release index')
  }
  if (publicationManifestSnapshot) {
    assert(errors, sameJson(publicationManifestSnapshot, publicationManifest), 'publication rehearsal publication manifest snapshot must match current template manifest')
  }
  if (aggregatePatchPreview) {
    assert(errors, aggregatePatchPreview.schema === 'echo.openlands.release_publication_rehearsal_patch_preview.v1', 'publication rehearsal aggregate patch preview schema mismatch')
    assert(errors, aggregatePatchPreview.patchApplied === false, 'publication rehearsal aggregate patch preview file must not apply')
    assert(errors, aggregatePatchPreview.patchAllowed === false, 'publication rehearsal aggregate patch preview file must not allow patch')
    assert(errors, aggregatePatchPreview.releaseId === releaseIndex?.releaseId, 'publication rehearsal aggregate patch preview release id mismatch')
    assert(errors, aggregatePatchPreview.moduleId === MODULE_ID, 'publication rehearsal aggregate patch preview module mismatch')
    assert(errors, aggregatePatchPreview.moduleVersion === VERSION, 'publication rehearsal aggregate patch preview version mismatch')
    assert(errors, aggregatePatchPreview.target === 'modules[].artifacts[].downloadUrl', 'publication rehearsal aggregate patch preview target mismatch')
    assert(errors, aggregatePatchPreview.blocker === 'local_rehearsal_does_not_publish_public_urls', 'publication rehearsal aggregate patch preview blocker mismatch')
    assert(errors, sameSet((aggregatePatchPreview.entries ?? []).map((entry) => entry.id), expectedTargets.map((target) => target.id)), 'publication rehearsal aggregate patch preview entries mismatch')
  }

  for (const blocker of REQUIRED_BLOCKERS) {
    assert(errors, report.blockedBy?.includes(blocker), `publication rehearsal missing blocker ${blocker}`)
  }
  for (const proof of REQUIRED_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `publication rehearsal missing proof ${proof}`)
  }

  for (const target of expectedTargets) {
    const result = (report.artifactResults ?? []).find((artifact) => artifact.id === target.id)
    assert(errors, result !== undefined, `publication rehearsal missing artifact ${target.id}`)
    if (!result) continue
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    const publication = publicationById(publicationManifest, target.id)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(errors, result.file === target.file, `publication rehearsal artifact ${target.id} file mismatch`)
    assert(errors, result.kind === target.releaseIndexArtifactKind, `publication rehearsal artifact ${target.id} kind mismatch`)
    assert(errors, result.runtimeTarget === target.runtimeTarget, `publication rehearsal artifact ${target.id} runtime target mismatch`)
    assert(errors, result.requiredForPublicAlpha === target.requiredForPublicAlpha, `publication rehearsal artifact ${target.id} public alpha flag mismatch`)
    assert(errors, sameResolvedPath(result.artifactPath, artifactPath), `publication rehearsal artifact ${target.id} artifact path mismatch`)
    assert(errors, result.releaseIndexEntryPresent === true, `publication rehearsal artifact ${target.id} missing release index entry flag`)
    assert(errors, releaseArtifact !== undefined, `publication rehearsal artifact ${target.id} release index entry missing`)
    assert(errors, releaseArtifact?.kind === target.releaseIndexArtifactKind, `publication rehearsal artifact ${target.id} release index kind mismatch`)
    assert(errors, releaseArtifact?.downloadUrl === '', `publication rehearsal artifact ${target.id} release index URL must remain empty`)
    assert(errors, result.releaseIndexDownloadUrl === '', `publication rehearsal artifact ${target.id} must not record public release index URL`)
    assert(errors, result.publicDownloadUrlPresent === false, `publication rehearsal artifact ${target.id} must keep public URL absent`)
    assert(errors, result.publicationTemplateStatus === 'missing_url', `publication rehearsal artifact ${target.id} template status mismatch`)
    assert(errors, fileExists(artifactPath), `publication rehearsal artifact file missing ${target.file}`)
    if (fileExists(artifactPath)) {
      assert(errors, result.localArtifactSha256 === sha256File(artifactPath), `publication rehearsal artifact ${target.id} sha mismatch`)
      assert(errors, result.localArtifactSize === fs.statSync(artifactPath).size, `publication rehearsal artifact ${target.id} size mismatch`)
    }
    assert(errors, result.localArtifactSha256 === releaseArtifact?.sha256, `publication rehearsal artifact ${target.id} release index sha mismatch`)
    assert(errors, result.localArtifactSize === releaseArtifact?.size, `publication rehearsal artifact ${target.id} release index size mismatch`)
    assert(errors, result.localArtifactSha256 === publication?.sha256, `publication rehearsal artifact ${target.id} publication manifest sha mismatch`)
    assert(errors, result.localArtifactSize === publication?.size, `publication rehearsal artifact ${target.id} publication manifest size mismatch`)
    assert(errors, publication?.downloadUrl === '', `publication rehearsal artifact ${target.id} publication URL must remain empty`)
    assert(errors, publication?.downloadVerification?.downloadAttempted === false, `publication rehearsal artifact ${target.id} must not claim public download in manifest`)
    assert(errors, publication?.releaseIndexPatch?.patchApplied === false, `publication rehearsal artifact ${target.id} manifest must not apply patch`)

    const cachePath = path.join(report.savedArtifactRoot ?? '', result.localDownloadBack?.cacheRelativePath ?? '')
    const verificationPath = path.join(report.savedArtifactRoot ?? '', result.localDownloadBack?.verificationArtifact ?? '')
    const previewPath = path.join(report.savedArtifactRoot ?? '', result.releaseIndexPatchPreview?.previewArtifact ?? '')
    const verification = readSavedJson(errors, verificationPath, `publication rehearsal artifact ${target.id} verification artifact`)
    const patchPreview = readSavedJson(errors, previewPath, `publication rehearsal artifact ${target.id} patch preview artifact`)
    assert(errors, result.localDownloadBack?.attempted === true, `publication rehearsal artifact ${target.id} local download-back must be attempted`)
    assert(errors, sameResolvedPath(result.localDownloadBack?.cachedArtifactPath, cachePath), `publication rehearsal artifact ${target.id} cached artifact path mismatch`)
    assert(errors, fileExists(cachePath), `publication rehearsal artifact ${target.id} cached artifact missing`)
    if (fileExists(cachePath)) {
      assert(errors, fs.statSync(cachePath).size > 0, `publication rehearsal artifact ${target.id} cached artifact must be non-empty`)
      assert(errors, result.localDownloadBack?.downloadedSha256 === sha256File(cachePath), `publication rehearsal artifact ${target.id} cached sha mismatch`)
      assert(errors, result.localDownloadBack?.downloadedSize === fs.statSync(cachePath).size, `publication rehearsal artifact ${target.id} cached size mismatch`)
    }
    if (verification) {
      assert(errors, verification.id === target.id, `publication rehearsal artifact ${target.id} verification id mismatch`)
      assert(errors, verification.file === target.file, `publication rehearsal artifact ${target.id} verification file mismatch`)
      assert(errors, sameResolvedPath(verification.sourceArtifactPath, artifactPath), `publication rehearsal artifact ${target.id} verification source path mismatch`)
      assert(errors, sameResolvedPath(verification.cachedArtifactPath, cachePath), `publication rehearsal artifact ${target.id} verification cache path mismatch`)
      assert(errors, verification.expectedSha256 === result.localArtifactSha256, `publication rehearsal artifact ${target.id} verification expected sha mismatch`)
      assert(errors, verification.downloadedSha256 === result.localDownloadBack?.downloadedSha256, `publication rehearsal artifact ${target.id} verification downloaded sha mismatch`)
      assert(errors, verification.expectedSize === result.localArtifactSize, `publication rehearsal artifact ${target.id} verification expected size mismatch`)
      assert(errors, verification.downloadedSize === result.localDownloadBack?.downloadedSize, `publication rehearsal artifact ${target.id} verification downloaded size mismatch`)
      assert(errors, verification.sha256Matches === true, `publication rehearsal artifact ${target.id} verification sha must pass`)
      assert(errors, verification.sizeMatches === true, `publication rehearsal artifact ${target.id} verification size must pass`)
      assert(errors, verification.publicDownloadAttempted === false, `publication rehearsal artifact ${target.id} verification must not claim public download`)
      assert(errors, verification.localCopyOnly === true, `publication rehearsal artifact ${target.id} verification must stay local-copy-only`)
    }
    assert(errors, result.localDownloadBack?.sha256Matches === true, `publication rehearsal artifact ${target.id} local sha match must pass`)
    assert(errors, result.localDownloadBack?.sizeMatches === true, `publication rehearsal artifact ${target.id} local size match must pass`)
    assert(errors, result.localDownloadBack?.downloadedSha256 === result.localArtifactSha256, `publication rehearsal artifact ${target.id} downloaded sha must match artifact`)
    assert(errors, result.localDownloadBack?.downloadedSize === result.localArtifactSize, `publication rehearsal artifact ${target.id} downloaded size must match artifact`)
    assert(errors, String(result.localRehearsalDownloadUrl ?? '').startsWith('local-rehearsal://openlands/'), `publication rehearsal artifact ${target.id} local URL scheme mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.previewDownloadUrl === result.localRehearsalDownloadUrl, `publication rehearsal artifact ${target.id} preview URL mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.patchAllowed === false, `publication rehearsal artifact ${target.id} patch preview must not be allowed`)
    assert(errors, result.releaseIndexPatchPreview?.patchApplied === false, `publication rehearsal artifact ${target.id} patch preview must not apply`)
    assert(errors, result.releaseIndexPatchPreview?.releaseIndexCommit === null, `publication rehearsal artifact ${target.id} patch preview must not record commit`)
    assert(errors, result.releaseIndexPatchPreview?.match?.moduleId === MODULE_ID, `publication rehearsal artifact ${target.id} patch match module mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.match?.version === VERSION, `publication rehearsal artifact ${target.id} patch match version mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.match?.filename === target.file, `publication rehearsal artifact ${target.id} patch match filename mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.match?.sha256 === result.localArtifactSha256, `publication rehearsal artifact ${target.id} patch match sha mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.match?.size === result.localArtifactSize, `publication rehearsal artifact ${target.id} patch match size mismatch`)
    assert(errors, result.releaseIndexPatchPreview?.blocker === 'local_rehearsal_does_not_publish_public_urls', `publication rehearsal artifact ${target.id} patch blocker mismatch`)
    if (patchPreview) {
      assert(errors, patchPreview.id === result.releaseIndexPatchPreview?.id, `publication rehearsal artifact ${target.id} patch file id mismatch`)
      assert(errors, patchPreview.target === result.releaseIndexPatchPreview?.target, `publication rehearsal artifact ${target.id} patch file target mismatch`)
      assert(errors, sameJson(patchPreview.match, result.releaseIndexPatchPreview?.match), `publication rehearsal artifact ${target.id} patch file match mismatch`)
      assert(errors, patchPreview.previewDownloadUrl === result.localRehearsalDownloadUrl, `publication rehearsal artifact ${target.id} patch file URL mismatch`)
      assert(errors, patchPreview.patchAllowed === false, `publication rehearsal artifact ${target.id} patch file must not allow patch`)
      assert(errors, patchPreview.patchApplied === false, `publication rehearsal artifact ${target.id} patch file must not apply`)
      assert(errors, patchPreview.releaseIndexCommit === null, `publication rehearsal artifact ${target.id} patch file must not record commit`)
      assert(errors, patchPreview.blocker === 'local_rehearsal_does_not_publish_public_urls', `publication rehearsal artifact ${target.id} patch file blocker mismatch`)
    }
    if (aggregatePatchPreview) {
      const aggregateEntry = (aggregatePatchPreview.entries ?? []).find((entry) => entry.id === target.id)
      assert(errors, aggregateEntry !== undefined, `publication rehearsal artifact ${target.id} missing aggregate patch preview entry`)
      assert(errors, aggregateEntry?.file === target.file, `publication rehearsal artifact ${target.id} aggregate patch file mismatch`)
      assert(errors, sameJson(aggregateEntry?.match, result.releaseIndexPatchPreview?.match), `publication rehearsal artifact ${target.id} aggregate patch match mismatch`)
      assert(errors, aggregateEntry?.previewDownloadUrl === result.localRehearsalDownloadUrl, `publication rehearsal artifact ${target.id} aggregate patch URL mismatch`)
      assert(errors, aggregateEntry?.patchAllowed === false, `publication rehearsal artifact ${target.id} aggregate patch must not allow patch`)
      assert(errors, aggregateEntry?.patchApplied === false, `publication rehearsal artifact ${target.id} aggregate patch must not apply`)
    }
    for (const savedArtifact of result.savedArtifacts ?? []) {
      const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
      assert(errors, fileExists(savedArtifactPath), `publication rehearsal saved artifact missing ${savedArtifact}`)
      if (fileExists(savedArtifactPath)) {
        assert(errors, fs.statSync(savedArtifactPath).size > 0, `publication rehearsal saved artifact empty ${savedArtifact}`)
      }
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    reportPath,
    artifactCount: report.artifactResults?.length ?? 0,
    localDownloadVerifiedCount: report.summary?.localDownloadVerifiedCount ?? 0,
    publicAlphaReady: report.publicAlphaReady,
    clearsDistributionGates: report.clearsDistributionGates,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const reportPath = args.report ? path.resolve(args.report) : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-rehearsal-report.json')
  const result = validate({ moduleRoot, releaseRoot, reportPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands release publication rehearsal validated: ${result.artifactCount} artifacts, localDownloadVerified=${result.localDownloadVerifiedCount}, publicAlphaReady=${result.publicAlphaReady}.`)
  } else {
    console.error(`Openlands release publication rehearsal failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-release-publication-rehearsal-report.mjs [options]

Options:
  --module-root <path>    Openlands module root. Auto-detected by default.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Report path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-rehearsal-report.json.
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
