import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-runtime-core-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-runtime-core-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-runtime-core-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleArtifact: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-artifact') args.moduleArtifact = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
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

function listJavaFiles(root) {
  const files = []
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) files.push(...listJavaFiles(absolute))
    else if (entry.isFile() && entry.name.endsWith('.java')) files.push(absolute)
  }
  return files
}

function defaultModuleArtifact(edition) {
  const scriptDir = path.dirname(fileURLToPath(import.meta.url))
  return path.resolve(scriptDir, '..', '..', '..', 'dist', 'echo-module-release', MODULE_ID, edition.artifactName)
}

function writeSmokeSource(outputRoot, edition) {
  const source = `
import com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsBuilderActionSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsCropGrowthSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsFirstHourRuntime;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsShelterSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsStarterSpawnSnapshot;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsWaystoneRuntime;
import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsWaystoneState;
import java.util.Map;

public final class OpenlandsEditionRuntimeCoreSmoke {
    public static void main(String[] args) {
        check(OpenlandsRuntimeContracts.MODULE_ID.equals("${MODULE_ID}"), "module id mismatch");
        check(OpenlandsRuntimeContracts.VERSION.equals("${VERSION}"), "module version mismatch");
        check(OpenlandsRuntimeContracts.RUNTIME_TARGETS.contains("${edition.runtimeTarget}"), "runtime target missing");
        check(OpenlandsRuntimeContracts.adapterManifest().containsKey("playableRuntimeCore"), "adapter manifest missing playableRuntimeCore");

        var standard = OpenlandsFirstHourRuntime.standardRules();
        check(standard.modeId().equals("openlands_standard"), "standard mode id mismatch");
        check(standard.hunger().equals("gentle"), "standard hunger must be gentle");
        check(standard.hardcoreMetersOff(), "standard rules must keep hardcore meters off");
        check(standard.deathPack().equals("recoverable"), "standard death pack must be recoverable");
        check(OpenlandsFirstHourRuntime.firstHourStepIds().equals(java.util.List.of(
                "safe_spawn",
                "first_gathering",
                "first_tools",
                "first_shelter",
                "sleep_and_recover",
                "first_exploration_hook",
                "first_waystone")), "first-hour route order mismatch");

        var goodSpawn = OpenlandsFirstHourRuntime.validateStarterSpawn(new OpenlandsStarterSpawnSnapshot(
                "woodlands", 52, 100, 20, true, true, true, true, true, true));
        check(goodSpawn.accepted(), "valid starter spawn must be accepted");

        var badSpawn = OpenlandsFirstHourRuntime.validateStarterSpawn(new OpenlandsStarterSpawnSnapshot(
                "marshlands", 90, 140, 6, false, false, true, false, true, false));
        check(!badSpawn.accepted(), "invalid starter spawn must be rejected");
        check(badSpawn.missing().contains("wood_source_found"), "bad spawn must report missing wood");
        check(badSpawn.missing().contains("loose_stone_found"), "bad spawn must report missing stone");

        var forgivingShelter = OpenlandsFirstHourRuntime.scoreShelter(new OpenlandsShelterSnapshot(
                0.7, 0.6, true, true, true, 20));
        check(forgivingShelter.sleepMilestoneAllowed(), "forgiving shelter must allow sleep");
        check(forgivingShelter.total() >= 55, "forgiving shelter score must reach 55");

        var weakShelter = OpenlandsFirstHourRuntime.scoreShelter(new OpenlandsShelterSnapshot(
                0.0, 0.15, false, false, false, 2));
        check(!weakShelter.sleepMilestoneAllowed(), "weak shelter must not allow sleep");

        Map<String, Integer> inputs = Map.of(
                "fieldstone_piece", 8,
                "repair_kit", 1,
                "copper_fitting", 4,
                "waystone_core", 1,
                "glow_crystal", 1,
                "route_binding", 1);
        OpenlandsWaystoneState state = OpenlandsWaystoneState.UNDISCOVERED;
        while (!state.active()) {
            var transition = OpenlandsFirstHourRuntime.advanceWaystone(state, inputs);
            check(transition.accepted(), "waystone transition rejected from " + state.id());
            state = transition.after();
        }
        check(state == OpenlandsWaystoneState.ACTIVE, "waystone must reach active");
        check(OpenlandsWaystoneRuntime.fastTravelUnlocked(2), "two active stones unlock fast travel");
        check(!OpenlandsWaystoneRuntime.fastTravelUnlocked(1), "one active stone must not unlock fast travel");

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
  const smokePath = path.join(outputRoot, 'OpenlandsEditionRuntimeCoreSmoke.java')
  fs.writeFileSync(smokePath, source, 'utf8')
  return smokePath
}

function artifactEntries(artifactPath) {
  const result = run('jar', ['tf', artifactPath], path.dirname(artifactPath))
  if (result.status !== 0) {
    throw new Error(`jar tf failed: ${result.stderr || result.stdout}`)
  }
  return result.stdout.split(/\r?\n/).filter(Boolean)
}

function buildReport({ editionKey, editionRoot, artifactPath, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`Unknown edition "${editionKey}". Expected one of ${Object.keys(EDITIONS).join(', ')}`)
  if (!fileExists(artifactPath)) throw new Error(`Openlands module artifact not found: ${artifactPath}`)
  if (!fileExists(editionRoot)) throw new Error(`Edition root not found: ${editionRoot}`)

  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), `openlands-${editionKey}-runtime-core-`))
  const packageEntries = artifactEntries(artifactPath)
  const nestedRuntimeEntry = packageEntries.find((entry) => /^lib\/.*-runtime\.jar$/.test(entry))
  let inspectArtifactPath = artifactPath
  let entries = packageEntries
  if (nestedRuntimeEntry) {
    const packageRoot = path.join(tempRoot, 'package')
    fs.mkdirSync(packageRoot, { recursive: true })
    const extractPackage = run('jar', ['xf', artifactPath], packageRoot)
    if (extractPackage.status !== 0) {
      throw new Error(`jar xf package failed: ${extractPackage.stderr || extractPackage.stdout}`)
    }
    inspectArtifactPath = path.join(packageRoot, nestedRuntimeEntry)
    entries = artifactEntries(inspectArtifactPath)
  }
  const sourceRequiredEntries = [
    'src/main/resources/META-INF/echo.mod.json',
    'src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json',
    'src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsFirstHourRuntime.java',
    'src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsShelterScoring.java',
    'src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneRuntime.java',
    'src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsHomesteadRuntime.java',
    'src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsBuilderUxRuntime.java',
  ]
  const compiledRequiredEntries = [
    'META-INF/echo.mod.json',
    'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json',
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsFirstHourRuntime.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsShelterScoring.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneRuntime.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsHomesteadRuntime.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsBuilderUxRuntime.class',
  ]
  const sourcePackaged = sourceRequiredEntries.every((entry) => entries.includes(entry))
  const compiledRuntime = compiledRequiredEntries.every((entry) => entries.includes(entry))
  const requiredEntries = compiledRuntime ? compiledRequiredEntries : sourceRequiredEntries
  const missingEntries = requiredEntries.filter((entry) => !entries.includes(entry))
  if (missingEntries.length) throw new Error(`Artifact missing required entries: ${missingEntries.join(', ')}`)

  const extractRoot = path.join(tempRoot, 'artifact')
  const classesRoot = path.join(tempRoot, 'classes')
  fs.mkdirSync(extractRoot, { recursive: true })
  fs.mkdirSync(classesRoot, { recursive: true })

  const extract = run('jar', ['xf', inspectArtifactPath], extractRoot)
  if (extract.status !== 0) {
    throw new Error(`jar xf failed: ${extract.stderr || extract.stdout}`)
  }

  const sourceRoot = path.join(extractRoot, 'src', 'main', 'java', 'com', 'knoxhack', 'echoopenlandsprotocol')
  const contractRoot = path.join(sourceRoot, 'contract')
  const runtimeRoot = path.join(sourceRoot, 'runtime')
  const smokePath = writeSmokeSource(tempRoot, edition)
  const sourceFiles = compiledRuntime
    ? [smokePath]
    : [
        ...listJavaFiles(contractRoot),
        ...listJavaFiles(runtimeRoot),
        smokePath,
      ]

  const compileArgs = compiledRuntime
    ? ['-cp', inspectArtifactPath, '-d', classesRoot, ...sourceFiles]
    : ['-d', classesRoot, ...sourceFiles]
  const compile = run('javac', compileArgs, editionRoot)
  if (compile.status !== 0) {
    throw new Error(`javac failed: ${compile.stderr || compile.stdout}`)
  }

  const smokeClasspath = compiledRuntime ? `${classesRoot}${path.delimiter}${inspectArtifactPath}` : classesRoot
  const smoke = run('java', ['-cp', smokeClasspath, 'OpenlandsEditionRuntimeCoreSmoke'], editionRoot)
  if (smoke.status !== 0) {
    throw new Error(`runtime core smoke failed: ${smoke.stderr || smoke.stdout}`)
  }

  const report = {
    schema: 'echo.openlands.edition.runtime_core_report.v1',
    status: 'passed',
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    artifactMode: compiledRuntime ? 'compiled-runtime' : sourcePackaged ? 'source-packaged' : 'unknown',
    artifactPath,
    inspectedArtifactPath: artifactPath,
    inspectedArtifactEntry: nestedRuntimeEntry ?? null,
    outputPath,
    artifactEntriesChecked: requiredEntries,
    compiledSources: sourceFiles.length,
    runtimeCorePackage: 'com.knoxhack.echoopenlandsprotocol.runtime',
    callableHooks: [
      'standardRules',
      'validateStarterSpawn',
      'scoreShelter',
      'advanceWaystone',
      'advanceCrop',
      'cookpotMealReady',
      'validateBuilderAction',
      'firstHourStepIds',
      'adapterBindingManifest',
    ],
    proofs: [
      'artifact_contains_playable_runtime_contract',
      compiledRuntime ? 'artifact_contains_compiled_runtime_core_classes' : 'artifact_contains_runtime_core_sources',
      'adapter_manifest_exposes_playable_runtime_core',
      'standard_rules_keep_hardcore_meters_off',
      'valid_starter_spawn_is_accepted',
      'invalid_starter_spawn_reports_missing_signals',
      'forgiving_shelter_score_reaches_sleep_threshold',
      'weak_shelter_score_does_not_reach_sleep_threshold',
      'waystone_advances_to_active_with_required_inputs',
      'two_active_waystones_unlock_fast_travel',
      'standard_crop_pauses_without_dying',
      'watered_composted_crop_advances',
      'cookpot_requires_three_ingredients_and_cook_time',
      'builder_hammer_requires_server_validation',
      'storage_commands_require_permission_and_server_authority',
    ],
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  return report
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const artifactPath = args.moduleArtifact ? path.resolve(args.moduleArtifact) : defaultModuleArtifact(edition)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildReport({
    editionKey: args.edition,
    editionRoot,
    artifactPath,
    outputPath,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} runtime core report ${action}: ${report.proofs.length} proofs.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-runtime-core-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-artifact <p>   Openlands artifact to inspect. Defaults to generated local dist artifact.
  --out <path>            Report output path. Defaults to evidence/<edition>-runtime-core-report.json.
  --dry-run               Compile and smoke-test without writing the report.
  --json                  Print JSON output.
  --help                  Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}
