package com.knoxhack.echoindex.integration;

import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRecipeBinding;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSourceFact;
import com.knoxhack.echocore.api.index.IndexSourceKind;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class MachineCoreIndexIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private MachineCoreIndexIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoCoreServices.registerIndexContentProvider(Provider.INSTANCE);
        }
    }

    private enum Provider implements IIndexContentProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return EchoIndex.id("provider/machinecore_runtime");
        }

        @Override
        public IndexContentSnapshot snapshot(IndexBuildContext context) {
            List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(context == null ? null : context.player());
            Map<Identifier, IndexRecipeCategory> categories = new LinkedHashMap<>();
            List<IndexEntry> entries = new ArrayList<>();
            List<IndexRecipeView> recipes = new ArrayList<>();
            List<IndexSourceFact> sourceFacts = new ArrayList<>();
            for (EchoMachineProfile profile : profiles) {
                Identifier categoryId = recipeCategoryId(profile);
                categories.putIfAbsent(categoryId, new IndexRecipeCategory(
                        categoryId,
                        title(profile.kind().name()),
                        machineStack(profile),
                        0xFF66E8FF,
                        520 + profile.kind().ordinal()));
                entries.add(entry(profile));
                sourceFacts.add(sourceFact(profile));
                recipes.addAll(recipeViews(profile, categoryId));
            }
            return new IndexContentSnapshot(id(), List.of(), entries, List.copyOf(categories.values()), recipes,
                    List.of(), sourceFacts, List.of(), List.of());
        }

        private static IndexEntry entry(EchoMachineProfile profile) {
            Identifier entryId = EchoIndex.id("machinecore/" + EchoMachineUiBridge.sanitizePath(profile.id().value()));
            return new IndexEntry(
                    entryId,
                    IndexIds.CATEGORY_MACHINES,
                    title(profile.id().value()),
                    "MachineCore runtime profile",
                    profile.defaultState().serializedName().replace('_', ' '),
                    "Published by " + profile.ownerModule().value() + " through MachineCore.",
                    machineStack(profile),
                    profile.ownerModule().value(),
                    List.of("machinecore", "machine", profile.kind().name().toLowerCase(java.util.Locale.ROOT)),
                    profile.degradedByDefault() ? IndexEntryState.LOCKED : IndexEntryState.DISCOVERED,
                    List.of(),
                    List.of(blockId(profile)),
                    profile.recipeBindings().stream()
                            .map(binding -> EchoMachineUiBridge.recipeIdentifier(binding, profile.id()))
                            .toList(),
                    520);
        }

        private static List<IndexRecipeView> recipeViews(EchoMachineProfile profile, Identifier categoryId) {
            List<EchoMachineRecipeBinding> bindings = profile.recipeBindings().isEmpty()
                    ? List.of(new EchoMachineRecipeBinding(null, null, "runtime", 0, List.of(), Map.of()))
                    : profile.recipeBindings();
            List<IndexRecipeView> views = new ArrayList<>();
            int index = 0;
            for (EchoMachineRecipeBinding binding : bindings) {
                Identifier recipeId = EchoIndex.id("machinecore/" + EchoMachineUiBridge.sanitizePath(profile.id().value())
                        + "/" + index++ + "/" + EchoMachineUiBridge.sanitizePath(
                                EchoMachineUiBridge.recipeIdentifier(binding, profile.id()).toString()));
                List<IndexRecipeSlot> slots = new ArrayList<>();
                slots.add(IndexRecipeSlot.machine(machineStack(profile)));
                if (binding != null && !binding.requiredUpgrades().isEmpty()) {
                    slots.add(IndexRecipeSlot.info("Requires " + String.join(", ", binding.requiredUpgrades())));
                }
                slots.add(IndexRecipeSlot.info("Slot: " + (binding == null ? "runtime" : binding.recipeSlot())));
                List<String> notes = List.of(
                        "MachineCore owner: " + profile.ownerModule().value(),
                        "Automation hooks: " + profile.automationHooks().size(),
                        "Optional integrations: " + profile.integrationRefs().optionalFeatures().size());
                views.add(new IndexRecipeView(recipeId, categoryId, title(profile.id().value()), machineStack(profile),
                        slots, notes, 0, false, profile.ownerModule().value()));
            }
            return List.copyOf(views);
        }

        private static IndexSourceFact sourceFact(EchoMachineProfile profile) {
            return new IndexSourceFact(
                    blockId(profile),
                    EchoIndex.id("source/machinecore/" + EchoMachineUiBridge.sanitizePath(profile.id().value())),
                    IndexSourceKind.MACHINE,
                    title(profile.id().value()),
                    List.of("Machine runtime/profile published by " + profile.ownerModule().value() + "."),
                    machineStack(profile),
                    profile.ownerModule().value());
        }

        private static Identifier recipeCategoryId(EchoMachineProfile profile) {
            return EchoIndex.id("recipe/machinecore/" + EchoMachineUiBridge.sanitizePath(profile.kind().name()));
        }

        private static Identifier blockId(EchoMachineProfile profile) {
            Identifier id = Identifier.tryParse(profile.id().value());
            return id == null ? Identifier.withDefaultNamespace("air") : id;
        }

        private static ItemStack machineStack(EchoMachineProfile profile) {
            Identifier id = blockId(profile);
            return BuiltInRegistries.BLOCK.getOptional(id)
                    .map(Block::asItem)
                    .filter(item -> item != Items.AIR)
                    .map(ItemStack::new)
                    .orElseGet(() -> new ItemStack(Items.CRAFTER));
        }

        private static String title(String value) {
            String path = value == null ? "machine" : value;
            int separator = path.indexOf(':');
            if (separator >= 0) {
                path = path.substring(separator + 1);
            }
            path = path.replace('_', ' ').replace('/', ' ').trim();
            if (path.isBlank()) {
                return "Machine";
            }
            return Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
    }
}
