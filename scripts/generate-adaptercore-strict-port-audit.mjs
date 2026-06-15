import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SCHEMA = 'echo.adaptercore.strict_port_audit.v1'
const DEFAULT_OUT_DIR = path.join('reports', 'runtime-parity')
const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const NEOFORGE_TOML_PATH = path.join('src', 'main', 'templates', 'META-INF', 'neoforge.mods.toml')
const NEOFORGE_RESOURCE_TOML_PATH = path.join('src', 'main', 'resources', 'META-INF', 'neoforge.mods.toml')
const TIER0_MODULES = new Set([
  'echocore',
  'echoplatformcore',
  'echoaddonapi',
  'echoschemacore',
  'echovalidationcore',
])
const TIER1_MODULE = 'echoadaptercore'
const REQUIRED_RUNTIMES = ['neoforge', 'echo_native', 'echo_runtime_standalone']
const LEGACY_SIGNALOS_ID = 'signalos'
const SIGNALOS_MODULE_ID = 'echosignalos'

export async function generateAdapterCoreStrictPortAudit({
  repoRoot = process.cwd(),
  outDir = DEFAULT_OUT_DIR,
  write = true,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedOutDir = path.resolve(normalizedRoot, outDir)
  const modules = await discoverModules(normalizedRoot)
  const adapterCoreSourceSignal = await readAdapterCoreSourceSignal(normalizedRoot)
  const rows = []

  for (const moduleInfo of modules) {
    rows.push(await auditModule(normalizedRoot, moduleInfo, adapterCoreSourceSignal))
  }

  const summary = summarizeRows(rows)
  const report = {
    schema: SCHEMA,
    generatedAt: new Date().toISOString(),
    generatedFrom: [
      'ECHO-Modules/addons/*/src/main/resources/META-INF/echo.mod.json',
      'ECHO-Modules/addons/*/build.gradle',
      'ECHO-Modules/addons/*/src/main/templates/META-INF/neoforge.mods.toml',
      'ECHO-Modules/addons/*/src/main/java',
      'ECHO-Modules/dist/echo-module-release/*',
      'ECHO-Modules/addons/echoadaptercore/src/main/java',
    ],
    summary,
    rows,
  }

  const backlog = rows
    .filter((row) => row.result !== 'pass')
    .map((row) => ({
      moduleId: row.moduleId,
      moduleDir: row.moduleDir,
      tier: row.tier,
      blockers: row.strictBlockers,
      recommendedFixes: row.recommendedFixes,
    }))

  const outputs = {
    json: path.join(normalizedOutDir, 'adaptercore-strict-port-audit.json'),
    markdown: path.join(normalizedOutDir, 'adaptercore-strict-port-audit.md'),
    backlogJson: path.join(normalizedOutDir, 'adaptercore-strict-port-fix-backlog.json'),
    backlogMarkdown: path.join(normalizedOutDir, 'adaptercore-strict-port-fix-backlog.md'),
  }

  if (write) {
    await fs.mkdir(normalizedOutDir, { recursive: true })
    await fs.writeFile(outputs.json, `${JSON.stringify(report, null, 2)}\n`)
    await fs.writeFile(outputs.markdown, renderMarkdown(report))
    if (backlog.length > 0) {
      await fs.writeFile(outputs.backlogJson, `${JSON.stringify({ schema: `${SCHEMA}.backlog`, generatedAt: report.generatedAt, backlog }, null, 2)}\n`)
      await fs.writeFile(outputs.backlogMarkdown, renderBacklogMarkdown(report, backlog))
    } else {
      await removeIfExists(outputs.backlogJson)
      await removeIfExists(outputs.backlogMarkdown)
    }
  }

  return { report, backlog, outputs }
}

async function discoverModules(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const moduleDir = entry.name
    const descriptorPath = path.join(addonsRoot, moduleDir, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const moduleId = String(descriptor.id ?? '').trim()
    const version = String(descriptor.version ?? '1.0.0').trim() || '1.0.0'
    modules.push({
      moduleDir,
      moduleId,
      version,
      descriptor,
      descriptorPath,
      addonRoot: path.join(addonsRoot, moduleDir),
    })
  }
  return modules.sort((a, b) => a.moduleId.localeCompare(b.moduleId))
}

async function auditModule(repoRoot, moduleInfo, adapterCoreSourceSignal) {
  const tier = tierFor(moduleInfo.moduleId)
  const descriptorRequires = descriptorRequiresAdapterCore(moduleInfo.descriptor)
  const runtimeAccess = adapterCoreRuntimes(moduleInfo.descriptor)
  const runtimeAccessStatus = REQUIRED_RUNTIMES.map((runtime) => ({
    runtime,
    declared: runtimeAccess.includes(runtime),
  }))
  const gradle = await gradleDependencyStatus(moduleInfo.addonRoot)
  const neoforgeToml = await neoForgeTomlDependencyStatus(moduleInfo.addonRoot, moduleInfo.moduleId)
  const javaSignal = await javaAdapterCoreSignalStatus(moduleInfo, adapterCoreSourceSignal)
  const truthLayer = truthLayerCoverageStatus(moduleInfo.moduleId, adapterCoreSourceSignal)
  const gameplayMutationProof = await adapterCoreGameplayMutationProofStatus(moduleInfo)
  const artifacts = await artifactStatus(repoRoot, moduleInfo)
  const signalOs = await signalOsStatus(moduleInfo)
  const strictBlockers = []
  const recommendedFixes = []

  const isAdapterCore = moduleInfo.moduleId === TIER1_MODULE
  const requiresAdapterCoreConsumer = !isAdapterCore && tier !== 'tier0'

  if (requiresAdapterCoreConsumer && !descriptorRequires.declared) {
    strictBlockers.push('descriptor requires does not include echoadaptercore')
    recommendedFixes.push('Add echoadaptercore to echo.mod.json requires for this non-foundation module.')
  }

  for (const runtime of runtimeAccessStatus) {
    if (!runtime.declared) {
      strictBlockers.push(`access.adapterCore.runtimes missing ${runtime.runtime}`)
      recommendedFixes.push('Align access.adapterCore.runtimes to neoforge, echo_native, and echo_runtime_standalone.')
    }
  }

  if (requiresAdapterCoreConsumer && gradle.mode === 'missing') {
    strictBlockers.push('Gradle dependency on :echoadaptercore is missing')
    recommendedFixes.push('Add implementation project(":echoadaptercore") unless the module only exposes an optional compatibility surface.')
  }

  if (requiresAdapterCoreConsumer && gradle.mode === 'compileOnly' && !gradle.runtimePresent) {
    strictBlockers.push('Gradle runtime wiring for :echoadaptercore is missing')
    recommendedFixes.push('Add localRuntime project(":echoadaptercore") or use implementation project(":echoadaptercore") so NeoForge runtime-strict runs install AdapterCore.')
  }

  if (requiresAdapterCoreConsumer && !neoforgeToml.required) {
    strictBlockers.push('NeoForge TOML required dependency on echoadaptercore is missing')
    recommendedFixes.push('Add a required [[dependencies.<mod_id>]] entry for modId="echoadaptercore".')
  }

  if (tier === 'tier0' && neoforgeToml.required) {
    strictBlockers.push('Tier 0 NeoForge TOML dependency on echoadaptercore must be optional/non-required')
    recommendedFixes.push('Keep Tier 0 AdapterCore TOML metadata optional and non-ordering to avoid loader cycles.')
  }

  if (tier === 'tier0' && neoforgeToml.present && neoforgeToml.ordering !== 'NONE') {
    strictBlockers.push('Tier 0 NeoForge TOML dependency on echoadaptercore must not impose load ordering')
    recommendedFixes.push('Set the Tier 0 echoadaptercore TOML dependency ordering to NONE.')
  }

  if (moduleInfo.moduleId === TIER1_MODULE && neoforgeToml.selfDependency) {
    strictBlockers.push('echoadaptercore declares a self dependency in NeoForge TOML')
    recommendedFixes.push('Remove any [[dependencies.echoadaptercore]] entry with modId="echoadaptercore".')
  }

  if (moduleInfo.moduleId === TIER1_MODULE && neoforgeToml.requiredTier0Dependencies.length > 0) {
    strictBlockers.push(`echoadaptercore declares required Tier 0 TOML dependency: ${neoforgeToml.requiredTier0Dependencies.join(', ')}`)
    recommendedFixes.push('Keep Tier 0 ordering hints optional/non-required in echoadaptercore TOML.')
  }

  if (!javaSignal.present && !truthLayer.covered) {
    strictBlockers.push('Java AdapterCore signal or truth-host coverage is missing')
    recommendedFixes.push('Add a real AdapterCore-backed path, truth bridge, runtime host, or compatibility catalog entry.')
  }

  if (moduleInfo.moduleId === 'echoashfallprotocol') {
    if (!gameplayMutationProof.dispatchProofSurfacePresent) {
      strictBlockers.push('Ashfall AdapterCore gameplay mutation dispatch proof surface is missing')
      recommendedFixes.push('Route Ashfall gameplay mutations through EchoRuntimeActionDispatcher outcomes with receipt-grade before/after, save, HUD, or event evidence.')
    }
    if (gameplayMutationProof.queuedOnlyEvidence.length > 0) {
      strictBlockers.push('Ashfall AdapterCore gameplay proof includes queued-only evidence')
      recommendedFixes.push('Replace queued-only AdapterCore gameplay proof with live mutation receipts or mark it non-release proof.')
    }
    if (gameplayMutationProof.diagnosticOnlyEvidence.length > 0) {
      strictBlockers.push('Ashfall AdapterCore gameplay proof includes diagnostic-only evidence')
      recommendedFixes.push('Keep diagnostic-only AdapterCore evidence out of Ashfall release proof paths.')
    }
    if (gameplayMutationProof.dispatcherBypassMutationEvidence.length > 0) {
      strictBlockers.push('Ashfall AdapterCore gameplay mutation proof bypasses dispatcher enforcement')
      recommendedFixes.push('Route canonical Ashfall first-spawn, early-event, and machine mutation claims through EchoRuntimeActionDispatcher outcomes.')
    }
  }

  for (const artifact of artifacts.required) {
    if (!artifact.present) {
      strictBlockers.push(`compiled ${artifact.runtime} artifact is missing: ${artifact.expected}`)
      recommendedFixes.push('Regenerate module release artifacts after source and metadata are consistent.')
    }
  }

  if (moduleInfo.moduleDir === SIGNALOS_MODULE_ID) {
    if (moduleInfo.moduleId !== SIGNALOS_MODULE_ID) {
      strictBlockers.push('SignalOS descriptor id must be echosignalos')
      recommendedFixes.push('Change addons/echosignalos echo.mod.json id from signalos to echosignalos.')
    }
    if (!signalOs.gradleModIdIsCanonical) {
      strictBlockers.push('SignalOS Gradle mod_id must be echosignalos')
      recommendedFixes.push('Change addons/echosignalos/gradle.properties mod_id to echosignalos.')
    }
    if (!signalOs.legacyContentNamespacePreserved) {
      strictBlockers.push('SignalOS legacy signalos content namespace evidence is missing')
      recommendedFixes.push('Keep signalos as the content/resource namespace while using echosignalos for module identity.')
    }
    if (!signalOs.legacyAliasEvidence) {
      strictBlockers.push('SignalOS legacy module-id alias evidence is missing')
      recommendedFixes.push('Add descriptor alias/replacement evidence for legacy module id signalos.')
    }
  }

  const result = strictBlockers.length === 0 ? 'pass' : 'fail'
  return {
    moduleId: moduleInfo.moduleId,
    moduleDir: moduleInfo.moduleDir,
    version: moduleInfo.version,
    tier,
    result,
    descriptor: {
      path: path.relative(repoRoot, moduleInfo.descriptorPath).replaceAll(path.sep, '/'),
      requires: descriptorRequires,
      adapterCoreRuntimes: runtimeAccessStatus,
    },
    gradleDependency: gradle,
    neoforgeTomlDependency: neoforgeToml,
    javaAdapterCoreSignal: javaSignal,
    adapterCoreTruthLayer: truthLayer,
    adapterCoreGameplayMutationProof: gameplayMutationProof,
    artifacts,
    signalOs: moduleInfo.moduleDir === SIGNALOS_MODULE_ID ? signalOs : undefined,
    strictBlockers,
    recommendedFixes: [...new Set(recommendedFixes)],
  }
}

function tierFor(moduleId) {
  if (moduleId === TIER1_MODULE) return 'tier1'
  if (TIER0_MODULES.has(moduleId)) return 'tier0'
  return 'tier2plus'
}

function descriptorRequiresAdapterCore(descriptor) {
  const requires = Array.isArray(descriptor.requires) ? descriptor.requires.map(String) : []
  return {
    declared: requires.includes(TIER1_MODULE),
    values: requires,
  }
}

function adapterCoreRuntimes(descriptor) {
  const runtimes = descriptor?.access?.adapterCore?.runtimes
  return Array.isArray(runtimes) ? runtimes.map(String) : []
}

async function gradleDependencyStatus(addonRoot) {
  const gradlePath = path.join(addonRoot, 'build.gradle')
  const text = await readTextIfExists(gradlePath)
  if (!text) {
    return { path: null, mode: 'missing', runtimePresent: false, evidence: [] }
  }
  const evidence = linesContaining(text, 'echoadaptercore')
  const mode = gradleModeFor(text)
  const runtimePresent = gradleRuntimeWiringFor(text)
  return {
    path: normalizePath(path.relative(process.cwd(), gradlePath)),
    mode,
    runtimePresent,
    evidence,
  }
}

function gradleModeFor(text) {
  if (/\bapi\s+project\((['"])?:echoadaptercore\1\)/.test(text) || /\bapi\s+project\((['"]):echoadaptercore\1\)/.test(text)) {
    return 'api'
  }
  if (/\bimplementation\s+project\((['"])?:echoadaptercore\1\)/.test(text) || /\bimplementation\s+project\((['"]):echoadaptercore\1\)/.test(text)) {
    return 'implementation'
  }
  if (/\bcompileOnly\s+project\((['"])?:echoadaptercore\1\)/.test(text) || /\bcompileOnly\s+project\((['"]):echoadaptercore\1\)/.test(text)) {
    return 'compileOnly'
  }
  if (/compileOnlyIfIncluded\s*\([^)]*['"]:echoadaptercore['"]/.test(text)
    || /optionalProject\s*\([^)]*['"]:echoadaptercore['"][^)]*['"]compileOnly['"]/.test(text)) {
    return 'compileOnly'
  }
  if (text.includes(':echoadaptercore')) {
    return 'helper'
  }
  return 'missing'
}

function gradleRuntimeWiringFor(text) {
  return /\b(api|implementation|localRuntime|runtimeOnly)\s+project\((['"])?:echoadaptercore\2\)/.test(text)
    || /\b(api|implementation|localRuntime|runtimeOnly)\s+project\((['"]):echoadaptercore\2\)/.test(text)
    || /localRuntimeIfIncluded\s*\(\s*['"]:echoadaptercore['"]\s*\)/.test(text)
    || /optionalProject\s*\(\s*['"]:echoadaptercore['"]\s*,\s*['"]localRuntime['"]\s*\)/.test(text)
}

async function neoForgeTomlDependencyStatus(addonRoot, moduleId) {
  const templateTomlPath = path.join(addonRoot, NEOFORGE_TOML_PATH)
  const resourceTomlPath = path.join(addonRoot, NEOFORGE_RESOURCE_TOML_PATH)
  const tomlPath = await exists(templateTomlPath) ? templateTomlPath : resourceTomlPath
  const text = await readTextIfExists(tomlPath)
  if (!text) {
    return {
      path: null,
      present: false,
      required: false,
      type: 'missing',
      ordering: 'missing',
      side: 'missing',
      selfDependency: false,
      requiredTier0Dependencies: [],
      evidence: [],
    }
  }
  const blocks = parseDependencyBlocks(text)
  const adapterBlocks = blocks.filter((block) => block.values.modId === TIER1_MODULE)
  const required = adapterBlocks.some((block) => block.values.type === 'required')
  const requiredTier0Dependencies = moduleId === TIER1_MODULE
    ? blocks
      .filter((block) => TIER0_MODULES.has(block.values.modId) && block.values.type === 'required')
      .map((block) => block.values.modId)
    : []
  return {
    path: normalizePath(path.relative(process.cwd(), tomlPath)),
    present: adapterBlocks.length > 0,
    required,
    type: adapterBlocks[0]?.values.type ?? 'missing',
    ordering: adapterBlocks[0]?.values.ordering ?? 'missing',
    side: adapterBlocks[0]?.values.side ?? 'missing',
    selfDependency: moduleId === TIER1_MODULE && adapterBlocks.length > 0,
    requiredTier0Dependencies,
    evidence: adapterBlocks.map((block) => block.raw),
  }
}

function parseDependencyBlocks(text) {
  const blocks = []
  const headerRe = /^\s*\[\[dependencies\.([^\]]+)]]\s*$/gm
  const matches = [...text.matchAll(headerRe)]
  for (let i = 0; i < matches.length; i++) {
    const start = matches[i].index
    const end = i + 1 < matches.length ? matches[i + 1].index : text.length
    const raw = text.slice(start, end).trim()
    const values = {}
    for (const line of raw.split(/\r?\n/)) {
      const match = line.match(/^\s*([A-Za-z0-9_.-]+)\s*=\s*"?([^"\r\n]+)"?\s*$/)
      if (match) values[match[1]] = match[2]
    }
    blocks.push({ owner: matches[i][1], raw, values })
  }
  return blocks
}

async function javaAdapterCoreSignalStatus(moduleInfo, adapterCoreSourceSignal) {
  const javaRoot = path.join(moduleInfo.addonRoot, 'src', 'main', 'java')
  const files = await listFiles(javaRoot, (file) => file.endsWith('.java'))
  const hits = []
  for (const file of files) {
    const text = await readTextIfExists(file)
    if (!text) continue
    if (text.includes('echo.adaptercore') || text.includes('AdapterCore') || text.includes('EchoRuntimeHost') || text.includes('TruthBridge')) {
      hits.push(normalizePath(path.relative(process.cwd(), file)))
    }
  }
  const adapterCoreCoverage = truthLayerCoverageStatus(moduleInfo.moduleId, adapterCoreSourceSignal)
  return {
    present: hits.length > 0 || adapterCoreCoverage.covered,
    sourceHits: hits.slice(0, 12),
    sourceHitCount: hits.length,
    adapterCoreCoverage: adapterCoreCoverage.covered,
  }
}

async function adapterCoreGameplayMutationProofStatus(moduleInfo) {
  const javaRoot = path.join(moduleInfo.addonRoot, 'src', 'main', 'java')
  const files = await listFiles(javaRoot, (file) => file.endsWith('.java'))
  const dispatchProofSurface = []
  const queuedOnlyEvidence = []
  const diagnosticOnlyEvidence = []
  const dispatcherBypassMutationEvidence = []
  for (const file of files) {
    const text = await readTextIfExists(file)
    if (!text) continue
    const relative = normalizePath(path.relative(process.cwd(), file))
    if (text.includes('EchoRuntimeActionDispatcher') && text.includes('EchoRuntimeActionOutcome')) {
      dispatchProofSurface.push(relative)
    }
    if (isCanonicalAshfallProofFile(relative)
        && containsMutationClaim(text)
        && !(text.includes('EchoRuntimeActionDispatcher') && text.includes('EchoRuntimeActionOutcome'))) {
      dispatcherBypassMutationEvidence.push(relative)
    }
    if (/\bQUEUED_ONLY\b|queued-only|queuedOnly/i.test(text)) {
      queuedOnlyEvidence.push(relative)
    }
    if (/\bDIAGNOSTIC_ONLY\b|diagnostic-only|diagnosticOnly/i.test(text)) {
      diagnosticOnlyEvidence.push(relative)
    }
  }
  return {
    dispatchProofSurfacePresent: dispatchProofSurface.length > 0,
    dispatchProofSurface: dispatchProofSurface.slice(0, 12),
    dispatchProofSurfaceCount: dispatchProofSurface.length,
    queuedOnlyEvidence: [...new Set(queuedOnlyEvidence)],
    diagnosticOnlyEvidence: [...new Set(diagnosticOnlyEvidence)],
    dispatcherBypassMutationEvidence: [...new Set(dispatcherBypassMutationEvidence)],
  }
}

function isCanonicalAshfallProofFile(relativePath) {
  return /(?:AshfallAdapterCoreFirstSpawnRuntime|AshfallAdapterCoreEarlyEventRuntime|AshfallAdapterCoreMachineRuntimeHost)\.java$/.test(relativePath)
}

function containsMutationClaim(text) {
  return /NativeResult\.mutated\(/.test(text)
    || /new\s+NativeResult\s*\([^;]*(?:true|"MUTATED"|MUTATED)/s.test(text)
}

function truthLayerCoverageStatus(moduleId, adapterCoreSourceSignal) {
  const needles = truthLayerNeedles(moduleId)
  const hits = []
  for (const source of adapterCoreSourceSignal.sources) {
    if (needles.some((needle) => source.text.includes(needle))) {
      hits.push(source.path)
    }
  }
  return {
    covered: hits.length > 0,
    evidence: hits,
    expectedNeedles: needles,
  }
}

function truthLayerNeedles(moduleId) {
  const classStem = moduleId
    .replace(/^echo/, 'Echo')
    .replace(/(^|[^A-Za-z0-9])([A-Za-z0-9])/g, (_, __, char) => char.toUpperCase())
  return [
    `"${moduleId}"`,
    `${moduleId}:runtime_host`,
    `${classStem}TruthBridge`,
    `${classStem}RuntimeHost`,
  ]
}

async function readAdapterCoreSourceSignal(repoRoot) {
  const root = path.join(repoRoot, 'addons', TIER1_MODULE, 'src', 'main', 'java')
  const files = await listFiles(root, (file) => file.endsWith('.java'))
  const sources = []
  for (const file of files) {
    const text = await readTextIfExists(file)
    sources.push({
      path: normalizePath(path.relative(repoRoot, file)),
      text,
    })
  }
  return { sources }
}

async function artifactStatus(repoRoot, moduleInfo) {
  const releaseDir = path.join(repoRoot, 'dist', 'echo-module-release', moduleInfo.moduleId)
  const legacySignalOsReleaseDir = moduleInfo.moduleDir === SIGNALOS_MODULE_ID
    ? path.join(repoRoot, 'dist', 'echo-module-release', LEGACY_SIGNALOS_ID)
    : null
  const libsDir = path.join(moduleInfo.addonRoot, 'build', 'libs')
  const expected = {
    neoforge: `${moduleInfo.moduleId}-${moduleInfo.version}-neoforge.jar`,
    native: `${moduleInfo.moduleId}-${moduleInfo.version}.echo-addon`,
    standalone: `${moduleInfo.moduleId}-${moduleInfo.version}-standalone.jar`,
    sources: `${moduleInfo.moduleId}-${moduleInfo.version}-sources.jar`,
  }
  const releaseFiles = await fileSet(releaseDir)
  const libsFiles = await fileSet(libsDir)
  const legacyReleaseFiles = legacySignalOsReleaseDir ? await fileSet(legacySignalOsReleaseDir) : new Set()
  const required = [
    artifactEntry('neoforge', expected.neoforge, releaseFiles, libsFiles, legacyReleaseFiles, moduleInfo),
    artifactEntry('native', expected.native, releaseFiles, libsFiles, legacyReleaseFiles, moduleInfo),
    artifactEntry('standalone', expected.standalone, releaseFiles, libsFiles, legacyReleaseFiles, moduleInfo),
  ]
  const sources = artifactEntry('sources', expected.sources, releaseFiles, libsFiles, legacyReleaseFiles, moduleInfo)
  return {
    releaseDir: normalizePath(path.relative(repoRoot, releaseDir)),
    buildLibsDir: normalizePath(path.relative(repoRoot, libsDir)),
    required,
    sources,
  }
}

function artifactEntry(runtime, expected, releaseFiles, libsFiles, legacyReleaseFiles, moduleInfo) {
  const legacyExpected = moduleInfo.moduleDir === SIGNALOS_MODULE_ID
    ? expected.replaceAll(SIGNALOS_MODULE_ID, LEGACY_SIGNALOS_ID)
    : null
  const presentInRelease = releaseFiles.has(expected)
  const presentInBuild = libsFiles.has(expected) || (
    runtime === 'neoforge'
    && libsFiles.has(`${moduleInfo.moduleId}-${moduleInfo.version}.jar`)
  )
  const legacyOnly = legacyExpected ? legacyReleaseFiles.has(legacyExpected) || libsFiles.has(legacyExpected) : false
  return {
    runtime,
    expected,
    present: presentInRelease || presentInBuild,
    presentInRelease,
    presentInBuild,
    legacyOnly,
  }
}

async function signalOsStatus(moduleInfo) {
  const gradlePropsPath = path.join(moduleInfo.addonRoot, 'gradle.properties')
  const gradleProps = await readTextIfExists(gradlePropsPath)
  const javaFiles = await listFiles(path.join(moduleInfo.addonRoot, 'src', 'main', 'java'), (file) => file.endsWith('.java'))
  const javaText = (await Promise.all(javaFiles.map(readTextIfExists))).join('\n')
  const descriptorText = JSON.stringify(moduleInfo.descriptor)
  return {
    gradleModIdIsCanonical: /^\s*mod_id\s*=\s*echosignalos\s*$/m.test(gradleProps),
    legacyContentNamespacePreserved: javaText.includes('"signalos"') || gradleProps.includes('content_namespace=signalos'),
    legacyAliasEvidence: descriptorText.includes('"signalos"') && (
      descriptorText.includes('legacy') || descriptorText.includes('alias') || descriptorText.includes('replacement')
    ),
  }
}

function summarizeRows(rows) {
  const resultCounts = countBy(rows, (row) => row.result)
  const tierCounts = countBy(rows, (row) => row.tier)
  const blockerCounts = new Map()
  for (const row of rows) {
    for (const blocker of row.strictBlockers) {
      blockerCounts.set(blocker, (blockerCounts.get(blocker) ?? 0) + 1)
    }
  }
  return {
    moduleCount: rows.length,
    resultCounts: {
      pass: resultCounts.pass ?? 0,
      fail: resultCounts.fail ?? 0,
    },
    tierCounts,
    blockingModuleIds: rows.filter((row) => row.result !== 'pass').map((row) => row.moduleId),
    blockerCounts: [...blockerCounts.entries()]
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
      .map(([blocker, count]) => ({ blocker, count })),
  }
}

function renderMarkdown(report) {
  const lines = []
  lines.push('# AdapterCore Strict Port Audit')
  lines.push('')
  lines.push(`- Schema: \`${report.schema}\``)
  lines.push(`- Generated: ${report.generatedAt}`)
  lines.push(`- Modules: ${report.summary.moduleCount}`)
  lines.push(`- Passing: ${report.summary.resultCounts.pass}`)
  lines.push(`- Failing: ${report.summary.resultCounts.fail}`)
  lines.push('')
  lines.push('## Result By Module')
  lines.push('')
  lines.push('| Module | Tier | Result | Blockers |')
  lines.push('| --- | --- | --- | --- |')
  for (const row of report.rows) {
    lines.push(`| ${row.moduleId} | ${row.tier} | ${row.result} | ${escapeCell(row.strictBlockers.join('; ') || 'none')} |`)
  }
  lines.push('')
  lines.push('## Blocker Counts')
  lines.push('')
  lines.push('| Blocker | Count |')
  lines.push('| --- | ---: |')
  for (const blocker of report.summary.blockerCounts) {
    lines.push(`| ${escapeCell(blocker.blocker)} | ${blocker.count} |`)
  }
  return `${lines.join('\n')}\n`
}

function renderBacklogMarkdown(report, backlog) {
  const lines = []
  lines.push('# AdapterCore Strict Port Backlog')
  lines.push('')
  lines.push(`- Generated: ${report.generatedAt}`)
  lines.push(`- Blocking modules: ${backlog.length}`)
  lines.push('')
  for (const item of backlog) {
    lines.push(`## ${item.moduleId}`)
    lines.push('')
    lines.push(`- Tier: ${item.tier}`)
    lines.push(`- Module dir: ${item.moduleDir}`)
    lines.push(`- Blockers: ${item.blockers.join('; ')}`)
    lines.push(`- Recommended fixes: ${item.recommendedFixes.join('; ') || 'none'}`)
    lines.push('')
  }
  return `${lines.join('\n')}\n`
}

function countBy(items, fn) {
  const counts = {}
  for (const item of items) {
    const key = fn(item)
    counts[key] = (counts[key] ?? 0) + 1
  }
  return counts
}

function linesContaining(text, needle) {
  return text
    .split(/\r?\n/)
    .map((line, index) => ({ line: index + 1, text: line.trim() }))
    .filter((line) => line.text.includes(needle))
    .slice(0, 12)
}

async function fileSet(dir) {
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true })
    return new Set(entries.filter((entry) => entry.isFile()).map((entry) => entry.name))
  } catch (error) {
    if (error.code === 'ENOENT') return new Set()
    throw error
  }
}

async function listFiles(root, predicate = () => true) {
  const files = []
  async function visit(dir) {
    let entries
    try {
      entries = await fs.readdir(dir, { withFileTypes: true })
    } catch (error) {
      if (error.code === 'ENOENT') return
      throw error
    }
    for (const entry of entries) {
      const next = path.join(dir, entry.name)
      if (entry.isDirectory()) {
        await visit(next)
      } else if (entry.isFile() && predicate(next)) {
        files.push(next)
      }
    }
  }
  await visit(root)
  return files
}

async function exists(file) {
  try {
    await fs.access(file)
    return true
  } catch {
    return false
  }
}

async function readJson(file) {
  return JSON.parse(await fs.readFile(file, 'utf8'))
}

async function readTextIfExists(file) {
  try {
    return await fs.readFile(file, 'utf8')
  } catch (error) {
    if (error.code === 'ENOENT') return ''
    throw error
  }
}

async function removeIfExists(file) {
  try {
    await fs.unlink(file)
  } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }
}

function normalizePath(value) {
  return String(value ?? '').replaceAll(path.sep, '/')
}

function escapeCell(value) {
  return String(value).replaceAll('|', '\\|').replace(/\r?\n/g, '<br>')
}

function parseArgs(argv) {
  const options = { repoRoot: process.cwd(), outDir: DEFAULT_OUT_DIR, write: true, strict: false }
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (arg === '--repo-root') {
      options.repoRoot = argv[++i]
    } else if (arg === '--out-dir') {
      options.outDir = argv[++i]
    } else if (arg === '--no-write') {
      options.write = false
    } else if (arg === '--strict') {
      options.strict = true
    } else if (arg === '--help' || arg === '-h') {
      options.help = true
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }
  return options
}

function printHelp() {
  console.log(`Usage: node scripts/generate-adaptercore-strict-port-audit.mjs [--repo-root <path>] [--out-dir <path>] [--no-write] [--strict]

Generates ${SCHEMA} for descriptor-backed ECHO addon modules.`)
}

const isCli = process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
if (isCli) {
  const options = parseArgs(process.argv.slice(2))
  if (options.help) {
    printHelp()
  } else {
    generateAdapterCoreStrictPortAudit(options)
      .then(({ report, outputs }) => {
        console.log(`AdapterCore strict port audit wrote ${outputs.json}`)
        console.log(`AdapterCore strict port audit wrote ${outputs.markdown}`)
        console.log(`AdapterCore strict port audit: ${report.summary.resultCounts.pass}/${report.summary.moduleCount} module(s) passing`)
        if (options.strict && report.summary.resultCounts.fail > 0) {
          throw new Error(`AdapterCore strict port audit failed: ${report.summary.resultCounts.fail} module(s) have blockers.`)
        }
      })
      .catch((error) => {
        console.error(error.stack || error.message)
        process.exitCode = 1
      })
  }
}
