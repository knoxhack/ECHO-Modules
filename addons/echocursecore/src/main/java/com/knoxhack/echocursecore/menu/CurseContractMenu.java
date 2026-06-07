package com.knoxhack.echocursecore.menu;

import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echocursecore.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class CurseContractMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 260;
    public static final int GUI_HEIGHT = 206;
    public static final int BUTTON_CLEANSE = 0;
    public static final int BUTTON_ACCEPT_BLOOD_DEBT = 1;
    public static final int BUTTON_ACCEPT_VOID_MARK = 2;
    public static final int BUTTON_BREAK_CONTRACT = 3;
    public static final int BUTTON_PAY_DEBT = 4;
    public static final int BUTTON_CLEANSE_ECHO_ROT = 5;
    public static final int BUTTON_CLEANSE_BLOOD_DEBT = 6;
    public static final int BUTTON_CLEANSE_VOID_MARK = 7;
    public static final int BUTTON_SEVER_CONTRACT = 8;

    private static final int DATA_ACTIVE = 0;
    private static final int DATA_CONTRACTS = 1;
    private static final int DATA_CLEANSEABLE = 2;
    private static final int DATA_ECHO_ROT = 3;
    private static final int DATA_BLOOD_DEBT = 4;
    private static final int DATA_VOID_MARK = 5;
    private static final int DATA_CONTRACT_DEBT = 6;
    private static final int DATA_RESISTANCE = 7;
    private static final int DATA_READINESS = 8;
    private static final int DATA_SEVER_READY = 9;
    private static final int DATA_PLAN = 10;
    private static final int DATA_PLAN_TARGET = 11;
    public static final int DATA_COUNT = 12;

    private final Player player;
    private final ContainerData data;

    public CurseContractMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player, new SimpleContainerData(DATA_COUNT));
    }

    public CurseContractMenu(int containerId, Inventory inventory, Player player) {
        this(containerId, inventory, player, new LiveData(player));
    }

    private CurseContractMenu(int containerId, Inventory inventory, Player player, ContainerData data) {
        super(ModMenus.CURSE_CONTRACT.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.player = player;
        this.data = data;
        addDataSlots(data);
    }

    public int activeCount() {
        return data.get(DATA_ACTIVE);
    }

    public int contractCount() {
        return data.get(DATA_CONTRACTS);
    }

    public int cleanseableCount() {
        return data.get(DATA_CLEANSEABLE);
    }

    public int echoRotStage() {
        return data.get(DATA_ECHO_ROT);
    }

    public int bloodDebtStage() {
        return data.get(DATA_BLOOD_DEBT);
    }

    public int voidMarkStage() {
        return data.get(DATA_VOID_MARK);
    }

    public int contractDebt() {
        return data.get(DATA_CONTRACT_DEBT);
    }

    public int contractResistance() {
        return data.get(DATA_RESISTANCE);
    }

    public int cleansingReadiness() {
        return data.get(DATA_READINESS);
    }

    public int severReadyCount() {
        return data.get(DATA_SEVER_READY);
    }

    public int cleansingPlanCode() {
        return data.get(DATA_PLAN);
    }

    public int cleansingPlanTargetCode() {
        return data.get(DATA_PLAN_TARGET);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }
        return switch (id) {
            case BUTTON_CLEANSE -> CurseCoreApi.cleanseFirstMinorCurse(serverPlayer);
            case BUTTON_ACCEPT_BLOOD_DEBT -> CurseCoreApi.acceptContract(serverPlayer, CurseCoreApi.BLOOD_DEBT, 2, "contract_screen");
            case BUTTON_ACCEPT_VOID_MARK -> CurseCoreApi.acceptContract(serverPlayer, CurseCoreApi.VOID_MARK, 1, "contract_screen");
            case BUTTON_BREAK_CONTRACT -> breakFirstContract(serverPlayer);
            case BUTTON_PAY_DEBT -> CurseCoreApi.stabilizeFirstContract(serverPlayer);
            case BUTTON_CLEANSE_ECHO_ROT -> CurseCoreApi.cleanseCurse(serverPlayer, CurseCoreApi.ECHO_ROT, 1, false, "contract_screen");
            case BUTTON_CLEANSE_BLOOD_DEBT -> CurseCoreApi.cleanseCurse(serverPlayer, CurseCoreApi.BLOOD_DEBT, 1, false, "contract_screen");
            case BUTTON_CLEANSE_VOID_MARK -> CurseCoreApi.cleanseCurse(serverPlayer, CurseCoreApi.VOID_MARK, 1, false, "contract_screen");
            case BUTTON_SEVER_CONTRACT -> severFirstReadyContract(serverPlayer);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player;
    }

    private static boolean breakFirstContract(net.minecraft.server.level.ServerPlayer player) {
        for (Identifier curse : CurseCoreApi.knownCurses()) {
            if (CurseCoreApi.contractBound(player, curse)) {
                return CurseCoreApi.breakContract(player, curse);
            }
        }
        return false;
    }

    private static boolean severFirstReadyContract(net.minecraft.server.level.ServerPlayer player) {
        for (Identifier curse : CurseCoreApi.knownCurses()) {
            if (CurseCoreApi.canSeverContract(player, curse)) {
                return CurseCoreApi.severContractAndCleanse(player, curse);
            }
        }
        return false;
    }

    private static final class LiveData implements ContainerData {
        private final Player player;

        private LiveData(Player player) {
            this.player = player;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ACTIVE -> CurseCoreApi.activeCurses(player).size();
                case DATA_CONTRACTS -> CurseCoreApi.contractCount(player);
                case DATA_CLEANSEABLE -> CurseCoreApi.cleanseableCount(player);
                case DATA_ECHO_ROT -> CurseCoreApi.stage(player, CurseCoreApi.ECHO_ROT);
                case DATA_BLOOD_DEBT -> CurseCoreApi.stage(player, CurseCoreApi.BLOOD_DEBT);
                case DATA_VOID_MARK -> CurseCoreApi.stage(player, CurseCoreApi.VOID_MARK);
                case DATA_CONTRACT_DEBT -> CurseCoreApi.totalContractDebt(player);
                case DATA_RESISTANCE -> CurseCoreApi.contractResistance(player);
                case DATA_READINESS -> CurseCoreApi.cleansingReadiness(player);
                case DATA_SEVER_READY -> CurseCoreApi.severReadyCount(player);
                case DATA_PLAN -> CurseCoreApi.cleansingPlanCode(player);
                case DATA_PLAN_TARGET -> CurseCoreApi.recommendedCleansingTargetCode(player);
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
