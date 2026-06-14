#!/usr/bin/env node
import { spawn } from 'node:child_process'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_OUTPUT = 'reports/neoforge-strict-play/neoforge-gametest-results.json'
const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')

export async function runNeoForgeGameTestsAndWriteEvidence({
  repoRoot = process.cwd(),
  modules = [],
  allWithGameTests = false,
  output = DEFAULT_OUTPUT,
  gradle = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew',
  continueOnFailure = false,
  timeoutMs = 15 * 60 * 1000,
  listOnly = false,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const requestedModules = modules.length > 0
    ? modules
    : (allWithGameTests ? (await discoverGameTestModules(normalizedRoot)).map((module) => module.moduleId) : [])
  const moduleSet = unique(requestedModules)
  if (moduleSet.length === 0) {
    throw new Error('No NeoForge GameTest modules selected. Pass --modules <ids> or --all-with-gametests.')
  }
  const discovered = await discoverGameTestModules(normalizedRoot)
  const byId = new Map(discovered.map((module) => [module.moduleId, module]))
  const selected = moduleSet.map((moduleId) => {
    const module = byId.get(moduleId)
    if (!module) throw new Error(`Selected module does not have indexed NeoForge GameTests: ${moduleId}`)
    return module
  })

  if (listOnly) {
    return evidenceReport({
      repoRoot: normalizedRoot,
      output,
      selected,
      results: [],
      listOnly: true,
    })
  }

  const results = []
  for (const module of selected) {
    const result = await runGradleGameTest({
      repoRoot: normalizedRoot,
      gradle,
      moduleId: module.moduleId,
      projectName: module.projectName,
      timeoutMs,
    })
    results.push({
      ...result,
      testNames: module.testNames,
      testFiles: module.testFiles,
    })
    if (result.exitCode !== 0 && !continueOnFailure) break
  }

  const report = evidenceReport({
    repoRoot: normalizedRoot,
    output,
    selected,
    results,
    listOnly: false,
  })
  const outputPath = path.resolve(normalizedRoot, output)
  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  return report
}

async function runGradleGameTest({ repoRoot, gradle, moduleId, projectName, timeoutMs }) {
  const startedAt = new Date().toISOString()
  const task = `:${projectName}:runGameTestServer`
  const command = [gradle, '-PechoAddonSet=all', task, '--console=plain']
  const result = await run(command[0], command.slice(1), {
    cwd: repoRoot,
    timeoutMs,
  })
  return {
    moduleId,
    projectName,
    task,
    command,
    startedAt,
    finishedAt: new Date().toISOString(),
    durationMs: result.durationMs,
    exitCode: result.exitCode,
    status: result.exitCode === 0 ? 'PASS' : 'FAIL',
    stdoutTail: tail(result.stdout),
    stderrTail: tail(result.stderr),
    timedOut: result.timedOut,
    spawnError: result.spawnError,
  }
}

function run(command, args, { cwd, timeoutMs }) {
  return new Promise((resolve) => {
    const started = Date.now()
    let stdout = ''
    let stderr = ''
    let settled = false
    const child = spawn(command, args, {
      cwd,
      windowsHide: true,
      shell: process.platform === 'win32' && /\.(bat|cmd)$/i.test(command),
    })
    const timeout = setTimeout(() => {
      child.kill('SIGTERM')
      finish({ exitCode: 124, timedOut: true, spawnError: '' })
    }, timeoutMs)
    child.stdout.setEncoding('utf8')
    child.stderr.setEncoding('utf8')
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', (error) => {
      finish({ exitCode: null, timedOut: false, spawnError: error.message })
    })
    child.on('close', (exitCode) => {
      finish({ exitCode, timedOut: false, spawnError: '' })
    })
    function finish({ exitCode, timedOut, spawnError }) {
      if (settled) return
      settled = true
      clearTimeout(timeout)
      resolve({
        exitCode,
        timedOut,
        spawnError,
        stdout,
        stderr,
        durationMs: Date.now() - started,
      })
    }
  })
}

function evidenceReport({ repoRoot, output, selected, results, listOnly }) {
  const passed = results.filter((result) => result.status === 'PASS')
  const failed = results.filter((result) => result.status !== 'PASS')
  const selectedIds = selected.map((module) => module.moduleId)
  const selectedProjectNames = selected.map((module) => module.projectName)
  const passedIds = passed.map((result) => result.moduleId)
  const status = listOnly
    ? 'READY'
    : (passedIds.length === selectedIds.length && failed.length === 0 ? 'PASS' : 'FAIL')
  return {
    schema: 'echo.neoforge.gametest_results.v1',
    generatedAt: new Date().toISOString(),
    status,
    runtime: 'neoforge',
    evidenceKind: listOnly ? 'neoforge-gametest-selection' : 'executed-neoforge-gametest-results',
    repoRoot: normalizePath(repoRoot),
    output: normalizePath(path.resolve(repoRoot, output)),
    moduleIds: passedIds,
    selectedModuleIds: selectedIds,
    selectedProjectNames,
    selectedModuleCount: selectedIds.length,
    passedModuleIds: passedIds,
    failedModuleIds: failed.map((result) => result.moduleId),
    allSelectedModulesPassed: !listOnly && passedIds.length === selectedIds.length && failed.length === 0,
    trustedMutations: passed.map((result) => `NeoForge GameTest server completed for ${result.moduleId}.`),
    visibleRoutes: [],
    saveEvidence: passed.flatMap((result) =>
      result.testNames.some((name) => /save|reload|sync|network|packet/i.test(name))
        ? [`NeoForge GameTest execution for ${result.moduleId} included save/reload/network-named tests.`]
        : []),
    networkEvidence: passed.flatMap((result) =>
      result.testNames.some((name) => /network|packet|sync/i.test(name))
        ? [`NeoForge GameTest execution for ${result.moduleId} included network/sync-named tests.`]
        : []),
    results,
    blockers: listOnly
      ? ['GameTest modules were listed but not executed; rerun without --list.']
      : failed.map((result) => `${result.moduleId}: ${result.timedOut ? 'timed out' : `Gradle task exited ${result.exitCode}`}`),
  }
}

async function discoverGameTestModules(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const moduleRoot = path.join(addonsRoot, entry.name)
    const buildGradlePath = path.join(moduleRoot, 'build.gradle')
    const buildGradle = await readTextIfExists(buildGradlePath)
    if (!buildGradle || !buildGradle.includes('gameTestServer')) continue
    const descriptorPath = path.join(moduleRoot, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const testFiles = await listFiles(path.join(moduleRoot, 'src', 'test', 'java'))
    const gameTestFiles = []
    const testNames = new Set()
    for (const file of testFiles) {
      const text = await fs.readFile(file.absolute, 'utf8')
      if (!/GameTestHelper|RegisterGameTestsEvent|TEST_FUNCTIONS|gametest/i.test(text)) continue
      gameTestFiles.push(normalizePath(path.relative(moduleRoot, file.absolute)))
      for (const match of text.matchAll(/TEST_FUNCTIONS\.register\("([^"]+)"/g)) {
        testNames.add(match[1])
      }
      for (const match of text.matchAll(/register\(event,[^;]*"([^"]+)"/g)) {
        testNames.add(match[1])
      }
    }
    if (gameTestFiles.length === 0) continue
    modules.push({
      moduleId: string(descriptor.id) || entry.name,
      projectName: entry.name,
      testFiles: unique(gameTestFiles),
      testNames: unique([...testNames]),
    })
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

async function listFiles(root, base = root) {
  if (!(await exists(root))) return []
  const entries = await fs.readdir(root, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) {
      files.push(...await listFiles(absolute, base))
    } else if (entry.isFile()) {
      files.push({ absolute, relative: normalizePath(path.relative(base, absolute)) })
    }
  }
  return files
}

async function readJson(filePath) {
  const text = await fs.readFile(filePath, 'utf8')
  return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
}

async function readTextIfExists(filePath) {
  try {
    return await fs.readFile(filePath, 'utf8')
  } catch (error) {
    if (error && error.code === 'ENOENT') return ''
    throw error
  }
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function parseArgs(argv) {
  const options = {
    repoRoot: process.cwd(),
    modules: [],
    allWithGameTests: false,
    output: DEFAULT_OUTPUT,
    gradle: process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew',
    continueOnFailure: false,
    timeoutMs: 15 * 60 * 1000,
    listOnly: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--modules') options.modules = splitList(argv[++index])
    else if (arg === '--all-with-gametests') options.allWithGameTests = true
    else if (arg === '--output') options.output = argv[++index]
    else if (arg === '--gradle') options.gradle = argv[++index]
    else if (arg === '--continue-on-failure') options.continueOnFailure = true
    else if (arg === '--timeout-ms') options.timeoutMs = Number(argv[++index])
    else if (arg === '--list') options.listOnly = true
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

function splitList(value) {
  return String(value ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function tail(value, maxLength = 12000) {
  const text = String(value ?? '')
  return text.length <= maxLength ? text : text.slice(text.length - maxLength)
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function unique(values) {
  return [...new Set(values)].sort()
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/run-neoforge-gametests-and-write-evidence.mjs --modules <id,id> | --all-with-gametests [--output <path>] [--continue-on-failure] [--timeout-ms <ms>] [--list]')
    } else {
      const report = await runNeoForgeGameTestsAndWriteEvidence(options)
      console.log(`${report.status} ${report.moduleIds.length}/${report.selectedModuleCount} NeoForge GameTest module(s): ${report.output}`)
      if (report.status === 'FAIL') {
        throw new Error(`NeoForge GameTest evidence failed: ${report.blockers.join('; ')}`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
