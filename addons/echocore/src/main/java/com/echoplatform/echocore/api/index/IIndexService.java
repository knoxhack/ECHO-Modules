package com.echoplatform.echocore.api.index;

public interface IIndexService {
    IIndexRegistry registry();

    IIndexRecipeService recipes();

    IIndexSearchService search();

    IIndexDiscoveryService discovery();

    IIndexOverlayService overlay();
}
