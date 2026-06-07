package com.knoxhack.echoterminal.integration;

import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRecipeBinding;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class MachineCoreTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private MachineCoreTerminalIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            TerminalRecipeRegistry.register(Provider.INSTANCE);
        }
    }

    private enum Provider implements TerminalRecipeProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return id("machinecore_recipes");
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            Map<Identifier, TerminalRecipeCategory> categories = new LinkedHashMap<>();
            for (EchoMachineProfile profile : EchoMachineRuntimeRegistry.profiles(player)) {
                Identifier id = categoryId(profile);
                categories.putIfAbsent(id, new TerminalRecipeCategory(
                        id,
                        title(profile.kind().name()),
                        machineStack(profile),
                        0xFF66E8FF,
                        520 + profile.kind().ordinal()));
            }
            return List.copyOf(categories.values());
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            List<TerminalRecipeEntry> entries = new ArrayList<>();
            for (EchoMachineProfile profile : EchoMachineRuntimeRegistry.profiles(player)) {
                List<EchoMachineRecipeBinding> bindings = profile.recipeBindings().isEmpty()
                        ? List.of(new EchoMachineRecipeBinding(null, null, "runtime", 0, List.of(), Map.of()))
                        : profile.recipeBindings();
                int index = 0;
                for (EchoMachineRecipeBinding binding : bindings) {
                    entries.add(entry(profile, binding, index++));
                }
            }
            return List.copyOf(entries);
        }

        private static TerminalRecipeEntry entry(EchoMachineProfile profile, EchoMachineRecipeBinding binding, int index) {
            Identifier recipeId = EchoMachineUiBridge.recipeIdentifier(binding, profile.id());
            List<TerminalRecipeSlot> slots = new ArrayList<>();
            slots.add(TerminalRecipeSlot.machine(machineStack(profile)));
            if (binding != null && !binding.requiredUpgrades().isEmpty()) {
                slots.add(TerminalRecipeSlot.text(TerminalRecipeSlot.Role.CATALYST,
                        "Requires " + String.join(", ", binding.requiredUpgrades())));
            }
            slots.add(TerminalRecipeSlot.text("Machine: " + profile.id().value()));

            List<TerminalRecipeNote> notes = new ArrayList<>();
            notes.add(TerminalRecipeNote.info("Source module: " + profile.ownerModule().value()));
            notes.add(TerminalRecipeNote.info("Recipe slot: " + (binding == null ? "runtime" : binding.recipeSlot())));
            notes.add(TerminalRecipeNote.info("Automation hooks: " + profile.automationHooks().size()));
            if (profile.maintenanceProfile() != null && profile.maintenanceProfile().supportsFieldRepair()) {
                notes.add(TerminalRecipeNote.info("Maintenance: repairable"));
            }

            return new TerminalRecipeEntry(
                    id("machinecore/" + EchoMachineUiBridge.sanitizePath(profile.id().value()) + "/" + index
                            + "/" + EchoMachineUiBridge.sanitizePath(recipeId.toString())),
                    categoryId(profile),
                    title(profile.id().value()),
                    machineStack(profile),
                    slots,
                    notes,
                    0,
                    false);
        }

        private static Identifier categoryId(EchoMachineProfile profile) {
            return id("machinecore/" + EchoMachineUiBridge.sanitizePath(profile.kind().name()));
        }

        private static ItemStack machineStack(EchoMachineProfile profile) {
            Identifier id = Identifier.tryParse(profile.id().value());
            if (id == null) {
                return new ItemStack(Items.CRAFTER);
            }
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

        private static Identifier id(String path) {
            return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
        }
    }
}
