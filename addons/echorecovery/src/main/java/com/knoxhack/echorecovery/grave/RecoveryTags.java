package com.knoxhack.echorecovery.grave;

import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class RecoveryTags {
    public static final TagKey<Item> SOULBOUND = item("soulbound");
    public static final TagKey<Item> ALWAYS_GRAVE = item("always_grave");
    public static final TagKey<Item> DROP_ON_DEATH = item("drop_on_death");
    public static final TagKey<Item> DESTROY_ON_DEATH = item("destroy_on_death");
    public static final TagKey<Item> PROTECTED = item("protected");
    public static final TagKey<Item> NO_GRAVE = item("no_grave");

    private RecoveryTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path));
    }
}
