package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.RenderCoreConfig;
import com.knoxhack.echorendercore.profile.VisualEffectBloomMaskMode;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;
import com.knoxhack.echorendercore.profile.VisualEffectTargetScope;
import com.knoxhack.echorendercore.profile.VisualBudgetProfile;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Optional V12 advanced FX bridge. Stable emissive rendering remains the source of truth;
 * this class only adds isolated bloom mask submissions when client hooks and resources allow it.
 */
public final class RenderCoreAdvancedFxPipeline {
   private static final Identifier ISOLATED_BLOOM = Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, "isolated_bloom");
   private static final Identifier FULLSCREEN_BLOOM = Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, "advanced_bloom");
   private static final Identifier BLOOM_MASK_TARGET_ID = Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, "bloom_mask");
   private static volatile TextureTarget bloomMaskTarget;
   private static final OutputTarget BLOOM_MASK_OUTPUT = new OutputTarget("rendercore_bloom_mask", RenderCoreAdvancedFxPipeline::bloomMaskTarget);
   private static final Object TARGET_LOCK = new Object();
   private static final Object RENDER_TYPE_LOCK = new Object();
   private static final Map<Identifier, RenderType> MASK_RENDER_TYPES = new HashMap<>();
   private static final Set<String> submittedChannels = ConcurrentHashMap.newKeySet();
   private static final AtomicInteger submittedEffects = new AtomicInteger();
   private static final AtomicInteger submittedMaskSubmissions = new AtomicInteger();
   private static final AtomicInteger submittedPasses = new AtomicInteger();
   private static final AtomicInteger submittedBloomCost = new AtomicInteger();
   private static final AtomicInteger skippedSubmissions = new AtomicInteger();
   private static final AtomicInteger submittedDownscale = new AtomicInteger();
   private static volatile int bloomMaskWidth;
   private static volatile int bloomMaskHeight;
   private static volatile boolean maskClearedThisFrame;
   private static volatile boolean hookSeen;
   private static volatile boolean chainAvailable;
   private static volatile boolean compileFailed;
   private static volatile String unavailableReason = "framegraph hook not seen";
   private static volatile String fallbackReason = "";
   private static volatile String statusLine = "effects stable";
   private static volatile int lastEffectCount;
   private static volatile int lastMaskSubmissionCount;
   private static volatile int lastSkippedSubmissions;
   private static volatile int lastPassCount;
   private static volatile int lastBloomCost;
   private static volatile int lastChannelCount;
   private static volatile int lastDownscale;

   private RenderCoreAdvancedFxPipeline() {
   }

   private static TextureTarget bloomMaskTarget() {
      return bloomMaskTarget;
   }

   public static void resetStatus() {
      submittedEffects.set(0);
      submittedMaskSubmissions.set(0);
      submittedPasses.set(0);
      submittedBloomCost.set(0);
      skippedSubmissions.set(0);
      submittedDownscale.set(0);
      submittedChannels.clear();
      chainAvailable = false;
      compileFailed = false;
      unavailableReason = RenderCoreEffectPipeline.advancedFxEnabled() ? "framegraph hook not seen" : "advanced FX disabled";
      fallbackReason = "";
      statusLine = RenderCoreEffectPipeline.advancedFxEnabled() ? "effects advanced unavailable" : "effects stable";
      lastEffectCount = 0;
      lastMaskSubmissionCount = 0;
      lastSkippedSubmissions = 0;
      lastPassCount = 0;
      lastBloomCost = 0;
      lastChannelCount = 0;
      lastDownscale = RenderCoreConfig.bloomDownscale();
      maskClearedThisFrame = false;
   }

   public static void submit(VisualEffectProfile effect, VisualEffectTargetScope runtimeScope) {
      submit(effect, runtimeScope, null, 0xFFFFFFFF, 1.0F);
   }

   public static MaskSubmission submit(VisualEffectProfile effect, VisualEffectTargetScope runtimeScope, Identifier texture, int color, float layerAlpha) {
      return submit(effect, runtimeScope, texture, color, layerAlpha, VisualBudgetProfile.DEFAULT);
   }

   public static MaskSubmission submit(VisualEffectProfile effect, VisualEffectTargetScope runtimeScope, Identifier texture, int color, float layerAlpha,
         VisualBudgetProfile budget) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      if (!eligible(resolved, runtimeScope)) {
         return null;
      }
      String channel = resolved.effectiveBloomChannel();
      int passes = resolved.advancedPassCount();
      int downscale = resolved.effectiveBloomDownscale(RenderCoreConfig.bloomDownscale());
      if (!acceptBudget(channel, passes, resolved.effectiveAdvancedPriority(), budget)) {
         skippedSubmissions.incrementAndGet();
         return null;
      }
      submittedEffects.incrementAndGet();
      submittedPasses.addAndGet(passes);
      submittedBloomCost.addAndGet(resolved.bloomCost());
      submittedChannels.add(channel);
      submittedDownscale.compareAndSet(0, downscale);
      RenderCoreConfig.AdvancedFxMode mode = RenderCoreConfig.advancedFxMode();
      if (mode != RenderCoreConfig.AdvancedFxMode.ISOLATED || texture == null
         || resolved.bloomMaskMode() == VisualEffectBloomMaskMode.NONE
         || resolved.bloomMaskMode() == VisualEffectBloomMaskMode.UNSUPPORTED) {
         return null;
      }
      if (!ensureMaskTarget(downscale)) {
         skippedSubmissions.incrementAndGet();
         return null;
      }
      RenderType renderType = maskRenderType(texture);
      if (renderType == null) {
         skippedSubmissions.incrementAndGet();
         return null;
      }
      submittedMaskSubmissions.incrementAndGet();
      float alpha = resolved.effectiveBloomMaskAlpha(layerAlpha);
      int maskColor = withAlpha(resolved.effectiveBloomTint(color), alpha);
      return new MaskSubmission(renderType, maskColor, downscale, channel, resolved.effectiveAdvancedPriority());
   }

   public static void onFrameGraphSetup(Object event) {
      hookSeen = true;
      Snapshot snapshot = snapshotAndReset();
      if (!RenderCoreEffectPipeline.advancedFxEnabled()) {
         chainAvailable = false;
         statusLine = "effects stable";
         unavailableReason = "advanced FX disabled by " + RenderCoreEffectPipeline.advancedFxSource();
         fallbackReason = "";
         return;
      }
      RenderCoreConfig.AdvancedFxMode mode = RenderCoreConfig.advancedFxMode();
      if (mode == RenderCoreConfig.AdvancedFxMode.STABLE) {
         chainAvailable = false;
         statusLine = "effects stable fallback";
         unavailableReason = "advanced FX mode is stable";
         fallbackReason = "config mode stable";
         return;
      }
      if (snapshot.effectCount <= 0) {
         chainAvailable = false;
         statusLine = "effects stable fallback";
         unavailableReason = "no bloom-capable submissions";
         fallbackReason = "";
         return;
      }
      if (mode == RenderCoreConfig.AdvancedFxMode.ISOLATED && snapshot.maskSubmissions > 0 && bloomMaskTarget != null) {
         if (tryIsolated(event, snapshot)) {
            return;
         }
      } else if (mode == RenderCoreConfig.AdvancedFxMode.ISOLATED) {
         fallbackReason = snapshot.maskSubmissions <= 0 ? "no isolated mask submissions accepted" : "bloom mask target unavailable";
      }
      if (RenderCoreConfig.fallbackToStable() && mode != RenderCoreConfig.AdvancedFxMode.STABLE && tryFullscreen(event, snapshot)) {
         return;
      }
      chainAvailable = false;
      statusLine = RenderCoreConfig.fallbackToStable() ? "effects stable fallback" : "effects advanced unavailable";
      if (unavailableReason.isBlank()) {
         unavailableReason = fallbackReason.isBlank() ? "advanced FX unavailable" : fallbackReason;
      }
   }

   public static boolean available() {
      return RenderCoreEffectPipeline.advancedFxEnabled() && chainAvailable;
   }

   public static boolean compileFailed() {
      return compileFailed;
   }

   public static int lastEffectCount() {
      return lastEffectCount;
   }

   public static int lastMaskSubmissionCount() {
      return lastMaskSubmissionCount;
   }

   public static int lastSkippedSubmissions() {
      return lastSkippedSubmissions;
   }

   public static int lastPassCount() {
      return lastPassCount;
   }

   public static int lastBloomCost() {
      return lastBloomCost;
   }

   public static int lastChannelCount() {
      return lastChannelCount;
   }

   public static int lastDownscale() {
      return lastDownscale == 0 ? RenderCoreConfig.bloomDownscale() : lastDownscale;
   }

   public static String unavailableReason() {
      return unavailableReason;
   }

   public static String fallbackReason() {
      return fallbackReason;
   }

   public static String modeLine() {
      return RenderCoreConfig.advancedFxMode().name().toLowerCase(Locale.ROOT) + " / " + RenderCoreEffectPipeline.advancedFxSource();
   }

   public static String statusLine() {
      if (!RenderCoreEffectPipeline.advancedFxEnabled()) {
         return "effects stable";
      }
      if (!hookSeen && !chainAvailable) {
         return "effects advanced unavailable";
      }
      return statusLine;
   }

   private static boolean eligible(VisualEffectProfile effect, VisualEffectTargetScope runtimeScope) {
      if (!RenderCoreEffectPipeline.advancedFxEnabled()) {
         return false;
      }
      if (!effect.advancedEnabled() || !effect.bloomCapable()) {
         return false;
      }
      if (RenderCoreConfig.advancedFxMode() == RenderCoreConfig.AdvancedFxMode.STABLE) {
         return false;
      }
      return matchesScope(effect.targetScope(), runtimeScope);
   }

   private static boolean acceptBudget(String channel, int passCount, int priority, VisualBudgetProfile budget) {
      VisualBudgetProfile profileBudget = budget == null ? VisualBudgetProfile.DEFAULT : budget;
      if (profileBudget.advancedFxDenied()) {
         fallbackReason = "profile_budget_denies_advanced_fx";
         return false;
      }
      int maxChannels = RenderCoreConfig.maxBloomChannels();
      if (maxChannels <= 0 || (!submittedChannels.contains(channel) && submittedChannels.size() >= maxChannels)) {
         fallbackReason = "advanced_effect_channel_limit";
         return false;
      }
      int maxSubmissions = Math.min(RenderCoreConfig.maxBloomSubmissions(), profileBudget.effectiveMaxMaskSubmissions());
      if (maxSubmissions <= 0 || submittedEffects.get() >= maxSubmissions) {
         fallbackReason = "advanced_effect_budget_exceeded";
         return false;
      }
      int maxPasses = RenderCoreConfig.maxBloomPasses();
      if (maxPasses <= 0 || submittedPasses.get() + passCount > maxPasses) {
         fallbackReason = "advanced_effect_budget_exceeded";
         return false;
      }
      if (submittedEffects.get() >= maxSubmissions - 1 && priority < 0) {
         fallbackReason = "advanced_effect_budget_exceeded";
         return false;
      }
      return true;
   }

   private static Snapshot snapshotAndReset() {
      lastEffectCount = submittedEffects.getAndSet(0);
      lastMaskSubmissionCount = submittedMaskSubmissions.getAndSet(0);
      lastSkippedSubmissions = skippedSubmissions.getAndSet(0);
      lastPassCount = submittedPasses.getAndSet(0);
      lastBloomCost = submittedBloomCost.getAndSet(0);
      lastChannelCount = submittedChannels.size();
      lastDownscale = submittedDownscale.getAndSet(0);
      submittedChannels.clear();
      maskClearedThisFrame = false;
      if (lastDownscale == 0) {
         lastDownscale = RenderCoreConfig.bloomDownscale();
      }
      return new Snapshot(lastEffectCount, lastMaskSubmissionCount, lastSkippedSubmissions, lastPassCount, lastBloomCost, lastChannelCount, lastDownscale);
   }

   private static FrameGraphBuilder frameGraphBuilder(Object event) {
      Object builder = invoke(event, "getFrameGrapBuilder");
      return builder instanceof FrameGraphBuilder typed ? typed : null;
   }

   private static Object invoke(Object target, String method) {
      if (target == null || method == null || method.isBlank()) {
         return null;
      }
      try {
         Method accessor = target.getClass().getMethod(method);
         return accessor.invoke(target);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return null;
      }
   }

   private static int intValue(Object target, String method) {
      Object value = invoke(target, method);
      return value instanceof Number number ? number.intValue() : 0;
   }

   private static boolean tryIsolated(Object event, Snapshot snapshot) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getShaderManager() == null) {
         recordUnavailable("shader manager unavailable", null);
         fallbackReason = "shader manager unavailable";
         return false;
      }
      try {
         HashSet<Identifier> targets = new HashSet<>(LevelTargetBundle.MAIN_TARGETS);
         targets.add(BLOOM_MASK_TARGET_ID);
         PostChain chain = minecraft.getShaderManager().getPostChain(ISOLATED_BLOOM, targets);
         if (chain == null) {
            recordUnavailable("isolated_bloom post-chain unavailable", null);
            fallbackReason = "isolated_bloom post-chain unavailable";
            return false;
         }
         FrameGraphBuilder builder = frameGraphBuilder(event);
         Object descriptor = invoke(event, "getRenderTargetDescriptor");
         Object targetBundle = invoke(event, "getTargetBundle");
         if (builder == null || descriptor == null || targetBundle == null) {
            recordUnavailable("framegraph context incomplete", null);
            return false;
         }
         ResourceHandle<RenderTarget> maskHandle = builder.importExternal("rendercore_bloom_mask", bloomMaskTarget);
         chain.addToFrame(builder, intValue(descriptor, "width"), intValue(descriptor, "height"),
            new BloomMaskTargetBundle((LevelTargetBundle) targetBundle, maskHandle));
         chainAvailable = true;
         compileFailed = false;
         unavailableReason = "";
         fallbackReason = "";
         statusLine = "effects advanced isolated";
         return true;
      } catch (RuntimeException exception) {
         recordUnavailable("advanced_effect_compile_failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(), exception);
         fallbackReason = "isolated mask post-chain failed";
         return false;
      }
   }

   private static boolean tryFullscreen(Object event, Snapshot snapshot) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getShaderManager() == null) {
         recordUnavailable("shader manager unavailable", null);
         return false;
      }
      try {
         PostChain chain = minecraft.getShaderManager().getPostChain(FULLSCREEN_BLOOM, LevelTargetBundle.MAIN_TARGETS);
         if (chain == null) {
            recordUnavailable("advanced_bloom fullscreen fallback unavailable", null);
            return false;
         }
         FrameGraphBuilder builder = frameGraphBuilder(event);
         Object descriptor = invoke(event, "getRenderTargetDescriptor");
         Object targetBundle = invoke(event, "getTargetBundle");
         if (builder == null || descriptor == null || targetBundle == null) {
            recordUnavailable("framegraph context incomplete", null);
            return false;
         }
         chain.addToFrame(builder, intValue(descriptor, "width"), intValue(descriptor, "height"),
            (LevelTargetBundle) targetBundle);
         chainAvailable = true;
         compileFailed = false;
         unavailableReason = "";
         if (fallbackReason.isBlank()) {
            fallbackReason = RenderCoreConfig.advancedFxMode() == RenderCoreConfig.AdvancedFxMode.FULLSCREEN
               ? "fullscreen fallback mode configured"
               : "isolated mode unavailable";
         }
         statusLine = "effects advanced fullscreen fallback";
         return true;
      } catch (RuntimeException exception) {
         recordUnavailable("advanced_effect_compile_failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(), exception);
         if (fallbackReason.isBlank()) {
            fallbackReason = "fullscreen fallback post-chain failed";
         }
         return false;
      }
   }

   private static boolean ensureMaskTarget(int downscale) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getMainRenderTarget() == null) {
         recordUnavailable("advanced_effect_mask_unavailable: main render target unavailable", null);
         return false;
      }
      RenderTarget main = minecraft.getMainRenderTarget();
      int width = Math.max(1, main.width / Math.max(1, downscale));
      int height = Math.max(1, main.height / Math.max(1, downscale));
      synchronized (TARGET_LOCK) {
         try {
            if (bloomMaskTarget == null) {
               bloomMaskTarget = new TextureTarget("rendercore:bloom_mask", width, height, false);
               bloomMaskWidth = width;
               bloomMaskHeight = height;
               maskClearedThisFrame = false;
            } else if (bloomMaskWidth != width || bloomMaskHeight != height) {
               bloomMaskTarget.resize(width, height);
               bloomMaskWidth = width;
               bloomMaskHeight = height;
               maskClearedThisFrame = false;
            }
            clearMaskTarget();
            return true;
         } catch (RuntimeException exception) {
            recordUnavailable("advanced_effect_mask_unavailable: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(), exception);
            return false;
         }
      }
   }

   private static void clearMaskTarget() {
      if (maskClearedThisFrame || bloomMaskTarget == null || bloomMaskTarget.getColorTexture() == null || RenderSystem.tryGetDevice() == null) {
         return;
      }
      RenderSystem.getDevice().createCommandEncoder().clearColorTexture(bloomMaskTarget.getColorTexture(), 0x00000000);
      maskClearedThisFrame = true;
   }

   private static RenderType maskRenderType(Identifier texture) {
      synchronized (RENDER_TYPE_LOCK) {
         RenderType cached = MASK_RENDER_TYPES.get(texture);
         if (cached != null) {
            return cached;
         }
         try {
            RenderSetup setup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
               .withTexture("Sampler0", texture)
               .useLightmap()
               .useOverlay()
               .setOutputTarget(BLOOM_MASK_OUTPUT)
               .createRenderSetup();
            Method create = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
            create.setAccessible(true);
            RenderType renderType = (RenderType)create.invoke(null, "rendercore_bloom_mask_" + safeName(texture), setup);
            MASK_RENDER_TYPES.put(texture, renderType);
            return renderType;
         } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            recordUnavailable("advanced_effect_mask_unavailable: mask render type unavailable: " + exception.getClass().getSimpleName(), exception);
            return null;
         }
      }
   }

   private static String safeName(Identifier id) {
      return id.toString().replace(':', '_').replace('/', '_');
   }

   private static int withAlpha(int color, float alpha) {
      int rgb = color & 0x00FFFFFF;
      int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      return (a << 24) | rgb;
   }

   private static boolean matchesScope(VisualEffectTargetScope configured, VisualEffectTargetScope runtime) {
      VisualEffectTargetScope scope = configured == null ? VisualEffectTargetScope.PROFILE : configured;
      VisualEffectTargetScope actual = runtime == null ? VisualEffectTargetScope.PROFILE : runtime;
      return scope == VisualEffectTargetScope.PROFILE
         || scope == VisualEffectTargetScope.GLOBAL
         || scope == actual;
   }

   private static void recordUnavailable(String reason, Throwable throwable) {
      chainAvailable = false;
      unavailableReason = reason == null || reason.isBlank() ? "advanced FX unavailable" : reason;
      compileFailed = unavailableReason.contains("compile_failed") || unavailableReason.contains("post-chain failed");
      statusLine = RenderCoreConfig.fallbackToStable() ? "effects stable fallback" : "effects advanced unavailable";
      if (RenderCoreConfig.logAdvancedFxFailures()) {
         if (throwable == null) {
            EchoRenderCore.LOGGER.warn("RenderCore advanced FX fallback: {}", unavailableReason);
         } else {
            EchoRenderCore.LOGGER.warn("RenderCore advanced FX fallback: {}", unavailableReason, throwable);
         }
      }
   }

   public record MaskSubmission(RenderType renderType, int color, int downscale, String channel, int priority) {
   }

   private record Snapshot(int effectCount, int maskSubmissions, int skippedSubmissions, int passCount, int bloomCost, int channelCount, int downscale) {
   }

   private static final class BloomMaskTargetBundle implements PostChain.TargetBundle {
      private final PostChain.TargetBundle delegate;
      private ResourceHandle<RenderTarget> maskHandle;

      private BloomMaskTargetBundle(PostChain.TargetBundle delegate, ResourceHandle<RenderTarget> maskHandle) {
         this.delegate = delegate;
         this.maskHandle = maskHandle;
      }

      @Override
      public void replace(Identifier id, ResourceHandle<RenderTarget> target) {
         if (BLOOM_MASK_TARGET_ID.equals(id)) {
            maskHandle = target;
         } else {
            delegate.replace(id, target);
         }
      }

      @Override
      public ResourceHandle<RenderTarget> get(Identifier id) {
         return BLOOM_MASK_TARGET_ID.equals(id) ? maskHandle : delegate.get(id);
      }
   }
}
