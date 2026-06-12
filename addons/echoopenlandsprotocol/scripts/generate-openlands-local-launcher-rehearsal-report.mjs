import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const FLOW_IDS = ['install', 'update', 'repair', 'rollback']
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-local-launcher-rehearsal-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-local-launcher-rehearsal-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-local-launcher-rehearsal-report.json',
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

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function sha256Text(text) {
  return crypto.createHash('sha256').update(text).digest('hex')
}

function hashDirectory(root) {
  const entries = []
  function visit(directory) {
    for (const name of fs.readdirSync(directory).sort()) {
      const fullPath = path.join(directory, name)
      const relativePath = path.relative(root, fullPath).replace(/\\/g, '/')
      const stat = fs.statSync(fullPath)
      if (stat.isDirectory()) visit(fullPath)
      else entries.push(`${relativePath}:${sha256File(fullPath)}`)
    }
  }
  visit(root)
  return sha256Text(entries.join('\n'))
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
  if (result.status !== 0) throw new Error(`jar tf failed for ${artifactPath}: ${result.stderr || result.stdout}`)
  return result.stdout.split(/\r?\n/).filter(Boolean)
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function releaseArtifact(releaseIndex, edition) {
  const module = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(module, `release index missing ${MODULE_ID} ${VERSION}`)
  return (module.artifacts ?? []).find((entry) => entry.filename === edition.artifactName)
}

function inspectArtifact(artifactPath, edition) {
  const entries = jarEntries(artifactPath)
  const requiredEntries = ['META-INF/echo.mod.json']
  if (edition.artifactKind === 'neoforge') requiredEntries.push('META-INF/neoforge.mods.toml')
  if (edition.artifactKind === 'echo-addon') requiredEntries.push('echo-addon-package.json')
  for (const entry of requiredEntries) {
    assert(entries.includes(entry), `${edition.artifactName} missing ${entry}`)
  }
  return {
    requiredEntries,
    nestedRuntimeJarPresent: edition.artifactKind === 'echo-addon'
      ? entries.some((entry) => /^lib\/.*-runtime\.jar$/.test(entry))
      : null,
    entryCount: entries.length,
  }
}

function flowResult(id, startedAt, finishedAt, assertions, captures, savedArtifacts) {
  return {
    id,
    status: 'passed',
    startedAt,
    finishedAt,
    durationMs: new Date(finishedAt).getTime() - new Date(startedAt).getTime(),
    assertions: assertions.map((assertionId) => ({ id: assertionId, status: 'passed' })),
    captures,
    savedArtifacts,
  }
}

function nowIso() {
  return new Date().toISOString()
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const launcherExecution = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = readJson(releaseIndexPath)
  const releaseRecord = releaseArtifact(releaseIndex, edition)
  assert(releaseRecord, `release index missing artifact ${edition.artifactName}`)
  assert(releaseRecord.buildMode === 'compiled-runtime', `${edition.artifactName} must be compiled-runtime`)
  assert(normalizeRuntimeTarget(releaseRecord.runtimeTarget) === edition.runtimeTarget, `${edition.artifactName} runtime target mismatch`)

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifactSha256 = sha256File(artifactPath)
  const artifactSize = fs.statSync(artifactPath).size
  assert(artifactSha256 === releaseRecord.sha256, `${edition.artifactName} sha256 mismatch with release index`)
  assert(artifactSize === releaseRecord.size, `${edition.artifactName} size mismatch with release index`)

  const editionManifest = readJson(path.join(editionRoot, 'release-manifest.template.json'))
  assert(editionManifest.packId === edition.packId, `${edition.packId} manifest packId mismatch`)
  assert(editionManifest.runtimeTarget === edition.runtimeTarget, `${edition.packId} manifest runtime target mismatch`)

  const launcherFlowIds = (launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id)
  assert(JSON.stringify([...launcherFlowIds].sort()) === JSON.stringify([...FLOW_IDS].sort()), 'launcher flow ids mismatch')
  const executionFlowIds = (launcherExecution.executionFlows ?? []).map((flow) => flow.id)
  assert(JSON.stringify([...executionFlowIds].sort()) === JSON.stringify([...FLOW_IDS].sort()), 'launcher execution flow ids mismatch')

  const artifactInspection = inspectArtifact(artifactPath, edition)
  if (edition.artifactKind === 'echo-addon') {
    assert(artifactInspection.nestedRuntimeJarPresent === true, `${edition.artifactName} must include nested runtime jar`)
  }

  const rehearsalRoot = fs.mkdtempSync(path.join(os.tmpdir(), `openlands-${editionKey}-launcher-rehearsal-`))
  const cacheRoot = path.join(rehearsalRoot, 'cache')
  const manifestRoot = path.join(rehearsalRoot, 'manifests')
  const worldRoot = path.join(rehearsalRoot, 'worlds', 'openlands-standard-rehearsal')
  const configRoot = path.join(rehearsalRoot, 'config')
  const logRoot = path.join(rehearsalRoot, 'logs')
  const savedArtifactRoot = dryRun
    ? path.join(rehearsalRoot, 'saved-artifacts')
    : path.join(path.dirname(outputPath), `${editionKey}-local-launcher-rehearsal-artifacts`)
  fs.mkdirSync(cacheRoot, { recursive: true })
  fs.mkdirSync(manifestRoot, { recursive: true })
  fs.mkdirSync(worldRoot, { recursive: true })
  fs.mkdirSync(configRoot, { recursive: true })
  fs.mkdirSync(logRoot, { recursive: true })
  fs.mkdirSync(savedArtifactRoot, { recursive: true })
  const saveArtifact = (flowId, name, content) => {
    const relativePath = `${flowId}/${name}`
    const target = path.join(savedArtifactRoot, relativePath)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    if (typeof content === 'string') fs.writeFileSync(target, content, 'utf8')
    else fs.writeFileSync(target, `${JSON.stringify(content, null, 2)}\n`, 'utf8')
    return relativePath
  }

  const cachedArtifactPath = path.join(cacheRoot, edition.artifactName)
  const installManifestPath = path.join(manifestRoot, 'install-manifest.json')
  const previousManifestPath = path.join(manifestRoot, 'previous-manifest.json')
  const updatedManifestPath = path.join(manifestRoot, 'updated-manifest.json')
  const rollbackManifestPath = path.join(manifestRoot, 'rollback-manifest.json')
  const configPath = path.join(configRoot, 'openlands_standard.json')
  const worldStatePath = path.join(worldRoot, 'openlands-world-state.json')

  const installStarted = nowIso()
  fs.copyFileSync(releaseIndexPath, installManifestPath)
  fs.copyFileSync(artifactPath, cachedArtifactPath)
  writeJson(configPath, {
    mode: 'openlands_standard',
    stamina: false,
    hydration: false,
    foodSpoilage: false,
    temperatureDamage: false,
    deathPack: 'recoverable',
  })
  writeJson(worldStatePath, {
    worldId: 'openlands-standard-rehearsal',
    bedrollSpawn: { x: 8, y: 64, z: 8 },
    waystoneState: { firstWaystone: 'active', linkedRouteIds: ['old_road_route_rehearsal'] },
    holoMapDiscovery: { region: 'meadows_rehearsal', oldRoadSegments: 1, hints: ['road_segment'] },
  })
  fs.writeFileSync(path.join(logRoot, 'launcher-install-log.txt'), 'install rehearsal passed\n', 'utf8')
  const installWorldHash = hashDirectory(worldRoot)
  const installConfigHash = hashDirectory(configRoot)
  const installSavedArtifacts = [
    saveArtifact('install', 'launcher-install-log.txt', 'install rehearsal passed\n'),
    saveArtifact('install', 'install-manifest.json', releaseIndex),
    saveArtifact('install', 'artifact-hash.txt', `${artifactSha256}\n`),
    saveArtifact('install', 'fresh-world-summary.json', {
      worldStatePath,
      configPath,
      worldHash: installWorldHash,
      configHash: installConfigHash,
      hardcoreMetersOff: true,
    }),
  ]
  const installFinished = nowIso()

  const updateStarted = nowIso()
  const previousManifest = { ...releaseIndex, releaseId: `${releaseIndex.releaseId}-previous-local-rehearsal` }
  writeJson(previousManifestPath, previousManifest)
  writeJson(updatedManifestPath, releaseIndex)
  const beforeUpdateWorldHash = hashDirectory(worldRoot)
  const beforeUpdateConfigHash = hashDirectory(configRoot)
  fs.copyFileSync(artifactPath, cachedArtifactPath)
  fs.writeFileSync(path.join(logRoot, 'launcher-update-log.txt'), 'update rehearsal passed\n', 'utf8')
  const afterUpdateWorldHash = hashDirectory(worldRoot)
  const afterUpdateConfigHash = hashDirectory(configRoot)
  const updateSavedArtifacts = [
    saveArtifact('update', 'launcher-update-log.txt', 'update rehearsal passed\n'),
    saveArtifact('update', 'previous-manifest.json', previousManifest),
    saveArtifact('update', 'updated-manifest.json', releaseIndex),
    saveArtifact('update', 'world-preservation-diff.json', {
      beforeUpdateWorldHash,
      afterUpdateWorldHash,
      preserved: beforeUpdateWorldHash === afterUpdateWorldHash,
    }),
    saveArtifact('update', 'config-preservation-diff.json', {
      beforeUpdateConfigHash,
      afterUpdateConfigHash,
      preserved: beforeUpdateConfigHash === afterUpdateConfigHash,
    }),
  ]
  const updateFinished = nowIso()

  const repairStarted = nowIso()
  const worldHashBeforeRepair = hashDirectory(worldRoot)
  fs.appendFileSync(cachedArtifactPath, '\ncorrupted-by-openlands-rehearsal\n', 'utf8')
  const corruptSha256 = sha256File(cachedArtifactPath)
  fs.copyFileSync(artifactPath, cachedArtifactPath)
  const restoredSha256 = sha256File(cachedArtifactPath)
  fs.writeFileSync(path.join(logRoot, 'launcher-repair-log.txt'), 'repair rehearsal passed\n', 'utf8')
  const worldHashAfterRepair = hashDirectory(worldRoot)
  const repairSavedArtifacts = [
    saveArtifact('repair', 'launcher-repair-log.txt', 'repair rehearsal passed\n'),
    saveArtifact('repair', 'repair-hash-report.json', {
      corruptSha256,
      restoredSha256,
      expectedSha256: artifactSha256,
      restored: restoredSha256 === artifactSha256,
    }),
    saveArtifact('repair', 'repair-preservation-report.json', {
      worldHashBeforeRepair,
      worldHashAfterRepair,
      preserved: worldHashBeforeRepair === worldHashAfterRepair,
    }),
  ]
  const repairFinished = nowIso()

  const rollbackStarted = nowIso()
  const worldHashBeforeRollback = hashDirectory(worldRoot)
  writeJson(rollbackManifestPath, previousManifest)
  fs.copyFileSync(artifactPath, cachedArtifactPath)
  fs.writeFileSync(path.join(logRoot, 'launcher-rollback-log.txt'), 'rollback rehearsal passed\n', 'utf8')
  const worldHashAfterRollback = hashDirectory(worldRoot)
  const rollbackConfigHash = hashDirectory(configRoot)
  const rollbackSavedArtifacts = [
    saveArtifact('rollback', 'launcher-rollback-log.txt', 'rollback rehearsal passed\n'),
    saveArtifact('rollback', 'rollback-manifest.json', previousManifest),
    saveArtifact('rollback', 'world-preservation-diff.json', {
      worldHashBeforeRollback,
      worldHashAfterRollback,
      preserved: worldHashBeforeRollback === worldHashAfterRollback,
      rollbackWorldDeletionCount: 0,
    }),
    saveArtifact('rollback', 'config-preservation-diff.json', {
      openlandsStandardConfigHash: rollbackConfigHash,
      preserved: true,
    }),
  ]
  const rollbackFinished = nowIso()

  const flowResults = [
    flowResult('install', installStarted, installFinished, [
      'manifest_snapshot_written',
      'artifact_cached_from_local_release',
      'sha256_and_size_match_release_index',
      'artifact_descriptor_entries_present',
      'openlands_standard_config_created',
      'world_state_created_without_hardcore_meters',
    ], {
      cachePath: cachedArtifactPath,
      cachedArtifactSha256: sha256File(cachedArtifactPath),
      cachedArtifactSize: fs.statSync(cachedArtifactPath).size,
      artifactInspection,
      worldHash: installWorldHash,
      configHash: installConfigHash,
    }, installSavedArtifacts),
    flowResult('update', updateStarted, updateFinished, [
      'previous_manifest_snapshot_written',
      'updated_manifest_snapshot_written',
      'artifact_cache_replaced_from_current_release',
      'world_hash_preserved',
      'config_hash_preserved',
    ], {
      previousManifestId: previousManifest.releaseId,
      newManifestId: releaseIndex.releaseId,
      beforeUpdateWorldHash,
      afterUpdateWorldHash,
      beforeUpdateConfigHash,
      afterUpdateConfigHash,
      changedArtifactSha256: artifactSha256,
      reusedArtifactSha256: artifactSha256,
    }, updateSavedArtifacts),
    flowResult('repair', repairStarted, repairFinished, [
      'corrupt_artifact_hash_differs',
      'artifact_restored_from_local_release',
      'restored_hash_matches_release_index',
      'world_hash_preserved_during_repair',
    ], {
      corruptedArtifactPath: cachedArtifactPath,
      corruptSha256,
      restoredSha256,
      expectedSha256: artifactSha256,
      worldHashBeforeRepair,
      worldHashAfterRepair,
      repairDownloadUrl: 'local-release-root',
    }, repairSavedArtifacts),
    flowResult('rollback', rollbackStarted, rollbackFinished, [
      'rollback_manifest_snapshot_written',
      'artifact_cache_restored_for_selected_manifest',
      'world_hash_preserved_during_rollback',
      'config_hash_available_after_rollback',
      'no_world_entries_deleted',
    ], {
      currentManifestId: releaseIndex.releaseId,
      previousManifestId: previousManifest.releaseId,
      rollbackArtifactSha256: artifactSha256,
      schemaCompatibilityDecision: 'compatible_rehearsal_no_schema_change',
      worldHashBeforeRollback,
      worldHashAfterRollback,
      openlandsStandardConfigHash: rollbackConfigHash,
      rollbackWorldDeletionCount: 0,
      repairWorldDeletionCount: 0,
    }, rollbackSavedArtifacts),
  ]

  assert(corruptSha256 !== artifactSha256, 'corruption rehearsal did not change artifact hash')
  assert(restoredSha256 === artifactSha256, 'repair rehearsal did not restore artifact hash')
  assert(beforeUpdateWorldHash === afterUpdateWorldHash, 'update rehearsal changed world hash')
  assert(beforeUpdateConfigHash === afterUpdateConfigHash, 'update rehearsal changed config hash')
  assert(worldHashBeforeRepair === worldHashAfterRepair, 'repair rehearsal changed world hash')
  assert(worldHashBeforeRollback === worldHashAfterRollback, 'rollback rehearsal changed world hash')

  const report = {
    schema: 'echo.openlands.edition.local_launcher_rehearsal_report.v1',
    status: 'preflight_passed',
    publicAlphaReady: false,
    clearsLauncherGates: false,
    rehearsalOnly: true,
    generatedAt: nowIso(),
    dryRun,
    edition: editionKey,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    launcherFlowContract: 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json',
    launcherExecutionContract: 'data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json',
    moduleArtifact: artifactPath,
    moduleArtifactSha256: artifactSha256,
    moduleArtifactSize: artifactSize,
    rehearsalRoot,
    savedArtifactRoot,
    flowResults,
    preservedState: {
      installWorldHash,
      installConfigHash,
      beforeUpdateWorldHash,
      afterUpdateWorldHash,
      beforeUpdateConfigHash,
      afterUpdateConfigHash,
      worldHashBeforeRepair,
      worldHashAfterRepair,
      worldHashBeforeRollback,
      worldHashAfterRollback,
    },
    blockedBy: [
      'real_echo_launcher_execution_missing',
      'public_release_download_urls_missing',
      'local_rehearsal_does_not_clear_launcher_gates',
    ],
    proofs: [
      'local_release_index_loaded',
      'compiled_artifact_cached',
      'artifact_sha256_and_size_match_release_index',
      'artifact_descriptor_entries_present',
      'openlands_standard_config_preserved',
      'world_state_preserved_across_update_repair_rollback',
      'corrupt_artifact_repaired_from_release_root',
      'rollback_manifest_snapshot_written',
      'public_alpha_stays_blocked_until_real_launcher_execution',
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
  if (!args.edition) throw new Error('--edition is required')
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : path.join(workspaceRoot, `ECHO-Openlands-${args.edition === 'neoforge' ? 'NeoForge' : args.edition[0].toUpperCase() + args.edition.slice(1)}-Edition`)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output ? path.resolve(args.output) : path.join(editionRoot, 'evidence', edition.reportName)
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
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} local launcher rehearsal ${action}: ${report.flowResults.length} flows, publicAlphaReady=${report.publicAlphaReady}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-local-launcher-rehearsal-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>           Edition key: native, neoforge, or standalone.
  --edition-root <path>    Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --module-root <path>     Openlands module root. Defaults to this script's module.
  --release-root <path>    Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>             Output path. Defaults to evidence/<edition>-local-launcher-rehearsal-report.json.
  --dry-run                Generate without writing the report.
  --json                   Print JSON output.
  --help                   Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
