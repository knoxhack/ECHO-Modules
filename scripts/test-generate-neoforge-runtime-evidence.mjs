#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generateNeoForgeRuntimeEvidence } from './generate-neoforge-runtime-evidence.mjs'

async function writeJson(root, relativePath, value) {
  const target = path.join(root, relativePath)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function readJson(root, relativePath) {
  return JSON.parse(await fs.readFile(path.join(root, relativePath), 'utf8'))
}

async function writeModule(root, directory, descriptor, sources) {
  const moduleRoot = path.join(root, 'addons', directory)
  await writeJson(moduleRoot, 'src/main/resources/META-INF/echo.mod.json', descriptor)
  for (const [className, source] of Object.entries(sources)) {
    const sourcePath = path.join(moduleRoot, 'src/main/java', `${className.replace(/\./g, path.sep)}.java`)
    await fs.mkdir(path.dirname(sourcePath), { recursive: true })
    await fs.writeFile(sourcePath, source, 'utf8')
  }
  const artifactName = `${descriptor.id}-${descriptor.version}-neoforge.jar`
  const artifactPath = path.join(moduleRoot, 'build', 'libs', artifactName)
  await fs.mkdir(path.dirname(artifactPath), { recursive: true })
  await fs.writeFile(artifactPath, 'compiled-neoforge-jar-fixture', 'utf8')
}

const repoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-neoforge-runtime-evidence-'))

try {
  await writeModule(repoRoot, 'echocore', {
    schema: 'echo.mod.v1',
    id: 'echocore',
    name: 'ECHO Core',
    version: '1.0.0',
    kind: 'library',
    role: 'foundation',
    entrypoint: 'com.example.EchoCore',
    access: {
      adapterCore: {
        runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'],
        domains: ['data'],
      },
    },
  }, {
    'com.example.EchoCore': 'package com.example; public final class EchoCore {}\n',
  })

  await writeModule(repoRoot, 'echoindex', {
    schema: 'echo.mod.v1',
    id: 'echoindex',
    name: 'ECHO Index',
    version: '1.0.0',
    kind: 'ui_pack',
    role: 'inventory_overlay',
    entrypoint: 'com.example.EchoIndex',
    provides: ['index.inventory', 'ui.screens', 'recipes'],
    access: {
      adapterCore: {
        runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'],
        domains: ['ui', 'items', 'recipes'],
      },
    },
  }, {
    'com.example.EchoIndex': 'package com.example; public final class EchoIndex { Object screen; Object item; }\n',
  })

  await generateNeoForgeRuntimeEvidence({ repoRoot })

  const missingUi = await readJson(repoRoot, 'reports/runtime-parity/neoforge-client-ui-results.json')
  assert.equal(missingUi.status, 'PARTIAL')
  assert.ok(missingUi.blockers.some((blocker) => blocker.includes('missing executed NeoForge strict-play input')))

  await writeJson(repoRoot, 'reports/neoforge-strict-play/neoforge-live-play-evidence.json', {
    schema: 'echo.neoforge.executed_live_play_evidence.v1',
    status: 'PASS',
    moduleIds: ['echocore', 'echoindex'],
    trustedMutations: ['NeoForge live session opened Index and mutated player inventory state.'],
    visibleRoutes: ['echoindex:index'],
    saveEvidence: ['NeoForge live session save/reload preserved Index state.'],
    networkEvidence: ['NeoForge live session received server sync acknowledgement.'],
  })
  await writeJson(repoRoot, 'reports/neoforge-strict-play/neoforge-gametest-results.json', {
    schema: 'echo.neoforge.executed_gametest_results.v1',
    status: 'PASS',
    moduleIds: ['echoindex'],
    trustedMutations: ['NeoForge GameTest placed and used an Index-owned item.'],
  })
  await writeJson(repoRoot, 'reports/neoforge-strict-play/neoforge-registry-content-results.json', {
    schema: 'echo.neoforge.executed_registry_content_results.v1',
    status: 'PASS',
    moduleIds: ['echoindex'],
    trustedMutations: ['NeoForge runtime registry dump included Index item and recipe content.'],
  })
  await writeJson(repoRoot, 'reports/neoforge-strict-play/neoforge-client-ui-results.json', {
    schema: 'echo.neoforge.executed_client_ui_results.v1',
    status: 'PASS',
    moduleIds: ['echoindex'],
    visibleRoutes: ['echoindex:index'],
  })

  await generateNeoForgeRuntimeEvidence({ repoRoot })

  const play = await readJson(repoRoot, 'reports/runtime-parity/neoforge-play-evidence.json')
  assert.equal(play.status, 'PASS')
  assert.deepEqual(play.moduleIds, ['echocore', 'echoindex'])
  assert.ok(play.trustedMutations.some((mutation) => mutation.includes('live session')))
  assert.deepEqual(play.visibleRoutes, ['echoindex:index'])

  const gameTests = await readJson(repoRoot, 'reports/runtime-parity/neoforge-module-gametest-results.json')
  assert.equal(gameTests.status, 'PASS')
  assert.deepEqual(gameTests.moduleIds, ['echoindex'])

  const registry = await readJson(repoRoot, 'reports/runtime-parity/neoforge-registry-content-results.json')
  assert.equal(registry.status, 'PASS')
  assert.deepEqual(registry.moduleIds, ['echoindex'])

  const ui = await readJson(repoRoot, 'reports/runtime-parity/neoforge-client-ui-results.json')
  assert.equal(ui.status, 'PASS')
  assert.deepEqual(ui.moduleIds, ['echoindex'])
} finally {
  await fs.rm(repoRoot, { recursive: true, force: true })
}

console.log('generate-neoforge-runtime-evidence tests passed')
