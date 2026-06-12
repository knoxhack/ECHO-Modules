import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generateCoreModuleIntegrationAudit } from './generate-core-module-integration-audit.mjs'

async function writeModule(root, directory, descriptor) {
  const moduleRoot = path.join(root, 'addons', directory)
  const descriptorPath = path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
  await fs.mkdir(path.dirname(descriptorPath), { recursive: true })
  await fs.mkdir(path.join(moduleRoot, 'src', 'main', 'java'), { recursive: true })
  await fs.writeFile(path.join(moduleRoot, 'build.gradle'), "plugins { id 'java-library' }\n", 'utf8')
  await fs.writeFile(descriptorPath, `${JSON.stringify(descriptor, null, 2)}\n`, 'utf8')
}

const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-core-module-audit-'))
try {
  await writeModule(root, 'echocore', {
    schema: 'echo.mod.v1',
    id: 'echocore',
    name: 'ECHO Core',
    version: '1.0.0',
    kind: 'library',
    role: 'foundation',
    official: true,
    standalone: true,
    access: {
      adapterCore: { runtimes: ['echo_native'] },
      nativeEntrypoint: 'com.example.EchoCoreNativeModule',
    },
  })
  await writeModule(root, 'echosample', {
    schema: 'echo.mod.v1',
    id: 'echosample',
    name: 'ECHO Sample',
    version: '1.0.0',
    kind: 'addon',
    role: 'story',
    official: true,
    standalone: true,
    requires: ['echocore'],
    access: {
      adapterCore: { runtimes: ['neoforge', 'echo_native'] },
      nativeEntrypoint: 'com.example.EchoSampleNativeModule',
    },
  })

  const { outputPath, report } = await generateCoreModuleIntegrationAudit({
    repoRoot: root,
    out: 'reports/echo-native/core-module-integration-audit.json',
    strictCounts: false,
  })

  assert.equal(report.schema, 'echo.native.core_module_integration_audit.v1')
  assert.equal(report.moduleCount, 2)
  assert.equal(report.bridgeableModuleCount, 2)
  assert.equal(report.coreSpineModuleCount, 1)
  assert.equal(report.modules[0].moduleId, 'echocore')
  assert.equal(report.modules[0].inCoreSpineAudit, true)
  assert.equal(report.modules[1].nativeIntegrationStatus, 'LEGACY_ADAPTER_BRIDGEABLE')
  assert.equal(report.modules[1].directory, 'addons/echosample')
  assert.equal(JSON.parse(await fs.readFile(outputPath, 'utf8')).moduleCount, 2)
} finally {
  await fs.rm(root, { recursive: true, force: true })
}

console.log('generate-core-module-integration-audit tests passed')
