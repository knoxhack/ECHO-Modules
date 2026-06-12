import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
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
    reportName: 'native-launcher-flow-report.json',
    attachmentPrefix: 'native',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-launcher-flow-report.json',
    attachmentPrefix: 'neoforge',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-launcher-flow-report.json',
    attachmentPrefix: 'standalone',
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

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
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

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function normalizeReleaseRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function sameList(actual, expected) {
  return JSON.stringify([...(actual ?? [])].sort()) === JSON.stringify([...(expected ?? [])].sort())
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function editionMatrixEntry(launcherFlow, edition) {
  return (launcherFlow.editionMatrix ?? []).find((entry) => entry.runtimeTarget === edition.runtimeTarget)
}

function artifactVerificationEntry(launcherFlow, edition) {
  return (launcherFlow.artifactVerification?.requiredBeforePublicAlpha ?? [])
    .find((entry) => entry.file === edition.artifactName)
}

function moduleReleaseArtifact(releaseManifest, edition) {
  const module = (releaseManifest.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(module, `release manifest missing ${MODULE_ID} ${VERSION}`)
  return (module.artifacts ?? []).find((entry) => entry.filename === edition.artifactName)
}

function inspectArtifact({ artifactPath, edition }) {
  const packageEntries = jarEntries(artifactPath)
  const requiredPackageEntries = ['META-INF/echo.mod.json']
  if (edition.artifactKind === 'neoforge') requiredPackageEntries.push('META-INF/neoforge.mods.toml')
  const nestedRuntimeEntry = packageEntries.find((entry) => /^lib\/.*-runtime\.jar$/.test(entry)) ?? null
  const checkedEntries = [...requiredPackageEntries]
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json',
    'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
    'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json',
    'data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json',
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
  ]

  for (const entry of requiredPackageEntries) {
    assert(packageEntries.includes(entry), `${edition.artifactName} missing ${entry}`)
  }

  if (edition.artifactKind === 'echo-addon') {
    assert(packageEntries.includes('echo-addon-package.json'), `${edition.artifactName} missing echo-addon-package.json`)
    assert(nestedRuntimeEntry, `${edition.artifactName} missing nested runtime jar`)
    checkedEntries.push('echo-addon-package.json', nestedRuntimeEntry)
    const packageRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-launcher-flow-package-'))
    const extract = run('jar', ['xf', artifactPath], packageRoot)
    if (extract.status !== 0) {
      throw new Error(`jar xf failed for ${artifactPath}: ${extract.stderr || extract.stdout}`)
    }
    const nestedRuntimePath = path.join(packageRoot, nestedRuntimeEntry)
    const nestedRuntimeEntries = jarEntries(nestedRuntimePath)
    for (const entry of runtimeEntriesChecked) {
      assert(nestedRuntimeEntries.includes(entry), `${edition.artifactName} nested runtime missing ${entry}`)
    }
  } else {
    for (const entry of runtimeEntriesChecked) {
      assert(packageEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
    }
  }

  return {
    packageEntriesChecked: checkedEntries,
    runtimeEntriesChecked,
    nestedRuntimeEntry,
  }
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const editionManifest = readJson(path.join(editionRoot, 'release-manifest.template.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))
  const releaseManifestPath = path.join(releaseRoot, 'echo-release.json')
  const releaseManifest = readJson(releaseManifestPath)

  const matrix = editionMatrixEntry(launcherFlow, edition)
  assert(matrix, `launcher flow matrix missing ${edition.runtimeTarget}`)
  assert(matrix.packId === edition.packId, `${edition.packId} matrix packId mismatch`)
  assert(matrix.artifactPattern === edition.artifactName, `${edition.packId} matrix artifact pattern mismatch`)
  assert(matrix.editionRepo === path.basename(editionRoot), `${edition.packId} matrix edition repo mismatch`)
  assert(editionManifest.packId === edition.packId, `${edition.packId} release manifest packId mismatch`)
  assert(editionManifest.runtimeTarget === edition.runtimeTarget, `${edition.packId} release manifest runtimeTarget mismatch`)
  assert(editionManifest.moduleArtifactFamily === edition.artifactKind, `${edition.packId} artifact family mismatch`)
  assert(editionManifest.requiredRuntimeEvidenceContract === 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json', `${edition.packId} runtime evidence contract mismatch`)
  assert(evidenceTemplate.launcherFlowContract === 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json', `${edition.packId} evidence template launcher contract mismatch`)

  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  assert(publicAlphaGate, 'runtime adapter load plan missing public_alpha gate')
  assert(sameList(editionManifest.requiredAdapterLoadPhases, publicAlphaGate.requiresPhases), `${edition.packId} adapter phases do not match public alpha gate`)
  assert(sameList(editionManifest.requiredPublicAlphaEvidence, publicAlphaGate.requiresEvidence), `${edition.packId} public alpha evidence does not match public alpha gate`)

  const artifactRecord = moduleReleaseArtifact(releaseManifest, edition)
  assert(artifactRecord, `release manifest missing artifact ${edition.artifactName}`)
  assert(artifactRecord.buildMode === 'compiled-runtime', `${edition.artifactName} must be compiled-runtime`)
  assert(normalizeReleaseRuntimeTarget(artifactRecord.runtimeTarget) === edition.runtimeTarget, `${edition.artifactName} runtime target mismatch`)

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifactSize = fs.statSync(artifactPath).size
  const artifactSha256 = sha256(artifactPath)
  assert(artifactRecord.size === artifactSize, `${edition.artifactName} size mismatch`)
  assert(artifactRecord.sha256 === artifactSha256, `${edition.artifactName} sha256 mismatch`)

  const artifactIndex = artifactVerificationEntry(launcherFlow, edition)
  assert(artifactIndex, `launcher flow artifact verification missing ${edition.artifactName}`)
  for (const field of artifactIndex.mustHaveIndexFields ?? []) {
    if (field === 'url') continue
    if (field === 'moduleId') assert(MODULE_ID, `${edition.artifactName} missing moduleId`)
    else if (field === 'version') assert(VERSION, `${edition.artifactName} missing version`)
    else if (field === 'runtimeTarget') assert(edition.runtimeTarget, `${edition.artifactName} missing runtimeTarget`)
    else assert(artifactRecord[field] !== undefined && artifactRecord[field] !== null && artifactRecord[field] !== '', `${edition.artifactName} missing index field ${field}`)
  }

  const artifactInspection = inspectArtifact({ artifactPath, edition })
  const flowResults = (launcherFlow.requiredLauncherFlows ?? [])
    .filter((flow) => (flow.appliesTo ?? []).includes(edition.runtimeTarget))
    .map((flow) => {
      assert(Array.isArray(flow.mustVerify) && flow.mustVerify.length > 0, `${edition.packId} flow ${flow.id} missing mustVerify`)
      assert(Array.isArray(flow.additionalAssertions) && flow.additionalAssertions.length > 0, `${edition.packId} flow ${flow.id} missing additionalAssertions`)
      assert(flow.worldStatePolicy, `${edition.packId} flow ${flow.id} missing worldStatePolicy`)
      return {
        id: flow.id,
        displayName: flow.displayName,
        status: 'preflight_mapped',
        preconditions: flow.preconditions,
        mustVerify: flow.mustVerify,
        additionalAssertions: flow.additionalAssertions,
        worldStatePolicy: flow.worldStatePolicy,
        evidenceAttachment: flow.evidenceAttachment,
        realLauncherExecutionRequiredBeforePublicAlpha: true,
      }
    })
  const requiredFlowIds = (launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id)
  assert(sameList(flowResults.map((flow) => flow.id), requiredFlowIds), `${edition.packId} launcher flows mismatch`)
  assert(sameList(evidenceTemplate.launcherFlows, requiredFlowIds), `${edition.packId} evidence template launcher flows mismatch`)

  const releaseIndexApproved = Boolean(artifactRecord.downloadUrl)
  const publicAlphaReady = false
  const blockedBy = [
    ...(releaseIndexApproved ? [] : ['release_artifact_download_url_missing']),
    'real_launcher_install_update_repair_rollback_execution_missing',
  ]

  const report = {
    schema: 'echo.openlands.edition.launcher_flow_report.v1',
    status: 'preflight_passed',
    publicAlphaReady,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseManifest: releaseManifestPath,
    releaseId: releaseManifest.releaseId,
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      size: artifactSize,
      sha256: artifactSha256,
      buildMode: artifactRecord.buildMode,
      downloadUrlPresent: Boolean(artifactRecord.downloadUrl),
      packageEntriesChecked: artifactInspection.packageEntriesChecked,
      runtimeEntriesChecked: artifactInspection.runtimeEntriesChecked,
      nestedRuntimeEntry: artifactInspection.nestedRuntimeEntry,
    },
    launcherFlowContract: 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json',
    distributionContract: 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json',
    releaseIndexStateAllowed: launcherFlow.artifactVerification?.currentIndexStateAllowed ?? distribution.releaseIndexStates?.currentAllowedState,
    requiredPublicAlphaEvidence: editionManifest.requiredPublicAlphaEvidence,
    flowResults,
    statePreservation: launcherFlow.statePreservation,
    blockedBy,
    outputPath,
    proofs: [
      'launcher_flow_contract_loaded',
      'edition_matrix_matches_manifest',
      'release_manifest_template_loaded',
      'compiled_runtime_artifact_present',
      'artifact_sha256_matches_release_manifest',
      'artifact_size_matches_release_manifest',
      'required_descriptors_present',
      'install_update_repair_rollback_flows_mapped',
      'state_preservation_fields_mapped',
      'public_alpha_blocked_until_real_launcher_execution',
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
    console.log(`Openlands ${args.edition} launcher flow report ${action}: ${report.flowResults.length} flows.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-launcher-flow-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-launcher-flow-report.json.
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
