package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.FactionStanding;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FactionPledgeItem extends Item {
    private final Faction faction;

    public FactionPledgeItem(Faction faction, Properties properties) {
        super(properties);
        this.faction = faction;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            EchoTerminalProgress progress = EchoTerminalProgress.get(player);
            boolean alreadyAligned = alreadyAligned(progress, faction);
            progress.alignFaction(player, faction);
            if (!alreadyAligned) {
                faction.grantReward(player);
            }
            if (!alreadyAligned && !player.hasInfiniteMaterials()) {
                player.getItemInHand(hand).shrink(1);
            }
            String message = alreadyAligned
                    ? "ECHO-7 // Faction alignment already active: " + faction.displayName() + ". Outpost support remains available."
                    : "ECHO-7 // Faction alignment accepted: " + faction.displayName() + ". Pledge support unlocked; Tier I outpost charters remain faction-neutral.";
            player.sendSystemMessage(Component.literal(message));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean alreadyAligned(EchoTerminalProgress progress, Faction faction) {
        return switch (faction) {
            case ORBITAL_REMNANT -> progress.orbitalRemnantStanding() == FactionStanding.ALIGNED;
            case VOID_SALVAGERS -> progress.voidSalvagerStanding() == FactionStanding.ALIGNED;
            case NEXUS_CHOIR -> progress.nexusChoirStanding() == FactionStanding.ALIGNED;
        };
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public enum Faction {
        ORBITAL_REMNANT(
                "Radwarden Compact",
                "Radwarden Orbital Containment",
                "orbital_remnant_relay",
                "Scan a Low Orbit Signal Relay or carry Orbit Survey Data for Radwarden orbital containment.",
                "Radwarden orbital containment cache authorized: oxygen, sealant, and route support delivered.",
                "Use to align with Radwarden orbital containment and receive suit-support rewards.") {
            @Override
            void grantReward(Player player) {
                give(player, new ItemStack(ModItems.OXYGEN_BOOSTER.get()));
                give(player, new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 3));
            }
        },
        VOID_SALVAGERS(
                "Crashbreak Salvage",
                "Crashbreak Orbital Salvage Manifest",
                "void_salvager_manifest",
                "Scan orbital salvage or turn in 1 Orbital Alloy and 1 Vacuum Circuit for Crashbreak salvage manifests.",
                "Crashbreak orbital salvage cache authorized: circuits, alloy, and repair salvage delivered.",
                "Use to align with Crashbreak orbital salvage and receive salvage rewards.") {
            @Override
            void grantReward(Player player) {
                give(player, new ItemStack(ModItems.ORBITAL_ALLOY.get(), 4));
                give(player, new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 2));
                give(player, new ItemStack(ModItems.CARGO_BAY_MODULE.get()));
            }
        },
        NEXUS_CHOIR(
                "Sporebound Sanctum",
                "Sporebound Anomaly Interpretation",
                "nexus_choir_anchor",
                "After ECHO-0, scan a Nexus Anchor/Growth or spend 1 Nexus Stabilizer Shard for Sporebound anomaly interpretation.",
                "Sporebound anomaly support cache authorized: stabilizers and anomaly supplies delivered.",
                "Use to align with Sporebound anomaly interpretation and receive forbidden rewards.") {
            @Override
            void grantReward(Player player) {
                give(player, new ItemStack(ModItems.NEXUS_DUST.get(), 3));
                give(player, new ItemStack(ModItems.NEXUS_PULSE_BLADE.get()));
            }
        };

        private final String displayName;
        private final String contractTitle;
        private final String contractId;
        private final String contractRequirement;
        private final String vendorCacheReport;
        private final String tooltip;

        Faction(String displayName, String contractTitle, String contractId, String contractRequirement,
                String vendorCacheReport, String tooltip) {
            this.displayName = displayName;
            this.contractTitle = contractTitle;
            this.contractId = contractId;
            this.contractRequirement = contractRequirement;
            this.vendorCacheReport = vendorCacheReport;
            this.tooltip = tooltip;
        }

        public String displayName() {
            return displayName;
        }

        public String contractTitle() {
            return contractTitle;
        }

        public String contractId() {
            return contractId;
        }

        public String contractRequirement() {
            return contractRequirement;
        }

        public String vendorCacheReport() {
            return vendorCacheReport;
        }

        public String tooltip() {
            return tooltip;
        }

        public static Faction fromContractId(String contract) {
            String id = contract == null ? "" : contract.split(":", 2)[0];
            for (Faction faction : values()) {
                if (faction.contractId.equals(id)) {
                    return faction;
                }
            }
            return null;
        }

        abstract void grantReward(Player player);
    }
}
