import crypto from 'node:crypto'
import fs from 'node:fs'
import https from 'node:https'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { publicHttpsUrlReason } from './openlands-public-download-url.mjs'
import { validatePublicationUrlMap } from './validate-openlands-publication-url-map.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DEFAULT_TIMEOUT_MS = 120000

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    manifest: null,
    urlMap: null,
    output: null,
    verificationRoot: null,
    timeoutMs: DEFAULT_TIMEOUT_MS,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--manifest') args.manifest = argv[++index]
    else if (arg === '--url-map') args.urlMap = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--verification-root') args.verificationRoot = argv[++index]
    else if (arg === '--timeout-ms') args.timeoutMs = Number.parseInt(argv[++index], 10)
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  if (!Number.isInteger(args.timeoutMs) || args.timeoutMs <= 0) throw new Error('--timeout-ms must be a positive integer')
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''))
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

function urlMapEntry(urlMap, id) {
  if (!urlMap) return null
  if (Array.isArray(urlMap.artifactUrls)) return urlMap.artifactUrls.find((entry) => entry.id === id) ?? null
  if (urlMap.urls && typeof urlMap.urls === 'object') {
    const value = urlMap.urls[id]
    if (typeof value === 'string') return { id, downloadUrl: value }
    if (value && typeof value === 'object') return { id, ...value }
  }
  const value = urlMap[id]
  if (typeof value === 'string') return { id, downloadUrl: value }
  if (value && typeof value === 'object') return { id, ...value }
  return null
}

function requirePublicUrl(downloadUrl, id) {
  assert(typeof downloadUrl === 'string' && downloadUrl.length > 0, `missing public download URL for ${id}`)
  const reason = publicHttpsUrlReason(downloadUrl)
  assert(reason === null, `download URL for ${id} must use a public https URL: ${downloadUrl}${reason ? ` (${reason})` : ''}`)
  const parsed = new URL(downloadUrl)
  return parsed.toString()
}

function requestClient(protocol) {
  if (protocol === 'https:') return https
  throw new Error(`Unsupported download protocol: ${protocol}`)
}

function downloadToFile(downloadUrl, outputPath, timeoutMs, redirectCount = 0) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(downloadUrl)
    const reason = publicHttpsUrlReason(downloadUrl)
    if (reason !== null) {
      reject(new Error(`Download URL and redirects must use a public https URL: ${downloadUrl} (${reason})`))
      return
    }
    const client = requestClient(parsed.protocol)
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    const request = client.get(parsed, {
      timeout: timeoutMs,
      headers: {
        'User-Agent': `openlands-publication-verifier/${VERSION}`,
      },
    }, (response) => {
      const status = response.statusCode ?? 0
      const location = response.headers.location
      if (status >= 300 && status < 400 && location) {
        response.resume()
        if (redirectCount >= 5) {
          reject(new Error(`Too many redirects for ${downloadUrl}`))
          return
        }
        const redirected = new URL(location, parsed)
        const redirectReason = publicHttpsUrlReason(redirected.toString())
        if (redirectReason !== null) {
          reject(new Error(`Download redirect for ${downloadUrl} must use a public https URL: ${redirected.toString()} (${redirectReason})`))
          return
        }
        downloadToFile(redirected.toString(), outputPath, timeoutMs, redirectCount + 1).then(resolve, reject)
        return
      }
      if (status !== 200) {
        response.resume()
        reject(new Error(`Download failed for ${downloadUrl}: HTTP ${status}`))
        return
      }
      const stream = fs.createWriteStream(outputPath)
      response.pipe(stream)
      stream.on('finish', () => {
        stream.close(() => resolve({
          finalUrl: parsed.toString(),
          statusCode: status,
          contentLength: response.headers['content-length'] ?? null,
        }))
      })
      stream.on('error', (error) => {
        fs.rmSync(outputPath, { force: true })
        reject(error)
      })
    })
    request.on('timeout', () => {
      request.destroy(new Error(`Download timed out after ${timeoutMs}ms: ${downloadUrl}`))
    })
    request.on('error', (error) => {
      fs.rmSync(outputPath, { force: true })
      reject(error)
    })
  })
}

async function buildVerifiedManifest({ moduleRoot, releaseRoot, manifestPath, urlMapPath, outputPath, verificationRoot, timeoutMs, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  assert(fileExists(manifestPath), `publication manifest not found: ${manifestPath}`)
  const releaseIndex = readJson(releaseIndexPath)
  const inputManifest = readJson(manifestPath)
  const urlMap = urlMapPath ? readJson(urlMapPath) : null
  assert(inputManifest.schema === contract.reportContract?.schema, 'publication manifest schema mismatch')
  assert(inputManifest.moduleId === MODULE_ID, 'publication manifest module id mismatch')
  assert(inputManifest.moduleVersion === VERSION, 'publication manifest version mismatch')
  assert(inputManifest.releaseId === releaseIndex.releaseId, 'publication manifest release id mismatch')
  if (urlMapPath) {
    const urlMapValidation = validatePublicationUrlMap({
      moduleRoot,
      releaseRoot,
      manifestPath,
      urlMapPath,
      requireUrls: true,
    })
    assert(
      urlMapValidation.status === 'passed',
      `publication URL map validation failed before download: ${urlMapValidation.errors.join('; ')}`,
    )
  }

  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)

  if (!dryRun) fs.rmSync(verificationRoot, { recursive: true, force: true })
  fs.mkdirSync(verificationRoot, { recursive: true })
  writeJson(path.join(verificationRoot, 'release-index-snapshot.json'), releaseIndex)
  writeJson(path.join(verificationRoot, 'input-publication-manifest-snapshot.json'), inputManifest)
  if (urlMap) writeJson(path.join(verificationRoot, 'publication-url-map-snapshot.json'), urlMap)

  const verifiedAt = new Date().toISOString()
  const artifactPublications = []
  const verificationResults = []
  for (const target of contract.artifactTargets ?? []) {
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(releaseArtifact, `release index missing artifact ${target.file}`)
    assert(releaseArtifact.kind === target.releaseIndexArtifactKind, `release index artifact kind mismatch for ${target.file}`)
    assert(normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === target.runtimeTarget, `release index runtime target mismatch for ${target.file}`)
    const localArtifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(fileExists(localArtifactPath), `local artifact missing: ${localArtifactPath}`)
    assert(sha256File(localArtifactPath) === releaseArtifact.sha256, `local artifact sha mismatch: ${target.file}`)
    assert(fs.statSync(localArtifactPath).size === releaseArtifact.size, `local artifact size mismatch: ${target.file}`)

    const inputPublication = publicationById(inputManifest, target.id)
    assert(inputPublication, `publication manifest missing artifact ${target.id}`)
    assert(inputPublication.file === target.file, `publication manifest file mismatch for ${target.id}`)
    assert(inputPublication.sha256 === releaseArtifact.sha256, `publication manifest sha mismatch for ${target.id}`)
    assert(inputPublication.size === releaseArtifact.size, `publication manifest size mismatch for ${target.id}`)
    const mapped = urlMapEntry(urlMap, target.id)
    const downloadUrl = requirePublicUrl(mapped?.downloadUrl ?? inputPublication.downloadUrl, target.id)
    const downloadPath = path.join(verificationRoot, 'downloads', target.file)
    const downloadMeta = await downloadToFile(downloadUrl, downloadPath, timeoutMs)
    const downloadedSha256 = sha256File(downloadPath)
    const downloadedSize = fs.statSync(downloadPath).size
    const sha256Matches = downloadedSha256 === releaseArtifact.sha256
    const sizeMatches = downloadedSize === releaseArtifact.size
    assert(sha256Matches, `downloaded sha mismatch for ${target.id}: expected ${releaseArtifact.sha256}, got ${downloadedSha256}`)
    assert(sizeMatches, `downloaded size mismatch for ${target.id}: expected ${releaseArtifact.size}, got ${downloadedSize}`)
    const verificationArtifact = path.join('verification', `${target.id}-public-download-verification.json`).replace(/\\/g, '/')
    const verification = {
      id: target.id,
      file: target.file,
      downloadUrl,
      finalUrl: downloadMeta.finalUrl,
      statusCode: downloadMeta.statusCode,
      contentLength: downloadMeta.contentLength,
      expectedSha256: releaseArtifact.sha256,
      downloadedSha256,
      expectedSize: releaseArtifact.size,
      downloadedSize,
      sha256Matches,
      sizeMatches,
      verifiedAt,
      downloadedArtifact: path.relative(verificationRoot, downloadPath).replace(/\\/g, '/'),
    }
    writeJson(path.join(verificationRoot, verificationArtifact), verification)
    verificationResults.push(verification)

    artifactPublications.push({
      id: target.id,
      file: target.file,
      kind: releaseArtifact.kind,
      runtimeTarget: target.runtimeTarget,
      requiredForPublicAlpha: target.requiredForPublicAlpha,
      sha256: releaseArtifact.sha256,
      size: releaseArtifact.size,
      downloadUrl,
      urlStatus: 'download_verified',
      uploadProvider: mapped?.uploadProvider ?? inputPublication.uploadProvider ?? '',
      storageKey: mapped?.storageKey ?? inputPublication.storageKey ?? '',
      publishedAt: mapped?.publishedAt ?? inputPublication.publishedAt ?? null,
      downloadVerification: {
        downloadAttempted: true,
        downloadedSha256,
        downloadedSize,
        sha256Matches,
        sizeMatches,
        verifiedAt,
        verificationArtifact: path.join(verificationRoot, verificationArtifact),
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
    })
  }

  writeJson(path.join(verificationRoot, 'public-download-verification-summary.json'), {
    schema: 'echo.openlands.release_publication_download_verification_summary.v1',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    verifiedAt,
    artifactCount: verificationResults.length,
    verificationResults,
  })

  const verifiedManifest = {
    schema: contract.reportContract.schema,
    status: 'verified',
    generatedAt: verifiedAt,
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    releasePublicationManifestContract: CONTRACT_PATH,
    sourceContracts: contract.sourceContracts,
    artifactPublications,
    releaseIndexPatchRules: contract.releaseIndexPatchRules,
    blockedBy: [
      'release_index_patch_not_approved',
      'distribution_approval_missing',
    ],
    nextSteps: [
      'Review the verified publication manifest and approve the Release Index downloadUrl patch.',
      'Patch echo-release.json only after approval records the patch id or commit.',
      'Attach the approved publication manifest to distribution approval evidence.',
    ],
    summary: {
      artifactCount: artifactPublications.length,
      missingDownloadUrlCount: artifactPublications.filter((artifact) => !artifact.downloadUrl).length,
      downloadVerifiedCount: artifactPublications.filter((artifact) => artifact.downloadVerification.sha256Matches && artifact.downloadVerification.sizeMatches).length,
      releaseIndexPatchAllowedCount: artifactPublications.filter((artifact) => artifact.releaseIndexPatch.patchAllowed).length,
    },
    verificationRoot,
    inputManifestPath: manifestPath,
    urlMapPath,
    outputPath,
  }

  if (!dryRun) writeJson(outputPath, verifiedManifest)
  return verifiedManifest
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
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json')
  const verificationRoot = args.verificationRoot
    ? path.resolve(args.verificationRoot)
    : args.dryRun
      ? fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-publication-verification-'))
      : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-verification-artifacts')
  const manifest = await buildVerifiedManifest({
    moduleRoot,
    releaseRoot,
    manifestPath,
    urlMapPath: args.urlMap ? path.resolve(args.urlMap) : null,
    outputPath,
    verificationRoot,
    timeoutMs: args.timeoutMs,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(manifest, null, 2))
  } else {
    const action = args.dryRun ? 'verified dry-run' : `wrote ${outputPath}`
    console.log(`Openlands publication downloads ${action}: status=${manifest.status}, verified=${manifest.summary.downloadVerifiedCount}/${manifest.summary.artifactCount}, patchAllowed=${manifest.summary.releaseIndexPatchAllowedCount}.`)
  }
  return manifest
}

function printHelp() {
  console.log(`Usage: node verify-openlands-release-publication-downloads.mjs [options]

Options:
  --module-root <path>       Openlands module root. Defaults to this script's module.
  --release-root <path>      Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --manifest <path>          Input publication manifest. Defaults to the blocked template.
  --url-map <path>           JSON map of artifact ids to public download URLs.
  --out <path>               Output verified manifest. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.verified.json.
  --verification-root <path> Saved download and verification evidence root.
  --timeout-ms <ms>          Per-request timeout. Defaults to ${DEFAULT_TIMEOUT_MS}.
  --dry-run                  Download and verify into a temp directory without writing the verified manifest.
  --json                     Print JSON output.
  --help                     Show this help.

URL map examples:
  { "urls": { "native": "https://host/echoopenlandsprotocol-0.1.0.echo-addon" } }
  { "artifactUrls": [{ "id": "native", "downloadUrl": "https://host/file", "uploadProvider": "github_release" }] }
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
