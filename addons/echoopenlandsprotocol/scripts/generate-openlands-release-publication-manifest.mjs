import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
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

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function buildManifest({ moduleRoot, releaseRoot, outputPath, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  const releaseIndex = readJson(releaseIndexPath)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)

  const distributionTargets = new Map((distribution.artifactTargets ?? []).map((target) => [target.id, target]))
  const artifactPublications = (contract.artifactTargets ?? []).map((target) => {
    const distributionTarget = distributionTargets.get(target.id)
    assert(distributionTarget?.file === target.file, `distribution target ${target.id} file mismatch`)
    assert(distributionTarget?.requiredForPublicAlpha === target.requiredForPublicAlpha, `distribution target ${target.id} public alpha flag mismatch`)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(releaseArtifact, `release index missing artifact ${target.file}`)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(fileExists(artifactPath), `artifact file missing: ${artifactPath}`)
    const fileSha256 = sha256File(artifactPath)
    const fileSize = fs.statSync(artifactPath).size
    assert(releaseArtifact.sha256 === fileSha256, `artifact sha mismatch: ${target.file}`)
    assert(releaseArtifact.size === fileSize, `artifact size mismatch: ${target.file}`)
    if (releaseArtifact.downloadUrl) {
      assert(isPublicHttpsUrl(releaseArtifact.downloadUrl), `release index downloadUrl for ${target.file} must use a public https URL`)
    }
    if (target.id !== 'sources') {
      assert(releaseArtifact.buildMode === 'compiled-runtime', `artifact ${target.file} must be compiled-runtime`)
      assert(normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === target.runtimeTarget, `artifact ${target.file} runtime target mismatch`)
    }

    return {
      id: target.id,
      file: target.file,
      kind: releaseArtifact.kind,
      runtimeTarget: target.runtimeTarget,
      requiredForPublicAlpha: target.requiredForPublicAlpha,
      sha256: releaseArtifact.sha256,
      size: releaseArtifact.size,
      downloadUrl: releaseArtifact.downloadUrl ?? '',
      urlStatus: releaseArtifact.downloadUrl ? 'url_recorded' : 'missing_url',
      uploadProvider: '',
      storageKey: '',
      publishedAt: null,
      downloadVerification: {
        downloadAttempted: false,
        downloadedSha256: null,
        downloadedSize: null,
        sha256Matches: false,
        sizeMatches: false,
        verifiedAt: null,
        verificationArtifact: null,
      },
      releaseIndexPatch: {
        target: 'modules[].artifacts[].downloadUrl',
        match: {
          moduleId: MODULE_ID,
          version: VERSION,
          filename: target.file,
          sha256: releaseArtifact.sha256,
          size: releaseArtifact.size,
        },
        patchAllowed: false,
        patchApplied: false,
        releaseIndexCommit: null,
      },
    }
  })

  const missingUrls = artifactPublications.filter((artifact) => !artifact.downloadUrl)
  const manifest = {
    schema: contract.reportContract.schema,
    status: missingUrls.length === 0 ? 'urls_pending' : contract.blockedTemplateRules.status,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    releasePublicationManifestContract: CONTRACT_PATH,
    sourceContracts: contract.sourceContracts,
    artifactPublications,
    releaseIndexPatchRules: contract.releaseIndexPatchRules,
    blockedBy: missingUrls.length === 0
      ? ['download_verification_missing', 'release_index_patch_not_approved']
      : contract.blockedTemplateRules.requiredBlockedBy,
    nextSteps: contract.blockedTemplateRules.requiredNextSteps,
    summary: {
      artifactCount: artifactPublications.length,
      missingDownloadUrlCount: missingUrls.length,
      downloadVerifiedCount: artifactPublications.filter((artifact) => artifact.downloadVerification.sha256Matches && artifact.downloadVerification.sizeMatches).length,
      releaseIndexPatchAllowedCount: artifactPublications.filter((artifact) => artifact.releaseIndexPatch.patchAllowed).length,
    },
    outputPath,
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
  }
  return manifest
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json')
  const manifest = buildManifest({ moduleRoot, releaseRoot, outputPath, dryRun: args.dryRun })
  if (args.json) {
    console.log(JSON.stringify(manifest, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands release publication manifest ${action}: status=${manifest.status}, missingUrls=${manifest.summary.missingDownloadUrlCount}.`)
  }
  return manifest
}

function printHelp() {
  console.log(`Usage: node generate-openlands-release-publication-manifest.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Manifest output path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json.
  --dry-run               Validate without writing.
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
