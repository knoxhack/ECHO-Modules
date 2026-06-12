import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const URL_MAP_SCHEMA = 'echo.openlands.release_publication_url_map_template.v1'

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

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function buildUrlMapTemplate({ manifestPath, outputPath, dryRun }) {
  assert(fileExists(manifestPath), `publication manifest not found: ${manifestPath}`)
  const manifest = readJson(manifestPath)
  assert(manifest.schema === 'echo.openlands.release_publication_manifest.v1', 'publication manifest schema mismatch')
  assert(manifest.moduleId === MODULE_ID, 'publication manifest module id mismatch')
  assert(manifest.moduleVersion === VERSION, 'publication manifest module version mismatch')

  const artifactUrls = (manifest.artifactPublications ?? []).map((artifact) => ({
    id: artifact.id,
    file: artifact.file,
    kind: artifact.kind,
    runtimeTarget: artifact.runtimeTarget,
    requiredForPublicAlpha: artifact.requiredForPublicAlpha === true,
    expectedSha256: artifact.sha256,
    expectedSize: artifact.size,
    downloadUrl: artifact.downloadUrl ?? '',
    uploadProvider: artifact.uploadProvider ?? '',
    storageKey: artifact.storageKey ?? '',
    publishedAt: artifact.publishedAt ?? null,
  }))
  assert(artifactUrls.length > 0, 'publication manifest must include artifact publications')

  return {
    schema: URL_MAP_SCHEMA,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: manifest.releaseId,
    sourceManifestPath: manifestPath,
    publicHttpsRequired: true,
    artifactUrls,
    verifierCommand: [
      'node',
      'addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs',
      '--module-root',
      'addons/echoopenlandsprotocol',
      '--url-map',
      outputPath,
    ],
    outputPath,
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
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-publication-url-map.template.json')
  const template = buildUrlMapTemplate({ manifestPath, outputPath, dryRun: args.dryRun })
  if (!args.dryRun) writeJson(outputPath, template)
  if (args.json) {
    console.log(JSON.stringify(template, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands publication URL map template ${action}: ${template.artifactUrls.length} artifacts.`)
  }
  return template
}

function printHelp() {
  console.log(`Usage: node generate-openlands-publication-url-map-template.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --manifest <path>       Publication manifest template path. Defaults to openlands-release-publication-manifest.template.json.
  --out <path>            URL map output path. Defaults to openlands-publication-url-map.template.json.
  --dry-run               Generate without writing.
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
