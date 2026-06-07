package com.knoxhack.echofamiliarcore.menu;

import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import com.knoxhack.echofamiliarcore.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class FamiliarCommandMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 246;
    public static final int GUI_HEIGHT = 176;
    public static final int BUTTON_FOLLOW = 0;
    public static final int BUTTON_STAY = 1;
    public static final int BUTTON_SCOUT = 2;
    public static final int BUTTON_DEFEND = 3;
    public static final int BUTTON_TRAIN = 4;
    public static final int BUTTON_UPGRADE_ATTUNEMENT = 5;
    public static final int BUTTON_UPGRADE_WARDING = 6;
    public static final int BUTTON_UPGRADE_SCOUTING = 7;

    private static final int DATA_KIND = 0;
    private static final int DATA_COMMAND = 1;
    private static final int DATA_BOND_LEVEL = 2;
    private static final int DATA_BOND_XP = 3;
    private static final int DATA_NEXT_XP = 4;
    private static final int DATA_HEALTH_PERCENT = 5;
    private static final int DATA_EVOLUTION = 6;
    private static final int DATA_UPGRADE_POINTS = 7;
    private static final int DATA_ATTUNEMENT_RANK = 8;
    private static final int DATA_WARDING_RANK = 9;
    private static final int DATA_SCOUTING_RANK = 10;
    private static final int DATA_EVOLUTION_FORM = 11;
    private static final int DATA_EVOLUTION_POWER = 12;
    private static final int DATA_EVOLUTION_ABILITY = 13;
    public static final int DATA_COUNT = 14;

    private final Player player;
    private final ContainerData data;

    public FamiliarCommandMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player, new SimpleContainerData(DATA_COUNT));
    }

    public FamiliarCommandMenu(int containerId, Inventory inventory, Player player) {
        this(containerId, inventory, player, new LiveData(player));
    }

    private FamiliarCommandMenu(int containerId, Inventory inventory, Player player, ContainerData data) {
        super(ModMenus.FAMILIAR_COMMAND.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.player = player;
        this.data = data;
        addDataSlots(data);
    }

    public int kind() {
        return data.get(DATA_KIND);
    }

    public int command() {
        return data.get(DATA_COMMAND);
    }

    public int bondLevel() {
        return data.get(DATA_BOND_LEVEL);
    }

    public int bondXp() {
        return data.get(DATA_BOND_XP);
    }

    public int nextXp() {
        return Math.max(1, data.get(DATA_NEXT_XP));
    }

    public int healthPercent() {
        return data.get(DATA_HEALTH_PERCENT);
    }

    public int evolutionTier() {
        return data.get(DATA_EVOLUTION);
    }

    public int upgradePoints() {
        return data.get(DATA_UPGRADE_POINTS);
    }

    public int attunementRank() {
        return data.get(DATA_ATTUNEMENT_RANK);
    }

    public int wardingRank() {
        return data.get(DATA_WARDING_RANK);
    }

    public int scoutingRank() {
        return data.get(DATA_SCOUTING_RANK);
    }

    public int evolutionFormCode() {
        return data.get(DATA_EVOLUTION_FORM);
    }

    public int evolutionPower() {
        return data.get(DATA_EVOLUTION_POWER);
    }

    public int evolutionAbilityCode() {
        return data.get(DATA_EVOLUTION_ABILITY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }
        if (id >= BUTTON_FOLLOW && id <= BUTTON_DEFEND) {
            return FamiliarCoreApi.setCommand(serverPlayer, id, "menu");
        }
        if (id == BUTTON_TRAIN) {
            return FamiliarCoreApi.addBondExperience(serverPlayer, 12, "menu_train");
        }
        if (id == BUTTON_UPGRADE_ATTUNEMENT) {
            return FamiliarCoreApi.spendUpgrade(serverPlayer, FamiliarCoreApi.UPGRADE_ATTUNEMENT);
        }
        if (id == BUTTON_UPGRADE_WARDING) {
            return FamiliarCoreApi.spendUpgrade(serverPlayer, FamiliarCoreApi.UPGRADE_WARDING);
        }
        if (id == BUTTON_UPGRADE_SCOUTING) {
            return FamiliarCoreApi.spendUpgrade(serverPlayer, FamiliarCoreApi.UPGRADE_SCOUTING);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && FamiliarCoreApi.activeFamiliar(player) != null;
    }

    public static String commandName(int command) {
        return switch (command) {
            case ArcanaFamiliarEntity.COMMAND_STAY -> "Stay";
            case ArcanaFamiliarEntity.COMMAND_SCOUT -> "Scout";
            case ArcanaFamiliarEntity.COMMAND_DEFEND -> "Defend";
            default -> "Follow";
        };
    }

    private static final class LiveData implements ContainerData {
        private final Player player;

        private LiveData(Player player) {
            this.player = player;
        }

        @Override
        public int get(int index) {
            ArcanaFamiliarEntity familiar = player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    ? FamiliarCoreApi.activeEntity(serverPlayer)
                    : null;
            return switch (index) {
                case DATA_KIND -> familiar == null ? -1 : familiar.familiarKind();
                case DATA_COMMAND -> familiar == null ? 0 : familiar.command();
                case DATA_BOND_LEVEL -> FamiliarCoreApi.bondLevel(player);
                case DATA_BOND_XP -> FamiliarCoreApi.bondExperience(player);
                case DATA_NEXT_XP -> FamiliarCoreApi.nextLevelExperience(player);
                case DATA_HEALTH_PERCENT -> familiar == null || familiar.getMaxHealth() <= 0.0F
                        ? 0 : Math.round(familiar.getHealth() * 100.0F / familiar.getMaxHealth());
                case DATA_EVOLUTION -> FamiliarCoreApi.evolutionTier(player);
                case DATA_UPGRADE_POINTS -> FamiliarCoreApi.upgradePoints(player);
                case DATA_ATTUNEMENT_RANK -> FamiliarCoreApi.upgradeRank(player, FamiliarCoreApi.UPGRADE_ATTUNEMENT);
                case DATA_WARDING_RANK -> FamiliarCoreApi.upgradeRank(player, FamiliarCoreApi.UPGRADE_WARDING);
                case DATA_SCOUTING_RANK -> FamiliarCoreApi.upgradeRank(player, FamiliarCoreApi.UPGRADE_SCOUTING);
                case DATA_EVOLUTION_FORM -> FamiliarCoreApi.evolutionFormCode(player);
                case DATA_EVOLUTION_POWER -> FamiliarCoreApi.evolutionPower(player);
                case DATA_EVOLUTION_ABILITY -> FamiliarCoreApi.evolutionAbilityCode(player);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
