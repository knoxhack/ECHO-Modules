import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(import.meta.dirname, '..')
const errors = []

const protocolModule = 'echoarcanadivisionprotocol'
const betaVersion = '1.0.0'
const forbiddenExperienceDeps = new Set(['echoashfallprotocol', 'echoopenlandsprotocol', 'echoskyrelayprotocol'])

const coreModules = [
  'echocore',
  'echoadaptercore',
  'echonetcore',
]

const foundationModules = [
  'echofoundationcore',
  'echomaterialcore',
  'echotoolcore',
  'echostationcore',
  'echoworldstarter',
  'echocommonloot',
  'echocreatureroles',
]

const arcanaModules = [
  'echoarcanacore',
  'echoaetherworks',
  'echocursecore',
  'echofamiliarcore',
  'echogrimoire',
  'echoriftworlds',
  'echoritualcore',
  'echospellcore',
]

const launcherSupportModules = [
  'echoholomap',
  'echoindex',
  'echolens',
  'echoterminal',
  'echothemecore',
  'echomissioncore',
]

const arcanaDivisionRuntimeModules = [
  ...coreModules,
  ...foundationModules,
  ...arcanaModules,
  ...launcherSupportModules,
]

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(path.join(root, relativePath), 'utf8'))
}

function requireFile(relativePath) {
  if (!fs.existsSync(path.join(root, relativePath))) {
    errors.push(`Missing required file: ${relativePath}`)
    return false
  }
  return true
}

function descriptor(moduleId) {
  const relativePath = `addons/${moduleId}/src/main/resources/META-INF/echo.mod.json`
  if (!requireFile(relativePath)) return null
  return readJson(relativePath)
}

function requireArrayExact(actual, expected, label) {
  const missing = expected.filter((item) => !actual.includes(item))
  const extra = actual.filter((item) => !expected.includes(item))
  if (missing.length) errors.push(`${label} missing: ${missing.join(', ')}`)
  if (extra.length) errors.push(`${label} has unexpected entries: ${extra.join(', ')}`)
}

function requireNoForbiddenDeps(moduleId, manifest) {
  for (const dependency of [...(manifest.requires ?? []), ...(manifest.optional ?? [])]) {
    const id = typeof dependency === 'string' ? dependency : dependency?.id
    if (forbiddenExperienceDeps.has(id)) {
      errors.push(`${moduleId} illegally depends on experience pack ${id}`)
    }
  }
}

const protocol = descriptor(protocolModule)
if (protocol) {
  if (protocol.version !== betaVersion) errors.push(`${protocolModule} version must be ${betaVersion}, got ${protocol.version}`)
  if (protocol.channel !== 'beta') errors.push(`${protocolModule} channel must be beta, got ${protocol.channel}`)
  if (protocol.apiStability !== 'beta') errors.push(`${protocolModule} apiStability must be beta, got ${protocol.apiStability}`)
  if (protocol.kind !== 'pack_root') errors.push(`${protocolModule} kind must be pack_root`)
  if (protocol.role !== 'official_pack') errors.push(`${protocolModule} role must be official_pack`)
  requireArrayExact(protocol.requires ?? [], arcanaDivisionRuntimeModules, `${protocolModule} requires`)
  if ((protocol.optional ?? []).length !== 0) errors.push(`${protocolModule} optional dependencies must be empty for beta pack-root validation`)
  requireNoForbiddenDeps(protocolModule, protocol)
}

for (const moduleId of arcanaModules) {
  const manifest = descriptor(moduleId)
  if (!manifest) continue
  if (manifest.version !== betaVersion) errors.push(`${moduleId} version must be ${betaVersion}, got ${manifest.version}`)
  if (manifest.channel !== 'beta') errors.push(`${moduleId} channel must be beta, got ${manifest.channel}`)
  if (manifest.apiStability !== 'beta') errors.push(`${moduleId} apiStability must be beta, got ${manifest.apiStability}`)
  requireNoForbiddenDeps(moduleId, manifest)
}

for (const moduleId of launcherSupportModules) {
  const manifest = descriptor(moduleId)
  if (!manifest) continue
  if (!manifest.version) errors.push(`${moduleId} must declare a version`)
  if (!['beta', 'stable'].includes(manifest.channel)) errors.push(`${moduleId} channel must be beta or stable, got ${manifest.channel}`)
  if (!['beta', 'stable'].includes(manifest.apiStability)) errors.push(`${moduleId} apiStability must be beta or stable, got ${manifest.apiStability}`)
  requireNoForbiddenDeps(moduleId, manifest)
}

const gatesPath = `addons/${protocolModule}/src/main/resources/data/${protocolModule}/arcana_division/contracts/release_gates.json`
if (requireFile(gatesPath)) {
  const gates = readJson(gatesPath)
  if (gates.channel !== 'beta') errors.push('Arcana release gates channel must be beta')
  if (gates.apiStability !== 'beta') errors.push('Arcana release gates apiStability must be beta')
  if (!Array.isArray(gates.requiredBeforeBeta) || gates.requiredBeforeBeta.length < 5) {
    errors.push('Arcana release gates must define requiredBeforeBeta with release criteria')
  }
  if (!(gates.evidence ?? []).includes('scripts/validate-arcana-division-beta.mjs')) {
    errors.push('Arcana release gates must cite scripts/validate-arcana-division-beta.mjs as evidence')
  }
}

if (arcanaDivisionRuntimeModules.length !== 24) {
  errors.push(`Arcana Division runtime module set must contain 24 modules, got ${arcanaDivisionRuntimeModules.length}`)
}

if (new Set(arcanaDivisionRuntimeModules).size !== arcanaDivisionRuntimeModules.length) {
  errors.push('Arcana Division runtime module set contains duplicate module IDs')
}

if (errors.length) {
  console.error('Arcana Division beta validation failed:')
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log(`Arcana Division beta validation passed for ${arcanaDivisionRuntimeModules.length} runtime module requirement(s).`)
