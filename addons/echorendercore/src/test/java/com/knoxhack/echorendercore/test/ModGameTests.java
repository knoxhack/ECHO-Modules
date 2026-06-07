package com.knoxhack.echorendercore.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.animation.AnimationChannel;
import com.knoxhack.echorendercore.animation.AnimationClip;
import com.knoxhack.echorendercore.animation.AnimationController;
import com.knoxhack.echorendercore.animation.AnimationKeyframe;
import com.knoxhack.echorendercore.animation.AnimationPlayer;
import com.knoxhack.echorendercore.animation.AnimationTrack;
import com.knoxhack.echorendercore.animation.AnimationTimeline;
import com.knoxhack.echorendercore.animation.Easing;
import com.knoxhack.echorendercore.animation.ModelPose;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.client.ParticleAnchorResolver;
import com.knoxhack.echorendercore.client.RenderCoreParticleOptionResolvers;
import com.knoxhack.echorendercore.client.RenderCoreScreenChromeStyle;
import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echorendercore.client.RenderCoreScreenVisuals;
import com.knoxhack.echorendercore.particle.RenderCoreSoftParticleOptions;
import com.knoxhack.echorendercore.profile.AnimationProfile;
import com.knoxhack.echorendercore.profile.AnimationProfileBuilder;
import com.knoxhack.echorendercore.profile.BlockPartSelectorProfile;
import com.knoxhack.echorendercore.profile.CreatorAddonIntegration;
import com.knoxhack.echorendercore.profile.CreatorCertificationResult;
import com.knoxhack.echorendercore.profile.CreatorAddonShowcaseCatalog;
import com.knoxhack.echorendercore.profile.CreatorPackManifest;
import com.knoxhack.echorendercore.profile.CreatorProfileDraft;
import com.knoxhack.echorendercore.profile.CreatorExportIndex;
import com.knoxhack.echorendercore.profile.CreatorMigrationReport;
import com.knoxhack.echorendercore.profile.CreatorVisualQaReport;
import com.knoxhack.echorendercore.profile.ProfileCacheMetrics;
import com.knoxhack.echorendercore.profile.ProfileDiagnosticsReport;
import com.knoxhack.echorendercore.profile.ProfilePerformanceIssue;
import com.knoxhack.echorendercore.profile.ProfilePerformanceReport;
import com.knoxhack.echorendercore.profile.ProfileValidationIssue;
import com.knoxhack.echorendercore.profile.ProfileValidationReport;
import com.knoxhack.echorendercore.profile.ProfileValidationSeverity;
import com.knoxhack.echorendercore.profile.ProfilePreviewExport;
import com.knoxhack.echorendercore.profile.ParticleProfileBuilder;
import com.knoxhack.echorendercore.profile.ParticleEmitter;
import com.knoxhack.echorendercore.profile.ParticleProfile;
import com.knoxhack.echorendercore.profile.ProfilePerformanceSummary;
import com.knoxhack.echorendercore.profile.ProfilePreviewReport;
import com.knoxhack.echorendercore.profile.ProfileScreenshotPreviewProvider;
import com.knoxhack.echorendercore.profile.RenderCoreJsonParsers;
import com.knoxhack.echorendercore.profile.RenderCoreCreatorPackExporter;
import com.knoxhack.echorendercore.profile.RenderCoreProfileComposer;
import com.knoxhack.echorendercore.profile.RenderCoreProfileMigration;
import com.knoxhack.echorendercore.profile.RenderCoreProfilePreviewer;
import com.knoxhack.echorendercore.profile.RenderCoreProfileValidator;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.RenderCoreScreenChromeQaCatalog;
import com.knoxhack.echorendercore.profile.RenderCoreVector;
import com.knoxhack.echorendercore.profile.VisualLayerKind;
import com.knoxhack.echorendercore.profile.VisualLayerProfile;
import com.knoxhack.echorendercore.profile.VisualLightMode;
import com.knoxhack.echorendercore.profile.VisualMaterial;
import com.knoxhack.echorendercore.profile.VisualEffectBloomMaskMode;
import com.knoxhack.echorendercore.profile.VisualEffectKind;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;
import com.knoxhack.echorendercore.profile.VisualEffectTargetScope;
import com.knoxhack.echorendercore.profile.VisualProfileBuilder;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.knoxhack.echorendercore.profile.VisualRenderPass;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoRenderCore.MODID);

   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROFILE_PARSERS =
      TEST_FUNCTIONS.register("profile_parsers", () -> ModGameTests::profileParsers);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INVALID_JSON_SAFETY =
      TEST_FUNCTIONS.register("invalid_json_safety", () -> ModGameTests::invalidJsonSafety);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ANIMATION_PLAYBACK =
      TEST_FUNCTIONS.register("animation_playback", () -> ModGameTests::animationPlayback);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V2_PROFILE_VALIDATION =
      TEST_FUNCTIONS.register("v2_profile_validation", () -> ModGameTests::v2ProfileValidation);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V3_LINT_REPORTS =
      TEST_FUNCTIONS.register("v3_lint_reports", () -> ModGameTests::v3LintReports);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V4_PROFILE_TOOLS =
      TEST_FUNCTIONS.register("v4_profile_tools", () -> ModGameTests::v4ProfileTools);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V5_DIAGNOSTICS_AND_SCHEMAS =
      TEST_FUNCTIONS.register("v5_diagnostics_and_schemas", () -> ModGameTests::v5DiagnosticsAndSchemas);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V6_PREVIEW_HOTSWAP_AND_MATERIALS =
      TEST_FUNCTIONS.register("v6_preview_hotswap_and_materials", () -> ModGameTests::v6PreviewHotSwapAndMaterials);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V7_COMPOSITION_PREVIEW_AND_MATERIALS =
      TEST_FUNCTIONS.register("v7_composition_preview_and_materials", () -> ModGameTests::v7CompositionPreviewAndMaterials);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V8_NEON_EFFECTS =
      TEST_FUNCTIONS.register("v8_neon_effects", () -> ModGameTests::v8NeonEffects);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V9_ADVANCED_FX =
      TEST_FUNCTIONS.register("v9_advanced_fx", () -> ModGameTests::v9AdvancedFx);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V10_ISOLATED_ADVANCED_FX =
      TEST_FUNCTIONS.register("v10_isolated_advanced_fx", () -> ModGameTests::v10IsolatedAdvancedFx);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V11_CREATOR_WORKBENCH =
      TEST_FUNCTIONS.register("v11_creator_workbench", () -> ModGameTests::v11CreatorWorkbench);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V12_PACK_CERTIFICATION =
      TEST_FUNCTIONS.register("v12_pack_certification", () -> ModGameTests::v12PackCertification);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V13_COMPLETE_VISION =
      TEST_FUNCTIONS.register("v13_complete_vision", () -> ModGameTests::v13CompleteVision);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V14_VISUAL_PROOF =
      TEST_FUNCTIONS.register("v14_visual_proof", () -> ModGameTests::v14VisualProof);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V15_REAL_RENDER_MOD =
      TEST_FUNCTIONS.register("v15_real_render_mod", () -> ModGameTests::v15RealRenderMod);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V16_MACHINE_SCREEN_VISUALS =
      TEST_FUNCTIONS.register("v16_machine_screen_visuals", () -> ModGameTests::v16MachineScreenVisuals);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V17_SCREEN_FRAME_OPTIONS =
      TEST_FUNCTIONS.register("v17_screen_frame_options", () -> ModGameTests::v17ScreenFrameOptions);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V21_SCREEN_CHROME_EVIDENCE =
      TEST_FUNCTIONS.register("v21_screen_chrome_evidence", () -> ModGameTests::v21ScreenChromeEvidence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> V22_SOFT_MOB_PARTICLES =
      TEST_FUNCTIONS.register("v22_soft_mob_particles", () -> ModGameTests::v22SoftMobParticles);

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      if (!shouldRegisterTests()) {
         return;
      }
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("rendercore"));
      register(event, environment, "profile_parsers", PROFILE_PARSERS.getId());
      register(event, environment, "invalid_json_safety", INVALID_JSON_SAFETY.getId());
      register(event, environment, "animation_playback", ANIMATION_PLAYBACK.getId());
      register(event, environment, "v2_profile_validation", V2_PROFILE_VALIDATION.getId());
      register(event, environment, "v3_lint_reports", V3_LINT_REPORTS.getId());
      register(event, environment, "v4_profile_tools", V4_PROFILE_TOOLS.getId());
      register(event, environment, "v5_diagnostics_and_schemas", V5_DIAGNOSTICS_AND_SCHEMAS.getId());
      register(event, environment, "v6_preview_hotswap_and_materials", V6_PREVIEW_HOTSWAP_AND_MATERIALS.getId());
      register(event, environment, "v7_composition_preview_and_materials", V7_COMPOSITION_PREVIEW_AND_MATERIALS.getId());
      register(event, environment, "v8_neon_effects", V8_NEON_EFFECTS.getId());
      register(event, environment, "v9_advanced_fx", V9_ADVANCED_FX.getId());
      register(event, environment, "v10_isolated_advanced_fx", V10_ISOLATED_ADVANCED_FX.getId());
      register(event, environment, "v11_creator_workbench", V11_CREATOR_WORKBENCH.getId());
      register(event, environment, "v12_pack_certification", V12_PACK_CERTIFICATION.getId());
      register(event, environment, "v13_complete_vision", V13_COMPLETE_VISION.getId());
      register(event, environment, "v14_visual_proof", V14_VISUAL_PROOF.getId());
      register(event, environment, "v15_real_render_mod", V15_REAL_RENDER_MOD.getId());
      register(event, environment, "v16_machine_screen_visuals", V16_MACHINE_SCREEN_VISUALS.getId());
      register(event, environment, "v17_screen_frame_options", V17_SCREEN_FRAME_OPTIONS.getId());
      register(event, environment, "v21_screen_chrome_evidence", V21_SCREEN_CHROME_EVIDENCE.getId());
      register(event, environment, "v22_soft_mob_particles", V22_SOFT_MOB_PARTICLES.getId());
   }

   private static void profileParsers(GameTestHelper helper) {
      helper.assertTrue(VisualState.byName("active", VisualState.IDLE) == VisualState.ACTIVE,
         "Visual states should parse serialized names.");
      helper.assertTrue(VisualVariant.of("  DUSTY_REPAIR ").id().equals("dusty_repair"),
         "Visual variants should normalize ids.");

      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("rover"), object("""
         {
           "base_texture": "echoconvoyprotocol:textures/entity/wasteland_rover.png",
           "glow_texture": "echoconvoyprotocol:textures/entity/wasteland_rover.png",
           "animation_profile": "echoconvoyprotocol:wasteland_rover",
           "particle_profile": "echoconvoyprotocol:wasteland_rover",
           "default_state": "ONLINE",
           "state_animations": { "ACTIVE": "drive" },
           "state_overlays": { "DAMAGED": ["echoconvoyprotocol:textures/entity/wasteland_rover.png"] },
           "anchors": { "exhaust_left": { "offset": [-0.7, 0.4, 1.1] } }
         }
         """));
      helper.assertTrue(visual.defaultState() == VisualState.ONLINE, "Visual profile default state should parse.");
      helper.assertTrue("drive".equals(visual.animationFor(VisualState.ACTIVE)), "State animation should parse.");
      helper.assertTrue(visual.anchor("exhaust_left") != null, "Profile anchors should parse.");

      AnimationProfile animation = RenderCoreJsonParsers.parseAnimationProfile(id("rover"), object("""
         {
           "animations": {
             "scanner_rotate": {
               "loop": true,
               "length": 4.0,
               "tracks": [
                 {
                   "part": "scanner",
                   "channel": "rotation_y",
                   "keyframes": [
                     { "time": 0.0, "value": 0.0 },
                     { "time": 4.0, "value": 360.0, "easing": "linear" }
                   ]
                 }
               ]
             }
           }
         }
         """));
      helper.assertTrue(animation.clip("scanner_rotate") != null, "Animation clips should parse.");

      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("rover"), object("""
         {
           "emitters": {
             "damaged_smoke": {
               "anchor": "exhaust_left",
               "particle": "minecraft:smoke",
               "state": "DAMAGED",
               "rate": 0.05,
               "offset": [0.0, 0.1, 0.0],
               "velocity": [0.0, 0.02, 0.0]
             },
             "dust": {
               "anchor": "wheel_back_left",
               "particle": "minecraft:dust",
               "states": ["ACTIVE", "WORKING"],
               "requires_moving": true,
               "rate": 0.1,
               "options": { "type": "minecraft:dust", "color": [0.4, 0.3, 0.2], "scale": 0.8, "lifetime": 20 }
             }
           }
         }
         """));
      helper.assertTrue(particles.emitters().containsKey("damaged_smoke"), "Particle emitters should parse.");
      helper.assertTrue(particles.emitters().get("dust").states().contains(VisualState.ACTIVE), "V2 emitter states should parse.");
      helper.assertTrue("minecraft:dust".equals(particles.emitters().get("dust").options().type()), "V3 particle option type should parse.");
      helper.assertTrue(close(particles.emitters().get("dust").options().scale(), 0.8F), "Dust particle options should parse.");
      helper.succeed();
   }

   private static void v5DiagnosticsAndSchemas(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("v5_machine"), object("""
         {
           "schema_version": 5,
           "base_texture": "echorendercore:textures/block/machine.png",
           "layers": [
             {
               "id": "active_core",
               "kind": "glow",
               "texture": "echorendercore:textures/block/machine.png",
               "states": ["ACTIVE"],
               "parts": ["powered_core", "list_core", "tinted_core"]
             }
           ],
           "block_parts": {
             "powered_core": { "directions": ["north"], "block_state": { "powered": "true" } },
             "list_core": { "directions": ["south"], "blockState": { "powered": ["true", "false"] } },
             "bad_property": { "block_state": { "missing": "true" } },
             "bad_value": { "block_state": { "powered": ["maybe", "false"] } },
             "tinted_core": { "tint_indices": [0, 4] }
           }
         }
         """));
      helper.assertTrue(visual.schemaVersion() == 5, "V5 schema version should parse.");
      helper.assertTrue(visual.blockParts().get("powered_core").blockState().get("powered").contains("true"),
         "V5 block_state string selector should parse.");
      helper.assertTrue(visual.blockParts().get("list_core").blockState().get("powered").contains("false"),
         "V5 block_state list selector should parse.");

      BlockState poweredLever = Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.POWERED, true);
      var blockReport = RenderCoreProfileValidator.validateBlockPartSelectors(visual, 1, poweredLever, Set.of(0));
      helper.assertTrue(blockReport.issues().stream().anyMatch(issue -> "block_state_property_missing".equals(issue.code())),
         "V5 block state validation should report missing properties.");
      helper.assertTrue(blockReport.issues().stream().anyMatch(issue -> "block_state_property_value_missing".equals(issue.code())),
         "V5 block state validation should report unmatched serialized values.");
      helper.assertTrue(blockReport.issues().stream().anyMatch(issue -> "block_part_tint_index_missing".equals(issue.code())),
         "V5 tint validation should report missing tint indices.");

      var diagnostics = RenderCoreProfileValidator.diagnostics(Map.of(visual.id(), visual), Map.of(), Map.of(), 3, 1, 2);
      helper.assertTrue(diagnostics.cacheMetrics().visualProfileCount() == 1, "V5 diagnostics should include visual profile count.");
      helper.assertTrue(diagnostics.cacheMetrics().discoveredJsonCount() == 3, "V5 diagnostics should preserve discovered JSON count.");
      helper.assertTrue(diagnostics.cacheMetrics().failedJsonCount() == 2, "V5 diagnostics should preserve failed JSON count.");
      helper.assertTrue(diagnostics.cacheMetrics().namespaces().contains(EchoRenderCore.MODID), "V5 metrics should collect namespaces.");
      helper.assertTrue(diagnostics.cacheMetrics().minSchemaVersion() == 5 && diagnostics.cacheMetrics().maxSchemaVersion() == 5,
         "V5 metrics should expose schema version range.");
      helper.assertTrue(diagnostics.cacheMetrics().validationWarningCount() == diagnostics.validationReport().warnings(),
         "V5 metrics should mirror merged validation warnings.");

      assertSchemaResource(helper, "visual_profile.schema.json");
      assertSchemaResource(helper, "animation_profile.schema.json");
      assertSchemaResource(helper, "particle_profile.schema.json");
      assertSchemaResource(helper, "profile_preview.schema.json");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v6PreviewHotSwapAndMaterials(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("v6_machine"), object("""
         {
           "schema_version": 6,
           "base_texture": "echorendercore:textures/block/machine.png",
           "animation_profile": "echorendercore:v6_animation",
           "particle_profile": "echorendercore:v6_particles",
           "preview": { "title": "V6 Machine" },
           "materials": {
             "stable_emissive": {
               "color": "#FF66E8FF",
               "alpha": 0.75,
               "emissive": true,
               "blend_mode": "additive",
               "light_mode": "fullbright",
               "render_pass": "emissive",
               "cull": false,
               "depth_write": false,
               "sort_order": 2
             },
             "bad_material": {
               "light_mode": "shader_magic",
               "render_pass": "future_pipeline"
             }
           },
           "layers": [
             {
               "id": "screen_glow",
               "kind": "glow",
               "texture": "echorendercore:textures/block/machine.png",
               "material": "stable_emissive",
               "states": ["ACTIVE"],
               "parts": ["screen"],
               "light_mode": "emissive",
               "render_pass": "translucent",
               "depth_write": false,
               "sort_order": 1
             }
           ],
           "anchors": { "screen": [0.0, 0.6, 0.0] }
         }
         """));
      helper.assertTrue(visual.schemaVersion() == 6, "V6 schema version should parse.");
      VisualMaterial stable = visual.material("stable_emissive");
      helper.assertTrue(stable.lightMode() == VisualLightMode.FULLBRIGHT, "V6 material light_mode should parse.");
      helper.assertTrue(stable.renderPass() == VisualRenderPass.EMISSIVE, "V6 material render_pass should parse.");
      helper.assertTrue(stable.sortOrder() == 2 && stable.depthWrite() == Boolean.FALSE, "V6 material render controls should parse.");
      helper.assertTrue(visual.layers().getFirst().lightMode() == VisualLightMode.EMISSIVE, "V6 layer light_mode should parse.");
      helper.assertTrue(visual.layers().getFirst().effectiveSortOrder(stable) == 3, "V6 layer/material sort orders should combine.");

      AnimationProfile animation = RenderCoreJsonParsers.parseAnimationProfile(id("v6_animation"), object("""
         { "animations": { "screen_pulse": { "loop": true, "length": 1.0, "tracks": [
           { "part": "screen", "channel": "alpha", "from": 0.4, "to": 1.0 }
         ] } } }
         """));
      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v6_particles"), object("""
         { "emitters": { "screen_sparks": {
           "anchor": "screen",
           "particle": "minecraft:dust",
           "state": "ACTIVE",
           "rate": 0.1,
           "options": { "type": "minecraft:dust", "color": "#FF66E8FF", "scale": 0.4 }
         } } }
         """));
      var diagnostics = RenderCoreProfileValidator.diagnostics(
         Map.of(visual.id(), visual),
         Map.of(animation.id(), animation),
         Map.of(particles.id(), particles),
         3,
         3,
         0
      );
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "unsupported_material_option".equals(issue.code())),
         "V6 validation should report unsupported material controls.");

      ProfilePreviewReport preview = RenderCoreProfilePreviewer.preview(
         Map.of(visual.id(), visual),
         Map.of(animation.id(), animation),
         Map.of(particles.id(), particles),
         diagnostics
      );
      helper.assertTrue(preview.entries().size() == 1 && preview.artifacts().size() == 1,
         "V6 preview reports should create one deterministic entry and artifact.");
      helper.assertTrue(preview.entries().getFirst().animationClipCount() == 1, "V6 preview should include animation clip counts.");
      helper.assertTrue(preview.entries().getFirst().emitterCount() == 1, "V6 preview should include emitter counts.");
      helper.assertTrue(preview.artifacts().getFirst().json().get("profile").getAsString().equals(visual.id().toString()),
         "V6 preview artifact JSON should include the profile id.");
      helper.assertTrue(preview.forNamespace(EchoRenderCore.MODID).entries().size() == 1,
         "V6 preview reports should filter by namespace.");

      RenderCoreProfiles.LoadedContent valid = loadedContent(visual, animation, particles, diagnostics, 3, 3, 0);
      RenderCoreProfiles.replace(valid);
      RenderCoreProfiles.LoadedContent failed = loadedContent(visual, animation, particles, diagnostics, 4, 3, 1);
      var rejected = RenderCoreProfiles.hotSwap(failed);
      helper.assertFalse(rejected.accepted(), "V6 hot-swap should reject failed reloads when a previous cache exists.");
      helper.assertTrue(RenderCoreProfiles.visual(visual.id()) == visual, "V6 hot-swap should keep the previous valid cache.");
      RenderCoreProfiles.replace(RenderCoreProfiles.LoadedContent.EMPTY);
      helper.succeed();
   }

   private static void v7CompositionPreviewAndMaterials(GameTestHelper helper) {
      VisualProfile base = RenderCoreJsonParsers.parseVisualProfile(id("v7_base"), object("""
         {
           "schema_version": 7,
           "base_texture": "echorendercore:textures/block/base_machine.png",
           "materials": {
             "shared": {
               "color": "#FF66E8FF",
               "alpha": 0.75,
               "light_override": 15728880,
               "overlay_override": 0,
               "outline_color": "#FF00FFFF",
               "render_priority": 3
             }
           },
           "layers": [
             {
               "id": "shared_glow",
               "kind": "glow",
               "texture": "echorendercore:textures/block/base_machine_glow.png",
               "material": "shared",
               "states": ["ONLINE", "ACTIVE"],
               "variants": ["industrial", "field"],
               "parts": ["core"],
               "render_priority": 2
             }
           ],
           "anchors": { "core": [0.0, 0.5, 0.0] },
           "block_parts": { "core": { "directions": ["north"], "ambient_occlusion": true } }
         }
         """));
      VisualProfile child = RenderCoreJsonParsers.parseVisualProfile(id("v7_child"), object("""
         {
           "schema_version": 7,
           "includes": [
             { "profile": "echorendercore:v7_base", "states": ["ACTIVE"], "variants": ["industrial"] }
           ],
           "base_texture": "echorendercore:textures/block/child_machine.png",
           "materials": {
             "shared": {
               "color": "#FFFF7A28",
               "render_priority": 4
             },
             "bad_material": {
               "light_override": -1,
               "overlay_override": -1,
               "render_priority": 2001
             }
           },
           "layers": [
             {
               "id": "shared_glow",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/child_machine_overlay.png",
               "material": "bad_material",
               "states": ["DAMAGED"],
               "parts": ["screen"],
               "light_override": 15728880,
               "overlay_override": 0,
               "outline_color": "#FFFFFFFF",
               "render_priority": 1
             }
           ],
           "preview": {
             "title": "V7 Child",
             "screenshot": { "enabled": true }
           },
           "anchors": { "screen": [0.0, 0.4, 0.5] }
         }
         """));
      VisualProfile missing = RenderCoreJsonParsers.parseVisualProfile(id("v7_missing"), object("""
         { "schema_version": 7, "includes": ["echorendercore:does_not_exist"] }
         """));
      VisualProfile cycleA = RenderCoreJsonParsers.parseVisualProfile(id("v7_cycle_a"), object("""
         { "schema_version": 7, "includes": ["echorendercore:v7_cycle_b"] }
         """));
      VisualProfile cycleB = RenderCoreJsonParsers.parseVisualProfile(id("v7_cycle_b"), object("""
         { "schema_version": 7, "includes": ["echorendercore:v7_cycle_a"] }
         """));

      var composition = RenderCoreProfileComposer.composeAll(Map.of(
         base.id(), base,
         child.id(), child,
         missing.id(), missing,
         cycleA.id(), cycleA,
         cycleB.id(), cycleB
      ));
      VisualProfile composed = composition.profiles().get(child.id());
      helper.assertTrue(composed.includes().size() == 1, "V7 includes should parse and remain visible for tooling.");
      helper.assertTrue(composed.layers().size() == 2, "V7 composition should merge included and root layers.");
      helper.assertTrue(composed.layers().getFirst().states().equals(Set.of(VisualState.ACTIVE)),
         "V7 include state filters should narrow inherited layers.");
      helper.assertTrue(composed.layers().getFirst().variants().equals(Set.of(VisualVariant.of("industrial"))),
         "V7 include variant filters should narrow inherited layers.");
      helper.assertTrue(composed.material("shared").renderPriority() == 4,
         "V7 root materials should override duplicate included material ids.");
      helper.assertTrue(composition.report().issues().stream().anyMatch(issue -> "duplicate_composed_layer".equals(issue.code())),
         "V7 composition should warn for duplicate layer ids.");
      helper.assertTrue(composition.report().issues().stream().anyMatch(issue -> "duplicate_composed_material".equals(issue.code())),
         "V7 composition should warn for duplicate material ids.");
      helper.assertTrue(composition.report().issues().stream().anyMatch(issue -> "missing_profile_include".equals(issue.code())),
         "V7 composition should warn for missing includes.");
      helper.assertTrue(composition.report().issues().stream().anyMatch(issue -> "profile_include_cycle".equals(issue.code())),
         "V7 composition should warn for include cycles.");

      var diagnostics = RenderCoreProfileValidator.diagnostics(composition.profiles(), Map.of(), Map.of(), 5, 5, 0, composition.report());
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "invalid_material_option".equals(issue.code())),
         "V7 diagnostics should warn for invalid material override values.");

      ProfilePreviewExport export = RenderCoreProfilePreviewer.export(composition.profiles(), Map.of(), Map.of(), diagnostics);
      helper.assertTrue(export.index().toJson().get("profiles").getAsJsonArray().size() == composition.profiles().size(),
         "V7 preview index should include every composed profile.");
      helper.assertTrue(export.snippets().size() == composition.profiles().size(),
         "V7 preview snippets should be generated for every composed profile.");
      helper.assertTrue(export.snippets().stream().allMatch(snippet -> snippet.json().has("includes")),
         "V7 preview snippets should include copy-pasteable include examples.");
      helper.assertTrue(export.report().issues().stream().anyMatch(issue -> "screenshot_preview_unavailable".equals(issue.code())),
         "V7 default screenshot provider should report no-op screenshot availability safely.");
      helper.assertTrue(export.index().toJson().get("profiles").getAsJsonArray().get(0).getAsJsonObject().has("screenshot_provider"),
         "V7 preview index should expose screenshot provider metadata.");
      helper.succeed();
   }

   private static void v8NeonEffects(GameTestHelper helper) {
      VisualProfile base = RenderCoreJsonParsers.parseVisualProfile(id("v8_base"), object("""
         {
           "schema_version": 8,
           "base_texture": "echorendercore:textures/block/v8_base.png",
           "effect": {
             "preset": "neon",
             "glow_intensity": 1.2,
             "pulse_speed": 1.5,
             "pulse_min_alpha": 0.55,
             "pulse_max_alpha": 0.95
           },
           "materials": {
             "hologram": {
               "color": "#FF66E8FF",
               "emissive": true,
               "effect": {
                 "preset": "hologram",
                 "scanline_strength": 0.35
               }
             },
             "unsupported": {
               "effect": {
                 "preset": "future_laser",
                 "glow_intensity": 12.0,
                 "bloom_intensity": 1.0,
                 "pulse_speed": 25.0
               }
             }
           },
           "layers": [
             {
               "id": "profile_effect_layer",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v8_profile.png"
             },
             {
               "id": "material_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v8_material.png",
               "material": "hologram"
             },
             {
               "id": "layer_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v8_layer.png",
               "material": "hologram",
               "effect": {
                 "preset": "energy_field",
                 "flicker_intensity": 0.1,
                 "hue_shift_speed": 0.05,
                 "depth_bias": 2.0
               }
             },
             {
               "id": "bad_effect_layer",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v8_bad.png",
               "material": "unsupported"
             }
           ]
         }
         """));
      helper.assertTrue(base.schemaVersion() == 8, "V8 schema version should parse.");
      helper.assertTrue(base.effect().kind() == VisualEffectKind.NEON, "V8 profile effect should parse.");
      helper.assertTrue(base.material("hologram").effect().kind() == VisualEffectKind.HOLOGRAM, "V8 material effect should parse.");
      helper.assertTrue(base.layers().get(2).effect().kind() == VisualEffectKind.ENERGY_FIELD, "V8 layer effect should parse.");
      helper.assertTrue(base.effectFor(base.layers().get(0)).kind() == VisualEffectKind.NEON, "V8 profile effect should be the fallback.");
      helper.assertTrue(base.effectFor(base.layers().get(1)).kind() == VisualEffectKind.HOLOGRAM, "V8 material effect should override profile effect.");
      helper.assertTrue(base.effectFor(base.layers().get(2)).kind() == VisualEffectKind.ENERGY_FIELD, "V8 layer effect should override material effect.");

      VisualProfileBuilder builder = VisualProfileBuilder.create(id("v8_built"))
         .schemaVersion(8)
         .baseTexture(id("textures/block/v8_built.png"))
         .effect(new VisualEffectProfile(VisualEffectKind.NEON, 1.0F, 0.0F, 1.0F, 0.6F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F))
         .material(new VisualMaterial(
            "terminal",
            0xFFCC66FF,
            0.9F,
            true,
            com.knoxhack.echorendercore.animation.AnimationBlendMode.ADDITIVE,
            VisualLightMode.FULLBRIGHT,
            VisualRenderPass.EMISSIVE,
            null,
            false,
            1,
            null,
            null,
            null,
            1,
            new VisualEffectProfile(VisualEffectKind.TERMINAL_HUD, 1.0F, 0.0F, 2.0F, 0.5F, 1.0F, 0.0F, 0.4F, 0.0F, 1.0F)
         ));
      builder.layer(new VisualLayerProfile(
         "terminal_overlay",
         VisualLayerKind.OVERLAY,
         id("textures/block/v8_terminal.png"),
         "terminal",
         Set.of(VisualState.ACTIVE),
         Set.of(),
         List.of(),
         0xFFFFFFFF,
         1.0F,
         true,
         VisualLightMode.FULLBRIGHT,
         VisualRenderPass.EMISSIVE,
         null,
         false,
         0,
         null,
         null,
         null,
         0,
         new VisualEffectProfile(VisualEffectKind.TERMINAL_HUD, 1.0F, 0.0F, 2.0F, 0.5F, 1.0F, 0.0F, 0.4F, 0.0F, 1.0F)
      ));
      VisualProfile built = RenderCoreJsonParsers.parseVisualProfile(builder.id(), builder.toJson());
      helper.assertTrue(built.effect().kind() == VisualEffectKind.NEON, "V8 builder profile effect should round-trip.");
      helper.assertTrue(built.material("terminal").effect().kind() == VisualEffectKind.TERMINAL_HUD, "V8 builder material effect should round-trip.");
      helper.assertTrue(built.layers().getFirst().effect().kind() == VisualEffectKind.TERMINAL_HUD, "V8 builder layer effect should round-trip.");

      VisualProfile older = RenderCoreJsonParsers.parseVisualProfile(id("v7_unchanged"), object("""
         { "schema_version": 7, "base_texture": "echorendercore:textures/block/v7.png" }
         """));
      helper.assertTrue(older.effect().kind() == VisualEffectKind.NONE, "V7 profiles should default to no effect.");

      var composition = RenderCoreProfileComposer.composeAll(Map.of(base.id(), base, built.id(), built, older.id(), older));
      helper.assertTrue(composition.profiles().get(base.id()).effect().kind() == VisualEffectKind.NEON,
         "V8 composition should preserve profile effects.");
      var diagnostics = RenderCoreProfileValidator.diagnostics(composition.profiles(), Map.of(), Map.of(), 3, 3, 0);
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "unsupported_effect_option".equals(issue.code())),
         "V8 diagnostics should warn for unsupported effect presets.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "invalid_effect_option".equals(issue.code())),
         "V8 diagnostics should warn for risky effect values.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "effect_pipeline_unavailable".equals(issue.code())),
         "V8 diagnostics should report advanced bloom fallback.");
      helper.assertTrue(diagnostics.performanceReport().summaries().get(base.id().toString()).activeEffectCount() >= 4,
         "V8 performance summaries should count active effects.");

      ProfilePreviewExport export = RenderCoreProfilePreviewer.export(composition.profiles(), Map.of(), Map.of(), diagnostics);
      JsonObject artifact = export.report().artifacts().stream()
         .filter(value -> value.id().equals(base.id()))
         .findFirst()
         .orElseThrow()
         .json();
      helper.assertTrue(artifact.has("effect"), "V8 preview artifacts should include profile effect metadata.");
      helper.assertTrue(artifact.get("layers").getAsJsonArray().get(0).getAsJsonObject().has("effect"),
         "V8 preview artifacts should include layer effect metadata.");
      helper.assertTrue(export.snippets().stream().allMatch(snippet -> snippet.json().get("schema_version").getAsInt() >= 8),
         "V8 preview snippets should use schema version 8.");
      helper.succeed();
   }

   private static void v9AdvancedFx(GameTestHelper helper) {
      VisualProfile v9 = RenderCoreJsonParsers.parseVisualProfile(id("v9_advanced"), object("""
         {
           "schema_version": 9,
           "base_texture": "echorendercore:textures/block/v9.png",
           "effect": {
             "preset": "neon",
             "glow_intensity": 1.4,
             "bloom_intensity": 0.8,
             "pulse_speed": 1.0,
             "advanced_enabled": true,
             "bloom_radius": 3.0,
             "bloom_threshold": 0.65,
             "bloom_passes": 3,
             "screen_blend": 0.35,
             "target_scope": "block"
           },
           "materials": {
             "hologram": {
               "color": "#FF44DFFF",
               "emissive": true,
               "effect": {
                 "preset": "hologram",
                 "glow_intensity": 1.1,
                 "advanced_enabled": true,
                 "bloom_radius": 1.5,
                 "bloom_threshold": 0.7,
                 "bloom_passes": 1,
                 "screen_blend": 0.2,
                 "target_scope": "entity"
               }
             }
           },
           "layers": [
             {
               "id": "profile_effect_layer",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v9_profile.png"
             },
             {
               "id": "material_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v9_material.png",
               "material": "hologram"
             },
             {
               "id": "layer_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v9_layer.png",
               "material": "hologram",
               "effect": {
                 "preset": "terminal_hud",
                 "glow_intensity": 1.0,
                 "bloom_intensity": 0.6,
                 "scanline_strength": 0.4,
                 "advanced_enabled": true,
                 "bloom_radius": 2.0,
                 "bloom_threshold": 0.6,
                 "bloom_passes": 2,
                 "screen_blend": 0.25,
                 "target_scope": "global"
               }
             }
           ]
         }
         """));
      helper.assertTrue(v9.schemaVersion() == 9, "V9 schema version should parse.");
      helper.assertTrue(v9.effect().advancedEnabled(), "V9 profile effect should parse advanced_enabled.");
      helper.assertTrue(close(v9.effect().bloomRadius(), 3.0F), "V9 profile effect should parse bloom_radius.");
      helper.assertTrue(close(v9.effect().bloomThreshold(), 0.65F), "V9 profile effect should parse bloom_threshold.");
      helper.assertTrue(v9.effect().bloomPasses() == 3, "V9 profile effect should parse bloom_passes.");
      helper.assertTrue(close(v9.effect().screenBlend(), 0.35F), "V9 profile effect should parse screen_blend.");
      helper.assertTrue(v9.effect().targetScope() == VisualEffectTargetScope.BLOCK, "V9 profile effect should parse target_scope.");
      helper.assertTrue(v9.effectFor(v9.layers().get(0)).targetScope() == VisualEffectTargetScope.BLOCK,
         "V9 profile effect should remain the fallback.");
      helper.assertTrue(v9.effectFor(v9.layers().get(1)).targetScope() == VisualEffectTargetScope.ENTITY,
         "V9 material effect should override profile effect.");
      helper.assertTrue(v9.effectFor(v9.layers().get(2)).targetScope() == VisualEffectTargetScope.GLOBAL,
         "V9 layer effect should override material effect.");

      VisualProfileBuilder builder = VisualProfileBuilder.create(id("v9_built"))
         .schemaVersion(9)
         .baseTexture(id("textures/block/v9_built.png"))
         .effect(new VisualEffectProfile(
            VisualEffectKind.NEON,
            1.2F,
            0.7F,
            1.0F,
            0.65F,
            1.0F,
            0.0F,
            0.0F,
            0.02F,
            0.0F,
            true,
            2.5F,
            0.55F,
            2,
            0.3F,
            VisualEffectTargetScope.ENTITY
         ));
      VisualProfile built = RenderCoreJsonParsers.parseVisualProfile(builder.id(), builder.toJson());
      helper.assertTrue(built.effect().advancedEnabled(), "V9 builder should round-trip advanced_enabled.");
      helper.assertTrue(built.effect().targetScope() == VisualEffectTargetScope.ENTITY, "V9 builder should round-trip target_scope.");

      VisualProfile v8 = RenderCoreJsonParsers.parseVisualProfile(id("v8_compatible"), object("""
         {
           "schema_version": 8,
           "base_texture": "echorendercore:textures/block/v8.png",
           "effect": { "preset": "neon", "bloom_intensity": 0.4 }
         }
         """));
      helper.assertFalse(v8.effect().advancedEnabled(), "V8 profiles should default advanced FX to disabled.");
      helper.assertTrue(v8.effect().targetScope() == VisualEffectTargetScope.PROFILE, "V8 profiles should default target_scope to profile.");

      var diagnostics = RenderCoreProfileValidator.diagnostics(Map.of(v9.id(), v9, built.id(), built, v8.id(), v8), Map.of(), Map.of(), 1, 1, 0);
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "effect_pipeline_unavailable".equals(issue.code())),
         "V9 diagnostics should report optional advanced pipeline fallback.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "advanced_effect_disabled".equals(issue.code())),
         "V9 diagnostics should warn when bloom metadata is present but advanced_enabled is false.");
      helper.assertTrue(diagnostics.performanceReport().issues().stream().anyMatch(issue -> "profile_perf_high_bloom_cost".equals(issue.code())),
         "V9 performance diagnostics should flag high bloom cost.");
      var summary = diagnostics.performanceReport().summaries().get(v9.id().toString());
      helper.assertTrue(summary.estimatedBloomCost() > 0, "V9 performance summary should include bloom cost.");
      helper.assertTrue(summary.advancedEffectPassCount() > 0, "V9 performance summary should include pass count.");
      helper.assertTrue("block".equals(summary.primaryEffectTargetScope()), "V9 performance summary should include target scope.");

      ProfilePreviewExport export = RenderCoreProfilePreviewer.export(Map.of(v9.id(), v9, built.id(), built, v8.id(), v8), Map.of(), Map.of(), diagnostics);
      JsonObject artifact = export.report().artifacts().stream()
         .filter(value -> value.id().equals(v9.id()))
         .findFirst()
         .orElseThrow()
         .json();
      JsonObject artifactEffect = artifact.get("effect").getAsJsonObject();
      helper.assertTrue(artifactEffect.get("advanced_enabled").getAsBoolean(), "V9 preview artifact should include advanced effect fields.");
      helper.assertTrue(artifact.get("summary").getAsJsonObject().has("bloom_cost"), "V9 preview summary should include bloom cost.");
      helper.assertTrue(export.snippets().stream().allMatch(snippet -> snippet.json().get("schema_version").getAsInt() >= 9),
         "V9 preview snippets should use schema version 9.");

      VisualProfile advancedExample = parseExampleVisualProfile(helper, "v9_advanced_neon.visual_profile.json");
      VisualProfile fallbackExample = parseExampleVisualProfile(helper, "v9_fallback_hologram.visual_profile.json");
      helper.assertTrue(advancedExample.schemaVersion() == 9 && advancedExample.effect().advancedEnabled(),
         "V9 advanced example should parse and opt into advanced FX.");
      helper.assertTrue(fallbackExample.schemaVersion() == 9 && !fallbackExample.effect().advancedEnabled(),
         "V9 fallback example should parse without enabling advanced FX.");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v10IsolatedAdvancedFx(GameTestHelper helper) {
      VisualProfile v10 = RenderCoreJsonParsers.parseVisualProfile(id("v10_isolated"), object("""
         {
           "schema_version": 10,
           "base_texture": "echorendercore:textures/block/v10.png",
           "effect": {
             "preset": "neon",
             "glow_intensity": 1.6,
             "bloom_intensity": 0.85,
             "advanced_enabled": true,
             "bloom_radius": 2.5,
             "bloom_threshold": 0.62,
             "bloom_passes": 2,
             "screen_blend": 0.35,
             "target_scope": "block",
             "bloom_mask_mode": "emissive",
             "bloom_tint": "#FFFF44FF",
             "bloom_mask_alpha": 0.72,
             "bloom_channel": "hero_core",
             "bloom_downscale": 2,
             "advanced_priority": 42
           },
           "materials": {
             "hud": {
               "color": "#FF44DDFF",
               "emissive": true,
               "effect": {
                 "preset": "terminal_hud",
                 "advancedEnabled": true,
                 "bloomIntensity": 0.5,
                 "bloomMaskMode": "layer_alpha",
                 "bloomTint": [0.2, 0.9, 1.0, 1.0],
                 "bloomMaskAlpha": 0.45,
                 "bloomChannel": "hud",
                 "bloomDownscale": 4,
                 "advancedPriority": 12,
                 "targetScope": "entity"
               }
             }
           },
           "layers": [
             {
               "id": "profile_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v10_profile.png"
             },
             {
               "id": "material_effect_layer",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v10_material.png",
               "material": "hud"
             },
             {
               "id": "layer_effect_layer",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v10_layer.png",
               "material": "hud",
               "effect": {
                 "preset": "energy_field",
                 "advanced_enabled": true,
                 "bloom_intensity": 0.6,
                 "bloom_mask_mode": "solid",
                 "bloom_channel": "layer_shell",
                 "bloom_downscale": 1,
                 "advanced_priority": 75,
                 "target_scope": "global"
               }
             }
           ]
         }
         """));
      helper.assertTrue(v10.schemaVersion() == 10, "V10 schema version should parse.");
      helper.assertTrue(v10.effect().bloomMaskMode() == VisualEffectBloomMaskMode.EMISSIVE, "V10 bloom_mask_mode should parse.");
      helper.assertTrue(v10.effect().bloomTint() == 0xFFFF44FF, "V10 bloom_tint should parse ARGB colors.");
      helper.assertTrue(close(v10.effect().bloomMaskAlpha(), 0.72F), "V10 bloom_mask_alpha should parse.");
      helper.assertTrue("hero_core".equals(v10.effect().bloomChannel()), "V10 bloom_channel should parse.");
      helper.assertTrue(v10.effect().bloomDownscale() == 2, "V10 bloom_downscale should parse.");
      helper.assertTrue(v10.effect().advancedPriority() == 42, "V10 advanced_priority should parse.");
      helper.assertTrue(v10.material("hud").effect().bloomMaskMode() == VisualEffectBloomMaskMode.LAYER_ALPHA,
         "V10 camelCase material effect aliases should parse.");
      helper.assertTrue(v10.effectFor(v10.layers().get(0)).bloomMaskMode() == VisualEffectBloomMaskMode.EMISSIVE,
         "V10 profile effect should remain the fallback.");
      helper.assertTrue(v10.effectFor(v10.layers().get(1)).bloomMaskMode() == VisualEffectBloomMaskMode.LAYER_ALPHA,
         "V10 material effect should override profile effect.");
      helper.assertTrue(v10.effectFor(v10.layers().get(2)).bloomMaskMode() == VisualEffectBloomMaskMode.SOLID,
         "V10 layer effect should override material effect.");

      VisualProfile older = RenderCoreJsonParsers.parseVisualProfile(id("v7_still_plain"), object("""
         { "schema_version": 7, "base_texture": "echorendercore:textures/block/v7.png" }
         """));
      helper.assertTrue(older.effect().kind() == VisualEffectKind.NONE, "Older profiles should still resolve missing effect to none.");

      VisualProfileBuilder builder = VisualProfileBuilder.create(id("v10_built"))
         .schemaVersion(10)
         .baseTexture(id("textures/block/v10_built.png"))
         .effect(new VisualEffectProfile(
            VisualEffectKind.NEON,
            1.4F,
            0.9F,
            1.2F,
            0.6F,
            1.0F,
            0.0F,
            0.1F,
            0.02F,
            0.0F,
            true,
            2.0F,
            0.6F,
            2,
            0.3F,
            VisualEffectTargetScope.ENTITY,
            VisualEffectBloomMaskMode.SOLID,
            0xFF66E8FF,
            0.8F,
            "builder_core",
            4,
            80
         ));
      VisualProfile built = RenderCoreJsonParsers.parseVisualProfile(builder.id(), builder.toJson());
      helper.assertTrue(built.effect().bloomMaskMode() == VisualEffectBloomMaskMode.SOLID, "V10 builder should round-trip mask mode.");
      helper.assertTrue(built.effect().bloomDownscale() == 4, "V10 builder should round-trip downscale.");
      helper.assertTrue(built.effect().advancedPriority() == 80, "V10 builder should round-trip priority.");

      VisualProfile invalid = RenderCoreJsonParsers.parseVisualProfile(id("v10_invalid"), object("""
         {
           "schema_version": 10,
           "base_texture": "echorendercore:textures/block/v10_invalid.png",
           "effect": {
             "preset": "neon",
             "bloom_intensity": 0.4,
             "advanced_enabled": true,
             "bloom_mask_mode": "warp",
             "bloom_mask_alpha": 1.25,
             "bloom_channel": "   ",
             "bloom_downscale": 3,
             "advanced_priority": 150
           }
         }
         """));

      VisualProfileBuilder budgetBuilder = VisualProfileBuilder.create(id("v10_budget"))
         .schemaVersion(10)
         .baseTexture(id("textures/block/v10_budget.png"));
      for (int i = 0; i < 100; i++) {
         budgetBuilder.layer(new VisualLayerProfile(
            "mask_" + i,
            VisualLayerKind.GLOW,
            id("textures/block/v10_mask_" + i + ".png"),
            "default",
            Set.of(),
            Set.of(),
            List.of(),
            0xFFFFFFFF,
            1.0F,
            true,
            VisualLightMode.FULLBRIGHT,
            VisualRenderPass.EMISSIVE,
            null,
            null,
            0,
            null,
            null,
            null,
            0,
            new VisualEffectProfile(
               VisualEffectKind.NEON,
               1.0F,
               0.5F,
               0.0F,
               1.0F,
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               true,
               1.0F,
               0.6F,
               1,
               0.2F,
               VisualEffectTargetScope.BLOCK,
               VisualEffectBloomMaskMode.AUTO,
               null,
               null,
               "channel_" + (i % 5),
               2,
               i % 2 == 0 ? 10 : -10
            )
         ));
      }
      VisualProfile budget = RenderCoreJsonParsers.parseVisualProfile(budgetBuilder.id(), budgetBuilder.toJson());
      var diagnostics = RenderCoreProfileValidator.diagnostics(Map.of(v10.id(), v10, built.id(), built, invalid.id(), invalid, budget.id(), budget), Map.of(), Map.of(), 1, 1, 0);
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "unsupported_effect_option".equals(issue.code())),
         "V10 diagnostics should warn for unsupported mask modes.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "invalid_effect_option".equals(issue.code())),
         "V10 diagnostics should warn for invalid mask values.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "advanced_effect_config_disabled".equals(issue.code())),
         "V10 diagnostics should report config-gated advanced FX.");
      helper.assertTrue(diagnostics.validationReport().issues().stream().anyMatch(issue -> "advanced_effect_mask_unavailable".equals(issue.code())),
         "V10 diagnostics should report client mask fallback risk.");
      helper.assertTrue(diagnostics.performanceReport().issues().stream().anyMatch(issue -> "advanced_effect_budget_exceeded".equals(issue.code())),
         "V10 performance diagnostics should report budget pressure.");
      helper.assertTrue(diagnostics.performanceReport().issues().stream().anyMatch(issue -> "advanced_effect_channel_limit".equals(issue.code())),
         "V10 performance diagnostics should report channel pressure.");
      var summary = diagnostics.performanceReport().summaries().get(budget.id().toString());
      helper.assertTrue(summary.estimatedMaskSubmissions() >= 96, "V10 performance summary should include mask submissions.");
      helper.assertTrue(summary.estimatedBloomChannelCount() == 5, "V10 performance summary should include channel count.");
      helper.assertTrue(summary.estimatedPrioritySkips() > 0, "V10 performance summary should include priority skips.");
      helper.assertTrue("auto".equals(summary.advancedFxMode()), "V10 performance summary should include the V12 advanced-FX budget policy.");

      ProfilePreviewExport export = RenderCoreProfilePreviewer.export(Map.of(v10.id(), v10, built.id(), built, budget.id(), budget), Map.of(), Map.of(), diagnostics);
      JsonObject artifact = export.report().artifacts().stream()
         .filter(value -> value.id().equals(v10.id()))
         .findFirst()
         .orElseThrow()
         .json();
      JsonObject artifactEffect = artifact.get("effect").getAsJsonObject();
      helper.assertTrue(artifactEffect.has("bloom_mask_mode"), "V10 preview artifact should include mask mode.");
      helper.assertTrue(artifact.get("summary").getAsJsonObject().has("estimated_mask_submissions"),
         "V10 preview summary should include mask submissions.");
      helper.assertTrue(export.snippets().stream().allMatch(snippet -> snippet.json().get("schema_version").getAsInt() >= 10),
         "V10 preview snippets should use schema version 10.");

      VisualProfile isolatedExample = parseExampleVisualProfile(helper, "v10_isolated_neon.visual_profile.json");
      VisualProfile compatibilityExample = parseExampleVisualProfile(helper, "v10_v9_compat.visual_profile.json");
      helper.assertTrue(isolatedExample.schemaVersion() == 10 && isolatedExample.effect().bloomMaskMode() == VisualEffectBloomMaskMode.EMISSIVE,
         "V10 isolated example should parse rich mask fields.");
      helper.assertTrue(compatibilityExample.schemaVersion() == 9 && compatibilityExample.effect().bloomMaskMode() == VisualEffectBloomMaskMode.AUTO,
         "V9 compatibility example should parse unchanged under V10 defaults.");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v11CreatorWorkbench(GameTestHelper helper) {
      VisualProfile v11 = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v11_creator"), object("""
         {
           "schema_version": 11,
           "base_texture": "echorendercore:textures/block/v11_creator.png",
            "particle_profile": "echorendercore:v11_particles",
            "animation_profile": "echorendercore:v11_animation",
            "default_state": "ACTIVE",
            "state_animations": { "ACTIVE": "boot" },
            "state_texture_variants": {
              "DAMAGED": "echorendercore:textures/block/v11_creator_damaged.png"
            },
            "variants": {
              "overcharged": "echorendercore:textures/block/v11_creator_overcharged.png"
            },
            "includes": [
              { "profile": "echorendercore:v11_shared_glass", "states": ["ACTIVE"] }
            ],
            "anchors": {
              "core": [0.0, 0.5, 0.0]
            },
            "effect": {
              "preset": "neon",
              "glow_intensity": 1.5,
             "bloom_intensity": 0.8,
             "advanced_enabled": true,
             "bloom_mask_mode": "emissive",
             "bloom_channel": "creator",
             "advanced_priority": 60
           },
           "materials": {
             "terminal": {
               "color": "#FF36E8FF",
               "emissive": true,
               "effect": {
                 "preset": "terminal_hud",
                 "scanline_strength": 0.35
               }
             }
           },
           "layers": [
             {
               "id": "terminal_overlay",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v11_terminal.png",
               "material": "terminal",
               "states": ["ACTIVE"]
             }
           ]
         }
         """));
      helper.assertTrue(v11.schemaVersion() == VisualProfile.CURRENT_SCHEMA_VERSION
            && v11.schemaVersion() == 12
            && "echorendercore".equals(v11.surface().ownerAddon())
            && !v11.screenChrome().scanlines(),
         "V11 runtime profiles should auto-migrate in memory to the V12 clean-glass contract.");
      helper.assertTrue(id("textures/block/v11_creator.png").equals(v11.baseTexture())
            && id("v11_particles").equals(v11.particleProfile())
            && id("v11_animation").equals(v11.animationProfile())
            && "boot".equals(v11.animationFor(VisualState.ACTIVE))
            && id("textures/block/v11_creator_damaged.png").equals(v11.textureFor(VisualState.DAMAGED, VisualVariant.DEFAULT))
            && id("textures/block/v11_creator_overcharged.png").equals(v11.textureFor(VisualState.ACTIVE, VisualVariant.of("overcharged")))
            && v11.anchor("core") != null
            && v11.includes().size() == 1
            && id("v11_shared_glass").equals(v11.includes().getFirst().profileId())
            && v11.material("terminal").effect().scanlineStrength() == 0.35F
            && v11.layers().stream().anyMatch(layer -> "terminal_overlay".equals(layer.id())
               && id("textures/block/v11_terminal.png").equals(layer.texture()))
            && v11.effect().advancedEnabled(),
         "V11 auto-migration should preserve textures, animation and particle profile ids, materials, includes, anchors, layers, and effects.");
      expectJsonFailure(helper, () -> RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v10_runtime_rejected"), object("""
         { "schema_version": 10, "base_texture": "echorendercore:textures/block/v10.png" }
         """)), "V12 runtime parser should reject V7-V10 legacy runtime profiles.");

      VisualProfileBuilder defaultBuilder = VisualProfileBuilder.create(id("v11_default_builder"))
         .baseTexture(id("textures/block/v11_default.png"));
      helper.assertTrue(defaultBuilder.toJson().get("schema_version").getAsInt() == VisualProfile.CURRENT_SCHEMA_VERSION,
         "Visual builders should default to the current V12 schema version.");

      List<CreatorMigrationReport> migrations = List.of(
         RenderCoreProfileMigration.migrateVisualProfile(id("legacy_v7"), object("""
            { "schema_version": 7, "base_texture": "echorendercore:textures/block/v7.png" }
            """)),
         RenderCoreProfileMigration.migrateVisualProfile(id("legacy_v8"), object("""
            { "schema_version": 8, "effects": { "preset": "neon" } }
            """)),
         RenderCoreProfileMigration.migrateVisualProfile(id("legacy_v9"), object("""
            { "schema_version": 9, "effect": { "preset": "neon", "advanced_enabled": true, "target_scope": "entity" } }
            """)),
         RenderCoreProfileMigration.migrateVisualProfile(id("legacy_v10"), object("""
            { "schema_version": 10, "effect": { "preset": "neon", "bloom_mask_mode": "emissive" } }
            """))
      );
      helper.assertTrue(migrations.stream().allMatch(CreatorMigrationReport::migrationRequired),
         "V7-V10 migration reports should all require creator migration before V12 runtime activation.");
      helper.assertTrue(migrations.stream().allMatch(report -> report.migratedJson().get("schema_version").getAsInt() == 12
            && report.migratedJson().has("surface")
            && report.migratedJson().has("fallback")
            && report.migratedJson().has("budget")
            && report.migratedJson().has("screen_chrome")
            && report.migratedJson().has("qa")),
         "V7-V10 migration output should normalize schema_version and V12 metadata sections.");
      helper.assertTrue(migrations.get(1).migratedJson().has("effect") && !migrations.get(1).migratedJson().has("effects"),
         "V12 migration should normalize effects aliases to effect.");

      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v11_particles"), object("""
         { "emitters": { "orbit": { "anchor": "core", "particle": "minecraft:dust", "rate": 0.1 } } }
         """));
      var diagnostics = RenderCoreProfileValidator.diagnostics(Map.of(v11.id(), v11), Map.of(), Map.of(particles.id(), particles), 2, 2, 0);
      CreatorExportIndex export = RenderCoreCreatorPackExporter.export(
         Map.of(v11.id(), v11),
         Map.of(),
         Map.of(particles.id(), particles),
         diagnostics,
         new ProfileScreenshotPreviewProvider() {
            @Override
            public String id() {
               return "test_screenshot";
            }

            @Override
            public boolean available(VisualProfile profile) {
               return profile.id().equals(v11.id());
            }
         }
      );
      helper.assertTrue(export.cards().size() == 1 && export.artifacts().size() == 1,
         "V12 creator export should produce one card and one artifact.");
      helper.assertTrue(export.cards().getFirst().screenshotAvailable(), "V12 creator cards should expose optional screenshot availability.");
      helper.assertTrue(!export.cards().getFirst().migrationRequired(), "V11 auto-migrated runtime profiles should not require creator migration.");
      JsonObject artifact = export.artifacts().getFirst().json();
      helper.assertTrue(artifact.get("normalized_profile").getAsJsonObject().get("schema_version").getAsInt() == 12
            && artifact.get("normalized_profile").getAsJsonObject().has("surface"),
         "V12 creator artifacts should include normalized profile JSON with surface metadata.");
      helper.assertTrue(export.toJson().get("profiles").getAsJsonArray().size() == 1,
         "V12 creator export index should serialize profile cards.");

      VisualProfile legacy = RenderCoreJsonParsers.parseVisualProfile(id("v10_legacy_card"), object("""
         { "schema_version": 10, "base_texture": "echorendercore:textures/block/v10_legacy.png" }
         """));
      CreatorExportIndex mixed = RenderCoreCreatorPackExporter.export(Map.of(v11.id(), v11, legacy.id(), legacy), Map.of(), Map.of(), null,
         ProfileScreenshotPreviewProvider.NO_OP);
      helper.assertTrue(mixed.cards().stream().anyMatch(card -> card.profileId().equals(legacy.id()) && card.migrationRequired()),
         "V12 creator export should flag V7-V10 legacy profile cards as migration-required.");

      VisualProfile example = parseExampleVisualProfile(helper, "v11_creator_workbench.visual_profile.json");
      helper.assertTrue(example.schemaVersion() == 12 && example.effect().advancedEnabled(),
         "Historical creator examples should ship as normalized V12 profile JSON.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v12PackCertification(GameTestHelper helper) {
      VisualProfile certified = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v12_certified"), object("""
         {
           "schema_version": 12,
           "surface": {
             "type": "static_block",
             "owner_addon": "echorendercore",
             "display_name": "V12 Certified",
             "tags": ["static_block"],
             "integration_mode": "direct"
           },
           "fallback": {
             "mode": "stable",
             "expectation": "Stable emissive fallback remains readable.",
             "allow_advanced_fx_fallback": true,
             "allow_missing_assets": false
           },
           "budget": { "tier": "normal", "advanced_fx": "auto" },
           "qa": { "required": false, "evidence": [], "reduced_motion": false },
           "base_texture": "echorendercore:textures/block/v12_certified.png",
           "default_state": "ACTIVE",
           "effect": { "preset": "neon", "glow_intensity": 0.75 },
           "layers": [
             {
               "id": "status",
               "kind": "glow",
               "texture": "echorendercore:textures/block/v12_status.png",
               "states": ["ACTIVE"]
             }
           ]
         }
         """));
      var cleanDiagnostics = RenderCoreProfileValidator.diagnostics(Map.of(certified.id(), certified), Map.of(), Map.of(), 1, 1, 0);
      CreatorExportIndex passExport = RenderCoreCreatorPackExporter.export(
         Map.of(certified.id(), certified),
         Map.of(),
         Map.of(),
         cleanDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(VisualProfile.CURRENT_SCHEMA_VERSION == 12, "Creator tooling should target the V12 runtime contract.");
      helper.assertTrue(passExport.manifest().schemaVersion() == CreatorPackManifest.CREATOR_PACK_VERSION,
         "Creator-pack exports should use the current creator tooling schema version.");
      helper.assertTrue(passExport.manifest().toJson().get("target_schema_version").getAsInt() == 12,
         "V21 creator-pack exports should target runtime schema 12.");
      helper.assertTrue(passExport.certification().status() == CreatorCertificationResult.PASS,
         "Clean V12 profiles with screenshots should certify as pass.");
      helper.assertTrue(passExport.toJson().has("certification"),
         "V21 creator export indexes should include certification data.");
      helper.assertTrue(passExport.toJson().has("visual_qa"),
         "V21 creator export indexes should include visual QA release-evidence data.");
      helper.assertTrue(passExport.summaryLine().contains("certification pass"),
         "V21 export summaries should include certification status.");

      CreatorExportIndex missingScreenshot = RenderCoreCreatorPackExporter.export(
         Map.of(certified.id(), certified),
         Map.of(),
         Map.of(),
         cleanDiagnostics,
         ProfileScreenshotPreviewProvider.NO_OP
      );
      helper.assertTrue(missingScreenshot.certification().status() == CreatorCertificationResult.WARN
            && !missingScreenshot.certification().failed(),
         "Missing optional screenshots should warn without failing certification.");

      for (int schema = 7; schema <= 10; schema++) {
         VisualProfile legacy = RenderCoreJsonParsers.parseVisualProfile(id("legacy_v" + schema + "_cert"), object("""
            { "schema_version": %s, "base_texture": "echorendercore:textures/block/legacy.png" }
            """.formatted(schema)));
         CreatorExportIndex legacyExport = RenderCoreCreatorPackExporter.export(
            Map.of(legacy.id(), legacy),
            Map.of(),
            Map.of(),
            null,
            alwaysScreenshotProvider()
         );
          helper.assertTrue(legacyExport.certification().status() == CreatorCertificationResult.FAIL
                && legacyExport.certification().migrationRequiredCount() == 1,
             "V7-V10 profiles should fail V21 certification through migration_required.");
       }

      ProfileDiagnosticsReport errorDiagnostics = new ProfileDiagnosticsReport(
         new ProfileValidationReport(List.of(new ProfileValidationIssue(
            ProfileValidationSeverity.ERROR,
            certified.id(),
            "manual_certification_error",
            "visual_profile",
            "Synthetic certification error.",
            "Fix the source profile before shipping."
         ))),
         ProfilePerformanceReport.EMPTY,
         cleanDiagnostics.cacheMetrics()
      );
      CreatorExportIndex errorExport = RenderCoreCreatorPackExporter.export(
         Map.of(certified.id(), certified),
         Map.of(),
         Map.of(),
         errorDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(errorExport.certification().status() == CreatorCertificationResult.FAIL
            && errorExport.certification().validationErrorCount() == 1,
         "Validation errors should fail V21 certification.");

      VisualProfile warningOnly = RenderCoreJsonParsers.parseVisualProfile(id("v12_warning_only"), object("""
         { "schema_version": 12, "effect": { "preset": "neon", "bloom_intensity": 0.1 } }
         """));
      var warningDiagnostics = RenderCoreProfileValidator.diagnostics(Map.of(warningOnly.id(), warningOnly), Map.of(), Map.of(), 1, 1, 0);
      CreatorExportIndex warningExport = RenderCoreCreatorPackExporter.export(
         Map.of(warningOnly.id(), warningOnly),
         Map.of(),
         Map.of(),
         warningDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(warningExport.certification().status() == CreatorCertificationResult.WARN
            && !warningExport.certification().failed(),
         "Validation warnings should warn, not fail, under errors_only certification.");

      ProfileDiagnosticsReport performanceDiagnostics = new ProfileDiagnosticsReport(
         ProfileValidationReport.EMPTY,
         new ProfilePerformanceReport(
            Map.of(),
            List.of(new ProfilePerformanceIssue(
               certified.id(),
               "profile_perf_high_layer_count",
               ProfileValidationSeverity.WARNING,
               "Synthetic performance warning.",
               13,
               12
            ))
         ),
         cleanDiagnostics.cacheMetrics()
      );
      CreatorExportIndex performanceExport = RenderCoreCreatorPackExporter.export(
         Map.of(certified.id(), certified),
         Map.of(),
         Map.of(),
         performanceDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(performanceExport.certification().status() == CreatorCertificationResult.WARN
            && performanceExport.certification().performanceWarningCount() == 1,
         "Performance warnings should warn without failing certification.");

      VisualProfile missingDependency = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v12_missing_dependency"), object("""
         {
           "schema_version": 12,
           "surface": { "type": "static_block", "owner_addon": "echorendercore" },
           "base_texture": "echorendercore:textures/block/v12_missing_dependency.png",
           "animation_profile": "echorendercore:missing_animation"
         }
         """));
      var dependencyDiagnostics = RenderCoreProfileValidator.diagnostics(Map.of(missingDependency.id(), missingDependency), Map.of(), Map.of(), 1, 1, 0);
      CreatorExportIndex dependencyExport = RenderCoreCreatorPackExporter.export(
         Map.of(missingDependency.id(), missingDependency),
         Map.of(),
         Map.of(),
         dependencyDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(dependencyExport.certification().status() == CreatorCertificationResult.FAIL
            && dependencyExport.certification().missingDependencyCount() == 1,
         "Missing required dependencies should fail V21 certification.");

      ProfileDiagnosticsReport malformedDiagnostics = new ProfileDiagnosticsReport(
         ProfileValidationReport.EMPTY,
         ProfilePerformanceReport.EMPTY,
         new ProfileCacheMetrics(0, 0, 0, 1, 0, 1, 0, 0, 0, Set.of(EchoRenderCore.MODID), 0, 0)
      );
      CreatorExportIndex malformedExport = RenderCoreCreatorPackExporter.export(
         Map.of(),
         Map.of(),
         Map.of(),
         malformedDiagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(malformedExport.certification().status() == CreatorCertificationResult.FAIL
            && malformedExport.certification().malformedSourceCount() == 1,
         "Malformed source JSON should fail V21 certification.");

      VisualProfile alpha = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("alpha_cert"), object("""
         { "schema_version": 12, "surface": { "type": "static_block", "owner_addon": "echorendercore" }, "base_texture": "echorendercore:textures/block/alpha.png" }
         """));
      VisualProfile beta = RenderCoreJsonParsers.parseRuntimeVisualProfile(Identifier.fromNamespaceAndPath("othermod", "beta_cert"), object("""
         { "schema_version": 12, "surface": { "type": "static_block", "owner_addon": "othermod" }, "base_texture": "othermod:textures/block/beta.png" }
         """));
      ProfileDiagnosticsReport deterministicDiagnostics = new ProfileDiagnosticsReport(
         new ProfileValidationReport(List.of(
            new ProfileValidationIssue(ProfileValidationSeverity.WARNING, beta.id(), "z_warning", "profile", "Beta warning.", ""),
            new ProfileValidationIssue(ProfileValidationSeverity.WARNING, alpha.id(), "a_warning", "profile", "Alpha warning.", "")
         )),
         ProfilePerformanceReport.EMPTY,
         cleanDiagnostics.cacheMetrics()
      );
      CreatorExportIndex deterministicExport = RenderCoreCreatorPackExporter.export(
         Map.of(alpha.id(), alpha, beta.id(), beta),
         Map.of(),
         Map.of(),
         deterministicDiagnostics,
         alwaysScreenshotProvider()
      );
      var summaries = deterministicExport.certification().toJson().get("issue_summaries").getAsJsonArray();
      helper.assertTrue(summaries.get(0).getAsJsonObject().get("profile").getAsString().equals(alpha.id().toString()),
         "V21 certification issue summaries should sort deterministically by profile id.");
      CreatorExportIndex namespaceOnly = deterministicExport.forNamespace(EchoRenderCore.MODID);
      helper.assertTrue(namespaceOnly.certification().checkedProfileCount() == 1
            && namespaceOnly.certification().issueSummaries().stream().allMatch(issue -> issue.profile().startsWith(EchoRenderCore.MODID + ":")),
         "V21 certification should support namespace-filtered reports.");

      VisualProfile example = parseExampleVisualProfile(helper, "v12_certified_pack.visual_profile.json");
      helper.assertTrue(example.schemaVersion() == 12 && example.surface().type().id().equals("static_block"),
         "V12 certified-pack examples should use the V12 runtime contract.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      helper.succeed();
   }

   private static void v13CompleteVision(GameTestHelper helper) {
      VisualProfile profile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v13_authoring"), object("""
         {
           "schema_version": 11,
           "base_texture": "echorendercore:textures/block/v13_authoring.png",
           "default_state": "ACTIVE",
           "effect": {
             "preset": "neon",
             "advanced_enabled": true,
             "bloom_mask_mode": "emissive",
             "bloom_channel": "showcase"
           },
           "layers": [
             {
               "id": "screen",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v13_authoring_screen.png",
               "states": ["ACTIVE"],
               "parts": ["screen"],
               "effect": { "preset": "terminal_hud", "scanline_strength": 0.35 }
             }
           ],
           "anchors": { "screen": [0.0, 0.7, -0.5] }
         }
         """));
      var diagnostics = RenderCoreProfileValidator.diagnostics(Map.of(profile.id(), profile), Map.of(), Map.of(), 1, 1, 0);
      CreatorExportIndex export = RenderCoreCreatorPackExporter.export(
         Map.of(profile.id(), profile),
         Map.of(),
         Map.of(),
         diagnostics,
         alwaysScreenshotProvider()
      );
      helper.assertTrue(export.manifest().schemaVersion() == CreatorPackManifest.CREATOR_PACK_VERSION,
         "Creator exports should use the current creator tooling version.");
      helper.assertTrue(export.manifest().toJson().get("target_schema_version").getAsInt() == 12,
         "V21 creator exports should target runtime schema 12.");
      helper.assertTrue(export.addonIntegrations().stream().anyMatch(entry -> "echoterminal".equals(entry.namespace()))
            && export.addonIntegrations().stream().anyMatch(entry -> "echonexusprotocol".equals(entry.namespace())),
         "V21 creator exports should include all-ECHO showcase coverage.");
      helper.assertTrue(export.toJson().get("addon_integrations").getAsJsonArray().size() == export.addonIntegrations().size(),
         "V21 creator export JSON should serialize addon integration coverage.");
      var card = export.cards().getFirst();
      helper.assertTrue(card.screenshotPath().contains("/rendercore/creator/screenshots/"),
         "V21 creator cards should include deterministic screenshot paths.");
      JsonObject artifact = export.artifacts().getFirst().json();
      helper.assertTrue(artifact.has("workbench_draft"),
         "V21 creator artifacts should include draft metadata for the Workbench editor.");
      CreatorProfileDraft draft = CreatorProfileDraft.from(profile, card)
         .withTitle("Edited V13 Profile")
         .withNotes("Workbench authoring metadata")
         .withScreenshotPath(card.screenshotPath())
         .withProfileEffectPreset("hologram")
         .withLayerEffectPreset("screen", "terminal_hud")
         .withMaterialEffectPreset("screen_material", "neon")
         .withAnchor("editor_anchor", new RenderCoreVector(0.0F, 1.0F, 0.0F))
         .withInclude(id("v13_neon_cube_core"));
      JsonObject profileJson = draft.toProfileJson();
      helper.assertTrue(profileJson.getAsJsonObject("preview").get("title").getAsString().equals("Edited V13 Profile"),
         "V13 drafts should write edited preview titles into generated profile JSON.");
      helper.assertTrue(profileJson.getAsJsonObject("effect").get("preset").getAsString().equals("hologram"),
         "V13 drafts should edit profile-level effects.");
      helper.assertTrue(profileJson.getAsJsonObject("anchors").has("editor_anchor")
            && profileJson.getAsJsonArray("includes").size() == 1,
         "V13 drafts should edit anchors and dependencies.");
      helper.assertTrue(!draft.toJson().get("generated_path").getAsString().contains(":\\"),
         "V13 draft metadata should avoid machine-local absolute paths.");
      VisualProfile example = parseExampleVisualProfile(helper, "v13_complete_vision.visual_profile.json");
      helper.assertTrue(example.schemaVersion() == 12 && example.effect().advancedEnabled(),
         "V13 complete-vision example should remain parseable as normalized V12 content with advanced-safe metadata.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v14VisualProof(GameTestHelper helper) {
      VisualProfile profile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v14_visual_proof"), object("""
         {
           "schema_version": 11,
           "base_texture": "echorendercore:textures/block/v14_visual_proof.png",
           "effect": {
             "preset": "neon",
             "advanced_enabled": true,
             "bloom_mask_mode": "emissive",
             "bloom_channel": "qa",
             "advanced_priority": 10
           },
           "layers": [
             {
               "id": "terminal",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v14_terminal.png",
               "effect": { "preset": "terminal_hud", "advanced_enabled": true, "bloom_mask_mode": "layer_alpha" }
             }
           ],
           "anchors": { "terminal": [0.0, 0.6, 0.0] }
         }
         """));
      CreatorExportIndex export = RenderCoreCreatorPackExporter.export(
         Map.of(profile.id(), profile),
         Map.of(),
         Map.of(),
         RenderCoreProfileValidator.diagnostics(Map.of(profile.id(), profile), Map.of(), Map.of(), 1, 1, 0),
         alwaysScreenshotProvider()
      );

      List<CreatorVisualQaReport.EvidenceSnapshot> snapshots = List.of(
         new CreatorVisualQaReport.EvidenceSnapshot(
            "entity_isolated_resize",
            "effects advanced isolated",
            "isolated / session override",
            "",
            "",
            2,
            0,
            2,
            1,
            2,
            3,
            24,
            false,
            "assets/echorendercore/rendercore/creator/screenshots/entity_isolated.png"
         ),
         new CreatorVisualQaReport.EvidenceSnapshot(
            "block_fullscreen_shader",
            "effects advanced fullscreen fallback",
            "fullscreen / session override",
            "isolated mode unavailable",
            "advanced_effect_compile_failed shader unavailable",
            1,
            1,
            2,
            1,
            2,
            2,
            18,
            true,
            ""
         ),
         new CreatorVisualQaReport.EvidenceSnapshot(
            "stable_fallback_reload",
            "effects stable fallback",
            "stable / config",
            "config mode stable",
            "",
            0,
            0,
            0,
            0,
            2,
            0,
            0,
            false,
            ""
         )
      );
      CreatorVisualQaReport report = CreatorVisualQaReport.fromSnapshots(snapshots, CreatorAddonShowcaseCatalog.echoVisionCoverage());
      CreatorExportIndex withEvidence = export.withVisualQa(report);
      JsonObject json = withEvidence.toJson();

      helper.assertTrue(withEvidence.manifest().schemaVersion() == CreatorPackManifest.CREATOR_PACK_VERSION
            && withEvidence.manifest().toJson().get("target_schema_version").getAsInt() == 12,
         "Creator exports should target the V12 runtime profile contract.");
      helper.assertTrue(report.remainingBlockers().isEmpty(),
         "V21 visual QA reports should clear blockers when all fallback modes have evidence.");
      helper.assertTrue(report.screenshotEvidenceCount() == 1
            && report.testedFallbackModes().containsAll(List.of("isolated", "fullscreen_fallback", "stable_fallback",
               "shader_unavailable", "resize_reload", "entity_masks", "block_masks")),
         "V21 visual QA reports should count screenshots and tested fallback modes deterministically.");
      helper.assertTrue(json.getAsJsonObject("visual_qa").get("snapshots").getAsJsonArray().size() == 3,
         "V21 creator-pack JSON should serialize visual QA evidence snapshots.");
      helper.assertTrue(!json.toString().contains(":\\\\"),
         "V21 visual QA exports should avoid machine-local absolute paths.");
      helper.assertTrue(withEvidence.summaryLine().contains("visual_qa"),
         "V21 export summaries should include visual QA status.");
      helper.assertTrue(CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
            .anyMatch(entry -> "echoterminal".equals(entry.namespace()) && "terminal_hud".equals(entry.surfaceType()))
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echoholomap".equals(entry.namespace()) && "holo_display".equals(entry.surfaceType()))
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echoconvoyprotocol".equals(entry.namespace()) && "motion_particles".equals(entry.surfaceType()))
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echoindustrialnexus".equals(entry.namespace()) && "machines_blocks".equals(entry.surfaceType()))
             && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
                .anyMatch(entry -> "echonexusprotocol".equals(entry.namespace()) && "global_atmosphere".equals(entry.surfaceType())),
         "V21 addon showcase registration should cover terminal, holo, motion, machines, and atmosphere targets.");
      helper.assertTrue(CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
            .anyMatch(entry -> "echoruntimeguard".equals(entry.namespace())
               && "declared_no_visual_surface".equals(entry.status())
               && !entry.notes().isBlank()),
         "V21 service-only addons should declare a no-visual-surface reason.");

      CreatorProfileDraft draft = CreatorProfileDraft.from(profile, withEvidence.cards().getFirst())
         .withProfileEffectPreset("neon")
         .withMaterialEffectPreset("workbench_material", "hologram")
         .withLayerEffectPreset("workbench_layer", "terminal_hud")
         .withAnchor("workbench_anchor", new RenderCoreVector(0.0F, 1.0F, 0.0F))
         .withInclude(id("v13_neon_cube_core"));
      JsonObject draftJson = draft.toProfileJson();
      helper.assertTrue(draftJson.getAsJsonObject("materials").getAsJsonObject("workbench_material")
            .getAsJsonObject("effect").get("preset").getAsString().equals("hologram")
            && draftJson.getAsJsonArray("layers").size() >= 2
            && draftJson.getAsJsonObject("anchors").has("workbench_anchor"),
         "V21 Workbench draft controls should round-trip material, layer, anchor, and include edits.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v15RealRenderMod(GameTestHelper helper) {
      helper.assertTrue(VisualProfile.CURRENT_SCHEMA_VERSION == 12,
         "V21 advances runtime visual profiles to V12.");
      helper.assertTrue(CreatorPackManifest.CREATOR_PACK_VERSION >= 15,
         "V15 and later creator tooling should advance export metadata only.");

      VisualProfile profile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v15_real_render_mob"), object("""
         {
           "schema_version": 11,
           "base_texture": "echonexusprotocol:textures/entity/nexus_husk.png",
           "particle_profile": "echonexusprotocol:echo_mobs/nexus_husk",
           "effect": { "preset": "hologram", "target_scope": "entity" },
           "layers": [
             {
               "id": "core_glow",
               "kind": "glow",
               "texture": "echonexusprotocol:textures/entity/rendercore_echo_mobs/nexus_husk_glow.png",
               "states": ["IDLE", "ACTIVE", "DAMAGED"],
               "effect": { "preset": "neon", "bloom_mask_mode": "emissive", "target_scope": "entity" }
             }
           ],
           "anchors": {
             "core": [0.0, 1.05, 0.0],
             "trail": [0.0, 0.18, 0.55],
             "head": [0.0, 1.5, -0.35]
           }
         }
         """));
      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v15_real_render_mob"), object("""
         {
           "emitters": {
             "aura": { "anchor": "core", "particle": "minecraft:dust", "rate": 0.05, "options": { "type": "minecraft:dust", "color": [0.3, 0.9, 1.0], "scale": 0.5 } },
             "trail": { "anchor": "trail", "particle": "minecraft:smoke", "requires_moving": true, "rate": 0.04 },
             "spark": { "anchor": "head", "particle": "minecraft:electric_spark", "state": "DAMAGED", "rate": 0.03 }
           }
         }
         """));
      helper.assertTrue(profile.schemaVersion() == 12 && profile.effect().targetScope() == VisualEffectTargetScope.ENTITY,
         "V11 mob visuals should auto-migrate to V12 without losing entity-scoped effect metadata.");
      helper.assertTrue(particles.emitters().values().stream().allMatch(emitter -> profile.anchor(emitter.anchor()) != null),
         "V15 particle emitters should reference anchors present in their matching visual profile.");
      helper.assertTrue(CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
            .anyMatch(entry -> "echonexusprotocol".equals(entry.namespace())
               && "rendercore_native".equals(entry.renderIntegrationStatus())
               && entry.convertedEntityCount() == 7)
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echoorbitalremnants".equals(entry.namespace())
                  && "rendercore_native".equals(entry.renderIntegrationStatus())
                  && entry.convertedEntityCount() == 11)
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echoindustrialnexus".equals(entry.namespace())
                  && entry.convertedEntityCount() == 2)
            && CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
               .anyMatch(entry -> "echologisticsnetwork".equals(entry.namespace())
                  && entry.convertedEntityCount() == 1),
         "V15 showcase coverage should report real converted entity counts.");
      helper.assertTrue(CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
            .filter(entry -> Set.of("echonexusprotocol", "echoorbitalremnants", "echoindustrialnexus", "echologisticsnetwork")
               .contains(entry.namespace()))
            .allMatch(entry -> entry.showcaseProfile() != null && entry.namespace().equals(entry.showcaseProfile().getNamespace())),
         "V15 converted addon showcase entries should point to real addon-owned RenderCore profiles.");
      JsonObject addonJson = CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
         .filter(entry -> "echonexusprotocol".equals(entry.namespace()))
         .findFirst()
         .orElseThrow()
         .toJson();
      helper.assertTrue(addonJson.getAsJsonObject("render_integration").get("status").getAsString().equals("rendercore_native")
            && addonJson.getAsJsonObject("render_integration").get("converted_entity_count").getAsInt() == 7,
         "V15 creator-pack JSON should serialize render integration metadata deterministically.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      helper.succeed();
   }

   private static void v16MachineScreenVisuals(GameTestHelper helper) {
      helper.assertTrue(VisualProfile.CURRENT_SCHEMA_VERSION == 12,
         "V21 advances runtime visual profiles to V12.");
      helper.assertTrue(CreatorPackManifest.CREATOR_PACK_VERSION >= 16,
         "V16 and later should advance creator/export tooling metadata only.");

      VisualProfile profile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v16_machine_screen_surface"), object("""
         {
           "schema_version": 11,
           "base_texture": "echorendercore:textures/block/v16_machine.png",
           "particle_profile": "echorendercore:v16_machine_screen_particles",
           "effect": { "preset": "terminal_hud", "target_scope": "block" },
           "materials": {
             "status_panel": {
               "color": "#FF39D8FF",
               "fullbright": true,
               "effect": { "preset": "terminal_hud", "scanline_strength": 0.45, "target_scope": "block" }
             }
           },
           "layers": [
             {
               "id": "status_screen",
               "kind": "emissive",
               "texture": "echorendercore:textures/block/v16_machine_screen.png",
               "material": "status_panel",
               "parts": ["screen_panel"],
               "effect": { "preset": "neon", "bloom_mask_mode": "emissive", "target_scope": "block" }
             },
             {
               "id": "hud_scanlines",
               "kind": "overlay",
               "texture": "echorendercore:textures/block/v16_scanlines.png",
               "alpha": 0.55,
               "parts": ["screen_panel"],
               "effect": { "preset": "terminal_hud", "target_scope": "block" }
             }
           ],
           "block_parts": {
             "screen_panel": { "directions": ["north"], "tint_indices": [0] },
             "core_mask": { "indices": [0, 1], "ambient_occlusion": false }
           },
           "anchors": {
             "core": [0.0, 0.5, 0.0],
             "screen": [0.0, 0.8, -0.51],
             "vent": [0.35, 0.2, 0.0]
           }
         }
         """));
      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v16_machine_screen_particles"), object("""
         {
           "emitters": {
             "screen_pixels": { "anchor": "screen", "particle": "minecraft:electric_spark", "rate": 0.04 },
             "vent_vapor": { "anchor": "vent", "particle": "minecraft:smoke", "rate": 0.03 },
             "core_aura": { "anchor": "core", "particle": "minecraft:dust", "rate": 0.02 }
           }
         }
         """));
      helper.assertTrue(profile.schemaVersion() == 12
            && profile.effect().targetScope() == VisualEffectTargetScope.BLOCK
            && profile.blockPart("screen_panel") != null
            && profile.layers().stream().allMatch(layer -> !layer.partFilter().isEmpty()),
         "V11 machine profiles should auto-migrate to V12 without losing block-scoped effects, block part masks, or anchors.");
      helper.assertTrue(particles.emitters().values().stream().allMatch(emitter -> profile.anchor(emitter.anchor()) != null),
         "V16 block/screen particle emitters should reference anchors present in their matching visual profile.");

      CreatorAddonIntegration terminal = CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
         .filter(entry -> "echoterminal".equals(entry.namespace()))
         .findFirst()
         .orElseThrow();
      CreatorAddonIntegration signalOs = CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
         .filter(entry -> "echosignalos".equals(entry.namespace()))
         .findFirst()
         .orElseThrow();
      CreatorAddonIntegration blockworks = CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
         .filter(entry -> "echoblockworks".equals(entry.namespace()))
         .findFirst()
         .orElseThrow();
      CreatorAddonIntegration multiblock = CreatorAddonShowcaseCatalog.echoVisionCoverage().stream()
         .filter(entry -> "echomultiblockcore".equals(entry.namespace()))
         .findFirst()
         .orElseThrow();
      helper.assertTrue(terminal.surfaceIntegration().worldSurfaceCount() == 1
            && terminal.surfaceIntegration().screenSurfaceCount() == 1
            && terminal.surfaceIntegration().convertedBlockEntityCount() == 1,
         "V16 terminal coverage should report one world block entity and one screen visual.");
      helper.assertTrue(signalOs.surfaceIntegration().worldSurfaceCount() == 2
            && signalOs.surfaceIntegration().screenSurfaceCount() == 2
            && signalOs.surfaceIntegration().profileIds().stream().map(Identifier::toString).anyMatch("signalos:server_rack"::equals),
         "V16 SignalOS coverage should report terminal and server rack world/screen surfaces.");
      helper.assertTrue(blockworks.surfaceIntegration().particleOnlyStaticSurfaceCount() == 6,
         "V16 Blockworks coverage should report six particle-only static surfaces.");
      helper.assertTrue(multiblock.surfaceIntegration().convertedBlockEntityCount() == 2,
         "V16 Multiblock coverage should report controller and robotic arm block entities.");

      CreatorExportIndex export = RenderCoreCreatorPackExporter.export(
         Map.of(profile.id(), profile),
         Map.of(),
         Map.of(particles.id(), particles),
         RenderCoreProfileValidator.diagnostics(Map.of(profile.id(), profile), Map.of(), Map.of(particles.id(), particles), 1, 1, 0),
         alwaysScreenshotProvider()
      );
      JsonObject json = export.toJson();
      JsonObject surface = json.getAsJsonObject("surface_integration");
      helper.assertTrue(json.getAsJsonObject("manifest").get("schema_version").getAsInt() >= 16
            && json.getAsJsonObject("manifest").get("target_schema_version").getAsInt() == 12,
         "V21 creator exports should target runtime schema 12.");
      helper.assertTrue(surface.get("world_surface_count").getAsInt() >= 6
            && surface.get("screen_surface_count").getAsInt() >= 6
            && surface.get("particle_only_static_surface_count").getAsInt() >= 8
            && export.summaryLine().contains("surfaces"),
         "V16 creator exports should include deterministic aggregate surface integration metadata.");
      helper.assertTrue(export.addonIntegrations().stream()
            .filter(entry -> "echoterminal".equals(entry.namespace()))
            .findFirst()
            .orElseThrow()
            .toJson()
            .getAsJsonObject("surface_integration")
            .get("screen_surface_count")
            .getAsInt() == 1,
         "V16 addon integration JSON should serialize per-addon surface integration metadata.");
      assertSchemaResource(helper, "creator_pack.schema.json");
      assertCommonPackagesClientSafe(helper);
      helper.succeed();
   }

   private static void v17ScreenFrameOptions(GameTestHelper helper) {
      RenderCoreScreenFrameOptions indexOptions =
         new RenderCoreScreenFrameOptions("", false, true, true, true, true);
      helper.assertTrue(indexOptions.label().isEmpty()
            && !indexOptions.drawLabel()
            && indexOptions.drawScanlines()
            && indexOptions.scanlines()
            && indexOptions.scanlinesBehindContent()
            && indexOptions.accentBars()
            && indexOptions.quietFallback()
            && indexOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS
            && !indexOptions.backdrop()
            && indexOptions.edgeGlow()
            && indexOptions.cornerBrackets()
            && indexOptions.accentRails()
            && indexOptions.glassGlints()
            && indexOptions.chromaticEdge(),
          "V17 Index frame options should allow RenderCore chrome without a RenderCore-owned title.");

      RenderCoreScreenFrameOptions legacyOptions = RenderCoreScreenFrameOptions.legacy("Legacy Label");
      helper.assertTrue("Legacy Label".equals(legacyOptions.label())
            && legacyOptions.drawLabel()
            && legacyOptions.drawScanlines()
            && !legacyOptions.scanlinesBehindContent()
            && legacyOptions.accentBars()
            && !legacyOptions.quietFallback()
            && legacyOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS
            && !legacyOptions.backdrop()
            && legacyOptions.edgeGlow()
            && legacyOptions.glassGlints()
            && legacyOptions.chromaticEdge(),
          "V17 legacy frame options should preserve the old labeled frame behavior.");

      RenderCoreScreenFrameOptions quietOptions = RenderCoreScreenFrameOptions.quiet();
      helper.assertTrue(quietOptions.label().isEmpty()
            && !quietOptions.drawLabel()
            && !quietOptions.drawScanlines()
            && quietOptions.scanlinesBehindContent()
            && !quietOptions.accentBars()
            && quietOptions.quietFallback()
            && quietOptions.style() == RenderCoreScreenChromeStyle.MINIMAL
            && !quietOptions.backdrop()
            && !quietOptions.edgeGlow()
            && !quietOptions.cornerBrackets()
            && !quietOptions.glassGlints()
            && !quietOptions.chromaticEdge(),
          "V17 quiet frame options should provide a low-noise fallback surface.");

      RenderCoreScreenFrameOptions builderOptions = RenderCoreScreenFrameOptions.builder()
         .label(" Cyberglass ")
         .scanlines(true)
         .quietFallback(true)
         .build();
      helper.assertTrue("Cyberglass".equals(builderOptions.label())
            && builderOptions.drawLabel()
            && builderOptions.drawScanlines()
            && builderOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS
            && builderOptions.backdrop()
            && builderOptions.edgeGlow()
            && builderOptions.cornerBrackets()
            && builderOptions.accentRails()
            && builderOptions.glassGlints()
            && builderOptions.chromaticEdge()
            && builderOptions.quietFallback(),
         "V17 builder options should default to cyberglass chrome with explicit controls.");

      RenderCoreScreenFrameOptions cleanDefaultOptions = RenderCoreScreenFrameOptions.builder().build();
      helper.assertTrue(cleanDefaultOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS
            && !cleanDefaultOptions.drawLabel()
            && !cleanDefaultOptions.drawScanlines()
            && cleanDefaultOptions.backdrop()
            && cleanDefaultOptions.edgeGlow()
            && cleanDefaultOptions.cornerBrackets()
            && cleanDefaultOptions.accentRails()
            && cleanDefaultOptions.glassGlints()
            && cleanDefaultOptions.chromaticEdge(),
         "V21 builder defaults should prefer clean cyberglass without scanline banding.");

      RenderCoreScreenFrameOptions cyberglassOptions =
         RenderCoreScreenFrameOptions.cyberglass(" Overlay ").backdrop(false).build();
      helper.assertTrue("Overlay".equals(cyberglassOptions.label())
            && cyberglassOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS
            && cyberglassOptions.drawLabel()
            && !cyberglassOptions.drawScanlines()
            && !cyberglassOptions.backdrop()
            && cyberglassOptions.chromaticEdge(),
         "V21 cyberglass convenience options should stay clean while allowing quiet overlay overrides.");

      RenderCoreScreenFrameOptions terminalOptions = RenderCoreScreenFrameOptions.terminal(" Terminal ").build();
      helper.assertTrue("Terminal".equals(terminalOptions.label())
            && terminalOptions.style() == RenderCoreScreenChromeStyle.TERMINAL
            && terminalOptions.drawLabel()
            && !terminalOptions.drawScanlines()
            && terminalOptions.accentRails()
            && !terminalOptions.backdrop()
            && !terminalOptions.glassGlints()
            && terminalOptions.chromaticEdge(),
         "V21 terminal convenience options should produce clean glass screen chrome without scanlines.");

      RenderCoreScreenFrameOptions hologramOptions = RenderCoreScreenFrameOptions.hologram("").build();
      helper.assertTrue(hologramOptions.style() == RenderCoreScreenChromeStyle.HOLOGRAM
            && !hologramOptions.drawLabel()
            && !hologramOptions.drawScanlines()
            && !hologramOptions.backdrop()
            && hologramOptions.glassGlints()
            && hologramOptions.chromaticEdge(),
         "V21 hologram convenience options should produce light glass chrome without labels by default.");

      RenderCoreScreenFrameOptions minimalOptions = RenderCoreScreenFrameOptions.minimal().build();
      helper.assertTrue(minimalOptions.style() == RenderCoreScreenChromeStyle.MINIMAL
            && !minimalOptions.drawLabel()
            && !minimalOptions.drawScanlines()
            && !minimalOptions.accentRails()
            && !minimalOptions.edgeGlow()
            && !minimalOptions.quietFallback(),
         "V21 minimal convenience options should stay visual-only without forcing fallback behavior.");

      RenderCoreScreenFrameOptions neonOptions = RenderCoreScreenFrameOptions.neon(" Alert ").build();
      helper.assertTrue("Alert".equals(neonOptions.label())
            && neonOptions.style() == RenderCoreScreenChromeStyle.NEON
            && neonOptions.drawLabel()
            && !neonOptions.drawScanlines()
            && !neonOptions.backdrop()
            && neonOptions.glassGlints()
            && neonOptions.chromaticEdge(),
         "V21 neon convenience options should keep high-emphasis chrome clean unless scanlines are explicitly enabled.");

      VisualProfile chromeProfile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v21_profile_screen_chrome"), object("""
         {
           "schema_version": 12,
           "surface": { "type": "screen", "owner_addon": "echorendercore", "display_name": "V21 Chrome" },
           "screen_chrome": {
             "style": "hologram",
             "label": " Map ",
             "backdrop": false,
             "edge_glow": true,
             "corner_brackets": true,
             "accent_rails": false,
             "scanlines": true,
             "glass_glints": true,
             "chromatic_edge": true,
             "quiet_fallback": false
           },
           "base_texture": "echorendercore:textures/gui/v21_profile_screen.png"
         }
         """));
      RenderCoreScreenFrameOptions profileOptions =
         RenderCoreScreenFrameOptions.fromProfile(chromeProfile, RenderCoreScreenFrameOptions.terminal("Fallback").build());
      RenderCoreScreenFrameOptions reducedProfileOptions =
         RenderCoreScreenFrameOptions.fromProfile(chromeProfile, RenderCoreScreenFrameOptions.terminal("Fallback").build(), true);
      helper.assertTrue("Map".equals(profileOptions.label())
            && profileOptions.style() == RenderCoreScreenChromeStyle.HOLOGRAM
            && profileOptions.drawLabel()
            && profileOptions.drawScanlines()
            && !profileOptions.backdrop()
            && !profileOptions.accentRails()
            && profileOptions.glassGlints()
            && profileOptions.chromaticEdge()
            && reducedProfileOptions.style() == RenderCoreScreenChromeStyle.HOLOGRAM
            && !reducedProfileOptions.drawScanlines()
            && !reducedProfileOptions.edgeGlow()
            && !reducedProfileOptions.glassGlints()
            && !reducedProfileOptions.chromaticEdge(),
         "V21 profile-derived frame options should honor V12 screen chrome metadata and reduced-motion cleanup.");

      RenderCoreScreenFrameOptions nullProfileFallback = RenderCoreScreenFrameOptions.minimal().quietFallback(true).build();
      RenderCoreScreenFrameOptions nullProfileOptions = RenderCoreScreenFrameOptions.fromProfile(null, nullProfileFallback);
      helper.assertTrue(nullProfileOptions.style() == RenderCoreScreenChromeStyle.MINIMAL
            && nullProfileOptions.quietFallback()
            && !nullProfileOptions.drawScanlines(),
         "V21 profile-derived frame options should use the supplied fallback when profile metadata is unavailable.");

      VisualProfile unsupportedChromeProfile = RenderCoreJsonParsers.parseRuntimeVisualProfile(id("v21_unsupported_screen_chrome"), object("""
         {
           "schema_version": 12,
           "surface": { "type": "screen", "owner_addon": "echorendercore", "display_name": "Unsupported Chrome" },
           "screen_chrome": {
             "style": "cassette_terminal",
             "label": "Unsupported",
             "backdrop": false,
             "edge_glow": true,
             "corner_brackets": true,
             "accent_rails": true,
             "scanlines": false,
             "glass_glints": true,
             "chromatic_edge": true,
             "quiet_fallback": false
           },
           "base_texture": "echorendercore:textures/gui/v21_unsupported_screen.png"
         }
         """));
      RenderCoreScreenFrameOptions unsupportedChromeOptions =
         RenderCoreScreenFrameOptions.fromProfile(unsupportedChromeProfile, RenderCoreScreenFrameOptions.neon("Fallback").build());
      RenderCoreScreenFrameOptions unsupportedChromeDefaultOptions =
         RenderCoreScreenFrameOptions.fromProfile(unsupportedChromeProfile, null);
      helper.assertTrue("Unsupported".equals(unsupportedChromeOptions.label())
            && unsupportedChromeOptions.style() == RenderCoreScreenChromeStyle.NEON
            && unsupportedChromeDefaultOptions.style() == RenderCoreScreenChromeStyle.CYBERGLASS,
         "V21 unsupported screen chrome styles should use the supplied fallback style, then cyberglass if no fallback is supplied.");

      RenderCoreScreenVisuals.ScreenVisualData fallback = RenderCoreScreenVisuals.resolve(null);
      helper.assertTrue(fallback.accentColor() == 0xFF66E8FF
            && fallback.panelColor() == 0xEE061018
            && fallback.borderColor() == 0xAA38DFF4,
         "V17 screen visuals should preserve fallback colors when no host or ThemeCore theme is available.");
      helper.succeed();
   }

   private static void v20ScreenChromeEvidence(GameTestHelper helper) {
      List<RenderCoreScreenChromeQaCatalog.ScreenChromeSurface> surfaces =
         RenderCoreScreenChromeQaCatalog.requiredSurfaces();
      Set<String> surfaceIds = surfaces.stream()
         .map(RenderCoreScreenChromeQaCatalog.ScreenChromeSurface::surfaceId)
         .collect(java.util.stream.Collectors.toSet());
      Set<String> styles = surfaces.stream()
         .map(RenderCoreScreenChromeQaCatalog.ScreenChromeSurface::chromeStyle)
         .collect(java.util.stream.Collectors.toSet());

      helper.assertTrue(CreatorPackManifest.CREATOR_PACK_VERSION == 21
            && VisualProfile.CURRENT_SCHEMA_VERSION == 12,
         "V21 should publish creator/export metadata 21 and runtime visual schema 12.");
      helper.assertTrue(surfaces.size() == 8
            && surfaceIds.containsAll(List.of(
               "echo_terminal",
               "echo_terminal_reduced_motion",
               "signalos_terminal",
               "signalos_rack",
               "holomap_minimap",
               "index_overlay",
               "lens_overlay",
               "rendercore_cyberglass_example"
            )),
         "V21 screen chrome QA catalog should cover every cyberglass adoption surface.");
      helper.assertTrue(styles.containsAll(List.of("TERMINAL", "CYBERGLASS", "HOLOGRAM")),
         "V21 screen chrome QA catalog should distinguish terminal, cyberglass, and hologram surfaces.");

      RenderCoreScreenChromeQaCatalog.ScreenChromeSurface reducedTerminal =
         RenderCoreScreenChromeQaCatalog.surface("echo_terminal_reduced_motion");
      helper.assertTrue(reducedTerminal != null
            && reducedTerminal.reducedMotion()
            && "TERMINAL".equals(reducedTerminal.chromeStyle())
            && "echoterminal:screen/terminal_hud".equals(reducedTerminal.profileId().toString()),
         "V21 terminal reduced-motion evidence should keep the terminal profile and reduced-motion flag.");

      CreatorVisualQaReport pendingReport =
         CreatorVisualQaReport.fromAddonIntegrations(CreatorAddonShowcaseCatalog.echoVisionCoverage());
      helper.assertTrue(pendingReport.screenChromeEvidence().size() == surfaces.size()
            && pendingReport.screenChromeBlockers().size() == surfaces.size()
            && pendingReport.screenChromeBlockers().contains("echo_terminal_screen_chrome_evidence_missing"),
         "V21 pending screen chrome QA should expose deterministic blockers before screenshots are captured.");

      List<CreatorVisualQaReport.ScreenChromeEvidence> captured = surfaces.stream()
         .map(surface -> surface.evidence(
            "pass",
            "visual_qa/screenshots/screen_chrome/" + surface.surfaceId() + ".png",
            "manual visual pass"
         ))
         .toList();
      CreatorVisualQaReport report = CreatorVisualQaReport.fromSnapshots(
         List.of(),
         CreatorAddonShowcaseCatalog.echoVisionCoverage(),
         captured
      );
      JsonObject json = report.toJson();
      JsonObject advancedFxJson = report.toAdvancedFxJson();
      JsonObject screenChromeJson = report.toScreenChromeJson();
      JsonObject worldSurfaceJson = report.toWorldSurfaceJson();
      helper.assertTrue(report.screenChromeEvidenceCount() == surfaces.size()
            && report.screenChromeBlockers().isEmpty()
            && report.worldSurfaceBlockers().isEmpty(),
         "V21 screen-only QA should clear screen blockers without adding unrelated world-surface blockers.");
      helper.assertTrue(json.get("screen_chrome_surface_count").getAsInt() == surfaces.size()
            && json.get("screen_chrome_evidence_count").getAsInt() == surfaces.size()
            && json.getAsJsonArray("screen_chrome_evidence").size() == surfaces.size()
            && json.getAsJsonArray("screen_chrome_evidence").get(0).getAsJsonObject().has("chrome_style"),
         "V21 visual QA JSON should serialize screen chrome surface metadata.");
      helper.assertTrue("advanced_fx".equals(advancedFxJson.get("evidence_kind").getAsString())
            && advancedFxJson.has("snapshots")
            && !advancedFxJson.has("screen_chrome_evidence")
            && "screen_chrome".equals(screenChromeJson.get("evidence_kind").getAsString())
            && screenChromeJson.has("screen_chrome_evidence")
            && !screenChromeJson.has("snapshots")
            && "world_surface".equals(worldSurfaceJson.get("evidence_kind").getAsString())
            && worldSurfaceJson.has("world_surface_evidence")
            && !worldSurfaceJson.has("screen_chrome_evidence"),
         "V21 visual QA exports should split advanced-FX, screen chrome, and world-surface evidence by concern.");

      CreatorVisualQaReport terminalOnly = report.forNamespace("echoterminal", CreatorAddonShowcaseCatalog.echoVisionCoverage());
      helper.assertTrue(terminalOnly.screenChromeEvidence().size() == 2
            && terminalOnly.screenChromeBlockers().isEmpty()
            && terminalOnly.worldSurfaceBlockers().isEmpty()
            && terminalOnly.screenChromeEvidence().stream().allMatch(evidence -> "echoterminal".equals(evidence.addonId())),
         "V21 namespace filtering should keep only matching screen chrome evidence entries without adding world blockers.");

      CreatorVisualQaReport absolutePathReport = CreatorVisualQaReport.fromSnapshots(
         List.of(),
         CreatorAddonShowcaseCatalog.echoVisionCoverage(),
         List.of(reducedTerminal.evidence("pass", "C:/tmp/terminal.png", "machine-local path"))
      );
      CreatorVisualQaReport.ScreenChromeEvidence sanitized = absolutePathReport.screenChromeEvidence().stream()
         .filter(evidence -> "echo_terminal_reduced_motion".equals(evidence.surfaceId()))
         .findFirst()
         .orElseThrow();
      helper.assertTrue(sanitized.screenshotPath().isBlank()
            && absolutePathReport.screenChromeBlockers().contains("echo_terminal_reduced_motion_screen_chrome_evidence_missing"),
         "V21 screen chrome evidence should reject machine-local absolute screenshot paths.");

      List<CreatorVisualQaReport.WorldSurfaceEvidence> worldEvidence = List.of(
         new CreatorVisualQaReport.WorldSurfaceEvidence(
            "echorendercore",
            "rendercore_advanced_fx_showcase",
            "echorendercore:v21_advanced_fx_showcase",
            "block_entity",
            "pass",
            "manual world-surface pass",
            "visual_qa/screenshots/world_surface/rendercore_advanced_fx_showcase.png"
          )
       );
      List<CreatorVisualQaReport.WorldSurfaceEvidence> pendingWorldEvidence = List.of(
         new CreatorVisualQaReport.WorldSurfaceEvidence(
            "echorendercore",
            "rendercore_required_world",
            "echorendercore:v21_required_world",
            "block_entity",
            "pending",
            "manual world-surface pending",
            ""
         )
      );
      CreatorVisualQaReport withPendingWorldEvidence = CreatorVisualQaReport.fromSnapshots(
         List.of(),
         CreatorAddonShowcaseCatalog.echoVisionCoverage(),
         captured,
         pendingWorldEvidence
      );
      helper.assertTrue(withPendingWorldEvidence.worldSurfaceEvidenceCount() == 0
            && withPendingWorldEvidence.worldSurfaceBlockers().contains("rendercore_required_world_world_surface_evidence_missing"),
         "V21 world-surface blockers should appear only when required world-surface evidence entries are present and still pending.");

      CreatorVisualQaReport withWorldEvidence = CreatorVisualQaReport.fromSnapshots(
         List.of(),
         CreatorAddonShowcaseCatalog.echoVisionCoverage(),
         captured,
         worldEvidence
      );
      helper.assertTrue(withWorldEvidence.worldSurfaceEvidenceCount() == 1
            && withWorldEvidence.worldSurfaceBlockers().isEmpty()
            && withWorldEvidence.toWorldSurfaceJson().getAsJsonArray("world_surface_evidence").size() == 1,
         "V21 world-surface QA evidence should serialize deterministic passing entries.");

      VisualProfile example = parseExampleVisualProfile(helper, "v21_cyberglass_overlay.visual_profile.json");
      helper.assertTrue(example.schemaVersion() == VisualProfile.CURRENT_SCHEMA_VERSION
            && "hud_overlay".equals(example.surface().type().id())
            && "cyberglass".equals(example.screenChrome().style())
            && !example.screenChrome().scanlines()
            && example.effect().bloomTint() != null
            && example.effect().scanlineStrength() == 0.0F,
         "V21 screen QA should keep the generic cyberglass example V12, tint-aware, and scanline-free.");
      VisualProfile terminalExample = parseExampleVisualProfile(helper, "v21_terminal_screen.visual_profile.json");
      helper.assertTrue(terminalExample.schemaVersion() == VisualProfile.CURRENT_SCHEMA_VERSION
            && "terminal".equals(terminalExample.screenChrome().style())
            && !terminalExample.screenChrome().scanlines(),
         "V21 terminal examples should use clean terminal chrome with scanlines opt-in only.");
      assertCreatorPackSchemaVersion(helper, 21);
      helper.succeed();
   }

   private static void v21ScreenChromeEvidence(GameTestHelper helper) {
      v20ScreenChromeEvidence(helper);
   }

   private static void v22SoftMobParticles(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("v22_soft_mob"), object("""
         {
           "schema_version": 12,
           "base_texture": "echorendercore:textures/entity/machine.png",
           "particle_profile": "echorendercore:v22_soft_mob_particles",
           "surface": { "type": "mob_family" },
           "anchors": {
             "eyes": { "part": "eyes", "offset": [0.0, 1.45, -0.35] },
             "legacy": [0.0, 1.0, 0.0],
             "missing_part": { "part": "missing_part", "offset": [0.0, 0.8, 0.0] }
           }
         }
         """));
      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v22_soft_mob_particles"), object("""
         {
           "emitters": {
             "soft_eye_mote": {
               "anchor": "eyes",
               "particle": "echorendercore:soft_mote",
               "states": ["IDLE", "ACTIVE"],
               "rate": 0.12,
               "options": {
                 "type": "echorendercore:soft_mote",
                 "color": "#CC66E8FF",
                 "scale": 0.55,
                 "lifetime": 26,
                 "alpha": 0.72,
                 "drag": 0.91,
                 "fade": true,
                 "style": "toxic_fleck",
                 "fullbright": false,
                 "gravity": -0.002,
                 "spin": 0.02,
                 "stretch": 1.2,
                 "flicker": 0.35
               }
             },
             "legacy_wisp": {
               "anchor": "legacy",
               "particle": "echorendercore:soft_wisp",
               "states": ["ACTIVE"],
               "rate": 0.1,
               "options": {
                 "type": "rendercore:soft_wisp",
                 "color": "#AAFFFFFF",
                 "scale": 0.8,
                 "lifetime": 34,
                 "alpha": 0.55,
                 "drag": 0.88,
                 "fade": false,
                 "style": "frost_breath",
                 "fullbright": false,
                 "gravity": -0.006,
                 "spin": 0.004,
                 "stretch": 1.35,
                 "flicker": 0.1
               }
             }
           }
         }
         """));

      helper.assertTrue("eyes".equals(visual.anchor("eyes").part()), "V22 object anchors should preserve named model parts.");
      helper.assertTrue("legacy".equals(visual.anchor("legacy").part()), "V22 legacy vector anchors should remain compatible.");

      ParticleEmitter softEmitter = particles.emitters().get("soft_eye_mote");
      ParticleOptions resolved = RenderCoreParticleOptionResolvers.resolve(softEmitter);
      helper.assertTrue(resolved instanceof RenderCoreSoftParticleOptions, "V22 soft_mote should resolve to custom RenderCore options.");
      RenderCoreSoftParticleOptions softOptions = (RenderCoreSoftParticleOptions)resolved;
      helper.assertTrue(softOptions.lifetime() == 26 && close(softOptions.alpha(), 0.72F) && close(softOptions.drag(), 0.91F) && softOptions.fade(),
         "V22 soft particle option fields should parse and resolve.");
      helper.assertTrue("toxic_fleck".equals(softOptions.style()) && !softOptions.fullbright() && close(softOptions.gravity(), -0.002F)
            && close(softOptions.spin(), 0.02F) && close(softOptions.stretch(), 1.2F) && close(softOptions.flicker(), 0.35F),
         "V22 themed soft particle option fields should parse and resolve.");

      var report = RenderCoreProfileValidator.validate(Map.of(visual.id(), visual), Map.of(), Map.of(particles.id(), particles));
      helper.assertFalse(report.issues().stream().anyMatch(issue -> "unsupported_particle_option".equals(issue.code())),
         "V22 soft particle options should be first-class validation fields.");

      LivingEntity entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
      entity.setYRot(0.0F);
      Vec3 fallback = ParticleAnchorResolver.resolve(entity, visual, "missing_part", null);
      helper.assertTrue(close((float)(fallback.y - entity.position().y), 0.8F),
         "V22 missing model parts should fall back to the static anchor offset.");
      helper.succeed();
   }

   private static void invalidJsonSafety(GameTestHelper helper) {
      expectJsonFailure(helper, () -> RenderCoreJsonParsers.parseParticleProfile(id("bad_particles"), object("""
         { "emitters": { "broken": { "anchor": "core" } } }
         """)), "Missing particle should fail cleanly.");
      expectJsonFailure(helper, () -> RenderCoreJsonParsers.parseVisualProfile(id("bad_visual"), object("""
         { "anchors": { "core": [0, 1] } }
         """)), "Bad anchor vectors should fail cleanly.");
      helper.succeed();
   }

   private static void animationPlayback(GameTestHelper helper) {
      AnimationClip loop = new AnimationClip(
         "loop",
         true,
         1.0F,
         List.of(new AnimationTrack("wheel", AnimationChannel.ROTATION_X, 0.0F, 360.0F, 0.0F, 1.0F, Easing.LINEAR))
      );
      ModelPose pose = new ModelPose();
      AnimationPlayer.sample(loop, 10.0F, pose);
      helper.assertTrue(close(pose.transform("wheel").xRot(), 180.0F), "Looping clip should interpolate.");

      AnimationClip oneShot = new AnimationClip(
         "shot",
         false,
         0.5F,
         List.of(new AnimationTrack("arm", AnimationChannel.POSITION_Y, 0.0F, 1.0F, 0.0F, 0.5F, Easing.EASE_OUT))
      );
      AnimationPlayer player = new AnimationPlayer();
      player.play(oneShot, 0.0F);
      player.sample(20.0F, pose);
      helper.assertFalse(player.playing(), "One-shot player should stop after clip length.");

      AnimationController controller = new AnimationController();
      controller.bind(VisualState.ACTIVE, loop);
      controller.update(VisualState.ACTIVE, 0.0F);
      controller.sample(10.0F, pose);
      helper.assertTrue(pose.parts().contains("wheel"), "State controller should sample bound clips.");

      AnimationClip keyframed = new AnimationClip(
         "keyframed",
         true,
         2.0F,
         List.of(new AnimationTrack(
            "scanner",
            AnimationChannel.ROTATION_Y,
            0.0F,
            0.0F,
            0.0F,
            2.0F,
            Easing.LINEAR,
            new AnimationTimeline(List.of(
               new AnimationKeyframe(0.0F, 0.0F, Easing.LINEAR),
               new AnimationKeyframe(1.0F, 90.0F, Easing.LINEAR),
               new AnimationKeyframe(2.0F, 180.0F, Easing.LINEAR)
            )),
            com.knoxhack.echorendercore.animation.AnimationBlendMode.REPLACE
         ))
      );
      AnimationPlayer.sample(keyframed, 20.0F, pose);
      helper.assertTrue(close(pose.transform("scanner").yRot(), 90.0F), "Keyframed clips should interpolate.");
      helper.succeed();
   }

   private static void v2ProfileValidation(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("machine"), object("""
         {
           "schema_version": 2,
           "base_texture": "echorendercore:textures/entity/machine.png",
           "animation_profile": "echorendercore:missing_animation",
           "particle_profile": "echorendercore:machine_particles",
           "transition_seconds": 0.25,
           "layers": [
             {
               "id": "glow",
               "kind": "glow",
               "texture": "echorendercore:textures/entity/machine.png",
               "states": ["ONLINE", "WORKING"],
               "color": "#FF66E8FF",
               "alpha": 0.75,
               "emissive": true
             }
           ],
           "anchors": { "core": [0.0, 0.5, 0.0] },
           "state_animations": { "WORKING": "work_loop" }
         }
         """));
      helper.assertTrue(visual.schemaVersion() == 2, "V2 schema version should parse.");
      helper.assertTrue(close(visual.transitionSeconds(), 0.25F), "Transition seconds should parse.");
      helper.assertTrue(!visual.layersFor(VisualState.WORKING, VisualVariant.DEFAULT).isEmpty(), "V2 layers should match states.");

      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("machine_particles"), object("""
         {
           "emitters": {
             "missing_anchor": {
               "anchor": "vent",
               "particle": "minecraft:smoke",
               "state": "DAMAGED",
               "rate": 0.1
             }
           }
         }
         """));
      var report = RenderCoreProfileValidator.validate(Map.of(visual.id(), visual), Map.of(), Map.of(particles.id(), particles));
      helper.assertTrue(report.warnings() >= 2, "Validation should report missing animation profile and particle anchor warnings.");
      helper.succeed();
   }

   private static void v3LintReports(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("v3_machine"), object("""
         {
           "schema_version": 3,
           "base_texture": "echorendercore:textures/entity/machine.png",
           "particle_profile": "echorendercore:v3_particles",
           "layers": [
             {
               "id": "masked_glow",
               "kind": "glow",
               "texture": "echorendercore:textures/entity/machine.png",
               "states": ["ONLINE"],
               "parts": ["core", "missing_core"]
             }
           ],
           "anchors": { "core": [0.0, 0.5, 0.0] }
         }
         """));
      helper.assertTrue(visual.schemaVersion() == 3, "V3 schema version should parse.");
      helper.assertTrue(visual.layers().getFirst().partFilter().contains("core"), "V3 layer parts should parse.");

      ParticleProfile particles = RenderCoreJsonParsers.parseParticleProfile(id("v3_particles"), object("""
         {
           "emitters": {
             "custom": {
               "anchor": "core",
               "particle": "minecraft:dust",
               "state": "ONLINE",
               "rate": 0.1,
               "options": {
                 "type": "minecraft:dust",
                 "color": "#FF66E8FF",
                 "scale": 0.6,
                 "lifetime": 12,
                 "drag": 0.2
               }
             }
           }
         }
         """));

      var report = RenderCoreProfileValidator.validate(Map.of(visual.id(), visual), Map.of(), Map.of(particles.id(), particles));
      helper.assertTrue(report.issues().stream().anyMatch(issue -> "unsupported_particle_option".equals(issue.code())),
         "V3 validation should expose stable unsupported particle option codes.");
      helper.assertTrue(report.forNamespace(EchoRenderCore.MODID).warnings() == report.warnings(),
         "Validation reports should filter by namespace.");

      var partReport = RenderCoreProfileValidator.validateLayerParts(visual, Set.of("core"));
      helper.assertTrue(partReport.issues().stream().anyMatch(issue -> "masked_part_missing".equals(issue.code())),
         "V3 layer part lint should report missing named part masks.");
      helper.succeed();
   }

   private static void v4ProfileTools(GameTestHelper helper) {
      VisualProfile visual = RenderCoreJsonParsers.parseVisualProfile(id("v4_machine"), object("""
         {
           "schema_version": 4,
           "base_texture": "echorendercore:textures/block/machine.png",
           "layers": [
             {
               "id": "core_glow",
               "kind": "glow",
               "texture": "echorendercore:textures/block/machine.png",
               "states": ["ONLINE"],
               "parts": ["core"]
             }
           ],
           "block_parts": {
             "core": { "indices": [0, 3], "directions": ["north"], "ambient_occlusion": true },
             "empty": {}
           }
         }
         """));
      helper.assertTrue(visual.schemaVersion() == 4, "V4 schema version should parse.");
      helper.assertTrue(visual.blockParts().containsKey("core"), "V4 block part aliases should parse.");
      var blockReport = RenderCoreProfileValidator.validateBlockPartSelectors(visual, 2);
      helper.assertTrue(blockReport.issues().stream().anyMatch(issue -> "block_part_index_out_of_range".equals(issue.code())),
         "V4 block part validation should report out-of-range collected indices.");
      helper.assertTrue(blockReport.issues().stream().anyMatch(issue -> "block_part_selector_empty".equals(issue.code())),
         "V4 block part validation should report empty selectors.");

      VisualProfileBuilder visualBuilder = VisualProfileBuilder.create(id("built_visual"))
         .schemaVersion(4)
         .baseTexture(id("textures/block/built_machine.png"))
         .animationProfile(id("built_animation"))
         .particleProfile(id("built_particles"))
         .defaultState(VisualState.ONLINE)
         .anchor("core", new RenderCoreVector(0.0F, 0.5F, 0.0F))
         .blockPart("core", new BlockPartSelectorProfile("core", List.of(), Set.of(Direction.NORTH), 0, true, List.of()));
      for (int i = 0; i < 13; i++) {
         visualBuilder.layer(new VisualLayerProfile(
            "layer_" + i,
            VisualLayerKind.GLOW,
            id("textures/block/built_machine.png"),
            "default",
            Set.of(VisualState.ONLINE),
            Set.of(),
            List.of("core"),
            0xFFFFFFFF,
            1.0F,
            true
         ));
      }
      VisualProfile builtVisual = RenderCoreJsonParsers.parseVisualProfile(visualBuilder.id(), visualBuilder.toJson());
      helper.assertTrue(builtVisual.layers().size() == 13, "V4 visual builder JSON should round-trip through the parser.");

      AnimationProfileBuilder animationBuilder = AnimationProfileBuilder.create(id("built_animation"));
      animationBuilder.clip("spin", true, 1.0F)
         .keyframeTrack("core", AnimationChannel.ROTATION_Y, Easing.LINEAR,
            new AnimationProfileBuilder.Keyframe(0.0F, 0.0F),
            new AnimationProfileBuilder.Keyframe(1.0F, 360.0F))
         .endClip();
      AnimationProfile builtAnimation = RenderCoreJsonParsers.parseAnimationProfile(animationBuilder.id(), animationBuilder.toJson());
      helper.assertFalse(builtAnimation.clip("spin").tracks().getFirst().timeline().empty(),
         "V4 animation builder keyframes should round-trip.");

      ParticleProfileBuilder particleBuilder = ParticleProfileBuilder.create(id("built_particles"));
      particleBuilder.emitter("dust_ramp", "core", Identifier.withDefaultNamespace("dust"))
         .state(VisualState.ONLINE)
         .rate(9.0F)
         .burstCount(2)
         .optionType("dust_transition")
         .option("from_color", "#FF66E8FF")
         .option("to_color", "#FFFF7A28")
         .endEmitter();
      ParticleProfile builtParticles = RenderCoreJsonParsers.parseParticleProfile(particleBuilder.id(), particleBuilder.toJson());
      helper.assertTrue(builtParticles.emitters().get("dust_ramp").options().custom().containsKey("from_color"),
         "V4 particle option fields should parse.");

      var report = RenderCoreProfileValidator.validate(
         Map.of(builtVisual.id(), builtVisual),
         Map.of(builtAnimation.id(), builtAnimation),
         Map.of(builtParticles.id(), builtParticles)
      );
      helper.assertFalse(report.issues().stream().anyMatch(issue -> "unsupported_particle_option".equals(issue.code())),
         "V4 built-in particle option fields should not be reported as unsupported.");

      var performance = RenderCoreProfileValidator.analyzePerformance(
         Map.of(builtVisual.id(), builtVisual),
         Map.of(builtAnimation.id(), builtAnimation),
         Map.of(builtParticles.id(), builtParticles)
      );
      helper.assertTrue(performance.issues().stream().anyMatch(issue -> "profile_perf_high_layer_count".equals(issue.code())),
         "V4 performance diagnostics should flag high layer counts.");
      helper.assertTrue(performance.issues().stream().anyMatch(issue -> "profile_perf_high_emitter_rate".equals(issue.code())),
         "V4 performance diagnostics should flag high emitter rates.");
      helper.succeed();
   }

   private static void expectJsonFailure(GameTestHelper helper, Runnable runnable, String message) {
      try {
         runnable.run();
      } catch (JsonParseException | IllegalArgumentException exception) {
         return;
      }
      helper.fail(message);
   }

   private static boolean close(float left, float right) {
      return Math.abs(left - right) < 0.001F;
   }

   private static JsonObject object(String json) {
      return JsonParser.parseString(json).getAsJsonObject();
   }

   private static void assertSchemaResource(GameTestHelper helper, String fileName) {
      String resource = "assets/" + EchoRenderCore.MODID + "/rendercore/schemas/" + fileName;
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
         helper.assertTrue(stream != null, "Schema resource should exist: " + resource);
         JsonObject schema = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         helper.assertTrue(schema.has("$schema") && schema.has("type"), "Schema resource should parse as a JSON object: " + resource);
      } catch (IOException | IllegalStateException exception) {
         helper.fail("Schema resource should parse cleanly: " + resource + " " + exception.getMessage());
      }
   }

   private static void assertCreatorPackSchemaVersion(GameTestHelper helper, int expectedVersion) {
      String resource = "assets/" + EchoRenderCore.MODID + "/rendercore/schemas/creator_pack.schema.json";
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
         helper.assertTrue(stream != null, "Creator-pack schema resource should exist: " + resource);
         JsonObject schema = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         JsonObject manifest = schema.getAsJsonObject("properties").getAsJsonObject("manifest");
         int schemaVersion = manifest.getAsJsonObject("properties")
            .getAsJsonObject("schema_version")
            .get("const")
            .getAsInt();
         helper.assertTrue(schemaVersion == expectedVersion,
            "Creator-pack schema should publish the V" + expectedVersion + " export contract.");
      } catch (IOException | IllegalStateException exception) {
         helper.fail("Creator-pack schema should parse cleanly: " + resource + " " + exception.getMessage());
      }
   }

   private static VisualProfile parseExampleVisualProfile(GameTestHelper helper, String fileName) {
      String resource = "assets/" + EchoRenderCore.MODID + "/rendercore/examples/" + fileName;
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
         helper.assertTrue(stream != null, "Example profile resource should exist: " + resource);
         JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         return RenderCoreJsonParsers.parseVisualProfile(id(fileName.replace(".visual_profile.json", "")), json);
      } catch (IOException | IllegalStateException exception) {
         helper.fail("Example profile resource should parse cleanly: " + resource + " " + exception.getMessage());
         return null;
      }
   }

   private static ProfileScreenshotPreviewProvider alwaysScreenshotProvider() {
      return new ProfileScreenshotPreviewProvider() {
         @Override
         public String id() {
            return "test_screenshot";
         }

         @Override
         public boolean available(VisualProfile profile) {
            return true;
         }
      };
   }

   private static RenderCoreProfiles.LoadedContent loadedContent(VisualProfile visual, AnimationProfile animation,
         ParticleProfile particles, com.knoxhack.echorendercore.profile.ProfileDiagnosticsReport diagnostics,
         int discovered, int loaded, int failed) {
      return new RenderCoreProfiles.LoadedContent(
         Map.of(visual.id(), visual),
         Map.of(animation.id(), animation),
         Map.of(particles.id(), particles),
         diagnostics.validationReport(),
         diagnostics.performanceReport(),
         diagnostics.cacheMetrics(),
         diagnostics,
         discovered,
         loaded,
         failed
      );
   }

   private static void assertCommonPackagesClientSafe(GameTestHelper helper) {
      Path root = Path.of("src/main/java/com/knoxhack/echorendercore");
      if (!Files.exists(root)) {
         root = Path.of("addons/echorendercore/src/main/java/com/knoxhack/echorendercore");
      }
      if (!Files.exists(root)) {
         return;
      }
      Path scanRoot = root;
      try (var paths = Files.walk(root)) {
         List<Path> offenders = paths
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> {
               String relative = scanRoot.relativize(path).toString().replace('\\', '/');
               return !relative.startsWith("client/") && !relative.startsWith("test/");
            })
            .filter(path -> {
               try {
                  return Files.readString(path).contains("net.minecraft.client");
               } catch (IOException exception) {
                  return true;
               }
            })
            .toList();
         helper.assertTrue(offenders.isEmpty(), "Common RenderCore packages must not import net.minecraft.client: " + offenders);
      } catch (IOException exception) {
         helper.fail("Could not scan RenderCore source safety: " + exception.getMessage());
      }
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
         String testName, Identifier functionId) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
         environment,
         Identifier.withDefaultNamespace("empty"),
         100,
         0,
         true,
         net.minecraft.world.level.block.Rotation.NONE,
         false,
         1,
         1,
         false,
         2
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static boolean shouldRegisterTests() {
      String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
      if (namespaces == null || namespaces.isBlank()) {
         return true;
      }
      for (String namespace : namespaces.split(",")) {
         String normalized = namespace.trim();
         if (normalized.equals(EchoRenderCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
            return true;
         }
      }
      return false;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, path);
   }
}
