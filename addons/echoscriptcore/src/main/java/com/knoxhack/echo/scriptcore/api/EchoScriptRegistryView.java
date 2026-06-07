package com.knoxhack.echo.scriptcore.api;

import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoEndingDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapLayerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapMarkerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLensScanDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLootProfileDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoRecipeUnlockDefinition;
import com.knoxhack.echo.scriptcore.model.EchoTutorialHintDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWeatherEventDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface EchoScriptRegistryView {
    List<EchoScriptDefinitionView> all();

    Optional<EchoScriptDefinitionView> get(Identifier id);

    List<EchoScriptDefinitionView> getByType(String type);

    List<EchoScriptDefinitionView> getByPack(String pack);

    Optional<EchoMissionDefinition> getMission(Identifier id);

    Optional<EchoArchiveEntryDefinition> getArchiveEntry(Identifier id);

    Optional<EchoLensScanDefinition> getLensScan(Identifier id);

    Optional<EchoHoloMapLayerDefinition> getHoloMapLayer(Identifier id);

    Optional<EchoHoloMapMarkerDefinition> getHoloMapMarker(Identifier id);

    Optional<EchoWeatherEventDefinition> getWeatherEvent(Identifier id);

    Optional<EchoFactionDefinition> getFaction(Identifier id);

    Optional<EchoWorldStateDefinition> getWorldState(Identifier id);

    Optional<EchoTutorialHintDefinition> getTutorialHint(Identifier id);

    Optional<EchoDialogueDefinition> getDialogue(Identifier id);

    Optional<EchoEndingDefinition> getEnding(Identifier id);

    Optional<EchoRecipeUnlockDefinition> getRecipeUnlock(Identifier id);

    Optional<EchoLootProfileDefinition> getLootProfile(Identifier id);

    Map<String, Integer> countByType();

    Map<String, Integer> countByPack();
}
