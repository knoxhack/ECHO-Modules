import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json'
const EDITIONS = new Map([
  ['native', { runtimeTarget: 'echo_native', repo: 'ECHO-Openlands-Native-Edition' }],
  ['neoforge', { runtimeTarget: 'neoforge', repo: 'ECHO-Openlands-NeoForge-Edition' }],
  ['standalone', { runtimeTarget: 'echo_runtime_standalone', repo: 'ECHO-Openlands-Standalone-Edition' }],
])

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    edition: null,
    editionRoot: null,
    implementedDrivers: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--implemented-drivers') args.implementedDrivers = argv[++index]
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

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function loadPlan(moduleRoot, planPath) {
  return readJson(path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', planPath))
}

function loadImplementedDrivers(filePath) {
  if (!filePath) return []
  const payload = readJson(path.resolve(filePath))
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.availableDriverSurfaces)) return payload.availableDriverSurfaces
  if (Array.isArray(payload.drivers)) return payload.drivers
  throw new Error(`Implemented driver file must be an array or contain availableDriverSurfaces/drivers: ${filePath}`)
}

function driverSurfaceIndex(moduleRoot, contract) {
  const result = new Map()
  for (const family of contract.harnessFamilies ?? []) {
    const plan = loadPlan(moduleRoot, family.plan)
    for (const driver of plan.driverSurfaces ?? []) {
      result.set(`${family.id}:${driver.id}`, {
        familyId: family.id,
        driver,
      })
    }
  }
  return result
}

function normalizeImplementedDriver(raw, surfaceIndex) {
  const id = raw.id ?? raw.driverSurfaceId
  const harnessFamily = raw.harnessFamily ?? raw.family
  if (!id || !harnessFamily) throw new Error('Implemented driver entries require id and harnessFamily')
  const surface = surfaceIndex.get(`${harnessFamily}:${id}`)
  if (!surface) throw new Error(`Implemented driver ${harnessFamily}:${id} is not required by any harness plan`)
  return {
    id,
    harnessFamily,
    implementationState: raw.implementationState ?? 'implementation_ready',
    adapterClassOrEntrypoint: raw.adapterClassOrEntrypoint ?? raw.entryPoint ?? '',
    driverVersion: raw.driverVersion ?? '',
    adapterCommit: raw.adapterCommit ?? '',
    methodsImplemented: sortedUnique(raw.methodsImplemented ?? []),
    capturesImplemented: sortedUnique(raw.capturesImplemented ?? []),
    evidenceRoot: raw.evidenceRoot ?? '',
    notes: raw.notes ?? '',
  }
}

function buildManifest({ moduleRoot, workspaceRoot, edition, editionRoot, implementedDriversPath, outputPath, dryRun }) {
  const editionConfig = EDITIONS.get(edition)
  if (!editionConfig) throw new Error(`Unknown edition: ${edition}`)
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const template = (contract.editionManifestTemplates ?? []).find((entry) => entry.edition === edition)
  if (!template) throw new Error(`Harness driver manifest contract missing edition template: ${edition}`)
  if (template.runtimeTarget !== editionConfig.runtimeTarget) throw new Error(`Contract runtime target mismatch for ${edition}`)

  const surfaceIndex = driverSurfaceIndex(moduleRoot, contract)
  const availableDriverSurfaces = loadImplementedDrivers(implementedDriversPath)
    .map((driver) => normalizeImplementedDriver(driver, surfaceIndex))
    .sort((left, right) => `${left.harnessFamily}:${left.id}`.localeCompare(`${right.harnessFamily}:${right.id}`))
  const availableDriverKeys = availableDriverSurfaces.map((driver) => `${driver.harnessFamily}:${driver.id}`)
  if (availableDriverKeys.length !== sortedUnique(availableDriverKeys).length) {
    throw new Error('Implemented driver entries must be unique by harnessFamily and id')
  }
  const availableByFamily = new Map()
  for (const driver of availableDriverSurfaces) {
    if (!availableByFamily.has(driver.harnessFamily)) availableByFamily.set(driver.harnessFamily, [])
    availableByFamily.get(driver.harnessFamily).push(driver.id)
  }

  const harnessFamilies = (contract.harnessFamilies ?? []).map((family) => {
    const availableDriverSurfaceIds = sortedUnique(availableByFamily.get(family.id) ?? [])
    const missingDriverSurfaceIds = sortedUnique((family.requiredDriverSurfaceIds ?? []).filter((id) => !availableDriverSurfaceIds.includes(id)))
    const status = availableDriverSurfaceIds.length === 0
      ? 'template_blocked'
      : missingDriverSurfaceIds.length === 0
        ? 'implementation_ready'
        : 'implementation_partial'
    const blockedBy = []
    if (missingDriverSurfaceIds.length > 0) blockedBy.push(family.driverMissingBlocker)
    blockedBy.push('real_harness_execution_not_run')
    return {
      id: family.id,
      plan: family.plan,
      bindingKey: family.bindingKey,
      bindingLabel: family.bindingLabel,
      requiredReport: family.requiredReportPattern.replace('{edition}', edition),
      requiredDriverSurfaceIds: family.requiredDriverSurfaceIds,
      requiredBindingIds: family.requiredBindingIds,
      availableDriverSurfaceIds,
      missingDriverSurfaceIds,
      status,
      blockedBy,
    }
  })

  const missingDriverSurfaces = harnessFamilies.flatMap((family) => (family.missingDriverSurfaceIds ?? []).map((id) => ({
    harnessFamily: family.id,
    id,
  })))
  const blockedTemplateStatus = contract.blockedTemplateRules?.familyStatus ?? 'template_blocked'
  const topStatus = availableDriverSurfaces.length === 0
    ? blockedTemplateStatus
    : missingDriverSurfaces.length === 0
      ? 'implementation_ready'
      : 'implementation_partial'
  const blockedBy = topStatus === blockedTemplateStatus
    ? contract.blockedTemplateRules.requiredBlockedBy
    : sortedUnique([
        ...harnessFamilies.flatMap((family) => family.blockedBy ?? []),
      ])

  const manifest = {
    schema: contract.reportContract.schema,
    edition,
    runtimeTarget: editionConfig.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    generatedAt: new Date().toISOString(),
    status: topStatus,
    harnessDriverManifestContract: CONTRACT_PATH,
    sourceContracts: contract.sourceContracts,
    artifactPattern: template.artifactPattern,
    harnessFamilies,
    availableDriverSurfaces,
    missingDriverSurfaces,
    blockedBy,
    nextSteps: topStatus === 'implementation_ready'
      ? [
          'Run real harness execution for runtime, launcher, final review, and distribution approval.',
          'Attach required captures and saved artifacts from each harness plan.',
          'Replace blocked reports only after every mapped binding passes.',
        ]
      : contract.blockedTemplateRules.requiredNextSteps,
    notes: [
      topStatus === blockedTemplateStatus
        ? 'This template is intentionally blocked and generated from the source harness driver manifest contract.'
        : 'This manifest records implemented driver surfaces, but real harness execution is still required before gates can clear.',
      'Generated manifests must be validated before they are used by run-*harness.mjs.',
    ],
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
  if (!args.edition) throw new Error('--edition is required')
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const editionConfig = EDITIONS.get(args.edition)
  if (!editionConfig) throw new Error(`Unknown edition: ${args.edition}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : path.join(workspaceRoot, editionConfig.repo)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(editionRoot, 'evidence', `${args.edition}-harness-driver-manifest.template.json`)
  const manifest = buildManifest({
    moduleRoot,
    workspaceRoot,
    edition: args.edition,
    editionRoot,
    implementedDriversPath: args.implementedDrivers,
    outputPath,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(manifest, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} harness driver manifest ${action}: status=${manifest.status}, available=${manifest.availableDriverSurfaces.length}, missing=${manifest.missingDriverSurfaces.length}.`)
  }
  return manifest
}

function printHelp() {
  console.log(`Usage: node generate-openlands-harness-driver-manifest.mjs --edition <native|neoforge|standalone> [options]

Options:
  --module-root <path>          Openlands module root. Defaults to this script's module.
  --edition <id>                Edition key: native, neoforge, or standalone.
  --edition-root <path>         Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --implemented-drivers <path>  Optional JSON file with availableDriverSurfaces/drivers.
  --out <path>                  Output manifest path. Defaults to evidence/<edition>-harness-driver-manifest.template.json.
  --dry-run                     Generate without writing.
  --json                        Print JSON output.
  --help                        Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
