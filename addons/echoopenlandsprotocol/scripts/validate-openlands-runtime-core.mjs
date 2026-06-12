import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const MODULE_ID = 'echoopenlandsprotocol'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    json: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const descriptor = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(descriptor)) return cursor
    const candidate = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(candidate)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function listJavaFiles(root) {
  const files = []
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) files.push(...listJavaFiles(absolute))
    else if (entry.isFile() && entry.name.endsWith('.java')) files.push(absolute)
  }
  return files
}

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    shell: false,
  })
  return {
    status: result.status,
    stdout: result.stdout?.trim() ?? '',
    stderr: result.stderr?.trim() ?? '',
  }
}

function writeSmokeSource(outputRoot) {
  const smokeSource = `
import com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsBuilderActionSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsCropGrowthSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsFirstHourRuntime;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsShelterSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsStarterSpawnSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsWaystoneRuntime;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsWaystoneState;
import java.util.Map;

public final class OpenlandsRuntimeCoreSmoke {
    public static void main(String[] args) {
        check(OpenlandsFirstHourRuntime.standardRules().hardcoreMetersOff(), "standard rules must keep hardcore meters off");
        check(OpenlandsFirstHourRuntime.standardRules().deathPack().equals("recoverable"), "standard rules must keep recoverable death pack");
        check(OpenlandsRuntimeContracts.adapterManifest().containsKey("playableRuntimeCore"), "adapter manifest must expose playable runtime core");
        check(OpenlandsFirstHourRuntime.firstHourStepIds().size() == 7, "first hour route must expose seven steps");

        var goodSpawn = OpenlandsFirstHourRuntime.validateStarterSpawn(new OpenlandsStarterSpawnSnapshot(
                "meadows", 48, 96, 24, true, true, true, true, true, true));
        check(goodSpawn.accepted(), "valid starter spawn must be accepted");

        var badSpawn = OpenlandsFirstHourRuntime.validateStarterSpawn(new OpenlandsStarterSpawnSnapshot(
                "stonehills", 80, 160, 4, true, false, false, true, false, false));
        check(!badSpawn.accepted(), "invalid starter spawn must be rejected");
        check(badSpawn.missing().contains("loose_stone_found"), "invalid starter spawn must report missing loose stone");
        check(badSpawn.missing().contains("water_or_well_hint_found"), "invalid starter spawn must report missing water or well hint");

        var forgivingShelter = OpenlandsFirstHourRuntime.scoreShelter(new OpenlandsShelterSnapshot(
                0.65, 0.65, true, true, true, 18));
        check(forgivingShelter.sleepMilestoneAllowed(), "forgiving shelter must allow sleep milestone");
        check(forgivingShelter.total() >= 55, "forgiving shelter must score at least 55");

        var weakShelter = OpenlandsFirstHourRuntime.scoreShelter(new OpenlandsShelterSnapshot(
                0.1, 0.1, false, false, false, 3));
        check(!weakShelter.sleepMilestoneAllowed(), "weak shelter must not allow sleep milestone");

        Map<String, Integer> inputs = Map.of(
                "fieldstone_piece", 8,
                "repair_kit", 1,
                "copper_fitting", 4,
                "waystone_core", 1,
                "glow_crystal", 1,
                "route_binding", 1);
        OpenlandsWaystoneState state = OpenlandsWaystoneState.UNDISCOVERED;
        int transitions = 0;
        while (!state.active()) {
            var transition = OpenlandsFirstHourRuntime.advanceWaystone(state, inputs);
            check(transition.accepted(), "waystone transition must be accepted from " + state.id());
            state = transition.after();
            transitions += 1;
        }
        check(transitions == 7, "waystone must advance through seven transitions to active");
        check(OpenlandsWaystoneRuntime.fastTravelUnlocked(2), "two active waystones must unlock fast travel");
        check(!OpenlandsWaystoneRuntime.fastTravelUnlocked(1), "one active waystone must not unlock fast travel");

        var pausedCrop = OpenlandsFirstHourRuntime.advanceCrop(new OpenlandsCropGrowthSnapshot(
                "grain", 1, 24, false, false, false));
        check(pausedCrop.paused(), "standard unwatered grain should pause");
        check(!pausedCrop.failed(), "standard unwatered grain must not fail or die");

        var advancedCrop = OpenlandsFirstHourRuntime.advanceCrop(new OpenlandsCropGrowthSnapshot(
                "root_crop", 2, 13, true, true, false));
        check(advancedCrop.afterStage() == 3, "watered composted root crop should advance");
        check(advancedCrop.harvestReady(), "root crop should become harvest ready at final stage");

        check(!OpenlandsFirstHourRuntime.cookpotMealReady(2, 400, false), "cookpot must require three ingredients");
        check(!OpenlandsFirstHourRuntime.cookpotMealReady(3, 100, false), "cookpot must require cook time");
        check(OpenlandsFirstHourRuntime.cookpotMealReady(3, 200, false), "cookpot should become ready with three ingredients and cook time");

        var hammer = OpenlandsFirstHourRuntime.validateBuilderAction(new OpenlandsBuilderActionSnapshot(
                "wooden_hammer", true, true, true, true, false, true, true));
        check(hammer.accepted(), "valid hammer action should be accepted");

        var unsafeHammer = OpenlandsFirstHourRuntime.validateBuilderAction(new OpenlandsBuilderActionSnapshot(
                "wooden_hammer", true, true, true, true, false, false, true));
        check(!unsafeHammer.accepted(), "hammer action must require server validation");

        var quickStack = OpenlandsFirstHourRuntime.validateBuilderAction(new OpenlandsBuilderActionSnapshot(
                "quick_stack", false, false, false, false, true, true, true));
        check(quickStack.accepted(), "quick stack should accept permitted server-authoritative transfer");

        var unsafeQuickStack = OpenlandsFirstHourRuntime.validateBuilderAction(new OpenlandsBuilderActionSnapshot(
                "quick_stack", false, false, false, false, true, false, true));
        check(!unsafeQuickStack.accepted(), "quick stack must reject non-server-authoritative transfer");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
`
  const smokePath = path.join(outputRoot, 'OpenlandsRuntimeCoreSmoke.java')
  fs.writeFileSync(smokePath, smokeSource, 'utf8')
  return smokePath
}

function validate({ moduleRoot }) {
  const errors = []
  const srcRoot = path.join(moduleRoot, 'src', 'main', 'java', 'com', 'knoxhack', 'echoopenlandsprotocol')
  const contractRoot = path.join(srcRoot, 'contract')
  const runtimeRoot = path.join(srcRoot, 'runtime')
  for (const root of [contractRoot, runtimeRoot]) {
    if (!fileExists(root)) errors.push(`Missing source root: ${root}`)
  }
  if (errors.length) {
    return { status: 'failed', moduleRoot, errors }
  }

  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-runtime-core-'))
  const classesRoot = path.join(tempRoot, 'classes')
  fs.mkdirSync(classesRoot, { recursive: true })
  const smokePath = writeSmokeSource(tempRoot)
  const sourceFiles = [
    ...listJavaFiles(contractRoot),
    ...listJavaFiles(runtimeRoot),
    path.join(srcRoot, 'EchoOpenlandsProtocol.java'),
    smokePath,
  ]

  const compile = run('javac', ['-d', classesRoot, ...sourceFiles], moduleRoot)
  if (compile.status !== 0) {
    errors.push(`javac failed: ${compile.stderr || compile.stdout}`)
    return { status: 'failed', moduleRoot, tempRoot, errors }
  }

  const smoke = run('java', ['-cp', classesRoot, 'OpenlandsRuntimeCoreSmoke'], moduleRoot)
  if (smoke.status !== 0) {
    errors.push(`runtime smoke failed: ${smoke.stderr || smoke.stdout}`)
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    moduleRoot,
    tempRoot,
    compiledSources: sourceFiles.length,
    errors,
  }
}

function printHelp() {
  console.log(`Usage: node scripts/validate-openlands-runtime-core.mjs [options]

Options:
  --module-root <path>   Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --json                 Print JSON output.
  --help                 Show this help.
`)
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
  } else {
    const moduleRoot = findModuleRoot(args.moduleRoot)
    const result = validate({ moduleRoot })
    if (args.json) {
      console.log(JSON.stringify(result, null, 2))
    } else if (result.status === 'passed') {
      console.log(`Openlands runtime core validation passed: compiled ${result.compiledSources} source files and ran first-hour smoke checks.`)
    } else {
      console.error(`Openlands runtime core validation failed with ${result.errors.length} error(s):`)
      for (const error of result.errors) console.error(`- ${error}`)
      process.exitCode = 1
    }
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
}
