#!/usr/bin/env node
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import test from 'node:test'

const repoRoot = process.cwd()
const ashfallRoot = path.join(repoRoot, 'addons', 'echoashfallprotocol', 'src', 'main', 'java')
const clientPath = path.join(ashfallRoot, 'com', 'knoxhack', 'echoashfallprotocol', 'EchoAshfallProtocolClient.java')
const loadingOverlayPath = path.join(ashfallRoot, 'com', 'knoxhack', 'echoashfallprotocol', 'client', 'screen', 'EchoNativeAshfallLoadingOverlay.java')
const bridgePath = path.join(ashfallRoot, 'com', 'knoxhack', 'echoashfallprotocol', 'nativebridge', 'AshfallNativeClientRouteBridge.java')

async function read(filePath) {
  return fs.readFile(filePath, 'utf8')
}

test('Ashfall NeoForge client entrypoints use bridge instead of direct Native route registrar linkage', async () => {
  const client = await read(clientPath)
  const loadingOverlay = await read(loadingOverlayPath)
  const bridge = await read(bridgePath)

  for (const [label, source] of [
    ['EchoAshfallProtocolClient.java', client],
    ['EchoNativeAshfallLoadingOverlay.java', loadingOverlay],
  ]) {
    assert.match(source, /AshfallNativeClientRouteBridge/u, `${label} should use the safe bridge.`)
    assert.doesNotMatch(source, /AshfallNativeClientRouteRegistrar/u, `${label} must not directly link the Native route registrar.`)
    assert.doesNotMatch(source, /dev\.echo\.nativeplatform\.contracts/u, `${label} must not import Native contracts in the NeoForge client path.`)
  }

  assert.match(bridge, /REGISTRAR_CLASS/u)
  assert.match(bridge, /Class\.forName\(REGISTRAR_CLASS/u)
  assert.match(bridge, /EchoNativeRuntimeEnvironmentBridge\.isNativeLoaderActive/u)
  assert.doesNotMatch(bridge, /import dev\.echo\.nativeplatform\.contracts/u)
})
