#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { runNeoForgeGameTestsAndWriteEvidence } from './run-neoforge-gametests-and-write-evidence.mjs'

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeText(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, value, 'utf8')
}

const repoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-neoforge-gametest-runner-'))

try {
  await writeJson(path.join(repoRoot, 'addons', 'echoindex-project', 'src', 'main', 'resources', 'META-INF', 'echo.mod.json'), {
    id: 'echoindex',
    name: 'ECHO Index',
    version: '1.0.0',
  })
  await writeText(
    path.join(repoRoot, 'addons', 'echoindex-project', 'build.gradle'),
    'neoForge { runs { gameTestServer { type = "gameTestServer" } } }\n',
  )
  await writeText(
    path.join(repoRoot, 'addons', 'echoindex-project', 'src', 'test', 'java', 'com', 'example', 'ModGameTests.java'),
    [
      'import net.minecraft.gametest.framework.GameTestHelper;',
      'final class ModGameTests {',
      '  static final Object TEST_FUNCTIONS = registry();',
      '  static void register() {',
      '    TEST_FUNCTIONS.register("index_catalog_opens");',
      '  }',
      '}',
      '',
    ].join('\n'),
  )

  const report = await runNeoForgeGameTestsAndWriteEvidence({
    repoRoot,
    allWithGameTests: true,
    listOnly: true,
  })

  assert.equal(report.schema, 'echo.neoforge.gametest_results.v1')
  assert.equal(report.status, 'READY')
  assert.deepEqual(report.selectedModuleIds, ['echoindex'])
  assert.deepEqual(report.selectedProjectNames, ['echoindex-project'])
  assert.equal(report.selectedModuleCount, 1)
  assert.deepEqual(report.moduleIds, [])
  assert.ok(report.blockers.some((blocker) => blocker.includes('not executed')))
} finally {
  await fs.rm(repoRoot, { recursive: true, force: true })
}

console.log('run-neoforge-gametests-and-write-evidence tests passed')
