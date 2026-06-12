import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json'
const EDITIONS = new Map([
  ['native', { runtimeTarget: 'echo_native', repo: 'ECHO-Openlands-Native-Edition', artifactPattern: 'echoopenlandsprotocol-0.1.0.echo-addon' }],
  ['neoforge', { runtimeTarget: 'neoforge', repo: 'ECHO-Openlands-NeoForge-Edition', artifactPattern: 'echoopenlandsprotocol-0.1.0-neoforge.jar' }],
  ['standalone', { runtimeTarget: 'echo_runtime_standalone', repo: 'ECHO-Openlands-Standalone-Edition', artifactPattern: 'echoopenlandsprotocol-0.1.0-standalone.jar' }],
])

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    edition: null,
    editionRoot: null,
    manifest: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--manifest') args.manifest = argv[++index]
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

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sortedUnique(actual)) === JSON.stringify(sortedUnique(expected))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function loadPlan(moduleRoot, planPath) {
  return readJson(path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', planPath))
}

function requireFields(errors, object, fields, label) {
  for (const field of fields) {
    assert(errors, object?.[field] !== undefined && object?.[field] !== null && object?.[field] !== '', `${label} missing ${field}`)
  }
}

function stableHarnessDriverManifest(manifest) {
  if (!manifest || typeof manifest !== 'object') return manifest
  const { generatedAt, ...stableManifest } = manifest
  return stableManifest
}

function runGeneratorJson({ moduleRoot, editionRoot, edition, manifestPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-harness-driver-manifest.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--module-root',
    moduleRoot,
    '--edition',
    edition,
    '--edition-root',
    editionRoot,
    '--out',
    manifestPath,
    '--dry-run',
    '--json',
  ], {
    cwd: moduleRoot,
    encoding: 'utf8',
    shell: false,
  })
  if (result.status !== 0) {
    return {
      error: `harness driver manifest generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `harness driver manifest generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function validate({ moduleRoot, edition, editionRoot, manifestPath }) {
  const errors = []
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const manifest = readJson(manifestPath)
  const manifestEdition = edition ?? manifest.edition
  const editionConfig = EDITIONS.get(manifestEdition)
  const harnessPlans = new Map()
  for (const family of contract.harnessFamilies ?? []) {
    harnessPlans.set(family.id, loadPlan(moduleRoot, family.plan))
  }

  assert(errors, manifest.schema === contract.reportContract?.schema, 'harness driver manifest schema mismatch')
  assert(errors, contract.reportContract?.allowedStatus?.includes(manifest.status), 'harness driver manifest status is not allowed')
  assert(errors, manifest.moduleId === MODULE_ID, 'harness driver manifest module id mismatch')
  assert(errors, manifest.moduleVersion === VERSION, 'harness driver manifest version mismatch')
  assert(errors, manifest.harnessDriverManifestContract === CONTRACT_PATH, 'harness driver manifest contract path mismatch')
  assert(errors, editionConfig !== undefined, `unknown harness driver manifest edition ${edition ?? manifest.edition}`)
  if (editionConfig) {
    assert(errors, manifest.runtimeTarget === editionConfig.runtimeTarget, 'harness driver manifest runtime target mismatch')
    assert(errors, manifest.artifactPattern === editionConfig.artifactPattern, 'harness driver manifest artifact pattern mismatch')
  }
  for (const field of contract.reportContract?.requiredTopLevelFields ?? []) {
    assert(errors, manifest[field] !== undefined && manifest[field] !== null, `harness driver manifest missing ${field}`)
  }
  for (const [key, value] of Object.entries(contract.sourceContracts ?? {})) {
    assert(errors, manifest.sourceContracts?.[key] === value, `harness driver manifest source contract ${key} mismatch`)
  }

  const familyById = new Map((manifest.harnessFamilies ?? []).map((family) => [family.id, family]))
  assert(errors, sameSet((manifest.harnessFamilies ?? []).map((family) => family.id), (contract.harnessFamilies ?? []).map((family) => family.id)), 'harness driver manifest family ids mismatch')

  const availableByFamily = new Map()
  const availableFlat = []
  for (const driver of manifest.availableDriverSurfaces ?? []) {
    requireFields(errors, driver, contract.reportContract?.availableDriverSurfaceFields ?? [], `available driver ${driver.harnessFamily}:${driver.id}`)
    availableFlat.push(`${driver.harnessFamily}:${driver.id}`)
    if (!availableByFamily.has(driver.harnessFamily)) availableByFamily.set(driver.harnessFamily, [])
    availableByFamily.get(driver.harnessFamily).push(driver.id)
    const plan = harnessPlans.get(driver.harnessFamily)
    const surface = (plan?.driverSurfaces ?? []).find((entry) => entry.id === driver.id)
    assert(errors, surface !== undefined, `available driver ${driver.harnessFamily}:${driver.id} not found in harness plan`)
    if (surface) {
      assert(errors, sameSet(driver.methodsImplemented, surface.requiredMethods), `available driver ${driver.harnessFamily}:${driver.id} methods do not match plan`)
      assert(errors, sameSet(driver.capturesImplemented, surface.mustCapture), `available driver ${driver.harnessFamily}:${driver.id} captures do not match plan`)
    }
    assert(errors, typeof driver.adapterClassOrEntrypoint === 'string' && driver.adapterClassOrEntrypoint.length > 0, `available driver ${driver.harnessFamily}:${driver.id} missing adapter entrypoint`)
    assert(errors, typeof driver.driverVersion === 'string' && driver.driverVersion.length > 0, `available driver ${driver.harnessFamily}:${driver.id} missing driver version`)
    assert(errors, typeof driver.adapterCommit === 'string' && driver.adapterCommit.length > 0, `available driver ${driver.harnessFamily}:${driver.id} missing adapter commit`)
    assert(errors, typeof driver.evidenceRoot === 'string' && driver.evidenceRoot.length > 0, `available driver ${driver.harnessFamily}:${driver.id} missing evidence root`)
  }
  assert(errors, availableFlat.length === sortedUnique(availableFlat).length, 'harness driver manifest available driver ids must be unique by family')

  const expectedFlatMissing = []
  for (const contractFamily of contract.harnessFamilies ?? []) {
    const family = familyById.get(contractFamily.id)
    assert(errors, family !== undefined, `harness driver manifest missing family ${contractFamily.id}`)
    if (!family) continue
    requireFields(errors, family, contract.reportContract?.familyFields ?? [], `harness family ${contractFamily.id}`)
    const plan = harnessPlans.get(contractFamily.id)
    assert(errors, family.plan === contractFamily.plan, `harness family ${contractFamily.id} plan mismatch`)
    assert(errors, family.bindingKey === contractFamily.bindingKey, `harness family ${contractFamily.id} binding key mismatch`)
    assert(errors, sameSet(family.requiredDriverSurfaceIds, contractFamily.requiredDriverSurfaceIds), `harness family ${contractFamily.id} required driver ids mismatch`)
    assert(errors, sameSet(family.requiredBindingIds, contractFamily.requiredBindingIds), `harness family ${contractFamily.id} binding ids mismatch`)
    assert(errors, sameSet(family.requiredBindingIds, (plan?.[contractFamily.bindingKey] ?? []).map((binding) => binding.id)), `harness family ${contractFamily.id} binding ids must match harness plan`)
    const expectedAvailable = sortedUnique(availableByFamily.get(contractFamily.id) ?? [])
    const expectedMissing = sortedUnique((contractFamily.requiredDriverSurfaceIds ?? []).filter((id) => !expectedAvailable.includes(id)))
    assert(errors, sameSet(family.availableDriverSurfaceIds, expectedAvailable), `harness family ${contractFamily.id} available driver ids mismatch`)
    assert(errors, sameSet(family.missingDriverSurfaceIds, expectedMissing), `harness family ${contractFamily.id} missing driver ids mismatch`)
    for (const id of expectedMissing) expectedFlatMissing.push(`${contractFamily.id}:${id}`)
    const expectedStatus = expectedAvailable.length === 0
      ? 'template_blocked'
      : expectedMissing.length === 0
        ? 'implementation_ready'
        : 'implementation_partial'
    assert(errors, family.status === expectedStatus, `harness family ${contractFamily.id} status mismatch`)
    if (expectedMissing.length > 0) {
      assert(errors, family.blockedBy?.includes(contractFamily.driverMissingBlocker), `harness family ${contractFamily.id} missing blocker ${contractFamily.driverMissingBlocker}`)
    }
    assert(errors, family.blockedBy?.includes('real_harness_execution_not_run'), `harness family ${contractFamily.id} missing real execution blocker`)
  }

  const actualFlatMissing = (manifest.missingDriverSurfaces ?? []).map((entry) => `${entry.harnessFamily}:${entry.id}`)
  assert(errors, sameSet(actualFlatMissing, expectedFlatMissing), 'harness driver manifest flat missing drivers mismatch')

  const blockedTemplateStatus = contract.blockedTemplateRules?.familyStatus ?? 'template_blocked'
  if (manifest.status === blockedTemplateStatus) {
    assert(errors, (manifest.availableDriverSurfaces ?? []).length === 0, 'template_blocked manifest must not list available drivers')
    for (const blocker of contract.blockedTemplateRules?.requiredBlockedBy ?? []) {
      assert(errors, manifest.blockedBy?.includes(blocker), `template_blocked manifest missing blocker ${blocker}`)
    }
    for (const nextStep of contract.blockedTemplateRules?.requiredNextSteps ?? []) {
      assert(errors, manifest.nextSteps?.includes(nextStep), `template_blocked manifest missing next step ${nextStep}`)
    }
  }
  if (manifest.status === 'implementation_partial') {
    assert(errors, (manifest.availableDriverSurfaces ?? []).length > 0, 'implementation_partial manifest must list available drivers')
    assert(errors, (manifest.missingDriverSurfaces ?? []).length > 0, 'implementation_partial manifest must still list missing drivers')
    assert(errors, manifest.blockedBy?.includes('real_harness_execution_not_run'), 'implementation_partial manifest must remain blocked until execution')
  }
  if (manifest.status === 'implementation_ready') {
    assert(errors, (manifest.availableDriverSurfaces ?? []).length > 0, 'implementation_ready manifest must list available drivers')
    assert(errors, (manifest.missingDriverSurfaces ?? []).length === 0, 'implementation_ready manifest must have no missing drivers')
    assert(errors, manifest.blockedBy?.includes('real_harness_execution_not_run'), 'implementation_ready manifest must still require real harness execution')
  }
  if (manifest.status === blockedTemplateStatus && manifestEdition && editionConfig) {
    const generated = runGeneratorJson({
      moduleRoot,
      editionRoot,
      edition: manifestEdition,
      manifestPath,
    })
    if (generated.error) {
      errors.push(generated.error)
    } else {
      assert(
        errors,
        sameJson(stableHarnessDriverManifest(manifest), stableHarnessDriverManifest(generated.json)),
        'harness driver manifest stale against generator dry-run',
      )
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    manifestPath,
    manifestStatus: manifest.status,
    edition: manifest.edition,
    runtimeTarget: manifest.runtimeTarget,
    availableDriverSurfaceCount: manifest.availableDriverSurfaces?.length ?? 0,
    missingDriverSurfaceCount: manifest.missingDriverSurfaces?.length ?? 0,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const edition = args.edition
  const editionConfig = edition ? EDITIONS.get(edition) : null
  const editionRoot = args.editionRoot
    ? path.resolve(args.editionRoot)
    : editionConfig
      ? path.join(workspaceRoot, editionConfig.repo)
      : process.cwd()
  const manifestPath = args.manifest
    ? path.resolve(args.manifest)
    : edition
      ? path.join(editionRoot, 'evidence', `${edition}-harness-driver-manifest.template.json`)
      : null
  if (!manifestPath) throw new Error('--manifest is required when --edition is not supplied')
  const result = validate({ moduleRoot, edition, editionRoot, manifestPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands harness driver manifest validated: edition=${result.edition}, status=${result.manifestStatus}, missing=${result.missingDriverSurfaceCount}.`)
  } else {
    console.error(`Openlands harness driver manifest failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-harness-driver-manifest.mjs [options]

Options:
  --module-root <path>   Openlands module root. Auto-detected by default.
  --edition <id>         Edition key: native, neoforge, or standalone.
  --edition-root <path>  Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --manifest <path>      Manifest path. Defaults to evidence/<edition>-harness-driver-manifest.template.json when --edition is supplied.
  --json                 Print JSON output.
  --help                 Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
