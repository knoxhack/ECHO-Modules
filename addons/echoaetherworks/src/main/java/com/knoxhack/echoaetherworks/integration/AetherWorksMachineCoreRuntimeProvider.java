package com.knoxhack.echoaetherworks.integration;

import com.knoxhack.echo.machinecore.EchoMachineId;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRecipeBinding;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeProvider;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AetherWorksMachineCoreRuntimeProvider implements EchoMachineRuntimeProvider {
    private static final AetherWorksMachineCoreRuntimeProvider INSTANCE = new AetherWorksMachineCoreRuntimeProvider();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int RADIUS = 32;
    private static final int Y_RADIUS = 8;
    private static final int LIMIT = 96;

    private AetherWorksMachineCoreRuntimeProvider() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoMachineRuntimeRegistry.register(INSTANCE);
            EchoAetherWorks.LOGGER.info("ECHO: AetherWorks MachineCore runtime provider registered.");
        }
    }

    @Override
    public Identifier providerId() {
        return EchoAetherWorks.id("machinecore_runtime");
    }

    @Override
    public Optional<EchoMachineRuntimeSnapshot> snapshot(Level level, BlockPos pos) {
        BlockEntity blockEntity = level == null || pos == null ? null : level.getBlockEntity(pos);
        if (blockEntity instanceof AetherStorageBlockEntity machine) {
            return Optional.of(AetherWorksMachineCoreAdapter.runtimeSnapshot(machine));
        }
        return Optional.empty();
    }

    @Override
    public List<EchoMachineRuntimeSnapshot> snapshots(Player player) {
        return machines(player).stream()
                .map(AetherWorksMachineCoreAdapter::runtimeSnapshot)
                .toList();
    }

    @Override
    public List<EchoMachineProfile> profiles(Player player) {
        Map<EchoMachineId, EchoMachineProfile> profiles = new LinkedHashMap<>();
        for (AetherStorageBlockEntity machine : machines(player)) {
            EchoMachineProfile profile = AetherWorksMachineCoreAdapter.profile(machine);
            profiles.merge(profile.id(), profile, AetherWorksMachineCoreRuntimeProvider::mergeProfiles);
        }
        return List.copyOf(profiles.values());
    }

    private static EchoMachineProfile mergeProfiles(EchoMachineProfile first, EchoMachineProfile second) {
        EchoMachineProfile representative = profileScore(second) > profileScore(first) ? second : first;
        return new EchoMachineProfile(
                representative.id(),
                representative.kind(),
                representative.defaultState(),
                representative.ownerModule(),
                representative.machineBlockReference(),
                mergeRecipeBindings(first.recipeBindings(), second.recipeBindings()),
                representative.upgradeSlots(),
                representative.maintenanceProfile(),
                representative.failureStates(),
                representative.automationHooks(),
                representative.integrationRefs(),
                representative.diagnostics(),
                representative.attributes()
        );
    }

    private static int profileScore(EchoMachineProfile profile) {
        int score = 0;
        for (EchoMachineRecipeBinding binding : profile.recipeBindings()) {
            if (binding == null) {
                continue;
            }
            score += binding.attributes().containsKey("aetherCost") ? 4 : binding.hasRecipe() ? 1 : 0;
        }
        score += profile.defaultState().degraded() ? 0 : 1;
        return score;
    }

    private static List<EchoMachineRecipeBinding> mergeRecipeBindings(List<EchoMachineRecipeBinding> first,
                                                                      List<EchoMachineRecipeBinding> second) {
        Map<String, EchoMachineRecipeBinding> bindings = new LinkedHashMap<>();
        addRecipeBindings(bindings, first);
        addRecipeBindings(bindings, second);
        return List.copyOf(bindings.values());
    }

    private static void addRecipeBindings(Map<String, EchoMachineRecipeBinding> bindings,
                                          List<EchoMachineRecipeBinding> source) {
        for (EchoMachineRecipeBinding binding : source) {
            if (binding == null) {
                continue;
            }
            bindings.putIfAbsent(recipeBindingKey(binding), binding);
        }
    }

    private static String recipeBindingKey(EchoMachineRecipeBinding binding) {
        if (binding.recipeId() != null) {
            return binding.recipeId().value();
        }
        if (binding.recipeReference() != null) {
            return binding.recipeReference().referenceId();
        }
        return binding.recipeSlot() + binding.attributes().toString();
    }

    private static List<AetherStorageBlockEntity> machines(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return List.of();
        }
        Level level = serverPlayer.level();
        BlockPos origin = serverPlayer.blockPosition();
        List<AetherStorageBlockEntity> machines = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -Y_RADIUS, -RADIUS), origin.offset(RADIUS, Y_RADIUS, RADIUS))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof AetherStorageBlockEntity machine) {
                machines.add(machine);
                if (machines.size() >= LIMIT) {
                    break;
                }
            }
        }
        return List.copyOf(machines);
    }
}
