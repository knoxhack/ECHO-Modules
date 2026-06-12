package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface IIndexSourceProvider {
    Identifier id();

    default List<IndexSourceFact> sourceFacts(Player player) {
        return List.of();
    }
}
