package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.world.entity.player.Player;

public interface IIndexSearchService {
    List<IndexSearchResult> search(Player player, String query, int maxResults);

    void invalidate();
}
