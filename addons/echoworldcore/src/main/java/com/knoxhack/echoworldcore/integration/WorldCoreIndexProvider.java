package com.knoxhack.echoworldcore.integration;

import com.knoxhack.echocore.api.WorldHazardDefinition;
import com.knoxhack.echocore.api.WorldRegionDefinition;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum WorldCoreIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final Identifier CATEGORY_WORLD_SOURCES =
            Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, "recipe/world_sources");

    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, "provider/index_recipes");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        Player player = context == null ? null : context.player();
        List<IndexRecipeView> recipes = recipes(player);
        return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
                IndexMachineLayoutTemplates.representativeAll(recipes, "world_field_source"),
                List.of(), List.of(), List.of());
    }

    public List<IndexRecipeCategory> recipeCategories(Player player) {
        return List.of(new IndexRecipeCategory(
                CATEGORY_WORLD_SOURCES,
                "World Sources",
                new ItemStack(Items.COMPASS),
                0xFFB7F7FF,
                600));
    }

    public List<IndexRecipeView> recipes(Player player) {
        List<IndexRecipeView> views = new ArrayList<>();
        WorldRegionService service = WorldRegionService.INSTANCE;
        for (WorldRegionDefinition region : service.regionDefinitions()) {
            views.add(regionView(region, service));
        }
        for (WorldHazardDefinition hazard : service.hazardDefinitions()) {
            views.add(hazardView(hazard));
        }
        return List.copyOf(views);
    }

    private static IndexRecipeView regionView(WorldRegionDefinition region, WorldRegionService service) {
        ItemStack machine = new ItemStack(Items.COMPASS);
        List<IndexRecipeSlot> slots = new ArrayList<>();
        for (Identifier biome : region.biomeIds()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Biome: " + biome));
        }
        for (Identifier tag : region.biomeTags()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Biome tag: #" + tag));
        }
        for (Identifier structure : region.structureIds()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Structure: " + structure));
        }
        for (Identifier hazard : region.hazardIds()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.INFO, List.of(), "Hazard: " + hazardTitle(service, hazard)));
        }
        slots.add(IndexRecipeSlot.machine(machine));
        slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "World source: " + region.displayName()));

        List<String> notes = new ArrayList<>();
        notes.add(region.summary());
        notes.add("Type: " + region.type().displayName());
        notes.add("Discovery: " + region.discoveryId());
        notes.add("Radius: " + region.radius());
        if (region.renderProfileId() != null) {
            notes.add("Render profile: " + region.renderProfileId());
        }
        if (region.audioProfileId() != null) {
            notes.add("Audio profile: " + region.audioProfileId());
        }
        if (!region.hazardIds().isEmpty()) {
            notes.add("Hazards: " + joinIds(region.hazardIds()));
        }
        return new IndexRecipeView(
                Identifier.fromNamespaceAndPath(EchoWorldCore.MODID,
                        "recipe/world_source/" + sanitize(region.id().getNamespace()) + "/" + sanitize(region.id().getPath())),
                CATEGORY_WORLD_SOURCES,
                region.displayName(),
                machine,
                slots,
                notes,
                0,
                false,
                region.id().getNamespace());
    }

    private static IndexRecipeView hazardView(WorldHazardDefinition hazard) {
        ItemStack machine = new ItemStack(Items.COMPASS);
        List<IndexRecipeSlot> slots = List.of(
                IndexRecipeSlot.machine(machine),
                new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), "Region hazard source"),
                new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Hazard: " + hazard.displayName()));
        return new IndexRecipeView(
                Identifier.fromNamespaceAndPath(EchoWorldCore.MODID,
                        "recipe/hazard/" + sanitize(hazard.id().getNamespace()) + "/" + sanitize(hazard.id().getPath())),
                CATEGORY_WORLD_SOURCES,
                hazard.displayName(),
                machine,
                slots,
                List.of(
                        hazard.summary(),
                        "Severity: " + hazard.defaultSeverity(),
                        "Ticking: " + (hazard.ticking() ? "yes" : "no")),
                0,
                false,
                hazard.id().getNamespace());
    }

    private static String hazardTitle(WorldRegionService service, Identifier hazardId) {
        return service.hazardDefinition(hazardId)
                .map(WorldHazardDefinition::displayName)
                .orElse(hazardId.toString());
    }

    private static String joinIds(List<Identifier> ids) {
        return ids.stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String sanitize(String path) {
        String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
