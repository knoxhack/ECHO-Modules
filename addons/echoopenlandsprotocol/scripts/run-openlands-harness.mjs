import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

import { main as generateDistributionApprovalReport } from './generate-openlands-distribution-approval-report.mjs'
import { main as generateFinalReleaseReviewReport } from './generate-openlands-final-release-review-report.mjs'
import { main as generateLauncherExecutionReport } from './generate-openlands-launcher-execution-report.mjs'
import { main as generateRuntimeExecutionReport } from './generate-openlands-runtime-execution-report.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'

const HARNESSES = {
  runtime: {
    displayName: 'runtime execution',
    planPath: ['systems', 'runtime_execution_harness_plan.json'],
    bindingKey: 'scenarioBindings',
    bindingLabel: 'scenarios',
    driverMissingBlocker: 'real_runtime_harness_drivers_missing',
    generator: generateRuntimeExecutionReport,
    reportReadyField: 'publicAlphaReady',
  },
  launcher: {
    displayName: 'launcher execution',
    planPath: ['systems', 'launcher_execution_harness_plan.json'],
    bindingKey: 'flowBindings',
    bindingLabel: 'flows',
    driverMissingBlocker: 'real_launcher_harness_drivers_missing',
    generator: generateLauncherExecutionReport,
    reportReadyField: 'publicAlphaReady',
  },
  finalReview: {
    displayName: 'final release review',
    planPath: ['systems', 'final_release_review_harness_plan.json'],
    bindingKey: 'reviewAreaBindings',
    bindingLabel: 'reviewAreas',
    driverMissingBlocker: 'final_review_harness_drivers_missing',
    generator: generateFinalReleaseReviewReport,
    reportReadyField: 'publicReleaseReady',
  },
  distributionApproval: {
    displayName: 'distribution approval',
    planPath: ['systems', 'distribution_approval_harness_plan.json'],
    bindingKey: 'approvalAreaBindings',
    bindingLabel: 'approvalAreas',
    driverMissingBlocker: 'distribution_approval_harness_drivers_missing',
    generator: generateDistributionApprovalReport,
    reportReadyField: 'publicAlphaReady',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    moduleArtifact: null,
    releaseRoot: null,
    output: null,
    driverManifest: null,
    requireDriverManifest: false,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--module-artifact') args.moduleArtifact = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--driver-manifest') args.driverManifest = argv[++index]
    else if (arg === '--require-driver-manifest') args.requireDriverManifest = true
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

function findModuleRoot(explicitRoot, editionRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = editionRoot ? path.resolve(editionRoot) : process.cwd()
  for (;;) {
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function normalizeDriverIds(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((entry) => {
      if (typeof entry === 'string') return entry
      if (entry && typeof entry === 'object') return entry.id ?? entry.driverSurfaceId ?? entry.name
      return null
    })
    .filter((entry) => typeof entry === 'string' && entry.length > 0)
}

function normalizeDriverSurfaces(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((entry) => {
      if (typeof entry === 'string') {
        return {
          id: entry,
          harnessFamily: null,
          methodsImplemented: [],
          capturesImplemented: [],
        }
      }
      if (entry && typeof entry === 'object') {
        return {
          ...entry,
          id: entry.id ?? entry.driverSurfaceId ?? entry.name,
          harnessFamily: entry.harnessFamily ?? entry.family ?? null,
          methodsImplemented: Array.isArray(entry.methodsImplemented) ? entry.methodsImplemented : [],
          capturesImplemented: Array.isArray(entry.capturesImplemented) ? entry.capturesImplemented : [],
        }
      }
      return null
    })
    .filter((entry) => typeof entry?.id === 'string' && entry.id.length > 0)
}

function loadDriverManifest(driverManifestPath) {
  if (!driverManifestPath) return null
  const manifest = readJson(path.resolve(driverManifestPath))
  const declaredDriverSurfaces = normalizeDriverSurfaces([
    ...(Array.isArray(manifest.availableDriverSurfaces) ? manifest.availableDriverSurfaces : []),
    ...(Array.isArray(manifest.driverSurfaces) ? manifest.driverSurfaces : []),
    ...(Array.isArray(manifest.drivers) ? manifest.drivers : []),
  ])
  return {
    path: path.resolve(driverManifestPath),
    schema: manifest.schema ?? null,
    declaredDriverSurfaces,
    raw: manifest,
  }
}

function defaultDriverManifestPath(editionRoot, edition) {
  const candidate = path.join(editionRoot, 'evidence', `${edition}-harness-driver-manifest.template.json`)
  return fileExists(candidate) ? candidate : null
}

async function invokeGenerator(generator, argv) {
  const originalLog = console.log
  const originalWarn = console.warn
  try {
    console.log = () => {}
    console.warn = () => {}
    return await generator(argv)
  } finally {
    console.log = originalLog
    console.warn = originalWarn
  }
}

function buildGeneratorArgs(kind, args, editionRoot, moduleRoot) {
  const generatorArgs = [
    '--edition',
    args.edition,
    '--edition-root',
    editionRoot,
    '--module-root',
    moduleRoot,
    '--status',
    'blocked',
    '--dry-run',
  ]
  if (args.output) generatorArgs.push('--out', path.resolve(args.output))
  if (args.moduleArtifact && kind !== 'distributionApproval') generatorArgs.push('--module-artifact', path.resolve(args.moduleArtifact))
  if (args.releaseRoot && kind === 'distributionApproval') generatorArgs.push('--release-root', path.resolve(args.releaseRoot))
  return generatorArgs
}

function harnessPlanPath(moduleRoot, config) {
  return path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', ...config.planPath)
}

function includesAll(actual, expected) {
  const actualSet = new Set(actual ?? [])
  return (expected ?? []).every((entry) => actualSet.has(entry))
}

function driverCompleteness(planDriver, manifestDriver) {
  const requiredMethods = planDriver.requiredMethods ?? []
  const requiredCaptures = planDriver.mustCapture ?? []
  const methodsImplemented = manifestDriver?.methodsImplemented ?? []
  const capturesImplemented = manifestDriver?.capturesImplemented ?? []
  const missingMethods = requiredMethods.filter((method) => !methodsImplemented.includes(method))
  const missingCaptures = requiredCaptures.filter((capture) => !capturesImplemented.includes(capture))
  return {
    id: planDriver.id,
    declared: Boolean(manifestDriver),
    complete: Boolean(manifestDriver)
      && includesAll(methodsImplemented, requiredMethods)
      && includesAll(capturesImplemented, requiredCaptures),
    requiredMethodCount: requiredMethods.length,
    implementedMethodCount: methodsImplemented.length,
    missingMethods,
    requiredCaptureCount: requiredCaptures.length,
    implementedCaptureCount: capturesImplemented.length,
    missingCaptures,
  }
}

function buildHarnessRun({ kind, config, args, plan, planPath, driverManifest, editionHarness }) {
  const requiredDrivers = plan.driverSurfaces ?? []
  const requiredDriverSurfaceIds = [...new Set(requiredDrivers.map((driver) => driver.id))].sort()
  const allDeclaredDriverSurfaces = driverManifest?.declaredDriverSurfaces ?? []
  const declaredDriverSurfaces = allDeclaredDriverSurfaces
    .filter((driver) => driver.harnessFamily === kind || driver.harnessFamily === null)
  const ignoredDriverSurfaces = allDeclaredDriverSurfaces
    .filter((driver) => driver.harnessFamily !== kind && driver.harnessFamily !== null)
  const declaredDriverSurfaceIds = [...new Set(declaredDriverSurfaces.map((driver) => driver.id))].sort()
  const ignoredDriverSurfaceIds = [...new Set(ignoredDriverSurfaces.map((driver) => `${driver.harnessFamily}:${driver.id}`))].sort()
  const manifestDriversById = new Map(declaredDriverSurfaces.map((driver) => [driver.id, driver]))
  const driverCompletenessDetails = requiredDrivers
    .map((planDriver) => driverCompleteness(planDriver, manifestDriversById.get(planDriver.id)))
    .sort((left, right) => left.id.localeCompare(right.id))
  const completeDriverSurfaceIds = driverCompletenessDetails
    .filter((driver) => driver.complete)
    .map((driver) => driver.id)
    .sort()
  const incompleteDriverSurfaceIds = driverCompletenessDetails
    .filter((driver) => driver.declared && !driver.complete)
    .map((driver) => driver.id)
    .sort()
  const missingDriverSurfaceIds = requiredDriverSurfaceIds.filter((id) => !completeDriverSurfaceIds.includes(id))
  const bindingCount = (plan[config.bindingKey] ?? []).length
  const blockedBy = []
  if (!driverManifest) blockedBy.push('harness_driver_manifest_missing')
  if (missingDriverSurfaceIds.length > 0) blockedBy.push(config.driverMissingBlocker)
  blockedBy.push('real_harness_execution_not_run')

  return {
    schema: 'echo.openlands.edition.harness_run.v1',
    status: 'blocked',
    generatedAt: new Date().toISOString(),
    generatedBy: `run-openlands-${kind}-harness.mjs`,
    harnessType: kind,
    harnessDisplayName: config.displayName,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    edition: args.edition,
    runtimeTarget: editionHarness?.runtimeTarget ?? null,
    harnessPlan: `data/${MODULE_ID}/openlands/${config.planPath.join('/')}`,
    harnessPlanSchema: plan.schema,
    harnessPlanPath: planPath,
    entryPoint: editionHarness?.entryPoint ?? null,
    driverKind: editionHarness?.driverKind ?? null,
    requiredReport: editionHarness?.requiredReport ?? null,
    artifactPattern: editionHarness?.artifactPattern ?? null,
    driverManifest: driverManifest
      ? {
          path: driverManifest.path,
          schema: driverManifest.schema,
        }
      : null,
    driverSummary: {
      requiredDriverSurfaceCount: requiredDriverSurfaceIds.length,
      declaredDriverSurfaceCount: declaredDriverSurfaceIds.length,
      ignoredDriverSurfaceCount: ignoredDriverSurfaceIds.length,
      completeDriverSurfaceCount: completeDriverSurfaceIds.length,
      incompleteDriverSurfaceCount: incompleteDriverSurfaceIds.length,
      availableDriverSurfaceCount: completeDriverSurfaceIds.length,
      missingDriverSurfaceCount: missingDriverSurfaceIds.length,
      requiredDriverSurfaceIds,
      declaredDriverSurfaceIds,
      ignoredDriverSurfaceIds,
      completeDriverSurfaceIds,
      incompleteDriverSurfaceIds,
      availableDriverSurfaceIds: completeDriverSurfaceIds,
      missingDriverSurfaceIds,
      driverCompletenessDetails,
    },
    bindingSummary: {
      bindingKey: config.bindingKey,
      bindingLabel: config.bindingLabel,
      bindingCount,
    },
    blockedBy,
    nextSteps: [
      'Provide a real driver manifest listing implemented harness driver surface ids.',
      'Replace blocked generator delegation with runtime-specific driver execution.',
      'Attach required captures and saved artifacts from the harness plan.',
      'Only clear gates after every mapped scenario, flow, review area, or approval area passes.',
    ],
  }
}

export async function runHarness(kind, argv = process.argv.slice(2)) {
  const config = HARNESSES[kind]
  if (!config) throw new Error(`Unknown Openlands harness kind: ${kind}`)
  const args = parseArgs(argv)
  if (args.help) {
    printHelp(kind, config)
    return null
  }
  if (!args.edition) throw new Error('--edition is required')

  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const moduleRoot = findModuleRoot(args.moduleRoot, editionRoot)
  const planPath = harnessPlanPath(moduleRoot, config)
  const plan = readJson(planPath)
  const editionHarness = (plan.editionHarnesses ?? []).find((entry) => entry.edition === args.edition)
  if (!editionHarness) throw new Error(`${config.displayName} harness plan missing edition ${args.edition}`)

  const driverManifestPath = args.driverManifest ?? defaultDriverManifestPath(editionRoot, args.edition)
  const driverManifest = driverManifestPath ? loadDriverManifest(driverManifestPath) : null
  if (args.requireDriverManifest && !driverManifest) {
    throw new Error('--require-driver-manifest was set, but no --driver-manifest path was provided')
  }

  const report = await invokeGenerator(config.generator, buildGeneratorArgs(kind, args, editionRoot, moduleRoot))
  report.generatedBy = `run-openlands-${kind}-harness.mjs`
  report.harnessRun = buildHarnessRun({
    kind,
    config,
    args,
    plan,
    planPath,
    driverManifest,
    editionHarness,
  })
  report[config.reportReadyField] = false

  const outputPath = report.outputPath
  if (!args.dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }

  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} ${config.displayName} harness ${action}: blocked, ${report.harnessRun.driverSummary.missingDriverSurfaceCount}/${report.harnessRun.driverSummary.requiredDriverSurfaceCount} driver surfaces missing.`)
  }
  return report
}

function printHelp(kind, config) {
  console.log(`Usage: node run-openlands-${kind}-harness.mjs --edition <native|neoforge|standalone> [options]

Runs the ${config.displayName} harness contract in honest blocked mode until real driver surfaces are implemented.

Options:
  --edition <id>              Edition key: native, neoforge, or standalone.
  --edition-root <path>       Edition repository root. Defaults to cwd.
  --module-root <path>        Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --module-artifact <path>    Runtime/launcher/final-review artifact override when supported.
  --release-root <path>       Distribution release output root when supported.
  --out <path>                Report output path. Defaults to the edition evidence report.
  --driver-manifest <path>    Optional JSON manifest listing available driver surfaces.
                              Defaults to evidence/<edition>-harness-driver-manifest.template.json when present.
  --require-driver-manifest   Fail if --driver-manifest is not supplied.
  --dry-run                   Generate without writing the report.
  --json                      Print JSON output.
  --help                      Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  runHarness('runtime').catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
