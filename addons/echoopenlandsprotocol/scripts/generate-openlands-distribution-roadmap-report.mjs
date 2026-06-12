import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RELEASE_ID = 'openlands-0.1.0-compiled'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const EXPECTED_ARTIFACT_TARGETS = ['native', 'standalone', 'neoforge', 'sources']
const EXPECTED_LAUNCHER_FLOWS = ['install', 'update', 'repair', 'rollback']
const EXPECTED_ROADMAP_PHASES = ['mvp', 'public_alpha', 'one_dot_zero', 'post_launch']
const EXPECTED_PUBLIC_ALPHA_EVIDENCE = [
  'openlands_contract_validator_pass',
  'native_standalone_neoforge_artifacts_uploaded_with_sha256',
  'launcher_install_update_repair_rollback_pass',
  'first_hour_runtime_playtest_pass',
  'waystone_state_save_load_pass',
  'legal_content_audit_pass',
]
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactTargetId: 'native',
    artifactFamily: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-distribution-roadmap-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactTargetId: 'neoforge',
    artifactFamily: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-distribution-roadmap-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactTargetId: 'standalone',
    artifactFamily: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-distribution-roadmap-report.json',
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

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function sameList(actual, expected) {
  return JSON.stringify(actual ?? []) === JSON.stringify(expected ?? [])
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function assertRuntimeParity(payload, label) {
  assert(sameSet(payload.runtimeParity ?? payload.runtimeTargets ?? [], EXPECTED_RUNTIMES), `${label} runtime parity mismatch`)
}

function byId(records, key = 'id') {
  return new Map((records ?? []).map((record) => [record[key], record]))
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const distributionApproval = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const roadmap = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))
  const launcher = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const parity = readJson(path.join(dataRoot, 'systems', 'cross_platform_parity.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))
  const releaseManifest = readJson(path.join(editionRoot, 'release-manifest.template.json'))
  const releaseIndex = readJson(path.join(releaseRoot, 'echo-release.json'))
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)

  assertRuntimeParity(distribution, 'distribution')
  assertRuntimeParity(distributionApproval, 'distribution approval')
  assertRuntimeParity(roadmap, 'roadmap')
  assertRuntimeParity(launcher, 'launcher')
  assert(sameSet((parity.runtimeTargets ?? []).map((target) => target.id), EXPECTED_RUNTIMES), 'parity runtime targets mismatch')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(releaseManifest.packId === edition.packId, `${edition.packId} release manifest packId mismatch`)
  assert(releaseManifest.runtimeTarget === edition.runtimeTarget, `${edition.packId} release manifest runtime target mismatch`)
  assert(releaseManifest.requiredRuntimeEvidenceContract === 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json', `${edition.packId} runtime evidence contract mismatch`)
  assert(sameList(releaseManifest.requiredPublicAlphaEvidence, EXPECTED_PUBLIC_ALPHA_EVIDENCE), `${edition.packId} public alpha evidence mismatch`)

  const artifactTargets = byId(distribution.artifactTargets)
  assert(sameSet([...artifactTargets.keys()], EXPECTED_ARTIFACT_TARGETS), 'distribution artifact target ids mismatch')
  for (const targetId of EXPECTED_ARTIFACT_TARGETS) {
    assert(artifactTargets.get(targetId)?.requiredForPublicAlpha === true, `artifact target ${targetId} must be required for Public Alpha`)
  }
  assert(artifactTargets.get(edition.artifactTargetId)?.editionRepo === `ECHO-Openlands-${editionKey === 'neoforge' ? 'NeoForge' : editionKey[0].toUpperCase() + editionKey.slice(1)}-Edition`, `${edition.packId} artifact target edition repo mismatch`)
  assert(artifactTargets.get(edition.artifactTargetId)?.file === edition.artifactName, `${edition.packId} artifact target filename mismatch`)

  assert(distribution.releaseIndexStates?.currentAllowedState === 'warning', 'distribution release index must stay warning before uploads')
  for (const requirement of [
    'all_artifact_targets_uploaded',
    'all_artifacts_have_sha256',
    'pack_manifest_indexed_for_each_edition',
    'launcher_install_update_repair_rollback_pass',
    'native_standalone_neoforge_parity_pass',
    'legal_content_audit_pass',
  ]) {
    assert(distribution.releaseIndexStates?.approvedRequires?.includes(requirement), `release index approved requirements missing ${requirement}`)
  }
  assert(sameSet((distribution.launcherGates ?? []).map((gate) => gate.id), EXPECTED_LAUNCHER_FLOWS), 'distribution launcher gates mismatch')
  assert(distribution.publicAlphaMinimum?.biomes === 4, 'Public Alpha minimum biomes mismatch')
  assert(distribution.publicAlphaMinimum?.blocks?.min === 50, 'Public Alpha block minimum mismatch')
  assert(distribution.publicAlphaMinimum?.blocks?.target === 70, 'Public Alpha block target mismatch')
  assert(distribution.publicAlphaMinimum?.items?.min === 45, 'Public Alpha item minimum mismatch')
  assert(distribution.publicAlphaMinimum?.items?.target === 60, 'Public Alpha item target mismatch')
  assert(distribution.publicAlphaMinimum?.creatures === 10, 'Public Alpha creature minimum mismatch')
  for (const loop of ['spawn_gather_tool_shelter_sleep', 'first_cave_or_old_road', 'kiln_and_forge_progression', 'first_waystone_repair', 'map_reveal', 'save_load']) {
    assert(distribution.publicAlphaMinimum?.requiredLoops?.includes(loop), `Public Alpha minimum missing loop ${loop}`)
  }
  assert(distribution.publicAlphaMinimum?.coOp?.targetPlayers === '1-8', 'Public Alpha co-op target mismatch')
  assert(distribution.publicAlphaMinimum?.coOp?.publicAlphaRequirement === 'dedicated_server_or_hosted_session_tested', 'Public Alpha co-op requirement mismatch')

  assert(roadmap.defaultRule.includes('relaxed default'), 'roadmap must preserve relaxed default rule')
  assert(sameList((roadmap.phases ?? []).map((phase) => phase.id), EXPECTED_ROADMAP_PHASES), 'launch roadmap phase order mismatch')
  const roadmapPhases = byId(roadmap.phases)
  assert(roadmapPhases.get('mvp')?.scope?.biomes === 4, 'MVP roadmap biome count mismatch')
  assert(roadmapPhases.get('mvp')?.scope?.blocks === '50-70', 'MVP roadmap block range mismatch')
  assert(roadmapPhases.get('mvp')?.scope?.items === '45-60', 'MVP roadmap item range mismatch')
  assert(roadmapPhases.get('mvp')?.scope?.players === '1', 'MVP roadmap player count mismatch')
  assert(roadmapPhases.get('public_alpha')?.scope?.biomes === '8-10', 'Public Alpha roadmap biome target mismatch')
  assert(roadmapPhases.get('public_alpha')?.scope?.blocks === '150+', 'Public Alpha roadmap block target mismatch')
  assert(roadmapPhases.get('public_alpha')?.scope?.items === '120+', 'Public Alpha roadmap item target mismatch')
  assert(roadmapPhases.get('public_alpha')?.scope?.players === '1-8 co-op', 'Public Alpha roadmap players mismatch')
  assert(roadmapPhases.get('one_dot_zero')?.scope?.coreLoops?.includes('full_waystone_network'), '1.0 roadmap must include full waystone network')
  assert(roadmapPhases.get('one_dot_zero')?.scope?.coreLoops?.includes('old_road_restoration'), '1.0 roadmap must include old road restoration')
  assert(roadmapPhases.get('one_dot_zero')?.scope?.coreLoops?.includes('server_multiplayer'), '1.0 roadmap must include server multiplayer')
  for (const invariant of [
    'Echo data IDs remain source of truth.',
    'Hardlands remains optional.',
    'No Minecraft branding, textures, sounds, copied mob silhouettes, or copied recipe identity.',
    'Native, Standalone, and NeoForge releases cannot be approved unless parity differences are documented.',
    'Launcher repair and rollback must work before public release.',
  ]) {
    assert(roadmap.nonNegotiableInvariants?.includes(invariant), `roadmap invariant missing ${invariant}`)
  }

  const launcherMatrix = byId(launcher.editionMatrix)
  assert(sameSet([...launcherMatrix.keys()], ['native', 'neoforge', 'standalone']), 'launcher edition matrix mismatch')
  const launcherEntry = launcherMatrix.get(edition.artifactTargetId)
  assert(launcherEntry?.runtimeTarget === edition.runtimeTarget, `${edition.packId} launcher runtime target mismatch`)
  assert(launcherEntry?.packId === edition.packId, `${edition.packId} launcher pack id mismatch`)
  assert(launcherEntry?.artifactFamily === edition.artifactFamily, `${edition.packId} launcher artifact family mismatch`)
  assert(launcherEntry?.artifactPattern === edition.artifactName, `${edition.packId} launcher artifact pattern mismatch`)
  assert(releaseManifest.loader === launcherEntry.launcherProfileKind, `${edition.packId} launcher profile kind mismatch`)
  assert(sameList(launcher.requiredLauncherFlows?.map((flow) => flow.id), EXPECTED_LAUNCHER_FLOWS), 'launcher required flows mismatch')
  assert(launcher.artifactVerification?.currentIndexStateAllowed === 'warning', 'launcher artifact verification must stay warning before uploaded URLs')
  assert(launcher.artifactVerification?.approvedStateRequiresArtifacts === true, 'launcher approved state must require artifacts')

  const parityTargets = byId(parity.runtimeTargets)
  const parityTarget = parityTargets.get(edition.runtimeTarget)
  assert(parityTarget?.editionRepo === launcherEntry.editionRepo, `${edition.packId} parity edition repo mismatch`)
  assert(parityTarget?.artifactFamily === edition.artifactFamily, `${edition.packId} parity artifact family mismatch`)
  assert(parityTarget?.artifactPattern === edition.artifactName, `${edition.packId} parity artifact pattern mismatch`)
  for (const surface of ['registry_ids', 'first_hour_save_load', 'standard_mode_rules', 'waystone_state_machine']) {
    assert((parity.paritySurfaces ?? []).some((entry) => entry.id === surface), `parity surface missing ${surface}`)
  }

  assert(releaseIndex.schemaVersion === 'echo.module.release.v1', 'release index schema mismatch')
  assert(releaseIndex.releaseId === RELEASE_ID, 'release id mismatch')
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)
  const artifactSummaries = []
  for (const target of distribution.artifactTargets ?? []) {
    const artifact = artifactByFile(releaseModule, target.file)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(artifact, `release index missing artifact ${target.file}`)
    assert(fileExists(artifactPath), `artifact file missing ${artifactPath}`)
    assert(artifact.sha256 === sha256File(artifactPath), `artifact sha mismatch for ${target.file}`)
    assert(artifact.size === fs.statSync(artifactPath).size, `artifact size mismatch for ${target.file}`)
    assert(typeof artifact.sha256 === 'string' && artifact.sha256.length === 64, `artifact ${target.file} sha256 must be recorded`)
    assert(Number.isInteger(artifact.size) && artifact.size > 0, `artifact ${target.file} size must be recorded`)
    if (target.id !== 'sources') {
      assert(artifact.buildMode === 'compiled-runtime', `artifact ${target.file} must be compiled-runtime`)
      assert(normalizeRuntimeTarget(artifact.runtimeTarget) === (target.id === 'native' ? 'echo_native' : target.id === 'standalone' ? 'echo_runtime_standalone' : 'neoforge'), `artifact ${target.file} runtime target mismatch`)
    }
    artifactSummaries.push({
      id: target.id,
      file: target.file,
      kind: artifact.kind,
      size: artifact.size,
      sha256: artifact.sha256,
      buildMode: artifact.buildMode ?? null,
      downloadUrlPresent: typeof artifact.downloadUrl === 'string' && artifact.downloadUrl.length > 0,
      requiredForPublicAlpha: target.requiredForPublicAlpha,
    })
  }

  const publicAlphaConformanceCounts = {
    biomes: conformance.biomeRegistry?.length ?? 0,
    blocks: (conformance.blockRegistry?.length ?? 0) + (conformance.foundationRegistries?.blocksMovedToFoundation?.length ?? 0),
    items: (conformance.itemRegistry?.length ?? 0) + (conformance.foundationRegistries?.itemsMovedToFoundation?.length ?? 0),
    creatures: conformance.creatureRegistry?.length ?? 0,
  }
  const mvpMinimumsMet = {
    biomes: conformance.biomeRegistry?.length >= distribution.publicAlphaMinimum.biomes,
    blocks: publicAlphaConformanceCounts.blocks >= distribution.publicAlphaMinimum.blocks.min,
    items: publicAlphaConformanceCounts.items >= distribution.publicAlphaMinimum.items.min,
    creatures: conformance.creatureRegistry?.length >= distribution.publicAlphaMinimum.creatures,
  }
  assert(Object.values(mvpMinimumsMet).every(Boolean), 'MVP conformance minimums do not meet Public Alpha floor')

  const report = {
    schema: 'echo.openlands.edition.distribution_roadmap_report.v1',
    status: 'preflight_passed',
    publicAlphaReady: false,
    realDistributionExecutionRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    contracts: {
      distribution: 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json',
      distributionApproval: 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json',
      launchRoadmap: 'data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json',
      launcherFlow: 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json',
      crossPlatformParity: 'data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json',
      conformance: 'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
    },
    releaseManifest: {
      packId: releaseManifest.packId,
      runtimeTarget: releaseManifest.runtimeTarget,
      loader: releaseManifest.loader,
      moduleArtifactFamily: releaseManifest.moduleArtifactFamily,
      moduleArtifactPattern: releaseManifest.moduleArtifactPattern,
      requiredPublicAlphaEvidence: releaseManifest.requiredPublicAlphaEvidence,
    },
    releaseIndex: {
      releaseId: releaseIndex.releaseId,
      currentAllowedState: distribution.releaseIndexStates.currentAllowedState,
      launcherCurrentIndexStateAllowed: launcher.artifactVerification.currentIndexStateAllowed,
      approvedRequires: distribution.releaseIndexStates.approvedRequires,
      artifactSummaries,
      uploadedArtifactUrlsPresent: artifactSummaries.every((artifact) => artifact.downloadUrlPresent),
    },
    editionMatrix: {
      id: edition.artifactTargetId,
      launcherEntry,
      parityTarget,
      artifactTarget: artifactTargets.get(edition.artifactTargetId),
    },
    publicAlphaMinimum: {
      ...distribution.publicAlphaMinimum,
      conformanceCounts: publicAlphaConformanceCounts,
      mvpMinimumsMet,
    },
    roadmap: {
      defaultRule: roadmap.defaultRule,
      phaseIds: roadmap.phases.map((phase) => phase.id),
      mvpScope: roadmapPhases.get('mvp').scope,
      publicAlphaScope: roadmapPhases.get('public_alpha').scope,
      oneDotZeroLoops: roadmapPhases.get('one_dot_zero').scope.coreLoops,
      nonNegotiableInvariants: roadmap.nonNegotiableInvariants,
    },
    launcherFlows: launcher.requiredLauncherFlows.map((flow) => ({
      id: flow.id,
      appliesTo: flow.appliesTo,
      mustVerify: flow.mustVerify,
      evidenceAttachment: flow.evidenceAttachment,
    })),
    blockedBy: [
      'release_index_download_urls_missing',
      'real_launcher_install_update_repair_rollback_execution_missing',
      'native_standalone_neoforge_runtime_parity_execution_missing',
      'public_alpha_coop_session_test_missing',
      'public_alpha_release_index_approval_missing',
    ],
    outputPath,
    proofs: [
      'distribution_contract_loaded',
      'distribution_approval_contract_loaded',
      'launch_roadmap_loaded',
      'launcher_flow_contract_loaded',
      'cross_platform_parity_contract_loaded',
      'edition_manifest_matches_distribution_matrix',
      'public_alpha_minimums_match_mvp_floor',
      'compiled_release_artifacts_have_sha256_and_size',
      'release_index_stays_warning_until_uploads',
      'roadmap_relaxed_default_and_invariants_declared',
      'public_alpha_blocked_until_real_distribution_execution',
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
    console.log(`Openlands ${args.edition} distribution roadmap report ${action}: ${report.releaseIndex.artifactSummaries.length} artifacts, ${report.roadmap.phaseIds.length} roadmap phases, publicAlphaReady=${report.publicAlphaReady}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-distribution-roadmap-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-distribution-roadmap-report.json.
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
