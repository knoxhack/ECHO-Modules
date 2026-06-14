import { spawn } from 'node:child_process'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_OUT_DIR = 'reports/runtime-parity'

export async function generateStrictPlayEvidence({
  repoRoot = process.cwd(),
  echoRoot = path.dirname(path.resolve(repoRoot)),
  outDir = DEFAULT_OUT_DIR,
  skipNative = false,
  skipStandalone = false,
  skipPackAcceptance = false,
  writePackAcceptance = true,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedEchoRoot = path.resolve(echoRoot)
  const generatedAt = new Date().toISOString()
  const commands = [
    {
      key: 'neoforge',
      repo: 'ECHO-Modules',
      cwd: normalizedRoot,
      command: 'node',
      args: ['scripts/generate-neoforge-runtime-evidence.mjs'],
    },
  ]
  if (!skipNative) {
    commands.push({
      key: 'native',
      repo: 'ECHO-Native-Platform',
      cwd: path.join(normalizedEchoRoot, 'ECHO-Native-Platform'),
      command: 'node',
      args: ['scripts/generate-native-strict-play-evidence.mjs'],
    })
  }
  if (!skipStandalone) {
    commands.push({
      key: 'standalone',
      repo: 'ECHO-Standalone-Runtime',
      cwd: path.join(normalizedEchoRoot, 'ECHO-Standalone-Runtime'),
      command: 'node',
      args: ['scripts/generate-standalone-strict-play-evidence.mjs'],
    })
  }
  if (!skipPackAcceptance) {
    commands.push({
      key: 'packAcceptance',
      repo: 'ECHO-Modules',
      cwd: normalizedRoot,
      command: 'node',
      args: [
        'scripts/generate-pack-acceptance-reports.mjs',
        ...(writePackAcceptance ? ['--write', '--seed-automated-evidence'] : []),
      ],
    })
  }

  const results = []
  for (const task of commands) {
    results.push(await runTask(task))
  }
  const report = {
    schema: 'echo.module.strict_play_evidence_refresh.v1',
    generatedAt,
    repoRoot: normalizePath(normalizedRoot),
    echoRoot: normalizePath(normalizedEchoRoot),
    status: results.every((result) => result.exitCode === 0) ? 'PASS' : 'FAIL',
    results,
  }
  const output = path.join(normalizedRoot, outDir, 'strict-play-evidence-refresh.json')
  await fs.mkdir(path.dirname(output), { recursive: true })
  await fs.writeFile(output, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  return { report, path: output }
}

function runTask(task) {
  return new Promise((resolve) => {
    const startedAt = Date.now()
    const child = spawn(task.command, task.args, {
      cwd: task.cwd,
      windowsHide: true,
    })
    let stdout = ''
    let stderr = ''
    child.stdout.setEncoding('utf8')
    child.stderr.setEncoding('utf8')
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', (error) => {
      resolve(taskResult(task, {
        exitCode: null,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr: `${stderr}${stderr ? '\n' : ''}${error.message}`,
        spawnError: error.message,
      }))
    })
    child.on('close', (exitCode) => {
      resolve(taskResult(task, {
        exitCode,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr,
        spawnError: '',
      }))
    })
  })
}

function taskResult(task, result) {
  return {
    key: task.key,
    repo: task.repo,
    cwd: normalizePath(task.cwd),
    command: [task.command, ...task.args],
    exitCode: result.exitCode,
    ok: result.exitCode === 0,
    durationMs: result.durationMs,
    stdoutTail: tail(result.stdout),
    stderrTail: tail(result.stderr),
    spawnError: result.spawnError,
  }
}

function tail(value, maxLength = 12000) {
  const text = String(value ?? '')
  return text.length <= maxLength ? text : text.slice(text.length - maxLength)
}

function parseArgs(argv) {
  const options = {
    repoRoot: process.cwd(),
    echoRoot: '',
    outDir: DEFAULT_OUT_DIR,
    skipNative: false,
    skipStandalone: false,
    skipPackAcceptance: false,
    writePackAcceptance: true,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--echo-root') options.echoRoot = argv[++index]
    else if (arg === '--out-dir') options.outDir = argv[++index]
    else if (arg === '--skip-native') options.skipNative = true
    else if (arg === '--skip-standalone') options.skipStandalone = true
    else if (arg === '--skip-pack-acceptance') options.skipPackAcceptance = true
    else if (arg === '--no-write-pack-acceptance') options.writePackAcceptance = false
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  if (!options.echoRoot) options.echoRoot = path.dirname(path.resolve(options.repoRoot))
  return options
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-strict-play-evidence.mjs [--repo-root <path>] [--echo-root <path>] [--out-dir <path>] [--skip-native] [--skip-standalone] [--skip-pack-acceptance] [--no-write-pack-acceptance]')
    } else {
      const { report, path: output } = await generateStrictPlayEvidence(options)
      console.log(`Wrote strict-play evidence refresh report: ${output}`)
      for (const result of report.results) {
        console.log(`${result.ok ? 'PASS' : 'FAIL'} ${result.repo}: ${result.command.join(' ')}`)
      }
      if (report.status !== 'PASS') {
        throw new Error(`Strict-play evidence refresh failed: ${report.results.filter((result) => !result.ok).length} command(s) failed.`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
