package com.knoxhack.echogalacticsurveyprotocol;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;
import com.knoxhack.echogalacticsurveyprotocol.registry.GalacticSurveyBlocks;
import com.knoxhack.echogalacticsurveyprotocol.registry.GalacticSurveyItems;

/**
 * Minimal Galactic Survey protocol entrypoint. The first implementation slice is
 * data-first so adapter work can bind to stable content and proof IDs.
 */
public final class EchoGalacticSurveyProtocol {
    public static final String MODID = GalacticSurveyRuntimeContracts.MODULE_ID;
    public static final String PACK_ID = GalacticSurveyRuntimeContracts.PACK_ID;
    public static final String LONG_RANGE_SURVEY_MODE = GalacticSurveyRuntimeContracts.LONG_RANGE_SURVEY_MODE;

    public EchoGalacticSurveyProtocol() {
        commonSetup();
    }

    public EchoGalacticSurveyProtocol(Object modEventBus) {
        GalacticSurveyBlocks.register(modEventBus);
        GalacticSurveyItems.register(modEventBus);
        commonSetup();
    }

    public void commonSetup() {
        System.out.println("ECHO Galactic Survey Protocol online: sectors="
                + GalacticSurveyRuntimeContracts.SECTOR_IDS.size()
                + " discoveries=" + GalacticSurveyRuntimeContracts.DISCOVERY_IDS.size()
                + " blocks=" + GalacticSurveyBlocks.ALL_BLOCKS.size()
                + " items=" + GalacticSurveyItems.CORE_ITEMS.size()
                + " phases=" + GalacticSurveyRuntimeContracts.PHASE_IDS.size());
    }
}
