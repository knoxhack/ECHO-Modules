import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generateRuntimeParityAudit } from './generate-runtime-parity-audit.mjs'

async function writeModule(repoRoot, directory, descriptor, sources = {}) {
  const moduleRoot = path.join(repoRoot, 'addons', directory)
  const descriptorPath = path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
  await fs.mkdir(path.dirname(descriptorPath), { recursive: true })
  await fs.writeFile(descriptorPath, `${JSON.stringify(descriptor, null, 2)}\n`, 'utf8')
  for (const [className, source] of Object.entries(sources)) {
    const sourcePath = path.join(moduleRoot, 'src', 'main', 'java', `${className.replace(/\./g, path.sep)}.java`)
    await fs.mkdir(path.dirname(sourcePath), { recursive: true })
    await fs.writeFile(sourcePath, source, 'utf8')
  }
}

async function writePackManifest(echoRoot, repoName, moduleIds) {
  const repoRoot = path.join(echoRoot, repoName)
  await fs.mkdir(repoRoot, { recursive: true })
  await fs.writeFile(
    path.join(repoRoot, 'release-manifest.template.json'),
    `${JSON.stringify({
      schema: 'echo.pack.release_manifest.v1',
      moduleArtifactFamily: 'echo-addon',
      moduleRequirements: moduleIds.map((id) => ({ id })),
    }, null, 2)}\n`,
    'utf8',
  )
}

const echoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-runtime-parity-audit-'))
const repoRoot = path.join(echoRoot, 'ECHO-Modules')

try {
  await fs.mkdir(path.join(repoRoot, 'docs'), { recursive: true })
  await fs.writeFile(
    path.join(repoRoot, 'docs', 'module-docs-index.md'),
    '# Module Docs\n\n- [echocore](../addons/echocore/README.md)\n',
    'utf8',
  )

  await writeModule(repoRoot, 'echocore', {
    schema: 'echo.mod.v1',
    id: 'echocore',
    name: 'ECHO Core',
    version: '1.0.0',
    kind: 'library',
    role: 'foundation',
    official: true,
    standalone: true,
    entrypoint: 'com.example.EchoCore',
    access: {
      adapterCore: { runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'] },
      nativeEntrypoint: 'com.example.EchoCoreNativeModule',
    },
  }, {
    'com.example.EchoCore': 'package com.example; public final class EchoCore {}\n',
    'com.example.EchoCoreNativeModule': 'package com.example; public final class EchoCoreNativeModule {}\n',
  })

  await writeModule(repoRoot, 'echoindex', {
    schema: 'echo.mod.v1',
    id: 'echoindex',
    name: 'ECHO Index',
    version: '1.0.0',
    kind: 'ui_pack',
    role: 'inventory_overlay',
    official: true,
    standalone: true,
    entrypoint: 'com.example.EchoIndex',
    provides: ['index.inventory', 'ui.screens', 'recipes'],
    access: {
      adapterCore: {
        runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'],
        domains: ['ui', 'items', 'recipes'],
      },
      nativeEntrypoint: 'com.example.EchoIndexNativeModule',
    },
  }, {
    'com.example.EchoIndex': 'package com.example; public final class EchoIndex {}\n',
    'com.example.EchoIndexNativeModule': [
      'package com.example;',
      'public final class EchoIndexNativeModule {',
      '  void describeNativeSurfaces() {}',
      '  String nativeSurfaceImplementationClass = "com.example.IndexSurface";',
      '  void ensureNativeClientRoutesRegisteredForNativeLoader() {}',
      '}',
      '',
    ].join('\n'),
  })

  await writePackManifest(echoRoot, 'ECHO-Ashfall-Native-Edition', ['echocore'])

  const { report, paths } = await generateRuntimeParityAudit({ repoRoot, echoRoot })

  assert.equal(report.schema, 'echo.module.runtime_parity_audit.v1')
  assert.equal(report.summary.moduleCount, 2)
  assert.equal(report.summary.runtimeRowCount, 6)
  assert.equal(report.summary.preferredPackManifestCount, 1)
  assert.equal(report.strictWouldFail, true)
  assert.equal(report.docsIndex.found, true)
  assert.deepEqual(report.docsIndex.missingModuleIds, ['echoindex'])
  assert.deepEqual(report.docsIndex.missingDirectories, ['echoindex'])

  const indexModule = report.modules.find((module) => module.moduleId === 'echoindex')
  assert.ok(indexModule.expectedFeatures.includes('inventory_overlay'))
  assert.ok(indexModule.expectedFeatures.includes('index'))

  const nativeIndexRow = report.rows.find((row) => row.moduleId === 'echoindex' && row.runtime === 'echo_native')
  assert.equal(nativeIndexRow.uiSurfaceStatus, 'registered-headless')
  assert.equal(nativeIndexRow.actionRouteStatus, 'registered')
  assert.ok(nativeIndexRow.blockers.some((blocker) => blocker.includes('visible/actionable proof')))

  const pack = report.packAudit.preferredManifests[0]
  assert.equal(pack.repo, 'ECHO-Ashfall-Native-Edition')
  assert.ok(pack.missingVisibleCoreSurfaceModules.includes('echoindex'))
  assert.ok(pack.missingContentBaselineModules.includes('echoblockworks'))

  assert.ok(report.backlog.some((item) => item.priority === 'P0' && item.ownerRepo === 'ECHO-Native-Platform'))
  assert.ok(report.backlog.some((item) => item.priority === 'P1' && item.subsystem === 'docs index'))

  assert.equal(JSON.parse(await fs.readFile(paths.json, 'utf8')).summary.runtimeRowCount, 6)
  assert.match(await fs.readFile(paths.markdown, 'utf8'), /ECHO Module Runtime Parity Audit/)
  assert.match(await fs.readFile(paths.backlog, 'utf8'), /ECHO Runtime Parity Fix Backlog/)
} finally {
  await fs.rm(echoRoot, { recursive: true, force: true })
}

console.log('generate-runtime-parity-audit tests passed')
