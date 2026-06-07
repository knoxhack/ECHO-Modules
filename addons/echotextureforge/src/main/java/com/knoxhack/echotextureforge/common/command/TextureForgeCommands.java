package com.knoxhack.echotextureforge.common.command;

import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.common.export.TextureApplyResult;
import com.knoxhack.echotextureforge.common.export.TextureSheetImportPlan;
import com.knoxhack.echotextureforge.common.review.TextureReviewStatus;
import com.knoxhack.echotextureforge.common.TextureForgeService;
import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.CommandDispatcher;

public final class TextureForgeCommands {
    private TextureForgeCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> echoSubcommand() {
        return textureForgeNode("textureforge");
    }

    public static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(textureForgeNode("echotextureforge"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> textureForgeNode(String literal) {
        return Commands.literal(literal)
                .requires(TextureForgeCommands::canUse)
                .then(Commands.literal("scan")
                        .executes(context -> scan(context.getSource(), ""))
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .executes(context -> scan(context.getSource(), StringArgumentType.getString(context, "modid")))))
                .then(Commands.literal("export")
                        .executes(context -> exportAll(context.getSource(), ""))
                        .then(Commands.literal("prompts")
                                .executes(context -> exportPrompts(context.getSource(), "")))
                        .then(Commands.literal("reports")
                                .executes(context -> exportReports(context.getSource(), ""))))
                .then(Commands.literal("validate")
                        .executes(context -> validate(context.getSource(), ""))
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .executes(context -> validate(context.getSource(), StringArgumentType.getString(context, "modid")))))
                .then(Commands.literal("spec")
                        .then(Commands.literal("list")
                                .executes(context -> specList(context.getSource())))
                        .then(Commands.literal("generate")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .executes(context -> specGenerate(context.getSource(), StringArgumentType.getString(context, "modid"))))))
                .then(Commands.literal("prompt")
                        .then(Commands.literal("missing")
                                .executes(context -> exportPrompts(context.getSource(), "")))
                        .then(Commands.literal("item")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .then(Commands.argument("item_id", StringArgumentType.string())
                                                .executes(context -> singlePrompt(context.getSource(),
                                                        StringArgumentType.getString(context, "modid"),
                                                        StringArgumentType.getString(context, "item_id"),
                                                        TextureKind.ITEM)))))
                        .then(Commands.literal("block")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .then(Commands.argument("block_id", StringArgumentType.string())
                                                .executes(context -> singlePrompt(context.getSource(),
                                                        StringArgumentType.getString(context, "modid"),
                                                        StringArgumentType.getString(context, "block_id"),
                                                        TextureKind.BLOCK))))))
                .then(Commands.literal("report")
                        .then(Commands.literal("open")
                                .executes(context -> reportOpen(context.getSource()))))
                .then(Commands.literal("import")
                        .then(Commands.literal("plan")
                                .then(Commands.argument("sheet_name", StringArgumentType.string())
                                        .executes(context -> importPlan(context.getSource(),
                                                StringArgumentType.getString(context, "sheet_name"), false))))
                        .then(Commands.literal("preview")
                                .then(Commands.argument("sheet_name", StringArgumentType.string())
                                        .executes(context -> importPlan(context.getSource(),
                                                StringArgumentType.getString(context, "sheet_name"), false))))
                        .then(Commands.literal("stage")
                                .then(Commands.argument("sheet_name", StringArgumentType.string())
                                        .executes(context -> importPlan(context.getSource(),
                                                StringArgumentType.getString(context, "sheet_name"), true)))))
                .then(Commands.literal("apply")
                        .then(Commands.literal("dryrun")
                                .executes(context -> apply(context.getSource(), "", true, false)))
                        .then(Commands.literal("staged")
                                .executes(context -> apply(context.getSource(), "", false, false))
                                .then(Commands.literal("--no-overwrite")
                                        .executes(context -> apply(context.getSource(), "", false, false)))
                                .then(Commands.literal("--overwrite-approved")
                                        .executes(context -> apply(context.getSource(), "", false, true)))
                                .then(Commands.literal("--modid")
                                        .then(Commands.argument("modid", StringArgumentType.word())
                                                .executes(context -> apply(context.getSource(),
                                                        StringArgumentType.getString(context, "modid"), false, false))
                                                .then(Commands.literal("--overwrite-approved")
                                                        .executes(context -> apply(context.getSource(),
                                                                StringArgumentType.getString(context, "modid"), false, true)))))))
                .then(Commands.literal("review")
                        .then(Commands.literal("list")
                                .executes(context -> reviewList(context.getSource())))
                        .then(Commands.literal("approve")
                                .then(Commands.argument("asset", StringArgumentType.string())
                                        .executes(context -> reviewUpdate(context.getSource(),
                                                StringArgumentType.getString(context, "asset"), TextureReviewStatus.APPROVED, ""))))
                        .then(Commands.literal("reject")
                                .then(Commands.argument("asset", StringArgumentType.string())
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> reviewUpdate(context.getSource(),
                                                        StringArgumentType.getString(context, "asset"),
                                                        TextureReviewStatus.REJECTED,
                                                        StringArgumentType.getString(context, "reason"))))))
                        .then(Commands.literal("mark")
                                .then(Commands.literal("needs_regen")
                                        .then(Commands.argument("asset", StringArgumentType.string())
                                                .executes(context -> reviewUpdate(context.getSource(),
                                                        StringArgumentType.getString(context, "asset"),
                                                        TextureReviewStatus.NEEDS_REGEN, "")))))
                        .then(Commands.literal("export")
                                .executes(context -> reviewExport(context.getSource()))));
    }

    private static boolean canUse(CommandSourceStack source) {
        return TextureForgeConfig.enabled()
                && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int scan(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, true, false,
                "Scan complete");
    }

    private static int exportAll(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, true, true,
                "Reports and prompts exported");
    }

    private static int exportPrompts(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, false, true,
                "Prompts exported");
    }

    private static int exportReports(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, true, false,
                "Reports exported");
    }

    private static int validate(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, true, false,
                "Validation complete");
    }

    private static int specGenerate(CommandSourceStack source, String namespace) {
        return run(source, namespace, true, true, true,
                "Specs generated from registry/resources and exported");
    }

    private static int run(CommandSourceStack source, String namespace, boolean includeRegistry,
                           boolean exportReports, boolean exportPrompts, String label) {
        try {
            TextureAuditReport report = TextureForgeService.INSTANCE.runAudit(namespace, includeRegistry, exportReports, exportPrompts);
            tell(source, label + ": " + report.issues().size() + " issue(s), "
                    + report.totalSpecs() + " spec(s), output " + report.outputRoot(), ChatFormatting.GREEN);
            tell(source, "Severity: CRITICAL=" + report.severitySummary().getOrDefault(TextureAuditSeverity.CRITICAL, 0)
                    + ", WARNING=" + report.severitySummary().getOrDefault(TextureAuditSeverity.WARNING, 0)
                    + ", INFO=" + report.severitySummary().getOrDefault(TextureAuditSeverity.INFO, 0), ChatFormatting.GRAY);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "TextureForge failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int specList(CommandSourceStack source) {
        TextureForgeService.INSTANCE.lastReport().ifPresentOrElse(report -> {
            tell(source, "TextureForge specs: " + report.specs().size(), ChatFormatting.AQUA);
            report.specs().stream().limit(24).forEach(spec -> tell(source,
                    spec.namespace() + ":" + spec.assetId() + " " + spec.assetKind().id()
                            + "/" + spec.textureType().id() + " -> assets/" + spec.namespace() + "/" + spec.outputPath(),
                    ChatFormatting.GRAY));
            if (report.specs().size() > 24) {
                tell(source, "Run export reports for the full spec list.", ChatFormatting.DARK_GRAY);
            }
        }, () -> tell(source, "No scan has run yet. Use /echo textureforge scan first.", ChatFormatting.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    private static int singlePrompt(CommandSourceStack source, String namespace, String assetId, TextureKind kind) {
        try {
            Path path = TextureForgeService.INSTANCE.exportSinglePrompt(namespace, assetId, kind);
            tell(source, "Prompt exported: " + path, ChatFormatting.GREEN);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Prompt export failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int reportOpen(CommandSourceStack source) {
        Path reports = TextureForgeService.INSTANCE.paths().reportsDir();
        tell(source, "TextureForge reports are written to: " + reports, ChatFormatting.AQUA);
        TextureForgeService.INSTANCE.lastReport().ifPresent(report ->
                tell(source, "Latest report had " + report.issues().size() + " issue(s).", ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int importPlan(CommandSourceStack source, String sheetName, boolean stage) {
        try {
            TextureSheetImportPlan plan = TextureForgeService.INSTANCE.planImport(sheetName, stage);
            tell(source, (stage ? "Import staged" : "Import plan exported") + ": "
                    + plan.cells().size() + " cell(s), conflicts "
                    + plan.cells().stream().filter(cell -> cell.conflict()).count(),
                    ChatFormatting.GREEN);
            tell(source, "Report: " + TextureForgeService.INSTANCE.paths().importDir().resolve("import_report.md"),
                    ChatFormatting.GRAY);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Import planning failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int apply(CommandSourceStack source, String modid, boolean dryRun, boolean overwriteApproved) {
        try {
            TextureApplyResult result = TextureForgeService.INSTANCE.applyStaged(modid, dryRun, overwriteApproved);
            tell(source, (dryRun ? "Apply dry-run complete" : "Apply complete") + ": copied="
                    + result.copied() + ", skipped=" + result.skipped() + ", conflicts=" + result.conflicts(),
                    result.conflicts() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
            tell(source, "Report: " + TextureForgeService.INSTANCE.paths().importDir().resolve("apply_report.md"),
                    ChatFormatting.GRAY);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Apply failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int reviewList(CommandSourceStack source) {
        var state = TextureForgeService.INSTANCE.reviewState();
        tell(source, "Review entries: " + state.entries().size(), ChatFormatting.AQUA);
        state.entries().stream().limit(24).forEach(entry -> tell(source,
                entry.status().id() + " " + entry.specId() + " -> " + entry.targetOutputPath(),
                ChatFormatting.GRAY));
        if (state.entries().size() > 24) {
            tell(source, "Run review export for the full list.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reviewUpdate(CommandSourceStack source, String asset, TextureReviewStatus status, String notes) {
        try {
            TextureForgeService.INSTANCE.updateReview(asset, status, notes);
            tell(source, "Review updated: " + asset + " -> " + status.id(), ChatFormatting.GREEN);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Review update failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int reviewExport(CommandSourceStack source) {
        try {
            Path path = TextureForgeService.INSTANCE.exportReviewState();
            tell(source, "Review exported: " + path, ChatFormatting.GREEN);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Review export failed: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static void tell(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal("[TextureForge] " + message).withStyle(color), false);
    }
}
