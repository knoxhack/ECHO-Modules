package com.knoxhack.echospellcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.menu.SpellDeckMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoSpellCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<SpellDeckMenu>> SPELL_DECK =
            EchoBackendRegistryBridge.register(MENUS, "spell_deck",
                    () -> EchoBackendMenuBridge.extendedMenuType(SpellDeckMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
