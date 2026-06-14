import assert from 'node:assert/strict'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { generateAdapterCoreStrictPortAudit } from './generate-adaptercore-strict-port-audit.mjs'

const RUNTIMES = ['neoforge', 'echo_native', 'echo_runtime_standalone']

async function main() {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'adaptercore-strict-port-'))
  await writeModule(root, {
    dir: 'echoadaptercore',
    id: 'echoadaptercore',
    requires: [],
    gradle: '',
    toml: toml('echoadaptercore', []),
    java: 'package test; public final class AdapterCoreSelf { String marker = "AdapterCore"; }',
    artifacts: true,
  })
  await writeModule(root, {
    dir: 'echocore',
    id: 'echocore',
    requires: [],
    gradle: '',
    toml: toml('echocore', [{ modId: 'echoadaptercore', type: 'optional', ordering: 'NONE' }]),
    java: 'package test; public final class EchoCoreCompat { String marker = "AdapterCore compatibility"; }',
    artifacts: true,
  })
  await writeModule(root, {
    dir: 'echometadataonly',
    id: 'echometadataonly',
    requires: ['echoadaptercore'],
    gradle: '',
    toml: toml('echometadataonly', []),
    java: '',
    artifacts: false,
  })
  await writeModule(root, {
    dir: 'echocompileonly',
    id: 'echocompileonly',
    requires: ['echoadaptercore'],
    gradle: 'dependencies { compileOnly project(":echoadaptercore") }\n',
    toml: toml('echocompileonly', [{ modId: 'echoadaptercore', type: 'required' }]),
    java: 'package test; public final class CompileOnlyCompat { String marker = "AdapterCore"; }',
    artifacts: true,
  })
  await writeModule(root, {
    dir: 'echosignalos',
    id: 'echosignalos',
    requires: ['echoadaptercore'],
    legacyAlias: true,
    gradle: 'dependencies { implementation project(":echoadaptercore") }\n',
    gradleProperties: 'mod_id=echosignalos\ncontent_namespace=signalos\nmod_version=1.0.0\n',
    toml: toml('echosignalos', [{ modId: 'echoadaptercore', type: 'required' }]),
    java: 'package test; public final class SignalOS { public static final String LOADER_MODID = "echosignalos"; public static final String MODID = "signalos"; }',
    artifacts: true,
  })
  await writeAdapterCoreCatalog(root, ['echocore', 'echocompileonly', 'echosignalos'])

  const { report } = await generateAdapterCoreStrictPortAudit({
    repoRoot: root,
    outDir: 'reports/runtime-parity',
    write: false,
  })

  assert.equal(report.schema, 'echo.adaptercore.strict_port_audit.v1')

  const rows = new Map(report.rows.map((row) => [row.moduleId, row]))
  assert.equal(rows.get('echoadaptercore').result, 'pass')
  assert.equal(rows.get('echoadaptercore').neoforgeTomlDependency.selfDependency, false)

  assert.equal(rows.get('echocore').tier, 'tier0')
  assert.equal(rows.get('echocore').gradleDependency.mode, 'missing')
  assert.equal(rows.get('echocore').result, 'pass')

  const metadataOnly = rows.get('echometadataonly')
  assert.equal(metadataOnly.result, 'fail')
  assertContains(metadataOnly.strictBlockers, 'Gradle dependency on :echoadaptercore is missing')
  assertContains(metadataOnly.strictBlockers, 'NeoForge TOML required dependency on echoadaptercore is missing')
  assertContains(metadataOnly.strictBlockers, 'Java AdapterCore signal or truth-host coverage is missing')
  assert(metadataOnly.strictBlockers.some((blocker) => blocker.includes('compiled neoforge artifact is missing')))

  const compileOnly = rows.get('echocompileonly')
  assert.equal(compileOnly.result, 'fail')
  assert.equal(compileOnly.gradleDependency.mode, 'compileOnly')
  assert.equal(compileOnly.gradleDependency.runtimePresent, false)
  assertContains(compileOnly.strictBlockers, 'Gradle runtime wiring for :echoadaptercore is missing')

  const signalOs = rows.get('echosignalos')
  assert.equal(signalOs.result, 'pass')
  assert.equal(signalOs.signalOs.gradleModIdIsCanonical, true)
  assert.equal(signalOs.signalOs.legacyContentNamespacePreserved, true)
  assert.equal(signalOs.signalOs.legacyAliasEvidence, true)

  await fs.writeFile(
    path.join(root, 'addons', 'echocore', 'src', 'main', 'templates', 'META-INF', 'neoforge.mods.toml'),
    toml('echocore', [{ modId: 'echoadaptercore', type: 'required', ordering: 'NONE' }]),
  )
  const { report: requiredTier0Report } = await generateAdapterCoreStrictPortAudit({
    repoRoot: root,
    outDir: 'reports/runtime-parity',
    write: false,
  })
  const requiredTier0Core = requiredTier0Report.rows.find((row) => row.moduleId === 'echocore')
  assert.equal(requiredTier0Core.result, 'fail')
  assertContains(requiredTier0Core.strictBlockers, 'Tier 0 NeoForge TOML dependency on echoadaptercore must be optional/non-required')

  await fs.writeFile(
    path.join(root, 'addons', 'echocore', 'src', 'main', 'templates', 'META-INF', 'neoforge.mods.toml'),
    toml('echocore', [{ modId: 'echoadaptercore', type: 'optional', ordering: 'AFTER' }]),
  )
  const { report: orderedTier0Report } = await generateAdapterCoreStrictPortAudit({
    repoRoot: root,
    outDir: 'reports/runtime-parity',
    write: false,
  })
  const orderedTier0Core = orderedTier0Report.rows.find((row) => row.moduleId === 'echocore')
  assert.equal(orderedTier0Core.result, 'fail')
  assertContains(orderedTier0Core.strictBlockers, 'Tier 0 NeoForge TOML dependency on echoadaptercore must not impose load ordering')

  console.log('generate-adaptercore-strict-port-audit tests PASS')
}

async function writeModule(root, options) {
  const moduleRoot = path.join(root, 'addons', options.dir)
  const descriptorDir = path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF')
  const tomlDir = path.join(moduleRoot, 'src', 'main', 'templates', 'META-INF')
  const javaDir = path.join(moduleRoot, 'src', 'main', 'java', 'test')
  await fs.mkdir(descriptorDir, { recursive: true })
  await fs.mkdir(tomlDir, { recursive: true })
  await fs.mkdir(javaDir, { recursive: true })
  await fs.writeFile(path.join(descriptorDir, 'echo.mod.json'), `${JSON.stringify(descriptor(options), null, 2)}\n`)
  await fs.writeFile(path.join(tomlDir, 'neoforge.mods.toml'), options.toml)
  await fs.writeFile(path.join(moduleRoot, 'build.gradle'), options.gradle ?? '')
  await fs.writeFile(path.join(moduleRoot, 'gradle.properties'), options.gradleProperties ?? `mod_id=${options.id}\nmod_version=1.0.0\n`)
  if (options.java) {
    await fs.writeFile(path.join(javaDir, `${options.id}.java`), options.java)
  }
  if (options.artifacts) {
    await writeArtifacts(root, options.id)
  }
}

function descriptor(options) {
  const value = {
    schema: 'echo.mod.v1',
    id: options.id,
    name: options.id,
    version: '1.0.0',
    requires: options.requires ?? [],
    access: {
      adapterCore: {
        runtimes: RUNTIMES,
      },
    },
  }
  if (options.legacyAlias) {
    value.legacyAliases = ['signalos']
  }
  return value
}

function toml(owner, deps) {
  const lines = [
    'modLoader="javafml"',
    'loaderVersion="[1,)"',
    '',
    '[[mods]]',
    `modId="${owner}"`,
    'version="1.0.0"',
  ]
  for (const dep of deps) {
    lines.push('')
    lines.push(`[[dependencies.${owner}]]`)
    lines.push(`modId="${dep.modId}"`)
    lines.push(`type="${dep.type}"`)
    lines.push('versionRange="[1.0.0,)"')
    lines.push(`ordering="${dep.ordering ?? 'AFTER'}"`)
    lines.push('side="BOTH"')
  }
  return `${lines.join('\n')}\n`
}

async function writeArtifacts(root, id) {
  const releaseDir = path.join(root, 'dist', 'echo-module-release', id)
  await fs.mkdir(releaseDir, { recursive: true })
  await fs.writeFile(path.join(releaseDir, `${id}-1.0.0-neoforge.jar`), '')
  await fs.writeFile(path.join(releaseDir, `${id}-1.0.0.echo-addon`), '')
  await fs.writeFile(path.join(releaseDir, `${id}-1.0.0-standalone.jar`), '')
  await fs.writeFile(path.join(releaseDir, `${id}-1.0.0-sources.jar`), '')
}

async function writeAdapterCoreCatalog(root, moduleIds) {
  const catalogDir = path.join(root, 'addons', 'echoadaptercore', 'src', 'main', 'java', 'com', 'knoxhack', 'echo', 'adaptercore', 'bridge')
  await fs.mkdir(catalogDir, { recursive: true })
  await fs.writeFile(
    path.join(catalogDir, 'EchoAdapterCoreModuleCompatibilityCatalog.java'),
    `package com.knoxhack.echo.adaptercore.bridge; final class EchoAdapterCoreModuleCompatibilityCatalog { String[] ids = { ${moduleIds.map((id) => `"${id}"`).join(', ')} }; }\n`,
  )
}

function assertContains(values, expected) {
  assert(values.includes(expected), `Expected ${JSON.stringify(values)} to include ${expected}`)
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exitCode = 1
})
