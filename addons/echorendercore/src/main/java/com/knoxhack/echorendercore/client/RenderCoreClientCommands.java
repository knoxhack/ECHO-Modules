package com.knoxhack.echorendercore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.api.IAdvancedVisualBlockEntity;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.profile.BlockPartSelectorProfile;
import com.knoxhack.echorendercore.profile.CreatorCertificationReport;
import com.knoxhack.echorendercore.profile.CreatorExportIndex;
import com.knoxhack.echorendercore.profile.CreatorMigrationReport;
import com.knoxhack.echorendercore.profile.CreatorProfileCard;
import com.knoxhack.echorendercore.profile.CreatorVisualQaReport;
import com.knoxhack.echorendercore.profile.ProfileValidationIssue;
import com.knoxhack.echorendercore.profile.RenderCoreCreatorPackExporter;
import com.knoxhack.echorendercore.profile.RenderCoreProfileMigration;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.RenderCoreScreenChromeQaCatalog;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class RenderCoreClientCommands {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String VISUAL_DIR = "rendercore/visual_profiles";

   private RenderCoreClientCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("rendercore")
            .then(Commands.literal("reload")
               .executes(context -> reload()))
            .then(Commands.literal("validate")
               .executes(context -> validate("all"))
               .then(Commands.argument("namespace", StringArgumentType.word())
                  .executes(context -> validate(StringArgumentType.getString(context, "namespace")))))
            .then(Commands.literal("creator")
               .executes(context -> creatorStatus())
               .then(Commands.literal("status")
                  .executes(context -> creatorStatus()))
               .then(Commands.literal("open")
                  .executes(context -> creatorOpen()))
               .then(Commands.literal("index")
                  .executes(context -> creatorIndex("all"))
                  .then(Commands.argument("namespace", StringArgumentType.word())
                     .executes(context -> creatorIndex(StringArgumentType.getString(context, "namespace")))))
               .then(Commands.literal("profile")
                  .then(Commands.argument("profile", StringArgumentType.word())
                     .executes(context -> creatorProfile(StringArgumentType.getString(context, "profile")))))
               .then(Commands.literal("export")
                  .executes(context -> creatorExport("all"))
                  .then(Commands.argument("namespace", StringArgumentType.word())
                     .executes(context -> creatorExport(StringArgumentType.getString(context, "namespace")))))
               .then(Commands.literal("certify")
                  .executes(context -> creatorCertify("all"))
                  .then(Commands.argument("namespace", StringArgumentType.word())
                     .executes(context -> creatorCertify(StringArgumentType.getString(context, "namespace")))))
               .then(Commands.literal("migrate")
                  .then(Commands.argument("namespace", StringArgumentType.word())
                     .then(Commands.literal("dryrun")
                        .executes(context -> creatorMigrate(StringArgumentType.getString(context, "namespace"), false)))
                     .then(Commands.literal("write")
                        .executes(context -> creatorMigrate(StringArgumentType.getString(context, "namespace"), true))))))
            .then(Commands.literal("debug")
               .then(Commands.literal("state")
                  .then(Commands.argument("state", StringArgumentType.word())
                     .executes(context -> forceState(StringArgumentType.getString(context, "state"), 30))
                     .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                        .executes(context -> forceState(
                           StringArgumentType.getString(context, "state"),
                           IntegerArgumentType.getInteger(context, "seconds")
                        )))))
               .then(Commands.literal("missingparts")
                  .then(Commands.argument("enabled", BoolArgumentType.bool())
                     .executes(context -> missingParts(BoolArgumentType.getBool(context, "enabled")))))
               .then(Commands.literal("hud")
                  .then(Commands.argument("enabled", BoolArgumentType.bool())
                     .executes(context -> hud(BoolArgumentType.getBool(context, "enabled")))))
               .then(Commands.literal("anchors")
                  .then(Commands.argument("enabled", BoolArgumentType.bool())
                     .executes(context -> anchors(BoolArgumentType.getBool(context, "enabled")))))
               .then(Commands.literal("advancedfx")
                  .executes(context -> advancedFxStatus())
                  .then(Commands.literal("status")
                     .executes(context -> advancedFxStatus()))
                  .then(Commands.literal("reset")
                     .executes(context -> advancedFxReset()))
                  .then(Commands.literal("evidence")
                     .then(Commands.literal("start")
                        .executes(context -> advancedFxEvidenceStart()))
                     .then(Commands.literal("capture")
                        .executes(context -> advancedFxEvidenceCapture("manual"))
                        .then(Commands.argument("label", StringArgumentType.word())
                           .executes(context -> advancedFxEvidenceCapture(StringArgumentType.getString(context, "label")))))
                     .then(Commands.literal("status")
                        .executes(context -> advancedFxEvidenceStatus()))
                     .then(Commands.literal("export")
                        .executes(context -> advancedFxEvidenceExport()))
                     .then(Commands.literal("reset")
                        .executes(context -> advancedFxEvidenceReset())))
                  .then(Commands.argument("enabled", BoolArgumentType.bool())
                     .executes(context -> advancedFx(BoolArgumentType.getBool(context, "enabled")))))
               .then(Commands.literal("screenchrome")
                  .executes(context -> screenChromeEvidenceStatus())
                  .then(Commands.literal("status")
                     .executes(context -> screenChromeEvidenceStatus()))
                  .then(Commands.literal("evidence")
                     .then(Commands.literal("start")
                        .executes(context -> screenChromeEvidenceStart()))
                     .then(Commands.literal("capture")
                        .then(Commands.argument("surface", StringArgumentType.word())
                           .executes(context -> screenChromeEvidenceCapture(StringArgumentType.getString(context, "surface")))))
                     .then(Commands.literal("status")
                        .executes(context -> screenChromeEvidenceStatus()))
                     .then(Commands.literal("export")
                        .executes(context -> screenChromeEvidenceExport()))
                     .then(Commands.literal("reset")
                        .executes(context -> screenChromeEvidenceReset()))))
               .then(Commands.literal("blockparts")
                  .executes(context -> blockParts())))
      );
   }

   private static int reload() {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.reloadResourcePacks();
      message("RenderCore profiles queued for reload.");
      return 1;
   }

   private static int forceState(String stateName, int seconds) {
      VisualState state = VisualState.byName(stateName, VisualState.IDLE);
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.hitResult == null) {
         message("No client target is available.");
         return 0;
      }
      HitResult hit = minecraft.hitResult;
      if (hit instanceof EntityHitResult entityHit) {
         Entity entity = entityHit.getEntity();
         DebugVisualOverrides.setEntity(entity.getUUID(), state, seconds);
         message("RenderCore state override set to " + state.name() + " for " + entity.getName().getString() + ".");
         return 1;
      }
      if (hit instanceof BlockHitResult blockHit) {
         BlockEntity blockEntity = minecraft.level.getBlockEntity(blockHit.getBlockPos());
         if (blockEntity instanceof IAdvancedVisualBlockEntity || blockEntity != null) {
            DebugVisualOverrides.setBlock(minecraft.level, blockHit.getBlockPos(), state, seconds);
            message("RenderCore block state override set to " + state.name() + ".");
            return 1;
         }
      }
      message("Target does not expose a RenderCore visual profile.");
      return 0;
   }

   private static int missingParts(boolean enabled) {
      DebugVisualOverrides.setMissingPartWarnings(enabled);
      message("RenderCore missing part warnings " + (enabled ? "enabled." : "disabled."));
      return 1;
   }

   private static int validate(String namespace) {
      var report = RenderCoreProfiles.loaded().validationReport().forNamespace(namespace);
      String normalized = namespace == null || namespace.isBlank() ? "all" : namespace;
      long shown = report.issues().stream()
         .limit(6)
         .peek(RenderCoreClientCommands::messageIssue)
         .count();
      if (RenderCoreAdvancedFxPipeline.compileFailed()) {
         message("[WARNING] advanced_effect_compile_failed: " + RenderCoreAdvancedFxPipeline.unavailableReason());
      }
      message("RenderCore validation: " + report.summaryLine() + " / " + RenderCoreEffectPipeline.statusLine()
         + " / mode " + RenderCoreAdvancedFxPipeline.modeLine()
         + " / masks " + RenderCoreAdvancedFxPipeline.lastMaskSubmissionCount()
         + " skipped " + RenderCoreAdvancedFxPipeline.lastSkippedSubmissions()
         + " channels " + RenderCoreAdvancedFxPipeline.lastChannelCount()
         + " downscale " + RenderCoreAdvancedFxPipeline.lastDownscale()
         + " passes " + RenderCoreAdvancedFxPipeline.lastPassCount()
         + " bloomCost " + RenderCoreAdvancedFxPipeline.lastBloomCost()
         + ". Showing " + shown + " issue(s) for " + normalized + ".");
      return report.hasErrors() ? 0 : 1;
   }

   private static int creatorStatus() {
      CreatorExportIndex export = creatorExportIndex();
      long migrationRequired = export.certification().migrationRequiredCount();
      long screenshots = export.cards().stream().filter(CreatorProfileCard::screenshotAvailable).count();
      message("RenderCore Creator Workbench: profiles " + export.cards().size()
         + ", certification " + export.certification().status().id()
         + ", visual QA blockers " + export.visualQa().totalBlockerCount()
         + ", migration required " + migrationRequired
         + ", screenshots " + screenshots
         + ", addon coverage " + export.addonIntegrations().size()
         + ", export root " + creatorRoot("all") + ".");
      return 1;
   }

   private static int creatorOpen() {
      Minecraft.getInstance().setScreen(new RenderCoreCreatorWorkbenchScreen(
         creatorExportIndex()
      ));
      return 1;
   }

   private static int creatorIndex(String namespace) {
      CreatorExportIndex export = creatorExportIndex().forNamespace(namespace);
      String normalized = normalizeNamespace(namespace);
      export.cards().stream()
         .sorted(Comparator.comparing(card -> card.profileId().toString()))
         .limit(8)
         .forEach(card -> message("Creator profile " + card.profileId()
            + " schema " + card.schemaVersion()
            + " effects " + card.effectPresets()
            + " warnings " + card.validationWarningCount()
            + (card.migrationRequired() ? " migration_required" : "")));
      message("RenderCore creator index " + normalized + ": " + export.summaryLine() + ".");
      return export.cards().isEmpty() ? 0 : 1;
   }

   private static int creatorCertify(String namespace) {
      CreatorExportIndex export = creatorExportIndex().forNamespace(namespace);
      CreatorCertificationReport certification = export.certification();
      certification.issueSummaries().stream()
         .limit(8)
         .forEach(issue -> message("Certification " + issue.severity() + " " + issue.code()
            + " " + issue.profile() + " [" + issue.path() + "]: " + issue.message()));
      EchoRenderCore.LOGGER.info("RenderCore creator certification {}: {}", normalizeNamespace(namespace), GSON.toJson(certification.toJson()));
      message("RenderCore creator certification " + normalizeNamespace(namespace) + ": "
         + certification.summaryLine() + ".");
      return certification.failed() ? 0 : 1;
   }

   private static int creatorProfile(String profileName) {
      Identifier id;
      try {
         id = Identifier.parse(profileName);
      } catch (RuntimeException exception) {
         message("Invalid RenderCore profile id: " + profileName);
         return 0;
      }
      CreatorExportIndex export = creatorExportIndex();
      CreatorProfileCard card = export.cards().stream()
         .filter(value -> id.equals(value.profileId()))
         .findFirst()
         .orElse(null);
      if (card == null) {
         message("RenderCore creator profile " + id + " is not in the active V12 cache.");
         return 0;
      }
      message("RenderCore creator profile " + id
         + ": schema " + card.schemaVersion()
         + ", layers " + card.layerCount()
         + ", materials " + card.materialCount()
         + ", effects " + card.effectPresets()
         + ", warnings " + card.validationWarningCount()
         + ", errors " + card.validationErrorCount()
         + ", screenshot " + (card.screenshotAvailable() ? card.screenshotProvider() : "metadata-card")
         + ".");
      return card.validationErrorCount() > 0 ? 0 : 1;
   }

   private static int creatorExport(String namespace) {
      CreatorExportIndex export = creatorExportIndex().forNamespace(namespace);
      Path root = creatorRoot(namespace);
      try {
         Files.createDirectories(root);
         writeJson(root.resolve("index.creator.json"), export.toJson());
         writeJson(root.resolve("visual_qa").resolve("advancedfx.evidence.json"), export.visualQa().toAdvancedFxJson());
         writeJson(root.resolve("visual_qa").resolve("screenchrome.evidence.json"), export.visualQa().toScreenChromeJson());
         writeJson(root.resolve("visual_qa").resolve("worldsurface.evidence.json"), export.visualQa().toWorldSurfaceJson());
         for (var artifact : export.artifacts()) {
            Path path = root.resolve("assets")
               .resolve(artifact.id().getNamespace())
               .resolve("rendercore")
               .resolve("creator")
               .resolve("profiles")
               .resolve(artifact.id().getPath() + ".creator.json");
            writeJson(path, artifact.json());
         }
      } catch (IOException exception) {
         message("RenderCore creator export failed: " + exception.getMessage());
         EchoRenderCore.LOGGER.warn("RenderCore creator export failed", exception);
         return 0;
      }
      message("RenderCore creator export wrote " + export.artifacts().size()
         + " artifact(s) and index for " + normalizeNamespace(namespace)
         + " to " + root
         + " with certification " + export.certification().status().id()
         + " and " + export.visualQa().totalBlockerCount() + " visual QA blocker(s).");
      return 1;
   }

   private static int creatorMigrate(String namespace, boolean write) {
      List<CreatorMigrationReport> reports = migrationReports(namespace);
      long required = reports.stream().filter(CreatorMigrationReport::migrationRequired).count();
      long writeSafeRequired = reports.stream().filter(CreatorMigrationReport::migrationRequired).filter(CreatorMigrationReport::writeSafe).count();
      reports.stream()
         .filter(CreatorMigrationReport::migrationRequired)
         .limit(8)
         .forEach(report -> message("Migration " + report.profileId()
            + " schema " + report.sourceSchemaVersion()
            + " -> " + report.targetSchemaVersion()
            + " changes " + report.changes().size()));
      if (write) {
         Path root = migrationRoot();
         try {
            for (CreatorMigrationReport report : reports) {
               if (!report.migrationRequired() || !report.writeSafe()) {
                  continue;
               }
               Path path = root.resolve("assets")
                  .resolve(report.profileId().getNamespace())
                  .resolve("rendercore")
                  .resolve("visual_profiles")
                  .resolve(report.profileId().getPath() + ".json");
               writeJson(path, report.migratedJson());
            }
         } catch (IOException exception) {
            message("RenderCore migration write failed: " + exception.getMessage());
            EchoRenderCore.LOGGER.warn("RenderCore migration write failed", exception);
            return 0;
         }
         message("RenderCore migration wrote " + writeSafeRequired + " V12 profile(s) to " + root + ".");
         return 1;
      }
      message("RenderCore migration dry-run for " + normalizeNamespace(namespace)
         + ": scanned " + reports.size() + " profile(s), " + required + " require V12 migration.");
      return required > 0 ? 1 : 0;
   }

   private static int hud(boolean enabled) {
      DebugVisualOverrides.setHudEnabled(enabled);
      message("RenderCore debug HUD " + (enabled ? "enabled." : "disabled."));
      return 1;
   }

   private static int anchors(boolean enabled) {
      DebugVisualOverrides.setAnchorsEnabled(enabled);
      message("RenderCore anchor debug " + (enabled ? "enabled." : "disabled."));
      return 1;
   }

   private static int advancedFx(boolean enabled) {
      RenderCoreEffectPipeline.setAdvancedFxEnabled(enabled);
      String detail = enabled ? " (" + RenderCoreAdvancedFxPipeline.unavailableReason() + ")" : "";
      message("RenderCore advanced FX session override " + (enabled ? "enabled: " : "disabled: ") + RenderCoreEffectPipeline.statusLine()
         + " / mode " + RenderCoreAdvancedFxPipeline.modeLine() + detail + ".");
      return 1;
   }

   private static int advancedFxStatus() {
      String fallback = RenderCoreAdvancedFxPipeline.fallbackReason().isBlank() ? "" : " / fallback " + RenderCoreAdvancedFxPipeline.fallbackReason();
      message("RenderCore advanced FX status: " + RenderCoreEffectPipeline.statusLine()
         + " / mode " + RenderCoreAdvancedFxPipeline.modeLine()
         + " / masks " + RenderCoreAdvancedFxPipeline.lastMaskSubmissionCount()
         + " active " + RenderCoreAdvancedFxPipeline.lastEffectCount()
         + " skipped " + RenderCoreAdvancedFxPipeline.lastSkippedSubmissions()
         + " channels " + RenderCoreAdvancedFxPipeline.lastChannelCount()
         + " downscale " + RenderCoreAdvancedFxPipeline.lastDownscale()
         + " passes " + RenderCoreAdvancedFxPipeline.lastPassCount()
         + " bloomCost " + RenderCoreAdvancedFxPipeline.lastBloomCost()
         + fallback
         + (RenderCoreAdvancedFxPipeline.unavailableReason().isBlank() ? "" : " / " + RenderCoreAdvancedFxPipeline.unavailableReason())
         + ".");
      return 1;
   }

   private static int advancedFxReset() {
      RenderCoreEffectPipeline.resetAdvancedFxOverride();
      message("RenderCore advanced FX session override reset: " + RenderCoreEffectPipeline.statusLine()
         + " / mode " + RenderCoreAdvancedFxPipeline.modeLine() + ".");
      return 1;
   }

   private static int advancedFxEvidenceStart() {
      CreatorVisualQaReport.EvidenceSnapshot snapshot = RenderCoreVisualQaEvidence.start();
      message("RenderCore advanced FX evidence started: " + snapshot.label()
         + " / " + snapshot.statusLine()
         + " / " + snapshot.modeLine() + ".");
      return 1;
   }

   private static int advancedFxEvidenceCapture(String label) {
      String screenshotPath = captureEvidenceScreenshot(label);
      CreatorVisualQaReport.EvidenceSnapshot snapshot = RenderCoreVisualQaEvidence.capture(label, screenshotPath);
      message("RenderCore advanced FX evidence captured: " + snapshot.label()
         + " / masks " + snapshot.maskSubmissions()
         + " active " + snapshot.activeEffects()
         + " skipped " + snapshot.skippedSubmissions()
         + (snapshot.screenshotPath().isBlank() ? "" : " / screenshot " + snapshot.screenshotPath())
         + " / " + snapshot.statusLine() + ".");
      return 1;
   }

   private static int advancedFxEvidenceStatus() {
      CreatorVisualQaReport report = RenderCoreVisualQaEvidence.report();
      message("RenderCore advanced FX evidence: snapshots " + report.snapshots().size()
         + ", screenshots " + report.screenshotEvidenceCount()
         + ", modes " + report.testedFallbackModes()
         + ", blockers " + report.remainingBlockers()
         + ".");
      return report.remainingBlockers().isEmpty() ? 1 : 0;
   }

   private static int advancedFxEvidenceExport() {
      CreatorVisualQaReport report = RenderCoreVisualQaEvidence.report();
      Path path = creatorRoot("all").resolve("visual_qa").resolve("advancedfx.evidence.json");
      try {
         writeJson(path, report.toAdvancedFxJson());
      } catch (IOException exception) {
         message("RenderCore advanced FX evidence export failed: " + exception.getMessage());
         EchoRenderCore.LOGGER.warn("RenderCore advanced FX evidence export failed", exception);
         return 0;
      }
      message("RenderCore advanced FX evidence exported to " + path
         + " with " + report.remainingBlockers().size() + " blocker(s).");
      return 1;
   }

   private static int advancedFxEvidenceReset() {
      RenderCoreVisualQaEvidence.reset();
      message("RenderCore advanced FX evidence reset.");
      return 1;
   }

   private static int screenChromeEvidenceStart() {
      RenderCoreScreenChromeQaEvidence.start();
      message("RenderCore screen chrome evidence started for " + RenderCoreScreenChromeQaCatalog.requiredSurfaces().size()
         + " surface(s): " + RenderCoreScreenChromeQaCatalog.surfaceIds() + ".");
      return 1;
   }

   private static int screenChromeEvidenceCapture(String surfaceId) {
      if (RenderCoreScreenChromeQaCatalog.surface(surfaceId) == null) {
         message("Unknown RenderCore screen chrome surface '" + surfaceId + "'. Known surfaces: "
            + RenderCoreScreenChromeQaCatalog.surfaceIds() + ".");
         return 0;
      }
      String screenshotPath = captureScreenChromeEvidenceScreenshot(surfaceId);
      CreatorVisualQaReport.ScreenChromeEvidence evidence = RenderCoreScreenChromeQaEvidence.capture(surfaceId, screenshotPath);
      if (evidence == null) {
         message("RenderCore screen chrome evidence capture failed for " + surfaceId + ".");
         return 0;
      }
      message("RenderCore screen chrome evidence captured: " + evidence.surfaceId()
         + " / " + evidence.chromeStyle()
         + " / " + evidence.qaStatus()
         + (evidence.screenshotPath().isBlank() ? "" : " / screenshot " + evidence.screenshotPath())
         + ".");
      return evidence.passed() ? 1 : 0;
   }

   private static int screenChromeEvidenceStatus() {
      CreatorVisualQaReport report = RenderCoreVisualQaEvidence.report();
      message("RenderCore screen chrome evidence: surfaces " + report.screenChromeEvidence().size()
         + ", screenshots " + report.screenChromeEvidenceCount()
         + ", blockers " + report.screenChromeBlockers()
         + ".");
      return report.screenChromeBlockers().isEmpty() ? 1 : 0;
   }

   private static int screenChromeEvidenceExport() {
      CreatorVisualQaReport report = RenderCoreVisualQaEvidence.report();
      Path path = creatorRoot("all").resolve("visual_qa").resolve("screenchrome.evidence.json");
      try {
         writeJson(path, report.toScreenChromeJson());
      } catch (IOException exception) {
         message("RenderCore screen chrome evidence export failed: " + exception.getMessage());
         EchoRenderCore.LOGGER.warn("RenderCore screen chrome evidence export failed", exception);
         return 0;
      }
      message("RenderCore screen chrome evidence exported to " + path
         + " with " + report.screenChromeBlockers().size() + " blocker(s).");
      return 1;
   }

   private static int screenChromeEvidenceReset() {
      RenderCoreScreenChromeQaEvidence.reset();
      message("RenderCore screen chrome evidence reset.");
      return 1;
   }

   private static String captureEvidenceScreenshot(String label) {
      return captureEvidenceScreenshot(Path.of("visual_qa", "screenshots"), label, "advanced FX");
   }

   private static String captureScreenChromeEvidenceScreenshot(String surfaceId) {
      return captureEvidenceScreenshot(Path.of("visual_qa", "screenshots", "screen_chrome"), surfaceId, "screen chrome");
   }

   private static String captureEvidenceScreenshot(Path relativeDirectory, String label, String evidenceType) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getMainRenderTarget() == null) {
         return "";
      }
      String safeLabel = safeEvidenceLabel(label);
      Path relative = relativeDirectory.resolve(safeLabel + ".png");
      Path absolute = creatorRoot("all").resolve(relative);
      try {
         Files.createDirectories(absolute.getParent());
         Screenshot.grab(
            absolute.getParent().toFile(),
            absolute.getFileName().toString(),
            minecraft.getMainRenderTarget(),
            1,
            component -> EchoRenderCore.LOGGER.info("RenderCore {} evidence screenshot {}: {}", evidenceType, safeLabel, component.getString())
         );
         return relative.toString().replace('\\', '/');
      } catch (IOException | RuntimeException exception) {
         EchoRenderCore.LOGGER.warn("RenderCore {} evidence screenshot failed for {}", evidenceType, safeLabel, exception);
         return "";
      }
   }

   private static String safeEvidenceLabel(String label) {
      String value = label == null || label.isBlank() ? "manual" : label.toLowerCase(java.util.Locale.ROOT);
      value = value.replaceAll("[^a-z0-9_\\-]+", "_").replaceAll("_+", "_");
      if (value.isBlank()) {
         return "manual";
      }
      return value.length() > 48 ? value.substring(0, 48) : value;
   }

   private static int blockParts() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult blockHit)) {
         message("Look at a RenderCore-supported block entity first.");
         return 0;
      }
      BlockEntity blockEntity = minecraft.level.getBlockEntity(blockHit.getBlockPos());
      if (blockEntity == null) {
         message("The looked-at block has no block entity.");
         return 0;
      }
      Identifier profileId = blockProfileId(minecraft, blockEntity);
      if (profileId == null) {
         message("The looked-at block entity has not exposed a RenderCore profile yet.");
         return 0;
      }
      VisualProfile profile = RenderCoreProfiles.visual(profileId);
      if (profile == null) {
         message("RenderCore profile " + profileId + " is not loaded.");
         return 0;
      }
      BlockState blockState = minecraft.level.getBlockState(blockHit.getBlockPos());
      List<BlockStateModelPart> collected = BakedBlockPartResolver.collect(blockState);
      Map<String, List<BlockStateModelPart>> aliases = BakedBlockPartResolver.resolve(collected, blockState, profile);
      var tintIndices = BakedBlockPartResolver.availableTintIndices(collected);
      var report = com.knoxhack.echorendercore.profile.RenderCoreProfileValidator.validateBlockPartSelectors(
         profile,
         collected.size(),
         blockState,
         tintIndices
      );
      message("RenderCore block parts: " + profileId + " collected " + collected.size() + ", aliases " + aliases.size()
         + ", warnings " + report.warnings() + ". See log for details.");
      EchoRenderCore.LOGGER.info("RenderCore block part export profile={} pos={} blockState={} collected={} aliases={} tintIndices={} warnings={}",
         profileId, blockHit.getBlockPos().toShortString(), blockState, collected.size(), aliases.keySet(), tintIndices, report.warnings());
      for (Map.Entry<String, BlockPartSelectorProfile> entry : profile.blockParts().entrySet()) {
         List<BlockStateModelPart> selected = aliases.getOrDefault(entry.getKey(), List.of());
         EchoRenderCore.LOGGER.info("RenderCore block alias {} matched indices {} selector {}",
            entry.getKey(), BakedBlockPartResolver.matchedIndices(collected, selected), selectorSummary(entry.getValue()));
      }
      for (ProfileValidationIssue issue : report.issues()) {
         EchoRenderCore.LOGGER.warn("RenderCore block part export {} {} [{}]: {}{}",
            issue.code(), issue.profileId(), issue.path(), issue.message(),
            issue.suggestion().isBlank() ? "" : " Suggestion: " + issue.suggestion());
      }
      return 1;
   }

   private static Identifier blockProfileId(Minecraft minecraft, BlockEntity blockEntity) {
      if (blockEntity instanceof IAdvancedVisualBlockEntity visual) {
         return visual.visualProfileId();
      }
      return RenderCoreDebugTargets.lookedAt(minecraft).map(RenderCoreDebugTargets.DebugTarget::profileId).orElse(null);
   }

   private static String selectorSummary(BlockPartSelectorProfile selector) {
      return "{indices=" + selector.indices()
         + ", directions=" + selector.directions()
         + ", material_flags=" + selector.materialFlags()
         + ", ambient_occlusion=" + selector.ambientOcclusion()
         + ", tint_indices=" + selector.tintIndices()
         + ", block_state=" + selector.blockState()
         + "}";
   }

   private static List<CreatorMigrationReport> migrationReports(String namespace) {
      Minecraft minecraft = Minecraft.getInstance();
      String normalized = normalizeNamespace(namespace);
      ArrayList<CreatorMigrationReport> reports = new ArrayList<>();
      for (Map.Entry<Identifier, Resource> entry : minecraft.getResourceManager().listResources(VISUAL_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
         Identifier resourceId = entry.getKey();
         Identifier id = contentId(resourceId, VISUAL_DIR);
         if (!"all".equals(normalized) && !id.getNamespace().equals(normalized)) {
            continue;
         }
         try {
            JsonObject json = readObject(entry.getValue());
            reports.add(RenderCoreProfileMigration.migrateVisualProfile(id, json));
         } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            JsonObject empty = new JsonObject();
            empty.addProperty("schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
            reports.add(new CreatorMigrationReport(
               id,
               0,
               VisualProfile.CURRENT_SCHEMA_VERSION,
               true,
               false,
               List.of("source JSON could not be parsed"),
               List.of(new ProfileValidationIssue(
                  com.knoxhack.echorendercore.profile.ProfileValidationSeverity.ERROR,
                  id,
                  "migration_parse_failed",
                  resourceId.toString(),
                  exception.getMessage(),
                  "Fix the source JSON before running the V12 migration writer."
               )),
               "generated/rendercore_migrations/assets/" + id.getNamespace() + "/rendercore/visual_profiles/" + id.getPath() + ".json",
               empty
            ));
         }
      }
      return reports.stream()
         .sorted(Comparator.comparing(report -> report.profileId().toString()))
         .toList();
   }

   private static JsonObject readObject(Resource resource) throws IOException {
      try (Reader reader = resource.openAsReader()) {
         JsonElement root = JsonParser.parseReader(reader);
         if (!root.isJsonObject()) {
            throw new JsonParseException("Root must be a JSON object.");
         }
         return root.getAsJsonObject();
      }
   }

   private static Identifier contentId(Identifier resourceId, String directory) {
      String path = resourceId.getPath();
      String prefix = directory + "/";
      if (path.startsWith(prefix)) {
         path = path.substring(prefix.length());
      }
      if (path.endsWith(".json")) {
         path = path.substring(0, path.length() - ".json".length());
      }
      return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
   }

   private static void writeJson(Path path, JsonObject json) throws IOException {
      Files.createDirectories(path.getParent());
      Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
   }

   private static Path creatorRoot(String namespace) {
      return Minecraft.getInstance().gameDirectory.toPath()
         .resolve("rendercore_creator")
         .resolve(normalizeNamespace(namespace));
   }

   private static Path migrationRoot() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("rendercore_migrations");
   }

   private static CreatorExportIndex creatorExportIndex() {
      return RenderCoreCreatorPackExporter.export(RenderCoreProfiles.loaded(), RenderCoreClientScreenshotPreviewProvider.INSTANCE)
         .withVisualQa(RenderCoreVisualQaEvidence.report());
   }

   private static String normalizeNamespace(String namespace) {
      return namespace == null || namespace.isBlank() ? "all" : namespace.toLowerCase(java.util.Locale.ROOT);
   }

   private static void messageIssue(ProfileValidationIssue issue) {
      message(issue.severity() + " " + issue.code() + " " + issue.profileId() + " [" + issue.path() + "]: " + issue.message());
   }

   private static void message(String text) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         minecraft.player.sendSystemMessage(Component.literal(text));
      }
   }
}
