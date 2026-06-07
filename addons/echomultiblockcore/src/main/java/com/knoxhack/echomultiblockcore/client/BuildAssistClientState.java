package com.knoxhack.echomultiblockcore.client;

import com.knoxhack.echomultiblockcore.api.BuildAssistAnchor;
import com.knoxhack.echomultiblockcore.api.BuildAssistGeometry;
import com.knoxhack.echomultiblockcore.api.BuildAssistMaterialChecklist;
import com.knoxhack.echomultiblockcore.api.BuildAssistPreviewIssue;
import com.knoxhack.echomultiblockcore.api.BuildAssistTransform;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistSnapshot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

final class BuildAssistClientState {
    private static final Identifier EMPTY_ID = Identifier.fromNamespaceAndPath("echomultiblockcore", "empty");
    private Identifier activeDefinitionId = EMPTY_ID;
    private BuildAssistTransform transform = BuildAssistTransform.DEFAULT;
    private BuildAssistAnchor anchor = BuildAssistAnchor.placement(BlockPos.ZERO);
    private PreviewReport report = PreviewReport.empty();

    void select(Identifier definitionId, MultiblockBuildAssistSnapshot snapshot) {
        if (definitionId == null || !definitionId.equals(activeDefinitionId)) {
            activeDefinitionId = definitionId == null ? EMPTY_ID : definitionId;
            transform = BuildAssistTransform.DEFAULT;
        }
        transform = BuildAssistGeometry.normalize(snapshot, transform);
    }

    void clearReport() {
        report = PreviewReport.empty();
    }

    Identifier activeDefinitionId() {
        return activeDefinitionId;
    }

    BuildAssistTransform transform() {
        return transform;
    }

    BuildAssistAnchor anchor() {
        return anchor;
    }

    void anchor(BuildAssistAnchor anchor) {
        this.anchor = anchor == null ? BuildAssistAnchor.placement(BlockPos.ZERO) : anchor;
    }

    PreviewReport report() {
        return report;
    }

    void report(PreviewReport report) {
        this.report = report == null ? PreviewReport.empty() : report;
    }

    void rotate(MultiblockBuildAssistSnapshot snapshot) {
        transform = BuildAssistGeometry.rotate(snapshot, transform);
    }

    void toggleMirror(MultiblockBuildAssistSnapshot snapshot) {
        transform = BuildAssistGeometry.toggleMirror(snapshot, transform);
    }

    void layerDelta(MultiblockBuildAssistSnapshot snapshot, int delta) {
        transform = BuildAssistGeometry.layerDelta(snapshot, transform, delta);
    }

    record PreviewReport(
            String title,
            String warning,
            BuildAssistTransform transform,
            BuildAssistAnchor anchor,
            int required,
            int valid,
            List<BuildAssistPreviewIssue> missing,
            List<BuildAssistPreviewIssue> wrong,
            BuildAssistMaterialChecklist checklist,
            int height) {
        PreviewReport {
            title = title == null ? "" : title;
            warning = warning == null ? "" : warning;
            transform = transform == null ? BuildAssistTransform.DEFAULT : transform;
            anchor = anchor == null ? BuildAssistAnchor.placement(BlockPos.ZERO) : anchor;
            required = Math.max(0, required);
            valid = Math.max(0, valid);
            missing = List.copyOf(missing == null ? List.of() : missing);
            wrong = List.copyOf(wrong == null ? List.of() : wrong);
            checklist = checklist == null ? BuildAssistMaterialChecklist.empty(EMPTY_ID) : checklist;
            height = Math.max(0, height);
        }

        static PreviewReport empty() {
            return new PreviewReport("", "", BuildAssistTransform.DEFAULT, BuildAssistAnchor.placement(BlockPos.ZERO),
                    0, 0, List.of(), List.of(), BuildAssistMaterialChecklist.empty(EMPTY_ID), 0);
        }

        boolean isEmptyReport() {
            return title == null || title.isBlank();
        }

        int completionPercent() {
            return required <= 0 ? 100 : Math.round((valid * 100.0F) / required);
        }
    }
}
