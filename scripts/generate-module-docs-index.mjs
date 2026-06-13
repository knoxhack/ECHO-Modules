import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const OUT_PATH = path.join('docs', 'module-docs-index.md')

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function readJson(filePath) {
  const text = await fs.readFile(filePath, 'utf8')
  return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function cleanList(value) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === 'string' && item.trim().length > 0).map((item) => item.trim())
    : []
}

function supports(runtimes, runtime) {
  return runtimes.includes(runtime) ? 'yes' : 'no'
}

async function moduleRows(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const descriptorPath = path.join(addonsRoot, entry.name, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const access = object(descriptor.access)
    const adapterCore = object(access.adapterCore)
    const runtimes = cleanList(adapterCore.runtimes)
    modules.push({
      moduleId: typeof descriptor.id === 'string' && descriptor.id ? descriptor.id : entry.name,
      directory: entry.name,
      version: typeof descriptor.version === 'string' && descriptor.version ? descriptor.version : 'unknown',
      runtimes,
    })
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

function renderIndex(rows) {
  const lines = [
    '# Module Docs Index',
    '',
    'This index is generated from `addons/<module>/gradle.properties` and `META-INF/echo.mod.json` so every module has a visible documentation landing page.',
    '',
    '| Module | Version | Native | NeoForge | Standalone |',
    '| --- | ---: | --- | --- | --- |',
  ]
  for (const row of rows) {
    lines.push(`| [${row.moduleId}](../addons/${row.directory}/README.md) | ${row.version} | ${supports(row.runtimes, 'echo_native')} | ${supports(row.runtimes, 'neoforge')} | ${supports(row.runtimes, 'echo_runtime_standalone')} |`)
  }
  return `${lines.join('\n')}\n`
}

export async function generateModuleDocsIndex({ repoRoot = process.cwd(), out = OUT_PATH } = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const rows = await moduleRows(normalizedRoot)
  const outputPath = path.resolve(normalizedRoot, out)
  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, renderIndex(rows), 'utf8')
  return { outputPath, moduleCount: rows.length }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const { outputPath, moduleCount } = await generateModuleDocsIndex()
    console.log(`Wrote module docs index: ${outputPath}`)
    console.log(`Indexed modules: ${moduleCount}`)
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
