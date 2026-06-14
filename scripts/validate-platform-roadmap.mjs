import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { FOUNDATION_NATIVE_MODULES, ROADMAP_MODULES } from './generate-platform-roadmap-modules.mjs'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const errors = []

function absolute(relativePath) {
  return path.join(repoRoot, relativePath)
}

function exists(relativePath) {
  return fs.existsSync(absolute(relativePath))
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), 'utf8'))
}

function requireFile(relativePath) {
  if (!exists(relativePath)) errors.push(`Missing required file: ${relativePath}`)
}

function requireTextIncludes(relativePath, needle) {
  requireFile(relativePath)
  if (!exists(relativePath)) return
  const text = fs.readFileSync(absolute(relativePath), 'utf8')
  if (!text.includes(needle)) errors.push(`${relativePath} missing required text: ${needle}`)
}

function descriptorPath(moduleId) {
  return `addons/${moduleId}/src/main/resources/META-INF/echo.mod.json`
}

function classToPath(className) {
  return className.replaceAll('.', '/') + '.java'
}

function requireArrayContainsAll(owner, actual, expected) {
  for (const value of expected) {
    if (!actual.includes(value)) errors.push(`${owner} missing ${value}`)
  }
}

function validateFoundationNativeEntrypoints() {
  for (const module of FOUNDATION_NATIVE_MODULES) {
    const relativeDescriptor = descriptorPath(module.id)
    requireFile(relativeDescriptor)
    if (!exists(relativeDescriptor)) continue
    const descriptor = readJson(relativeDescriptor)
    const entrypointPackage = descriptor.entrypoint.split('.').slice(0, -1).join('.')
    const nativeEntrypoint = `${entrypointPackage}.${module.nativeClassName}`
    if (descriptor.access?.nativeEntrypoint !== nativeEntrypoint) {
      errors.push(`${module.id} access.nativeEntrypoint expected ${nativeEntrypoint}, got ${descriptor.access?.nativeEntrypoint}`)
    }
    const nativePath = `addons/${module.id}/src/main/java/${classToPath(nativeEntrypoint)}`
    requireFile(nativePath)
    if (exists(nativePath)) {
      const text = fs.readFileSync(absolute(nativePath), 'utf8')
      if (!text.includes('implements EchoNativeSurfaceModuleEntrypoint')) {
        errors.push(`${nativePath} does not implement EchoNativeSurfaceModuleEntrypoint`)
      }
      const registryMutatedFalse = text.includes('registryMutated", false')
      const registryMutatedTrue = text.includes('registryMutated", true')
      if (!registryMutatedFalse && !registryMutatedTrue) {
        errors.push(`${nativePath} does not report registryMutated false or true`)
      }
      if (!text.includes('transformsPerformed", false')) {
        errors.push(`${nativePath} does not report transformsPerformed false`)
      }
    }
  }
}

function validateRoadmapModules() {
  for (const module of ROADMAP_MODULES) {
    const base = `addons/${module.id}`
    const relativeDescriptor = descriptorPath(module.id)
    const contractPath = `${base}/src/main/resources/data/${module.id}/roadmap/contracts.json`
    requireFile(`${base}/README.md`)
    requireFile(`${base}/docs/artifacts.md`)
    requireFile(`${base}/build.gradle`)
    requireFile(`${base}/gradle.properties`)
    requireFile(`${base}/src/main/templates/META-INF/neoforge.mods.toml`)
    requireFile(contractPath)
    requireFile(relativeDescriptor)
    if (!exists(relativeDescriptor)) continue

    const descriptor = readJson(relativeDescriptor)
    const expectedEntrypoint = `com.knoxhack.echo.${module.id.replace(/^echo/, '')}.${module.className}`
    const expectedNativeEntrypoint = `com.knoxhack.echo.${module.id.replace(/^echo/, '')}.${module.nativeClassName}`
    const expectedRequires = [...new Set(['echocore', 'echoadaptercore', ...module.requires])]

    if (descriptor.id !== module.id) errors.push(`${module.id} descriptor id mismatch: ${descriptor.id}`)
    if (descriptor.version !== '0.1.0') errors.push(`${module.id} version must be 0.1.0`)
    if (descriptor.channel !== 'alpha') errors.push(`${module.id} channel must be alpha`)
    if (descriptor.apiStability !== 'alpha') errors.push(`${module.id} apiStability must be alpha`)
    if (descriptor.official !== true) errors.push(`${module.id} official must be true`)
    if (descriptor.trustLevel !== 'official') errors.push(`${module.id} trustLevel must be official`)
    if (descriptor.side !== 'common') errors.push(`${module.id} side must be common`)
    if (descriptor.standalone !== true) errors.push(`${module.id} standalone must be true`)
    if (descriptor.entrypoint !== expectedEntrypoint) errors.push(`${module.id} entrypoint expected ${expectedEntrypoint}`)
    if (descriptor.access?.nativeEntrypoint !== expectedNativeEntrypoint) errors.push(`${module.id} nativeEntrypoint expected ${expectedNativeEntrypoint}`)
    if (descriptor.role !== module.role) errors.push(`${module.id} role expected ${module.role}`)
    if (descriptor.role.includes('/')) errors.push(`${module.id} role contains slash`)
    requireArrayContainsAll(`${module.id} requires`, descriptor.requires ?? [], expectedRequires)
    requireArrayContainsAll(`${module.id} provides`, descriptor.provides ?? [], module.provides)
    requireArrayContainsAll(`${module.id} consumes`, descriptor.consumes ?? [], module.consumes)
    if (descriptor.access?.requiresConfirmationForWriteActions !== true) {
      errors.push(`${module.id} must keep write actions behind confirmation`)
    }

    const nativePath = `addons/${module.id}/src/main/java/${classToPath(expectedNativeEntrypoint)}`
    requireFile(nativePath)
    if (exists(nativePath)) {
      const text = fs.readFileSync(absolute(nativePath), 'utf8')
      for (const required of [
        'activated", true',
        'adapterCoreUsed", true',
        'nativeAdapterCodeExecuted", true',
        'registeredFeatureContracts"',
        'logicalRegistrationCount"',
        'referenceProbe"',
        'registryMutated",',
        'transformsPerformed", false',
      ]) {
        if (!text.includes(required)) errors.push(`${nativePath} missing native output field ${required}`)
      }
    }

    const contract = readJson(`${base}/src/main/resources/data/${module.id}/roadmap/contracts.json`)
    const isRuntimeReady = contract.status === 'runtime-ready'
    requireTextIncludes(`${base}/README.md`, '## Review Status')
    requireTextIncludes(`${base}/README.md`, '## Native Probe')
    requireTextIncludes(`${base}/README.md`, '## Contract Boundary')
    if (isRuntimeReady) {
      requireTextIncludes(`${base}/README.md`, 'Runtime implementation is present.')
    } else {
      requireTextIncludes(`${base}/README.md`, 'It is not a finished gameplay/runtime implementation.')
    }
    if (isRuntimeReady) {
      requireTextIncludes(`${base}/docs/artifacts.md`, 'Status: Runtime Ready.')
    } else {
      requireTextIncludes(`${base}/docs/artifacts.md`, 'Status: Not Player Ready.')
    }
    requireTextIncludes(`${base}/docs/artifacts.md`, '## Review Checklist')
    if (contract.schema !== 'echo.platform_roadmap.module_contract.v1') {
      errors.push(`${module.id} roadmap contract schema mismatch`)
    }
    if (contract.status !== 'contract-first' && contract.status !== 'runtime-ready') {
      errors.push(`${module.id} roadmap contract status must be 'contract-first' or 'runtime-ready'`)
    }
    const boundary = String(contract.implementationBoundary ?? '')
    if (!isRuntimeReady && !boundary.includes('Gameplay/runtime mutation must be added in later implementation phases.')) {
      errors.push(`${module.id} roadmap contract implementation boundary is not explicit`)
    }
    if (isRuntimeReady && !boundary.includes('Runtime implementation is present')) {
      errors.push(`${module.id} runtime-ready roadmap contract must state that runtime implementation is present`)
    }
    requireArrayContainsAll(`${module.id} mvpContracts`, contract.mvpContracts ?? [], module.mvpContracts)
  }
}

function validateDescriptorHygiene() {
  const addonApi = readJson('addons/echoaddonapi/src/main/resources/META-INF/echo.mod.json')
  if (addonApi.channel !== 'experimental') errors.push('echoaddonapi channel must be experimental')

  const galactic = readJson('addons/echogalacticcore/src/main/resources/META-INF/echo.mod.json')
  if (galactic.apiStability !== 'beta') errors.push('echogalacticcore apiStability must be beta')

  const addonDirs = fs.readdirSync(absolute('addons'), { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
  for (const addon of addonDirs) {
    const file = descriptorPath(addon)
    if (!exists(file)) continue
    const descriptor = readJson(file)
    if (typeof descriptor.role === 'string' && descriptor.role.includes('/')) {
      errors.push(`${addon} role contains slash: ${descriptor.role}`)
    }
    if (typeof descriptor.apiStability === 'string' && descriptor.apiStability !== descriptor.apiStability.toLowerCase()) {
      errors.push(`${addon} apiStability must be lower-case: ${descriptor.apiStability}`)
    }
    if (descriptor.channel === '') errors.push(`${addon} channel must not be blank`)
  }
}

function validateBundles() {
  for (const bundleId of ['foundation', 'openlands_official', 'sky_relay_official', 'deep_reach_official', 'arcana_division', 'creator_tooling']) {
    const file = `metadata/bundles/${bundleId}.json`
    requireFile(file)
    if (!exists(file)) continue
    const bundle = readJson(file)
    if (!Array.isArray(bundle.modules) || bundle.modules.length === 0) {
      errors.push(`${bundleId} bundle has no modules`)
    }
    if (bundle.docsPath !== 'docs/MODULE_BUNDLES.md') {
      errors.push(`${bundleId} docsPath must point at docs/MODULE_BUNDLES.md`)
    }
  }
  requireFile('docs/MODULE_BUNDLES.md')
  requireFile('docs/ECHO_PLATFORM_ROADMAP.md')
  requireTextIncludes('docs/ECHO_PLATFORM_ROADMAP.md', '## Review Baseline')
  requireTextIncludes('docs/ECHO_PLATFORM_ROADMAP.md', '## Contract Boundary')
  requireTextIncludes('docs/ECHO_PLATFORM_ROADMAP.md', '## Review Commands')
  requireTextIncludes('docs/ECHO_PLATFORM_ROADMAP.md', 'Module catalog baseline: 133 descriptors after roadmap generation.')
  requireTextIncludes('docs/ECHO_PLATFORM_ROADMAP.md', 'They do not mutate runtime state, register gameplay content, execute server operations, or claim completed player-facing loops.')
}

function validateIndex() {
  const index = readJson('metadata/modules/index.json')
  const ids = new Set(index.modules.map((module) => module.id))
  for (const module of ROADMAP_MODULES) {
    if (!ids.has(module.id)) errors.push(`metadata/modules/index.json missing ${module.id}`)
  }
  if (index.moduleCount !== 133) {
    errors.push(`metadata/modules/index.json moduleCount expected 133, got ${index.moduleCount}`)
  }
}

validateFoundationNativeEntrypoints()
validateRoadmapModules()
validateDescriptorHygiene()
validateBundles()
validateIndex()

if (errors.length > 0) {
  console.error('Platform roadmap validation failed:')
  for (const error of errors) console.error(`- ${error}`)
  process.exit(1)
}

console.log(`Platform roadmap validation passed for ${ROADMAP_MODULES.length} module(s).`)
