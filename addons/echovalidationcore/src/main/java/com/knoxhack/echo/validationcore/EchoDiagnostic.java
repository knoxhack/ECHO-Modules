package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record EchoDiagnostic(
        EchoDiagnosticCode code,
        EchoDiagnosticSeverity severity,
        String title,
        String summary,
        EchoModuleId moduleId,
        EchoPackId packId,
        EchoAffectedFeature affectedFeature,
        EchoValidationCategory category,
        String cause,
        String playerFix,
        String developerDetails,
        boolean repairable,
        List<EchoRepairSuggestion> suggestedRepairActions,
        List<EchoLikelyOwner> likelyOwners,
        List<String> likelyFiles,
        String suggestedAgentLane,
        List<String> safeCommands,
        List<String> relatedDocs
) {
    public EchoDiagnostic {
        Objects.requireNonNull(code, "code");
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        title = ValidationContractGuards.requireText(title, "diagnostic title");
        summary = ValidationContractGuards.optionalText(summary);
        category = category == null ? EchoValidationCategory.UNKNOWN : category;
        cause = ValidationContractGuards.optionalText(cause);
        playerFix = ValidationContractGuards.optionalText(playerFix);
        developerDetails = ValidationContractGuards.optionalText(developerDetails);
        suggestedRepairActions = ValidationContractGuards.immutableList(suggestedRepairActions);
        likelyOwners = ValidationContractGuards.immutableList(likelyOwners);
        likelyFiles = ValidationContractGuards.immutableList(likelyFiles);
        suggestedAgentLane = ValidationContractGuards.optionalText(suggestedAgentLane);
        safeCommands = ValidationContractGuards.immutableList(safeCommands);
        relatedDocs = ValidationContractGuards.immutableList(relatedDocs);
        repairable = repairable || !suggestedRepairActions.isEmpty();
    }

    public static Builder builder(EchoDiagnosticCode code, EchoDiagnosticSeverity severity, String title, String summary) {
        return new Builder(code, severity, title, summary);
    }

    public boolean blocking() {
        return severity.blocking();
    }

    public static final class Builder {
        private final EchoDiagnosticCode code;
        private final EchoDiagnosticSeverity severity;
        private final String title;
        private final String summary;
        private EchoModuleId moduleId;
        private EchoPackId packId;
        private EchoAffectedFeature affectedFeature;
        private EchoValidationCategory category = EchoValidationCategory.UNKNOWN;
        private String cause = "";
        private String playerFix = "";
        private String developerDetails = "";
        private boolean repairable;
        private final List<EchoRepairSuggestion> suggestedRepairActions = new ArrayList<>();
        private final List<EchoLikelyOwner> likelyOwners = new ArrayList<>();
        private final List<String> likelyFiles = new ArrayList<>();
        private String suggestedAgentLane = "";
        private final List<String> safeCommands = new ArrayList<>();
        private final List<String> relatedDocs = new ArrayList<>();

        private Builder(EchoDiagnosticCode code, EchoDiagnosticSeverity severity, String title, String summary) {
            this.code = Objects.requireNonNull(code, "code");
            this.severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
            this.title = title;
            this.summary = summary;
        }

        public Builder moduleId(EchoModuleId moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        public Builder packId(EchoPackId packId) {
            this.packId = packId;
            return this;
        }

        public Builder affectedFeature(EchoAffectedFeature affectedFeature) {
            this.affectedFeature = affectedFeature;
            return this;
        }

        public Builder category(EchoValidationCategory category) {
            this.category = category;
            return this;
        }

        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        public Builder playerFix(String playerFix) {
            this.playerFix = playerFix;
            return this;
        }

        public Builder developerDetails(String developerDetails) {
            this.developerDetails = developerDetails;
            return this;
        }

        public Builder repairable(boolean repairable) {
            this.repairable = repairable;
            return this;
        }

        public Builder addRepairSuggestion(EchoRepairSuggestion suggestion) {
            this.suggestedRepairActions.add(Objects.requireNonNull(suggestion, "suggestion"));
            return this;
        }

        public Builder likelyOwner(EchoLikelyOwner owner) {
            this.likelyOwners.add(Objects.requireNonNull(owner, "owner"));
            return this;
        }

        public Builder likelyFile(String likelyFile) {
            this.likelyFiles.add(ValidationContractGuards.requireText(likelyFile, "likely file"));
            return this;
        }

        public Builder suggestedAgentLane(String suggestedAgentLane) {
            this.suggestedAgentLane = suggestedAgentLane;
            return this;
        }

        public Builder safeCommand(String safeCommand) {
            this.safeCommands.add(ValidationContractGuards.requireText(safeCommand, "safe command"));
            return this;
        }

        public Builder relatedDoc(String relatedDoc) {
            this.relatedDocs.add(ValidationContractGuards.requireText(relatedDoc, "related doc"));
            return this;
        }

        public EchoDiagnostic build() {
            return new EchoDiagnostic(
                    code,
                    severity,
                    title,
                    summary,
                    moduleId,
                    packId,
                    affectedFeature,
                    category,
                    cause,
                    playerFix,
                    developerDetails,
                    repairable,
                    suggestedRepairActions,
                    likelyOwners,
                    likelyFiles,
                    suggestedAgentLane,
                    safeCommands,
                    relatedDocs
            );
        }
    }
}
