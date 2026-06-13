#!/usr/bin/env node
import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import { existsSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const modulesRoot = path.resolve(path.dirname(__filename), '..')
const workspaceRoot = path.resolve(modulesRoot, '..')
const releaseIndexRoot = path.join(workspaceRoot, 'ECHO-Release-Index')
const selectionPath = path.join(modulesRoot, 'metadata', 'official-pack-module-selections.json')

const args = new Set(process.argv.slice(2))
const writeMode = args.has('--write')
const checkMode = args.has('--check') || !writeMode

const lanes = {
  native: {
    label: 'Native',
    suffix: 'native-edition',
    artifactFamily: 'echo-addon',
    installDir: 'addons',
    artifactPattern: '<module>-<version>.echo-addon',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'echo_native',
    loader: 'echo-native-loader'
  },
  neoforge: {
    label: 'NeoForge',
    suffix: 'neoforge-edition',
    artifactFamily: 'neoforge',
    installDir: 'mods',
    artifactPattern: '<module>-<version>-neoforge.jar',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'neoforge',
    loader: 'neoforge'
  },
  standalone: {
    label: 'Standalone',
    suffix: 'standalone-edition',
    artifactFamily: 'standalone',
    installDir: 'mods',
    artifactPattern: '<module>-<version>-standalone.jar',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'echo_runtime_standalone',
    loader: 'echo-standalone-runtime'
  }
}

const packRepoNames = {
  ashfall: 'ECHO-Ashfall',
  openlands: 'ECHO-Openlands',
  'arcana-division': 'ECHO-Arcana-Division',
  'sky-relay': 'ECHO-Sky-Relay',
  'galactic-survey': 'ECHO-Galactic-Survey'
}

const foundationModules = [
  'echoadaptercore',
  'echoblockworks',
  'echocommonloot',
  'echocontentcore',
  'echocore',
  'echocreatureroles',
  'echofoundationcore',
  'echoholomap',
  'echohudcore',
  'echoindex',
  'echolens',
  'echomaterialcore',
  'echomissioncore',
  'echonetcore',
  'echoplatformcore',
  'echoruntimeguard',
  'echoschemacore',
  'echoscreencore',
  'echosoundcore',
  'echostationcore',
  'echoterminal',
  'echothemecore',
  'echotoolcore',
  'echotutorialcore',
  'echovalidationcore',
  'echoworldcore',
  'echoworldstarter'
]

const packRootModules = {
  ashfall: 'echoashfallprotocol',
  openlands: 'echoopenlandsprotocol',
  'arcana-division': 'echoarcanadivisionprotocol',
  'sky-relay': 'echoskyrelayprotocol',
  'galactic-survey': 'echogalacticsurveyprotocol'
}

const virtualFiles = new Map()
const changedFiles = []
const errors = []

function formatJson(value) {
  return `${JSON.stringify(value, null, 2)}\n`
}

async function readJson(file) {
  return JSON.parse(await fs.readFile(file, 'utf8'))
}

async function readJsonIfExists(file) {
  if (!existsSync(file) && !hasVirtualFile(file)) return null
  return JSON.parse(await readTextMaybeVirtual(file))
}

function hasVirtualFile(file) {
  return virtualFiles.has(path.resolve(file))
}

function getVirtualFile(file) {
  return virtualFiles.get(path.resolve(file))
}

function setVirtualFile(file, content) {
  virtualFiles.set(path.resolve(file), content)
}

async function readTextMaybeVirtual(file) {
  const resolved = path.resolve(file)
  if (virtualFiles.has(resolved)) return virtualFiles.get(resolved)
  return fs.readFile(resolved, 'utf8')
}

async function writeTextIfChanged(file, content) {
  const resolved = path.resolve(file)
  const before = existsSync(resolved) ? await readTextMaybeVirtual(resolved) : null
  setVirtualFile(resolved, content)
  if (before === content) return false
  changedFiles.push(resolved)
  if (writeMode) {
    await fs.mkdir(path.dirname(resolved), { recursive: true })
    await fs.writeFile(resolved, content)
  }
  return true
}

async function writeJsonIfChanged(file, value) {
  return writeTextIfChanged(file, formatJson(value))
}

function hashBytes(bytes) {
  return {
    size: bytes.length,
    sha256: crypto.createHash('sha256').update(bytes).digest('hex')
  }
}

async function fileDigest(file) {
  const resolved = path.resolve(file)
  if (hasVirtualFile(resolved)) {
    return hashBytes(Buffer.from(getVirtualFile(resolved), 'utf8'))
  }
  const bytes = await fs.readFile(resolved)
  return hashBytes(bytes)
}

function fileExistsOrVirtual(file) {
  return existsSync(file) || hasVirtualFile(file)
}

function artifactNameFor(moduleId, version, lane) {
  if (lane.artifactFamily === 'echo-addon') return `${moduleId}-${version}.echo-addon`
  if (lane.artifactFamily === 'neoforge') return `${moduleId}-${version}-neoforge.jar`
  if (lane.artifactFamily === 'standalone') return `${moduleId}-${version}-standalone.jar`
  throw new Error(`Unsupported artifact family ${lane.artifactFamily}`)
}

function packIdFor(packKey, laneKey) {
  return `${packKey}-${lanes[laneKey].suffix}`
}

function repoNameFor(packKey, laneKey) {
  return `${packRepoNames[packKey]}-${lanes[laneKey].label}-Edition`
}

function versionFor(moduleId, descriptors) {
  const descriptor = descriptors.get(moduleId)
  if (!descriptor) throw new Error(`No descriptor for ${moduleId}`)
  return String(descriptor.version)
}

function richRequirement(moduleId, lane, descriptors, previousById = new Map(), releaseDir = null) {
  const version = versionFor(moduleId, descriptors)
  const artifactName = artifactNameFor(moduleId, version, lane)
  const entry = {
    id: moduleId,
    moduleId,
    version,
    artifactFamily: lane.artifactFamily,
    assetName: artifactName,
    artifactName,
    path: `${lane.installDir}/${artifactName}`,
    required: true,
    side: 'both'
  }

  const artifactPath = releaseDir ? path.join(releaseDir, artifactName) : null
  const previous = previousById.get(moduleId)
  if (artifactPath && existsSync(artifactPath)) {
    const stat = statSync(artifactPath)
    entry.size = stat.size
  } else if (
    previous &&
    (previous.artifactName === artifactName || previous.assetName === artifactName || previous.path === entry.path)
  ) {
    if (previous.sha256) entry.sha256 = previous.sha256
    if (Number.isFinite(previous.size)) entry.size = previous.size
  }
  return entry
}

async function decorateLocalArtifactHashes(requirements, releaseDir) {
  for (const requirement of requirements) {
    const artifactPath = path.join(releaseDir, requirement.assetName)
    if (!existsSync(artifactPath)) continue
    const digest = await fileDigest(artifactPath)
    requirement.sha256 = digest.sha256
    requirement.size = digest.size
  }
}

function simpleRequirement(moduleId, descriptors) {
  return {
    id: moduleId,
    version: versionFor(moduleId, descriptors)
  }
}

function catalogRequirement(moduleId, descriptors, previousById, preferRanges) {
  const version = versionFor(moduleId, descriptors)
  const previousVersion = previousById.get(moduleId)?.version
  if (typeof previousVersion === 'string' && previousVersion.startsWith('>=')) {
    return { id: moduleId, version: `>=${version}` }
  }
  if (previousVersion) return { id: moduleId, version }
  if (preferRanges && !moduleId.endsWith('protocol')) return { id: moduleId, version: `>=${version}` }
  return { id: moduleId, version }
}

function entriesByModuleId(entries = []) {
  return new Map(
    entries
      .map((entry) => [String(entry?.id ?? entry?.moduleId ?? '').toLowerCase(), entry])
      .filter(([id]) => id)
  )
}

function moduleIdsFromRequirements(entries = []) {
  return entries.map((entry) => String(entry?.id ?? entry?.moduleId ?? '').toLowerCase()).filter(Boolean)
}

function sameModuleIds(actual, expected) {
  if (actual.length !== expected.length) return false
  return actual.every((id, index) => id === expected[index])
}

async function collectDescriptors() {
  const descriptors = new Map()
  const addonsRoot = path.join(modulesRoot, 'addons')
  const dirs = await fs.readdir(addonsRoot, { withFileTypes: true })
  for (const dir of dirs) {
    if (!dir.isDirectory()) continue
    const descriptorPath = path.join(addonsRoot, dir.name, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (!existsSync(descriptorPath)) continue
    const descriptor = await readJson(descriptorPath)
    if (!descriptor.id) {
      errors.push(`${descriptorPath} is missing id`)
      continue
    }
    descriptors.set(String(descriptor.id).toLowerCase(), {
      ...descriptor,
      descriptorPath,
      moduleDir: path.join(addonsRoot, dir.name)
    })
  }
  return descriptors
}

function validateSelections(selectionSource, descriptors) {
  for (const [packKey, pack] of Object.entries(selectionSource.packs)) {
    const modules = pack.modules.map((moduleId) => moduleId.toLowerCase())
    const selected = new Set(modules)
    if (modules.length !== pack.expectedCount) {
      errors.push(`${packKey} expectedCount is ${pack.expectedCount}, but list has ${modules.length}`)
    }
    if (new Set(modules).size !== modules.length) {
      errors.push(`${packKey} has duplicate module IDs`)
    }
    for (const moduleId of modules) {
      if (!descriptors.has(moduleId)) errors.push(`${packKey} selects missing module descriptor ${moduleId}`)
    }
    for (const moduleId of foundationModules) {
      if (!selected.has(moduleId)) errors.push(`${packKey} is missing foundation module ${moduleId}`)
    }
    for (const [otherPack, rootModule] of Object.entries(packRootModules)) {
      if (otherPack !== packKey && selected.has(rootModule)) {
        errors.push(`${packKey} includes cross-pack root module ${rootModule}`)
      }
    }
    const ownRoot = packRootModules[packKey]
    if (ownRoot && !selected.has(ownRoot)) {
      errors.push(`${packKey} is missing its root module ${ownRoot}`)
    }
    const echoAddonApiRequiredBy = []
    for (const moduleId of modules) {
      const descriptor = descriptors.get(moduleId)
      if (!descriptor) continue
      for (const requiredId of descriptor.requires ?? []) {
        const normalizedRequiredId = String(requiredId).toLowerCase()
        if (!selected.has(normalizedRequiredId)) {
          errors.push(`${packKey} selects ${moduleId}, but is missing required dependency ${normalizedRequiredId}`)
        }
        if (normalizedRequiredId === 'echoaddonapi' && moduleId !== 'echoaddonapi') {
          echoAddonApiRequiredBy.push(moduleId)
        }
      }
    }
    if (selected.has('echoaddonapi') && echoAddonApiRequiredBy.length === 0) {
      errors.push(`${packKey} includes echoaddonapi, but no selected module requires it`)
    }
    if (!selected.has('echoaddonapi') && echoAddonApiRequiredBy.length > 0) {
      errors.push(`${packKey} is missing echoaddonapi required by ${echoAddonApiRequiredBy.join(', ')}`)
    }
  }
}

async function walkFiles(root, predicate, found = []) {
  if (!existsSync(root)) return found
  const entries = await fs.readdir(root, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(root, entry.name)
    if (entry.isDirectory()) {
      if (entry.name === '.git' || entry.name === 'node_modules') continue
      await walkFiles(fullPath, predicate, found)
    } else if (predicate(fullPath)) {
      found.push(fullPath)
    }
  }
  return found
}

function updatePackManifestObject(manifest, selection, lane, descriptors, releaseDir = null) {
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const requirements = selection.map((moduleId) => richRequirement(moduleId, lane, descriptors, previousById, releaseDir))
  manifest.moduleArtifactFamily = lane.artifactFamily
  manifest.moduleArtifactPattern = lane.artifactPattern
  manifest.moduleSourcePattern = manifest.moduleSourcePattern ?? lane.sourcePattern
  manifest.moduleRequirements = requirements
  if ('runtimeTarget' in manifest || lane.runtimeTarget) manifest.runtimeTarget = lane.runtimeTarget
  if ('loader' in manifest || lane.loader) manifest.loader = lane.loader
  return requirements
}

function updatePackSnapshotObject(manifest, selection, lane, descriptors, releaseDir) {
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const requirements = selection.map((moduleId) => richRequirement(moduleId, lane, descriptors, previousById, releaseDir))
  manifest.moduleArtifactFamily = lane.artifactFamily
  manifest.moduleRequirements = requirements
  if (Array.isArray(manifest.modules)) manifest.modules = [...selection]
  if (Array.isArray(manifest.requiredArtifacts)) {
    const nonModuleArtifacts = manifest.requiredArtifacts.filter((artifact) => artifact?.kind && artifact.kind !== 'module')
    manifest.requiredArtifacts = [
      ...selection.map((moduleId) => ({
        id: moduleId,
        kind: 'module',
        version: versionFor(moduleId, descriptors),
        artifactFamily: lane.artifactFamily
      })),
      ...nonModuleArtifacts
    ]
  }
  if (Array.isArray(manifest.files)) {
    const selected = new Set(selection)
    manifest.files = manifest.files
      .filter((file) => {
        const moduleId = String(file?.moduleId ?? file?.id ?? '').toLowerCase()
        return !moduleId || selected.has(moduleId)
      })
      .map((file) => {
        const moduleId = String(file?.moduleId ?? file?.id ?? '').toLowerCase()
        if (!moduleId || !selected.has(moduleId)) return file
        const version = versionFor(moduleId, descriptors)
        const artifactName = artifactNameFor(moduleId, version, lane)
        const next = {
          ...file,
          path: `${lane.installDir}/${artifactName}`,
          assetName: artifactName,
          artifactName,
          moduleId,
          required: true,
          side: file.side ?? 'both'
        }
        if (file.artifactFamily) next.artifactFamily = lane.artifactFamily
        if (!(file.assetName === artifactName || file.artifactName === artifactName || file.path === next.path)) {
          delete next.sha256
          delete next.size
        }
        return next
      })
  }
  if ('moduleRequirementCount' in manifest) manifest.moduleRequirementCount = selection.length
  return requirements
}

async function updateReleaseManifestTemplate(repoRoot, packKey, laneKey, selection, descriptors) {
  const lane = lanes[laneKey]
  const file = path.join(repoRoot, 'release-manifest.template.json')
  let manifest = await readJsonIfExists(file)
  if (!manifest) {
    const packId = packIdFor(packKey, laneKey)
    manifest = {
      packId,
      displayName: `${selection.displayName} ${lane.label} Edition`,
      sourceRepo: `knoxhack/${repoNameFor(packKey, laneKey)}`,
      launcherFeed: 'github-releases',
      runtimeTarget: lane.runtimeTarget,
      loader: lane.loader,
      moduleArtifactFamily: lane.artifactFamily,
      moduleArtifactPattern: lane.artifactPattern,
      moduleSourcePattern: lane.sourcePattern,
      moduleRequirements: [],
      requiredModuleDescriptors: laneKey === 'neoforge'
        ? ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml']
        : ['META-INF/echo.mod.json'],
      artifacts: []
    }
  }
  updatePackManifestObject(manifest, selection.modules, lane, descriptors)
  await refreshArtifactMetadata(manifest, repoRoot)
  await writeJsonIfChanged(file, manifest)
}

async function refreshArtifactMetadata(manifest, repoRoot) {
  if (!Array.isArray(manifest.artifacts)) return
  for (const artifact of manifest.artifacts) {
    const candidates = []
    if (artifact.path) candidates.push(path.join(repoRoot, artifact.path))
    if (artifact.file) candidates.push(path.join(repoRoot, artifact.file))
    const existing = candidates.find((candidate) => fileExistsOrVirtual(candidate))
    if (!existing) continue
    const digest = await fileDigest(existing)
    artifact.size = digest.size
    artifact.sha256 = digest.sha256
  }
}

async function updatePackSnapshots(repoRoot, packKey, laneKey, selection, descriptors) {
  const snapshotRoots = [
    path.join(repoRoot, 'release-assets'),
    path.join(repoRoot, 'dist')
  ]
  const packFiles = []
  for (const snapshotRoot of snapshotRoots) {
    packFiles.push(...await walkFiles(snapshotRoot, (file) => file.endsWith('.pack.json')))
  }
  const changedDirs = new Set()
  for (const file of packFiles) {
    const manifest = await readJson(file)
    const requirements = updatePackSnapshotObject(manifest, selection.modules, lanes[laneKey], descriptors, path.dirname(file))
    await decorateLocalArtifactHashes(requirements, path.dirname(file))
    await writeJsonIfChanged(file, manifest)
    changedDirs.add(path.dirname(file))
  }
  for (const dir of changedDirs) {
    await refreshReleaseSidecars(dir, selection.modules.length)
  }
}

async function refreshReleaseSidecars(releaseDir, moduleRequirementCount) {
  const packFiles = await walkFiles(releaseDir, (file) => file.endsWith('.pack.json'))
  const packDigests = new Map()
  for (const packFile of packFiles) {
    packDigests.set(path.basename(packFile), await fileDigest(packFile))
  }

  const echoReleasePath = path.join(releaseDir, 'echo-release.json')
  if (existsSync(echoReleasePath) || hasVirtualFile(echoReleasePath)) {
    const echoRelease = JSON.parse(await readTextMaybeVirtual(echoReleasePath))
    const manifestName = echoRelease.manifestAsset ?? [...packDigests.keys()][0]
    const manifestDigest = packDigests.get(manifestName)
    if (manifestDigest) {
      echoRelease.manifestAsset = manifestName
      echoRelease.manifestSha256 = manifestDigest.sha256
      if ('manifestSize' in echoRelease) echoRelease.manifestSize = manifestDigest.size
    }
    if ('moduleRequirementCount' in echoRelease) echoRelease.moduleRequirementCount = moduleRequirementCount
    if (Array.isArray(echoRelease.assets)) {
      for (const asset of echoRelease.assets) {
        const digest = packDigests.get(asset.name)
        if (digest) {
          asset.sha256 = digest.sha256
          asset.size = digest.size
        }
      }
    }
    if (echoRelease.artifacts?.manifest?.file) {
      const digest = packDigests.get(echoRelease.artifacts.manifest.file)
      if (digest) {
        echoRelease.artifacts.manifest.sha256 = digest.sha256
        echoRelease.artifacts.manifest.size = digest.size
      }
    }
    await writeJsonIfChanged(echoReleasePath, echoRelease)
  }

  const checksumsPath = path.join(releaseDir, 'checksums.txt')
  if (existsSync(checksumsPath) || hasVirtualFile(checksumsPath)) {
    const checksumTargets = new Map(packDigests)
    if (fileExistsOrVirtual(echoReleasePath)) {
      checksumTargets.set('echo-release.json', await fileDigest(echoReleasePath))
    }
    const original = await readTextMaybeVirtual(checksumsPath)
    const lines = original.split(/\r?\n/u).filter((line) => line.trim().length > 0)
    const seen = new Set()
    const nextLines = lines.map((line) => {
      const match = line.match(/^([a-fA-F0-9]{64})\s+(.+)$/u)
      if (!match) return line
      const name = match[2].trim()
      const digest = checksumTargets.get(name)
      if (!digest) return line
      seen.add(name)
      return `${digest.sha256}  ${name}`
    })
    for (const [name, digest] of checksumTargets) {
      if (!seen.has(name)) nextLines.push(`${digest.sha256}  ${name}`)
    }
    await writeTextIfChanged(checksumsPath, `${nextLines.join('\n')}\n`)
  }

  const releaseAuditPath = path.join(releaseDir, 'release-audit.json')
  if (existsSync(releaseAuditPath) || hasVirtualFile(releaseAuditPath)) {
    const releaseAudit = JSON.parse(await readTextMaybeVirtual(releaseAuditPath))
    const assetDigests = new Map(packDigests)
    if (fileExistsOrVirtual(echoReleasePath)) {
      assetDigests.set('echo-release.json', await fileDigest(echoReleasePath))
    }
    if (fileExistsOrVirtual(checksumsPath)) {
      assetDigests.set('checksums.txt', await fileDigest(checksumsPath))
    }
    if (Array.isArray(releaseAudit.assets)) {
      for (const asset of releaseAudit.assets) {
        const digest = assetDigests.get(asset.name)
        if (!digest) continue
        asset.sha256 = digest.sha256
        asset.size = digest.size
      }
    }
    if (Array.isArray(releaseAudit.checksumEntries)) {
      for (const entry of releaseAudit.checksumEntries) {
        const digest = assetDigests.get(entry.file)
        if (!digest) continue
        entry.expectedSha256 = digest.sha256
        entry.actualSha256 = digest.sha256
        entry.size = digest.size
        entry.ok = true
      }
      if (releaseAudit.checksumCoverage) {
        releaseAudit.checksumCoverage.total = releaseAudit.checksumEntries.length
        releaseAudit.checksumCoverage.missing = releaseAudit.missingChecksumEntries?.length ?? 0
        releaseAudit.checksumCoverage.mismatched = releaseAudit.mismatchedChecksumEntries?.length ?? 0
      }
    }
    await writeJsonIfChanged(releaseAuditPath, releaseAudit)
  }
}

async function updateReleaseIndexPack(packKey, laneKey, selection, descriptors) {
  const file = path.join(releaseIndexRoot, 'packs', `${packIdFor(packKey, laneKey)}.json`)
  const manifest = await readJsonIfExists(file)
  if (!manifest) {
    errors.push(`Missing Release Index pack entry ${file}`)
    return
  }
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const preferRanges = (manifest.moduleRequirements ?? []).some((requirement) => String(requirement?.version ?? '').startsWith('>='))
  manifest.moduleArtifactFamily = lanes[laneKey].artifactFamily
  manifest.moduleArtifactPattern = lanes[laneKey].artifactPattern
  manifest.moduleRequirements = selection.modules.map((moduleId) =>
    catalogRequirement(moduleId, descriptors, previousById, preferRanges)
  )
  await writeJsonIfChanged(file, manifest)
}

async function assertLaneParity(packKey, expectedModules) {
  for (const laneKey of Object.keys(lanes)) {
    const repoRoot = path.join(workspaceRoot, repoNameFor(packKey, laneKey))
    const template = await readJsonIfExists(path.join(repoRoot, 'release-manifest.template.json'))
    if (template) {
      const actual = moduleIdsFromRequirements(template.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`${repoNameFor(packKey, laneKey)} release-manifest.template.json moduleRequirements differ from ${packKey}`)
      }
    }
    const packFiles = await walkFiles(path.join(repoRoot, 'release-assets'), (file) => file.endsWith('.pack.json'))
    for (const packFile of packFiles) {
      const manifest = JSON.parse(await readTextMaybeVirtual(packFile))
      const actual = moduleIdsFromRequirements(manifest.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`${packFile} moduleRequirements differ from ${packKey}`)
      }
    }
    const indexEntry = await readJsonIfExists(path.join(releaseIndexRoot, 'packs', `${packIdFor(packKey, laneKey)}.json`))
    if (indexEntry) {
      const actual = moduleIdsFromRequirements(indexEntry.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`Release Index ${packIdFor(packKey, laneKey)} moduleRequirements differ from ${packKey}`)
      }
    }
  }
}

async function main() {
  const selections = await readJson(selectionPath)
  const descriptors = await collectDescriptors()
  validateSelections(selections, descriptors)
  if (errors.length > 0) {
    throw new Error(`Selection validation failed:\n${errors.map((error) => `- ${error}`).join('\n')}`)
  }

  for (const [packKey, selection] of Object.entries(selections.packs)) {
    for (const laneKey of Object.keys(lanes)) {
      const repoRoot = path.join(workspaceRoot, repoNameFor(packKey, laneKey))
      if (!existsSync(repoRoot)) {
        errors.push(`Missing pack repo ${repoRoot}`)
        continue
      }
      await updatePackSnapshots(repoRoot, packKey, laneKey, selection, descriptors)
      await updateReleaseManifestTemplate(repoRoot, packKey, laneKey, selection, descriptors)
      await updateReleaseIndexPack(packKey, laneKey, selection, descriptors)
    }
    await assertLaneParity(packKey, selection.modules.map((moduleId) => moduleId.toLowerCase()))
  }

  if (errors.length > 0) {
    throw new Error(`Sync validation failed:\n${errors.map((error) => `- ${error}`).join('\n')}`)
  }

  const uniqueChangedFiles = [...new Set(changedFiles)].sort()
  if (checkMode && uniqueChangedFiles.length > 0) {
    console.error('Official pack module selections are out of sync:')
    for (const file of uniqueChangedFiles) console.error(`- ${path.relative(workspaceRoot, file)}`)
    process.exitCode = 1
    return
  }

  if (writeMode) {
    if (uniqueChangedFiles.length === 0) {
      console.log('Official pack module selections already synchronized.')
    } else {
      console.log(`Synchronized ${uniqueChangedFiles.length} files:`)
      for (const file of uniqueChangedFiles) console.log(`- ${path.relative(workspaceRoot, file)}`)
    }
    return
  }

  console.log('Official pack module selections are synchronized.')
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
