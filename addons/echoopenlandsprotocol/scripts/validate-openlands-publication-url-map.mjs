import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { publicHttpsUrlReason } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const URL_MAP_SCHEMA = 'echo.openlands.release_publication_url_map_template.v1'
const URL_MAP_METADATA_KEYS = new Set([
  'schema',
  'generatedAt',
  'dryRun',
  'moduleId',
  'moduleVersion',
  'releaseId',
  'sourceManifestPath',
  'publicHttpsRequired',
  'artifactUrls',
  'urls',
  'verifierCommand',
  'outputPath',
  'notes',
  'instructions',
])

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    releaseRoot: null,
    manifest: null,
    urlMap: null,
    requireUrls: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--manifest') args.manifest = argv[++index]
    else if (arg === '--url-map') args.urlMap = argv[++index]
    else if (arg === '--require-urls') args.requireUrls = true
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
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''))
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
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

function duplicateValues(values) {
  const seen = new Set()
  const duplicates = new Set()
  for (const value of values ?? []) {
    if (seen.has(value)) duplicates.add(value)
    seen.add(value)
  }
  return [...duplicates].sort()
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
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

function declaredIds(urlMap) {
  if (Array.isArray(urlMap?.artifactUrls)) {
    return urlMap.artifactUrls.map((entry) => entry?.id).filter(Boolean)
  }
  if (urlMap?.urls && typeof urlMap.urls === 'object') return Object.keys(urlMap.urls)
  return Object.entries(urlMap ?? {})
    .filter(([key, value]) => !URL_MAP_METADATA_KEYS.has(key) && (typeof value === 'string' || (value && typeof value === 'object')))
    .map(([key]) => key)
}

export function validatePublicationUrlMap({ moduleRoot, releaseRoot, manifestPath, urlMapPath, requireUrls }) {
  const errors = []
  assert(errors, fileExists(manifestPath), `publication manifest not found: ${manifestPath}`)
  assert(errors, fileExists(urlMapPath), `publication URL map not found: ${urlMapPath}`)
  if (!fileExists(manifestPath) || !fileExists(urlMapPath)) {
    return {
      status: 'failed',
      manifestPath,
      urlMapPath,
      errors,
    }
  }

  const manifest = readJson(manifestPath)
  const urlMap = readJson(urlMapPath)
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const expectedArtifacts = manifest.artifactPublications ?? []
  const expectedIds = expectedArtifacts.map((artifact) => artifact.id)
  const urlIds = declaredIds(urlMap)
  const duplicateUrlIds = duplicateValues(urlIds)

  assert(errors, manifest.schema === 'echo.openlands.release_publication_manifest.v1', 'publication manifest schema mismatch')
  assert(errors, manifest.moduleId === MODULE_ID, 'publication manifest moduleId mismatch')
  assert(errors, manifest.moduleVersion === VERSION, 'publication manifest moduleVersion mismatch')
  assert(errors, expectedArtifacts.length > 0, 'publication manifest must include artifactPublications')
  assert(errors, releaseModule !== undefined, `${MODULE_ID} ${VERSION} missing from release index`)
  if (urlMap.schema !== undefined) assert(errors, urlMap.schema === URL_MAP_SCHEMA, `URL map schema must be ${URL_MAP_SCHEMA}`)
  if (urlMap.moduleId !== undefined) assert(errors, urlMap.moduleId === MODULE_ID, 'URL map moduleId mismatch')
  if (urlMap.moduleVersion !== undefined) assert(errors, urlMap.moduleVersion === VERSION, 'URL map moduleVersion mismatch')
  if (urlMap.releaseId !== undefined) assert(errors, urlMap.releaseId === manifest.releaseId, 'URL map releaseId must match publication manifest')
  if (Array.isArray(urlMap.artifactUrls)) {
    for (const [index, entry] of urlMap.artifactUrls.entries()) {
      assert(errors, entry && typeof entry === 'object' && !Array.isArray(entry), `URL map artifactUrls[${index}] must be an object`)
      assert(errors, typeof entry?.id === 'string' && entry.id.length > 0, `URL map artifactUrls[${index}] must include an id`)
    }
  }
  assert(errors, duplicateUrlIds.length === 0, `URL map contains duplicate artifact ids: ${duplicateUrlIds.join(', ')}`)
  assert(errors, sameSet(urlIds, expectedIds), 'URL map artifact ids must cover exactly the manifest artifact ids')

  const artifactResults = expectedArtifacts.map((artifact) => {
    const entry = urlMapEntry(urlMap, artifact.id)
    const releaseArtifact = artifactByFile(releaseModule, artifact.file)
    const downloadUrl = entry?.downloadUrl ?? ''
    const urlMissing = typeof downloadUrl !== 'string' || downloadUrl.length === 0
    const urlReason = urlMissing ? 'missing' : publicHttpsUrlReason(downloadUrl)
    assert(errors, entry !== null, `URL map missing artifact id ${artifact.id}`)
    assert(errors, releaseArtifact !== undefined, `release index missing artifact ${artifact.file}`)
    assert(errors, releaseArtifact?.sha256 === artifact.sha256, `release index sha256 mismatch for ${artifact.id}`)
    assert(errors, releaseArtifact?.size === artifact.size, `release index size mismatch for ${artifact.id}`)
    if (entry) {
      if (entry.file !== undefined) assert(errors, entry.file === artifact.file, `URL map file mismatch for ${artifact.id}`)
      if (entry.kind !== undefined) assert(errors, entry.kind === artifact.kind, `URL map kind mismatch for ${artifact.id}`)
      if (entry.runtimeTarget !== undefined) assert(errors, entry.runtimeTarget === artifact.runtimeTarget, `URL map runtimeTarget mismatch for ${artifact.id}`)
      if (entry.requiredForPublicAlpha !== undefined) assert(errors, entry.requiredForPublicAlpha === (artifact.requiredForPublicAlpha === true), `URL map requiredForPublicAlpha mismatch for ${artifact.id}`)
      if (entry.expectedSha256 !== undefined) assert(errors, entry.expectedSha256 === artifact.sha256, `URL map expectedSha256 mismatch for ${artifact.id}`)
      if (entry.expectedSize !== undefined) assert(errors, entry.expectedSize === artifact.size, `URL map expectedSize mismatch for ${artifact.id}`)
    }
    if (requireUrls) assert(errors, !urlMissing, `URL map missing required downloadUrl for ${artifact.id}`)
    if (!urlMissing) assert(errors, urlReason === null, `URL map downloadUrl for ${artifact.id} must be public HTTPS: ${urlReason}`)
    return {
      id: artifact.id,
      file: artifact.file,
      present: entry !== null,
      downloadUrlPresent: !urlMissing,
      publicHttps: !urlMissing && urlReason === null,
      urlReason,
    }
  })

  return {
    status: errors.length === 0 ? 'passed' : 'failed',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: manifest.releaseId,
    manifestPath,
    urlMapPath,
    requireUrls,
    artifactCount: expectedArtifacts.length,
    missingUrlCount: artifactResults.filter((artifact) => !artifact.downloadUrlPresent).length,
    publicHttpsUrlCount: artifactResults.filter((artifact) => artifact.publicHttps).length,
    artifactResults,
    errors,
  }
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
  const urlMapPath = args.urlMap
    ? path.resolve(args.urlMap)
    : path.join(releaseRoot, MODULE_ID, 'openlands-publication-url-map.template.json')
  const result = validatePublicationUrlMap({
    moduleRoot,
    releaseRoot,
    manifestPath,
    urlMapPath,
    requireUrls: args.requireUrls,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands publication URL map validated: artifacts=${result.artifactCount}, missingUrls=${result.missingUrlCount}, publicHttpsUrls=${result.publicHttpsUrlCount}.`)
  } else {
    console.error(`Openlands publication URL map failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-publication-url-map.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --manifest <path>       Publication manifest path. Defaults to openlands-release-publication-manifest.template.json.
  --url-map <path>        URL map path. Defaults to openlands-publication-url-map.template.json.
  --require-urls          Fail when any artifact downloadUrl is empty.
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
