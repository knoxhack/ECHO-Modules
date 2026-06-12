import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RELEASE_PUBLICATION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DISTRIBUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    manifest: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--manifest') args.manifest = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
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

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function publicationById(manifest, id) {
  return (manifest?.artifactPublications ?? []).find((publication) => publication.id === id)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function buildPatchPreview(releaseIndex, artifactResults) {
  return {
    schema: 'echo.openlands.release_publication_rehearsal_patch_preview.v1',
    patchApplied: false,
    patchAllowed: false,
    releaseId: releaseIndex.releaseId,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    target: 'modules[].artifacts[].downloadUrl',
    entries: artifactResults.map((artifact) => ({
      id: artifact.id,
      file: artifact.file,
      match: artifact.releaseIndexPatchPreview.match,
      previewDownloadUrl: artifact.localRehearsalDownloadUrl,
      patchApplied: false,
      patchAllowed: false,
    })),
    blocker: 'local_rehearsal_does_not_publish_public_urls',
  }
}

function buildReport({ moduleRoot, releaseRoot, manifestPath, outputPath, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const releasePublicationContract = readJson(path.join(resourcesRoot, RELEASE_PUBLICATION_CONTRACT))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  assert(fileExists(manifestPath), `publication manifest template not found: ${manifestPath}`)
  const releaseIndex = readJson(releaseIndexPath)
  const publicationManifest = readJson(manifestPath)
  assert(publicationManifest.schema === releasePublicationContract.reportContract?.schema, 'publication manifest schema mismatch')
  assert(publicationManifest.status === 'template_blocked', 'publication rehearsal requires the blocked template before public URLs exist')
  assert(publicationManifest.moduleId === MODULE_ID, 'publication manifest module id mismatch')
  assert(publicationManifest.moduleVersion === VERSION, 'publication manifest version mismatch')
  assert(publicationManifest.releaseId === releaseIndex.releaseId, 'publication manifest release id mismatch')

  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)

  const savedArtifactRoot = dryRun
    ? path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-publication-rehearsal-')), 'saved-artifacts')
    : path.join(path.dirname(outputPath), 'openlands-release-publication-rehearsal-artifacts')
  if (!dryRun) fs.rmSync(savedArtifactRoot, { recursive: true, force: true })
  fs.mkdirSync(savedArtifactRoot, { recursive: true })
  writeJson(path.join(savedArtifactRoot, 'release-index-snapshot.json'), releaseIndex)
  writeJson(path.join(savedArtifactRoot, 'publication-manifest-template-snapshot.json'), publicationManifest)

  const distributionTargets = new Map((distribution.artifactTargets ?? []).map((target) => [target.id, target]))
  const artifactResults = (releasePublicationContract.artifactTargets ?? []).map((target) => {
    const distributionTarget = distributionTargets.get(target.id)
    assert(distributionTarget?.file === target.file, `distribution target ${target.id} file mismatch`)
    assert(distributionTarget?.requiredForPublicAlpha === target.requiredForPublicAlpha, `distribution target ${target.id} public alpha flag mismatch`)

    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(releaseArtifact, `release index missing artifact ${target.file}`)
    assert(releaseArtifact.kind === target.releaseIndexArtifactKind, `release artifact ${target.file} kind mismatch`)
    assert(normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === target.runtimeTarget, `release artifact ${target.file} runtime target mismatch`)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(fileExists(artifactPath), `artifact file missing: ${artifactPath}`)
    const artifactSha256 = sha256File(artifactPath)
    const artifactSize = fs.statSync(artifactPath).size
    assert(releaseArtifact.sha256 === artifactSha256, `release index sha mismatch for ${target.file}`)
    assert(releaseArtifact.size === artifactSize, `release index size mismatch for ${target.file}`)

    const publication = publicationById(publicationManifest, target.id)
    assert(publication, `publication manifest missing artifact ${target.id}`)
    assert(publication.file === target.file, `publication manifest file mismatch for ${target.id}`)
    assert(publication.sha256 === artifactSha256, `publication manifest sha mismatch for ${target.id}`)
    assert(publication.size === artifactSize, `publication manifest size mismatch for ${target.id}`)
    assert(publication.downloadUrl === '', `publication manifest must keep public URL empty for ${target.id}`)
    assert(publication.urlStatus === 'missing_url', `publication manifest must keep missing_url for ${target.id}`)
    assert(publication.downloadVerification?.downloadAttempted === false, `publication manifest must not claim public download for ${target.id}`)
    assert(publication.releaseIndexPatch?.patchAllowed === false, `publication manifest must not allow real patch for ${target.id}`)
    assert(publication.releaseIndexPatch?.patchApplied === false, `publication manifest must not apply real patch for ${target.id}`)

    const cacheRelativePath = path.join('download-cache', target.file).replace(/\\/g, '/')
    const cachedArtifactPath = path.join(savedArtifactRoot, cacheRelativePath)
    fs.mkdirSync(path.dirname(cachedArtifactPath), { recursive: true })
    fs.copyFileSync(artifactPath, cachedArtifactPath)
    const downloadedSha256 = sha256File(cachedArtifactPath)
    const downloadedSize = fs.statSync(cachedArtifactPath).size
    const verificationRelativePath = path.join('verification', `${target.id}-download-back-verification.json`).replace(/\\/g, '/')
    const verification = {
      id: target.id,
      file: target.file,
      sourceArtifactPath: artifactPath,
      cachedArtifactPath,
      expectedSha256: artifactSha256,
      downloadedSha256,
      expectedSize: artifactSize,
      downloadedSize,
      sha256Matches: downloadedSha256 === artifactSha256,
      sizeMatches: downloadedSize === artifactSize,
      publicDownloadAttempted: false,
      localCopyOnly: true,
    }
    writeJson(path.join(savedArtifactRoot, verificationRelativePath), verification)

    const previewRelativePath = path.join('patch-preview', `${target.id}-release-index-patch-preview.json`).replace(/\\/g, '/')
    const patchPreview = {
      id: target.id,
      target: 'modules[].artifacts[].downloadUrl',
      match: {
        moduleId: MODULE_ID,
        version: VERSION,
        filename: target.file,
        sha256: artifactSha256,
        size: artifactSize,
      },
      previewDownloadUrl: `local-rehearsal://openlands/${target.file}`,
      patchAllowed: false,
      patchApplied: false,
      releaseIndexCommit: null,
      blocker: 'local_rehearsal_does_not_publish_public_urls',
    }
    writeJson(path.join(savedArtifactRoot, previewRelativePath), patchPreview)

    return {
      id: target.id,
      file: target.file,
      kind: releaseArtifact.kind,
      runtimeTarget: target.runtimeTarget,
      requiredForPublicAlpha: target.requiredForPublicAlpha,
      artifactPath,
      releaseIndexEntryPresent: true,
      releaseIndexDownloadUrl: releaseArtifact.downloadUrl ?? '',
      publicDownloadUrlPresent: false,
      publicationTemplateStatus: publication.urlStatus,
      localArtifactSha256: artifactSha256,
      localArtifactSize: artifactSize,
      localRehearsalDownloadUrl: patchPreview.previewDownloadUrl,
      localDownloadBack: {
        attempted: true,
        cacheRelativePath,
        cachedArtifactPath,
        downloadedSha256,
        downloadedSize,
        sha256Matches: downloadedSha256 === artifactSha256,
        sizeMatches: downloadedSize === artifactSize,
        verificationArtifact: verificationRelativePath,
      },
      releaseIndexPatchPreview: {
        ...patchPreview,
        previewArtifact: previewRelativePath,
      },
      savedArtifacts: [
        cacheRelativePath,
        verificationRelativePath,
        previewRelativePath,
      ],
    }
  })

  const aggregatePatchPreview = buildPatchPreview(releaseIndex, artifactResults)
  const aggregatePatchPreviewArtifact = path.join('patch-preview', 'release-index-download-url-patch.preview.json').replace(/\\/g, '/')
  writeJson(path.join(savedArtifactRoot, aggregatePatchPreviewArtifact), aggregatePatchPreview)

  const localDownloadVerifiedCount = artifactResults.filter((artifact) =>
    artifact.localDownloadBack.sha256Matches && artifact.localDownloadBack.sizeMatches).length
  const patchPreviewCount = artifactResults.filter((artifact) =>
    artifact.releaseIndexPatchPreview.patchAllowed === false && artifact.releaseIndexPatchPreview.patchApplied === false).length

  const report = {
    schema: 'echo.openlands.release_publication_rehearsal_report.v1',
    status: 'preflight_passed',
    publicAlphaReady: false,
    rehearsalOnly: true,
    clearsDistributionGates: false,
    clearsReleasePublicationGates: false,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    releasePublicationManifestPath: manifestPath,
    releasePublicationManifestContract: RELEASE_PUBLICATION_CONTRACT,
    distributionContract: DISTRIBUTION_CONTRACT,
    savedArtifactRoot,
    artifactResults,
    patchPreview: {
      status: 'preview_only',
      patchApplied: false,
      patchAllowed: false,
      artifact: aggregatePatchPreviewArtifact,
      entryCount: aggregatePatchPreview.entries.length,
      localPlaceholderScheme: 'local-rehearsal://openlands/',
    },
    summary: {
      artifactCount: artifactResults.length,
      requiredForPublicAlphaCount: artifactResults.filter((artifact) => artifact.requiredForPublicAlpha).length,
      missingPublicDownloadUrlCount: artifactResults.filter((artifact) => !artifact.publicDownloadUrlPresent).length,
      localDownloadVerifiedCount,
      patchPreviewCount,
      releaseIndexPatchAppliedCount: artifactResults.filter((artifact) => artifact.releaseIndexPatchPreview.patchApplied === true).length,
    },
    blockedBy: [
      'public_release_download_urls_missing',
      'public_download_verification_missing',
      'release_index_patch_approval_missing',
      'distribution_approval_missing',
      'local_rehearsal_does_not_publish_public_urls',
    ],
    proofs: [
      'release_publication_contract_loaded',
      'release_index_loaded',
      'publication_manifest_loaded',
      'all_local_artifacts_present',
      'local_download_back_hashes_match',
      'release_index_patch_preview_generated',
      'public_alpha_stays_blocked_until_public_urls_verified',
    ],
    outputPath,
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  return report
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const manifestPath = args.manifest
    ? path.resolve(args.manifest)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json')
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-rehearsal-report.json')
  const report = buildReport({ moduleRoot, releaseRoot, manifestPath, outputPath, dryRun: args.dryRun })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands release publication rehearsal ${action}: ${report.summary.artifactCount} artifacts, localDownloadVerified=${report.summary.localDownloadVerifiedCount}, publicAlphaReady=${report.publicAlphaReady}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-release-publication-rehearsal-report.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --manifest <path>       Publication manifest template path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json.
  --out <path>            Report output path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-rehearsal-report.json.
  --dry-run               Generate saved artifacts in a temp directory without writing the report.
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
