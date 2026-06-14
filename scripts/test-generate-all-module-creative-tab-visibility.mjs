import assert from 'node:assert/strict'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { generateAllModuleCreativeTabVisibility } from './generate-all-module-creative-tab-visibility.mjs'

const tempRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-creative-tab-visibility-'))
const modulesRoot = path.join(tempRoot, 'ECHO-Modules')
const runtimeRoot = path.join(tempRoot, 'ECHO-Native-Platform')
const moduleRoot = path.join(modulesRoot, 'addons', 'echoexample')

await fs.mkdir(path.join(moduleRoot, 'src', 'main', 'java', 'com', 'example'), { recursive: true })
await fs.mkdir(path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF'), { recursive: true })
await fs.mkdir(path.join(moduleRoot, 'src', 'main', 'resources', 'assets', 'echoexample', 'models', 'item'), { recursive: true })
await fs.mkdir(path.join(moduleRoot, 'src', 'main', 'resources', 'assets', 'echoexample', 'lang'), { recursive: true })
await fs.writeFile(path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json'), JSON.stringify({
  id: 'echoexample',
  name: 'Echo Example',
  version: '1.0.0',
  kind: 'addon',
  role: 'content',
  access: { adapterCore: { domains: ['items'] } },
}, null, 2))
await fs.writeFile(path.join(moduleRoot, 'src', 'main', 'resources', 'assets', 'echoexample', 'models', 'item', 'test_item.json'), '{}')
await fs.writeFile(path.join(moduleRoot, 'src', 'main', 'resources', 'assets', 'echoexample', 'lang', 'en_us.json'), JSON.stringify({
  'itemGroup.echoexample': 'Echo Example',
}))
await fs.writeFile(path.join(moduleRoot, 'src', 'main', 'java', 'com', 'example', 'EchoExampleNativeModule.java'), `
package com.example;

public final class EchoExampleNativeModule {
    void register() {
        host.registerCreativeTab("echoexample:example_tab");
    }
}
`)

await fs.mkdir(path.join(runtimeRoot, 'reports', 'echo-native', 'live'), { recursive: true })
await fs.writeFile(path.join(runtimeRoot, 'reports', 'echo-native', 'live', 'module-activation.json'), JSON.stringify({
  status: 'FAIL',
  runtimeBridge: {
    registryBridge: {
      registeredCreativeTabs: [{
        firstClassNativeCreativeTabPresent: true,
        registered: true,
        nativeRegistryContentBacked: true,
        releaseCreativeTabTrusted: true,
        creativeTabOutputBacked: true,
        creativeTabSearchOutputBacked: true,
        declaredCreativeTabItemsBackedByNativeRegistry: true,
        declaredIconItemBackedByNativeRegistry: true,
        resolvedIconItemBackedByNativeRegistry: true,
        fallbackOnlyCreativeVisibility: false,
        searchVisible: true,
        creativeTabItemsFromNativeRegistry: ['echoexample:test_item'],
        creativeTabOutputProofItemIds: ['echoexample:test_item'],
        creativeTabSearchOutputProofItemIds: ['echoexample:test_item'],
      }],
    },
  },
}, null, 2))

const { report } = await generateAllModuleCreativeTabVisibility({
  modulesRoot,
  runtimeRoot,
  runtime: 'echo_native',
  out: 'out/report.json',
})

assert.equal(report.status, 'FAIL')
assert.equal(report.summary.expectedCreativeTabModuleCount, 1)
assert.equal(report.summary.registryBackedModuleCount, 1)
assert.equal(report.summary.visibleParentModuleCount, 1)
assert.equal(report.summary.visibleSearchModuleCount, 1)
assert.equal(report.summary.selectableModuleCount, 0)
assert.equal(report.summary.playableModuleCount, 0)
assert.deepEqual(report.registryBackedModuleIds, ['echoexample'])
assert.deepEqual(report.visibleParentModuleIds, ['echoexample'])
assert.deepEqual(report.visibleSearchModuleIds, ['echoexample'])
assert.equal(report.modules[0].creativeTabStatus, 'visible-search')
assert.deepEqual(report.modules[0].missingCreativeTabEntries, [])
assert.deepEqual(report.modules[0].missingCreativeSearchEntries, [])
assert(report.modules[0].blockers.includes('no module creative-tab entry is proven selectable into inventory or hotbar'))
assert(report.modules[0].blockers.includes('no selected creative-tab block/item is proven usable in gameplay'))

await fs.rm(tempRoot, { recursive: true, force: true })
console.log('generate-all-module-creative-tab-visibility tests passed')
