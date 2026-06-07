package com.knoxhack.echoorbitalremnants.block;

import com.mojang.serialization.Codec;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class OrbitalMachineBlock extends Block implements EntityBlock {
    private final MachineKind kind;

    public OrbitalMachineBlock(MachineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public MachineKind kind() {
        return kind;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new OrbitalMachineBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return type == ModBlockEntities.ORBITAL_MACHINE.get()
                ? (tickLevel, pos, state, blockEntity) -> OrbitalMachineBlockEntity.tick(tickLevel, pos, state, (OrbitalMachineBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
            sendOpenMessage(player);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
            sendOpenMessage(player);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.CONSUME;
    }

    private void sendOpenMessage(Player player) {
        if (kind == MachineKind.ROCKET_ASSEMBLY_FRAME) {
            LaunchReadiness readiness = LaunchReadiness.evaluateForAssembly(player);
            if (readiness.ready()) {
                player.sendSystemMessage(Component.literal("ECHO-7 // Rocket Assembly Frame checklist green. Emergency Rocket output online."));
                return;
            }
            String missing = String.join(", ", readiness.missing().stream()
                    .limit(4)
                    .map(component -> component.getString().replaceFirst("^- ", ""))
                    .toList());
            if (readiness.missing().size() > 4) {
                missing += ", +" + (readiness.missing().size() - 4) + " more";
            }
            player.sendSystemMessage(Component.literal("ECHO-7 // Rocket Assembly Frame missing: " + missing + "."));
            return;
        }
        player.sendSystemMessage(Component.literal("ECHO-7 // " + kind.displayName() + " menu opened."));
    }

    public enum MachineKind implements StringRepresentable {
        ROCKET_ASSEMBLY_FRAME("rocket_assembly_frame", "Rocket Assembly Frame"),
        OXYGEN_COMPRESSOR("oxygen_compressor", "Oxygen Compressor"),
        FUEL_REFINERY("fuel_refinery", "Fuel Refinery"),
        HEAT_SHIELD_FABRICATOR("heat_shield_fabricator", "Heat Shield Fabricator"),
        ORBITAL_FABRICATOR("orbital_fabricator", "Orbital Fabricator"),
        VACUUM_SMELTER("vacuum_smelter", "Vacuum Smelter"),
        SOLAR_RECLAIMER("solar_reclaimer", "Solar Reclaimer"),
        SUIT_CHARGING_STATION("suit_charging_station", "Suit Charging Station"),
        SIGNAL_ANALYZER("signal_analyzer", "Signal Analyzer"),
        NAVIGATION_CONSOLE("navigation_console", "Navigation Console"),
        STATION_LIFE_SUPPORT_CORE("station_life_support_core", "Station Life Support Core");

        public static final Codec<MachineKind> CODEC = StringRepresentable.fromEnum(MachineKind::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, MachineKind> STREAM_CODEC =
                ByteBufCodecs.idMapper(MachineKind::byId, MachineKind::ordinal).cast();
        private static final MachineKind[] BY_ID = values();
        private final String serializedName;
        private final String displayName;

        MachineKind(String serializedName, String displayName) {
            this.serializedName = serializedName;
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public boolean processingRecipeDriven() {
            return this != ROCKET_ASSEMBLY_FRAME && this != NAVIGATION_CONSOLE && this != STATION_LIFE_SUPPORT_CORE;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        private static MachineKind byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : OXYGEN_COMPRESSOR;
        }
    }
}
