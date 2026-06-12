import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-adapter-boot-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-adapter-boot-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-adapter-boot-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    releaseRoot: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-root') args.moduleRoot = argv[++index]
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

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    shell: false,
  })
  return {
    status: result.status,
    stdout: result.stdout?.trim() ?? '',
    stderr: result.stderr?.trim() ?? '',
  }
}

function jarEntries(artifactPath) {
  const result = run('jar', ['tf', artifactPath], path.dirname(artifactPath))
  if (result.status !== 0) {
    throw new Error(`jar tf failed for ${artifactPath}: ${result.stderr || result.stdout}`)
  }
  return result.stdout.split(/\r?\n/).filter(Boolean)
}

function extractJar(artifactPath, entryNames) {
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-adapter-boot-'))
  const extract = run('jar', ['xf', artifactPath, ...entryNames], extractRoot)
  if (extract.status !== 0) {
    throw new Error(`jar xf failed for ${artifactPath}: ${extract.stderr || extract.stdout}`)
  }
  return extractRoot
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

function sameList(actual, expected) {
  return JSON.stringify(actual ?? []) === JSON.stringify(expected ?? [])
}

function artifactRuntimeEntries(artifactPath, edition) {
  const packageEntries = jarEntries(artifactPath)
  if (edition.artifactKind !== 'echo-addon') {
    return { packageEntries, runtimeEntries: packageEntries, nestedRuntimeEntry: null }
  }
  const nestedRuntimeEntry = packageEntries.find((entry) => /^lib\/.*-runtime\.jar$/.test(entry))
  assert(nestedRuntimeEntry, `${edition.artifactName} missing nested runtime jar`)
  const extractRoot = extractJar(artifactPath, [nestedRuntimeEntry])
  const runtimeEntries = jarEntries(path.join(extractRoot, nestedRuntimeEntry))
  return { packageEntries, runtimeEntries, nestedRuntimeEntry }
}

function resourcePathForId(resourceId) {
  if (resourceId === 'META-INF/echo.mod.json') return 'META-INF/echo.mod.json'
  if (resourceId === 'assets/sounds.json') return `assets/${MODULE_ID}/sounds.json`
  return `data/${MODULE_ID}/openlands/${resourceId}.json`
}

function sourcePathForResource(resourcesRoot, resourceId) {
  if (resourceId === 'META-INF/echo.mod.json') return path.join(resourcesRoot, 'META-INF', 'echo.mod.json')
  if (resourceId === 'assets/sounds.json') return path.join(resourcesRoot, 'assets', MODULE_ID, 'sounds.json')
  return path.join(resourcesRoot, 'data', MODULE_ID, 'openlands', `${resourceId}.json`)
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const descriptor = readJson(path.join(resourcesRoot, 'META-INF', 'echo.mod.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))
  const releaseManifest = readJson(path.join(releaseRoot, 'echo-release.json'))

  assert(descriptor.id === MODULE_ID, 'descriptor id mismatch')
  assert(descriptor.version === VERSION, 'descriptor version mismatch')
  assert(descriptor.official === true, 'descriptor must be official')
  assert(descriptor.kind === 'pack_root', 'descriptor kind must be pack_root')
  assert(descriptor.role === 'official_pack', 'descriptor role must be official_pack')
  assert(descriptor.access?.adapterCore?.runtimes?.includes(edition.runtimeTarget), `${edition.runtimeTarget} missing from descriptor adapter runtimes`)
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  const phaseIds = (runtimePlan.phases ?? []).map((phase) => phase.id)
  assert(sameList(phaseIds, evidenceTemplate.requiredAdapterLoadPhases), `${edition.packId} adapter phase list mismatch`)
  const sortedPhases = [...(runtimePlan.phases ?? [])].sort((left, right) => left.order - right.order)
  assert(sameList(sortedPhases.map((phase) => phase.id), phaseIds), 'adapter phases must be ordered by ascending order value')

  const evidenceIds = new Set((runtimePlan.runtimeEvidenceRequirements ?? []).map((entry) => entry.id))
  for (const evidence of evidenceTemplate.requiredRuntimeEvidenceIds ?? []) {
    assert(evidenceIds.has(evidence), `${edition.packId} evidence template references unknown evidence ${evidence}`)
  }

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const releaseModule = (releaseManifest.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const releaseArtifact = (releaseModule?.artifacts ?? []).find((entry) => entry.filename === edition.artifactName)
  assert(releaseArtifact?.buildMode === 'compiled-runtime', `${edition.artifactName} must be compiled-runtime`)

  const loadStepSummaries = []
  const artifactEntriesChecked = new Set([
    'META-INF/echo.mod.json',
    `data/${MODULE_ID}/openlands/systems/runtime_adapter_load_plan.json`,
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
  ])
  for (const entry of artifactEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  for (const step of runtimePlan.loadSteps ?? []) {
    assert(phaseIds.includes(step.phase), `load step ${step.id} references unknown phase ${step.phase}`)
    assert(step.runtimeTargets?.includes(edition.runtimeTarget), `load step ${step.id} does not include runtime ${edition.runtimeTarget}`)
    assert(Array.isArray(step.requiredEvidence) && step.requiredEvidence.length > 0, `load step ${step.id} missing required evidence`)
    for (const evidence of step.requiredEvidence ?? []) {
      assert(evidenceIds.has(evidence), `load step ${step.id} references unknown evidence ${evidence}`)
    }
    const resourceEntries = []
    for (const resourceId of step.resourceIds ?? []) {
      const sourcePath = sourcePathForResource(resourcesRoot, resourceId)
      assert(fileExists(sourcePath), `load step ${step.id} missing source resource ${resourceId}`)
      const runtimeEntry = resourcePathForId(resourceId)
      assert(artifact.runtimeEntries.includes(runtimeEntry), `${edition.artifactName} missing runtime resource ${runtimeEntry}`)
      artifactEntriesChecked.add(runtimeEntry)
      resourceEntries.push(runtimeEntry)
    }
    loadStepSummaries.push({
      id: step.id,
      phase: step.phase,
      successSignal: step.successSignal,
      resources: resourceEntries.length,
      requiredEvidence: step.requiredEvidence.length,
    })
  }

  const report = {
    schema: 'echo.openlands.edition.adapter_boot_report.v1',
    status: 'preflight_passed',
    realAdapterBootRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    runtimeEvidenceContract: 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      entriesChecked: [...artifactEntriesChecked].sort(),
    },
    descriptor: {
      id: descriptor.id,
      version: descriptor.version,
      kind: descriptor.kind,
      role: descriptor.role,
      official: descriptor.official,
      runtimes: descriptor.access.adapterCore.runtimes,
    },
    phases: runtimePlan.phases.map((phase) => ({
      id: phase.id,
      order: phase.order,
      gate: phase.gate,
    })),
    loadStepSummaries,
    runtimeEvidenceIds: [...evidenceIds],
    requiredPublicAlphaEvidence: evidenceTemplate.requiredPublicAlphaEvidence,
    adapterReadySignal: evidenceTemplate.adapterReadySignal,
    blockedBy: [
      'real_runtime_adapter_boot_missing',
      'runtime_smoke_test_missing',
      'registry_parity_execution_missing',
    ],
    outputPath,
    proofs: [
      'adapter_load_plan_loaded',
      'descriptor_identity_verified',
      'runtime_target_accepted',
      'adapter_phases_match_evidence_template',
      'load_step_resources_exist_in_source',
      'compiled_artifact_contains_load_step_resources',
      'load_step_evidence_ids_resolve',
      'adapter_ready_signal_declared',
      'compiled_artifact_contains_runtime_contracts',
      'public_alpha_blocked_until_real_adapter_boot',
    ],
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
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildReport({
    editionKey: args.edition,
    editionRoot,
    moduleRoot,
    releaseRoot,
    outputPath,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} adapter boot report ${action}: ${report.loadStepSummaries.length} load steps.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-adapter-boot-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-adapter-boot-report.json.
  --dry-run               Validate without writing the report.
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
