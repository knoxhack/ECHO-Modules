import assert from 'node:assert/strict'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { generateAshfallNeoForgeRendererAudit } from './verify-ashfall-neoforge-renderers.mjs'

async function writeFile(root, relativePath, content) {
  const filePath = path.join(root, relativePath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, content, 'utf8')
}

async function writeManifest(root, moduleIds) {
  const manifestPath = path.join(root, 'ECHO-Ashfall-NeoForge-Edition', 'release-manifest.template.json')
  await writeFile(root, path.relative(root, manifestPath), `${JSON.stringify({
    pack: 'ashfall-neoforge-edition',
    moduleRequirements: moduleIds.map((moduleId) => ({ moduleId })),
  }, null, 2)}\n`)
  return manifestPath
}

async function writeEntityModule(root, moduleId, options = {}) {
  const sourceRoot = path.join('ECHO-Modules', 'addons', moduleId, 'src', 'main', 'java', 'com', 'example', moduleId)
  await writeFile(root, path.join(sourceRoot, 'ModEntities.java'), [
    `package com.example.${moduleId};`,
    'public final class ModEntities {',
    '  public static final EchoBackendRegistryEntry<EntityType<TestMobEntity>> TEST_MOB = register("test_mob");',
    '  public static final EchoBackendRegistryEntry<EntityType<TestDroneEntity>> TEST_DRONE = register("test_drone");',
    '  private static <T> EchoBackendRegistryEntry<EntityType<T>> register(String name) { return null; }',
    '}',
    '',
  ].join('\n'))
  await writeFile(root, path.join(sourceRoot, 'TestClient.java'), [
    `package com.example.${moduleId};`,
    'public final class TestClient {',
    '  static void register(Object event) {',
    '    EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TEST_MOB.get(), Renderer::new);',
    '    EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TEST_DRONE.get(), Renderer::new);',
    '  }',
    '}',
    '',
  ].join('\n'))
  await writeFile(root, path.join(sourceRoot, 'TestRenderCoreClientIntegration.java'), [
    `package com.example.${moduleId};`,
    'public final class TestRenderCoreClientIntegration {',
    '  static void register(Object event) {',
    '    EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TEST_MOB.get(), Renderer::new);',
    options.includeDroneInRenderCore
      ? '    EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TEST_DRONE.get(), Renderer::new);'
      : '',
    '  }',
    '}',
    '',
  ].join('\n'))
}

test('Ashfall NeoForge renderer audit detects incomplete RenderCore coverage', async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-renderer-audit-'))
  try {
    const manifest = await writeManifest(root, ['echorendercore', 'echosample'])
    await fs.mkdir(path.join(root, 'ECHO-Modules', 'addons', 'echorendercore'), { recursive: true })
    await writeEntityModule(root, 'echosample', { includeDroneInRenderCore: false })

    const report = await generateAshfallNeoForgeRendererAudit({
      repoRoot: path.join(root, 'ECHO-Modules'),
      manifest,
    })

    assert.equal(report.ok, false)
    assert.deepEqual(report.issues, [{
      moduleId: 'echosample',
      missingRendererRegistrations: [],
      missingRenderCoreRegistrations: ['TEST_DRONE'],
    }])
  } finally {
    await fs.rm(root, { recursive: true, force: true })
  }
})

test('Ashfall NeoForge renderer audit passes when general and RenderCore paths are complete', async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-renderer-audit-'))
  try {
    const manifest = await writeManifest(root, ['echorendercore', 'echosample'])
    await fs.mkdir(path.join(root, 'ECHO-Modules', 'addons', 'echorendercore'), { recursive: true })
    await writeEntityModule(root, 'echosample', { includeDroneInRenderCore: true })

    const report = await generateAshfallNeoForgeRendererAudit({
      repoRoot: path.join(root, 'ECHO-Modules'),
      manifest,
    })

    assert.equal(report.ok, true)
    assert.equal(report.entityCount, 2)
    assert.equal(report.issueCount, 0)
  } finally {
    await fs.rm(root, { recursive: true, force: true })
  }
})
