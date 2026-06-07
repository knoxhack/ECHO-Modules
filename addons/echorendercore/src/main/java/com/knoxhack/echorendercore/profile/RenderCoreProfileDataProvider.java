package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class RenderCoreProfileDataProvider implements DataProvider {
   private final PackOutput.PathProvider visualProfiles;
   private final PackOutput.PathProvider animationProfiles;
   private final PackOutput.PathProvider particleProfiles;
   private final PackOutput.PathProvider creatorArtifacts;
   private final Map<Identifier, JsonObject> visuals = new LinkedHashMap<>();
   private final Map<Identifier, JsonObject> animations = new LinkedHashMap<>();
   private final Map<Identifier, JsonObject> particles = new LinkedHashMap<>();
   private final String name;

   protected RenderCoreProfileDataProvider(PackOutput output, String name) {
      this.name = name == null || name.isBlank() ? "RenderCore Profiles" : name;
      this.visualProfiles = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "rendercore/visual_profiles");
      this.animationProfiles = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "rendercore/animations");
      this.particleProfiles = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "rendercore/particles");
      this.creatorArtifacts = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "rendercore/creator");
   }

   @Override
   public final CompletableFuture<?> run(CachedOutput output) {
      visuals.clear();
      animations.clear();
      particles.clear();
      registerProfiles();
      java.util.ArrayList<CompletableFuture<?>> futures = new java.util.ArrayList<>();
      visuals.forEach((id, json) -> futures.add(DataProvider.saveStable(output, json, visualProfiles.json(id))));
      animations.forEach((id, json) -> futures.add(DataProvider.saveStable(output, json, animationProfiles.json(id))));
      particles.forEach((id, json) -> futures.add(DataProvider.saveStable(output, json, particleProfiles.json(id))));
      if (generateCreatorPackArtifacts()) {
         CreatorExportIndex export = RenderCoreCreatorPackExporter.export(parseVisuals(), parseAnimations(), parseParticles(), null,
            ProfileScreenshotPreviewProvider.NO_OP);
         export.artifacts()
            .forEach(artifact -> futures.add(DataProvider.saveStable(output, artifact.json(), creatorArtifacts.json(creatorArtifactId(artifact.id())))));
         if (generateCreatorPackIndex()) {
            namespaces(export).forEach(namespace -> {
               CreatorExportIndex index = export.forNamespace(namespace);
               futures.add(DataProvider.saveStable(output, index.toJson(), creatorArtifacts.json(Identifier.fromNamespaceAndPath(namespace, "index.creator"))));
            });
         }
      }
      return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
   }

   @Override
   public String getName() {
      return name;
   }

   protected abstract void registerProfiles();

   protected boolean generateCreatorPackArtifacts() {
      return true;
   }

   protected boolean generateCreatorPackIndex() {
      return true;
   }

   @Deprecated
   protected boolean generatePreviewArtifacts() {
      return generateCreatorPackArtifacts();
   }

   @Deprecated
   protected boolean generatePreviewIndex() {
      return generateCreatorPackIndex();
   }

   protected void visual(VisualProfileBuilder builder) {
      visuals.put(builder.id(), builder.toJson());
   }

   protected void animation(AnimationProfileBuilder builder) {
      animations.put(builder.id(), builder.toJson());
   }

   protected void particle(ParticleProfileBuilder builder) {
      particles.put(builder.id(), builder.toJson());
   }

   private Map<Identifier, VisualProfile> parseVisuals() {
      Map<Identifier, VisualProfile> values = new LinkedHashMap<>();
      visuals.forEach((id, json) -> values.put(id, RenderCoreJsonParsers.parseVisualProfile(id, json)));
      return RenderCoreProfileComposer.composeAll(values).profiles();
   }

   private Map<Identifier, AnimationProfile> parseAnimations() {
      Map<Identifier, AnimationProfile> values = new LinkedHashMap<>();
      animations.forEach((id, json) -> values.put(id, RenderCoreJsonParsers.parseAnimationProfile(id, json)));
      return values;
   }

   private Map<Identifier, ParticleProfile> parseParticles() {
      Map<Identifier, ParticleProfile> values = new LinkedHashMap<>();
      particles.forEach((id, json) -> values.put(id, RenderCoreJsonParsers.parseParticleProfile(id, json)));
      return values;
   }

   private static Identifier creatorArtifactId(Identifier id) {
      return Identifier.fromNamespaceAndPath(id.getNamespace(), "profiles/" + id.getPath() + ".creator");
   }

   private static java.util.Set<String> namespaces(CreatorExportIndex export) {
      java.util.TreeSet<String> values = new java.util.TreeSet<>();
      export.cards().forEach(card -> values.add(card.profileId().getNamespace()));
      return values;
   }
}
