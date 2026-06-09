import assert from 'node:assert/strict'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { generateModuleRelease } from './generate-module-release.mjs'

async function makeRepo() {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-modules-release-'))
  const moduleDir = path.join(root, 'addons', 'echosample')
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/META-INF'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/java/dev/echo/sample'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'build/libs'), { recursive: true })
  await fs.writeFile(path.join(moduleDir, 'src/main/java/dev/echo/sample/Sample.java'), 'package dev.echo.sample; public final class Sample {}')
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/META-INF/neoforge.mods.toml'), 'modLoader="javafml"\n')
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/META-INF/echo.mod.json'), JSON.stringify({
    schema: 'echo.mod.v1',
    id: 'echosample',
    name: 'ECHO Sample',
    version: '1.2.3',
    standalone: true,
    requires: ['echoaddonapi'],
    optional: [],
    access: {
      nativeEntrypoint: 'dev.echo.sample.SampleModule',
    },
  }, null, 2))
  await fs.writeFile(path.join(moduleDir, 'build/libs/echosample-1.2.3.jar'), 'compiled jar placeholder')
  return root
}

test('generates per-module release artifacts and metadata', async () => {
  const repoRoot = await makeRepo()
  const release = await generateModuleRelease({
    repoRoot,
    releaseId: 'test-release',
    modules: ['echosample'],
  })

  assert.equal(release.releaseId, 'test-release')
  assert.equal(release.modules.length, 1)
  const moduleRecord = release.modules[0]
  assert.equal(moduleRecord.moduleId, 'echosample')
  assert.deepEqual(moduleRecord.artifacts.map((artifact) => artifact.filename).sort(), [
    'echosample-1.2.3-neoforge.jar',
    'echosample-1.2.3-sources.jar',
    'echosample-1.2.3-standalone.jar',
    'echosample-1.2.3.echo-addon',
  ])
  assert.equal(moduleRecord.artifacts.find((artifact) => artifact.kind === 'neoforge').buildMode, 'compiled-runtime')
  assert.equal(moduleRecord.artifacts.find((artifact) => artifact.kind === 'standalone').buildMode, 'compiled-runtime')
  assert.equal(moduleRecord.artifacts.find((artifact) => artifact.kind === 'echo-addon').buildMode, 'compiled-runtime')

  const outputDir = path.join(repoRoot, 'dist', 'echo-module-release', 'echosample')
  await fs.access(path.join(outputDir, 'META-INF', 'echo.mod.json'))
  await fs.access(path.join(outputDir, 'META-INF', 'neoforge.mods.toml'))
  await fs.access(path.join(outputDir, 'echo-addon-package.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'echo-release.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'checksums.txt'))
})

test('fails by default when runtime jars are missing', async () => {
  const repoRoot = await makeRepo()
  await fs.rm(path.join(repoRoot, 'addons', 'echosample', 'build'), { recursive: true, force: true })

  await assert.rejects(
    () => generateModuleRelease({ repoRoot, modules: ['echosample'] }),
    /missing neoforge runtime jar, standalone runtime jar, echo-addon runtime jar/u,
  )
})

test('can emit runtime-named archives from source when build outputs are absent', async () => {
  const repoRoot = await makeRepo()
  await fs.rm(path.join(repoRoot, 'addons', 'echosample', 'build'), { recursive: true, force: true })

  const release = await generateModuleRelease({
    repoRoot,
    releaseId: 'source-packaged-test',
    modules: ['echosample'],
    packageFromSource: true,
  })

  const artifacts = release.modules[0].artifacts
  assert.deepEqual(artifacts.map((artifact) => artifact.filename).sort(), [
    'echosample-1.2.3-neoforge.jar',
    'echosample-1.2.3-sources.jar',
    'echosample-1.2.3-standalone.jar',
    'echosample-1.2.3.echo-addon',
  ])
  assert.equal(artifacts.find((artifact) => artifact.kind === 'neoforge').buildMode, 'source-packaged')
  assert.equal(artifacts.find((artifact) => artifact.kind === 'standalone').buildMode, 'source-packaged')
  assert.equal(artifacts.find((artifact) => artifact.kind === 'echo-addon').buildMode, 'source-packaged')
})
