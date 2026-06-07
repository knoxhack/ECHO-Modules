package com.knoxhack.echomultiblockcore.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echomultiblockcore.Config;
import com.knoxhack.echomultiblockcore.api.BuildAssistAnchor;
import com.knoxhack.echomultiblockcore.api.BuildAssistGeometry;
import com.knoxhack.echomultiblockcore.api.BuildAssistMaterialChecklist;
import com.knoxhack.echomultiblockcore.api.BuildAssistPreviewIssue;
import com.knoxhack.echomultiblockcore.api.BuildAssistTransform;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistCell;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistSnapshot;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.block.MultiblockControllerBlock;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.item.BlueprintItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class MultiblockPreviewRenderer {
    private static final int COLOR_VALID = 0xFF2EE88A;
    private static final int COLOR_MISSING = 0xFFFFA726;
    private static final int COLOR_WRONG = 0xFFFF3B5C;
    private static final int COLOR_OPTIONAL = 0xFF87AFC7;
    private static final int COLOR_AIR = 0xFF45D6FF;
    private static final int COLOR_CONTROLLER = 0xFFFFFF66;
    private static final int DEFAULT_MAX_RENDER_CELLS = 1024;
    private static final int PREVIEW_CACHE_TICKS = 5;
    private static final BuildAssistClientState STATE = new BuildAssistClientState();
    private static PreviewCacheKey cachedPreviewKey;
    private static CachedPreview cachedPreview;
    private static long cachedPreviewTick = Long.MIN_VALUE;

    private MultiblockPreviewRenderer() {
    }

    public static void render(Object event) {
        PoseStack poseStack = EchoBackendClientBridge.renderPoseStack(event);
        if (poseStack == null) {
            return;
        }
        if (!enabled()) {
            STATE.clearReport();
            clearPreviewCache();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() == HitResult.Type.MISS) {
            STATE.clearReport();
            clearPreviewCache();
            return;
        }
        ItemStack stack = heldBlueprint(minecraft);
        if (stack.isEmpty()) {
            STATE.clearReport();
            clearPreviewCache();
            return;
        }
        Identifier definitionId = BlueprintItem.definitionId(stack, null);
        MultiblockBuildAssistSnapshot snapshot = MultiblockClientPackets.buildAssist(definitionId);
        if (snapshot == null) {
            STATE.clearReport();
            clearPreviewCache();
            return;
        }

        STATE.select(definitionId, snapshot);
        BuildAssistTransform transform = STATE.transform();
        BuildAssistAnchor anchor = anchor(minecraft, hit, definitionId);
        STATE.anchor(anchor);
        CachedPreview preview = preview(minecraft, definitionId, snapshot, transform, anchor, previewMaxRenderCells());
        if (!snapshot.complete()) {
            STATE.report(preview.report());
            return;
        }

        Vec3 camera = EchoBackendClientBridge.renderCameraPosition(event);
        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (RenderCell cell : preview.renderCells()) {
            renderCell(poseStack, consumer, cell);
        }
        poseStack.popPose();
        STATE.report(preview.report());
    }

    private static CachedPreview preview(Minecraft minecraft, Identifier definitionId,
            MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform, BuildAssistAnchor anchor,
            int maxRenderCells) {
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        PreviewCacheKey key = new PreviewCacheKey(definitionId, System.identityHashCode(snapshot), transform, anchor,
                minecraft.player == null ? BlockPos.ZERO : minecraft.player.blockPosition(), maxRenderCells);
        if (key.equals(cachedPreviewKey) && cachedPreview != null && gameTime - cachedPreviewTick < PREVIEW_CACHE_TICKS) {
            return cachedPreview;
        }
        BuildAssistMaterialChecklist checklist = BuildAssistMaterialChecklist.from(snapshot.materials(),
                minecraft.player.getInventory());
        CachedPreview next;
        if (!snapshot.complete()) {
            next = new CachedPreview(new BuildAssistClientState.PreviewReport(snapshot.displayName(), snapshot.warning(),
                    transform, anchor, 0, 0, List.of(), List.of(), checklist, snapshot.height()), List.of());
        } else {
            next = buildPreview(minecraft, snapshot, transform, anchor, checklist, maxRenderCells);
        }
        cachedPreviewKey = key;
        cachedPreview = next;
        cachedPreviewTick = gameTime;
        return next;
    }

    private static CachedPreview buildPreview(Minecraft minecraft, MultiblockBuildAssistSnapshot snapshot,
            BuildAssistTransform transform, BuildAssistAnchor anchor, BuildAssistMaterialChecklist checklist,
            int maxRenderCells) {
        PreviewTally tally = new PreviewTally();
        List<RenderCell> renderCells = new ArrayList<>();
        boolean capped = false;
        for (MultiblockBuildAssistCell cell : snapshot.cells()) {
            if (cell.wildcard() || !BuildAssistGeometry.isVisibleLayer(snapshot, transform, cell.localPos())) {
                continue;
            }
            BlockPos worldPos = BuildAssistGeometry.localToWorld(snapshot, transform, anchor.controllerPos(), cell.localPos());
            BlockState state = minecraft.level.getBlockState(worldPos);
            CellStatus status = status(cell, state);
            tally.record(cell, status);
            if (cell.air() && status == CellStatus.VALID && transform.layer() < 0) {
                continue;
            }
            if (renderCells.size() >= maxRenderCells) {
                capped = true;
                continue;
            }
            boolean controllerCell = cell.localPos().equals(snapshot.controllerLocalPos());
            renderCells.add(new RenderCell(worldPos, cell, status, controllerCell));
        }
        return new CachedPreview(tally.report(snapshot, transform, anchor, checklist, capped, maxRenderCells),
                List.copyOf(renderCells));
    }

    private static void renderCell(PoseStack poseStack, VertexConsumer consumer, RenderCell renderCell) {
        int color = renderCell.controllerCell() && renderCell.status() == CellStatus.VALID
                ? COLOR_CONTROLLER
                : color(renderCell.cell(), renderCell.status());
        double inflate = renderCell.controllerCell() ? 0.018D : 0.006D;
        float alpha = renderCell.controllerCell() ? 0.82F : 0.62F;
        AABB bounds = new AABB(renderCell.worldPos()).inflate(inflate);
        ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(bounds), 0, 0, 0, color, alpha);
    }

    public static void renderHud(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        BuildAssistClientState.PreviewReport report = STATE.report();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null || report.isEmptyReport()) {
            return;
        }
        Font font = minecraft.font;
        int width = 260;
        int x = 12;
        int y = 18;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(report.title()));
        lines.add(Component.translatable("hud.echomultiblockcore.build_assist.mode",
                layerName(report.transform()), rotationName(report.transform().rotation()),
                report.transform().mirrored()
                        ? Component.translatable("hud.echomultiblockcore.build_assist.mirror_on")
                        : Component.translatable("hud.echomultiblockcore.build_assist.mirror_off"),
                anchorModeName(report.anchor().mode())));
        lines.add(Component.translatable("hud.echomultiblockcore.build_assist.completion", report.completionPercent()));
        if (!report.warning().isBlank()) {
            lines.add(Component.translatable("hud.echomultiblockcore.build_assist.warning", report.warning()));
        }
        if (!report.checklist().placeableEntries().isEmpty()) {
            lines.add(Component.translatable("hud.echomultiblockcore.build_assist.materials",
                    report.checklist().compactLine(3)));
        }
        report.missing().stream().limit(3).forEach(issue ->
                lines.add(Component.translatable("hud.echomultiblockcore.build_assist.missing", issue.line())));
        report.wrong().stream().limit(3).forEach(issue ->
                lines.add(Component.translatable("hud.echomultiblockcore.build_assist.wrong", issue.line())));

        int height = Math.max(38, 10 + lines.size() * 11);
        graphics.fill(x, y, x + width, y + height, 0xAA071013);
        graphics.outline(x, y, width, height, 0xCC24D8FF);
        int rowY = y + 6;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFF55E8FF : 0xFFE6F7F8;
            graphics.text(font, fit(font, lines.get(i), width - 12), x + 6, rowY, color, false);
            rowY += 11;
        }
    }

    public static void rotatePreview() {
        MultiblockBuildAssistSnapshot snapshot = currentSnapshot();
        if (snapshot != null) {
            STATE.rotate(snapshot);
        }
    }

    public static void toggleMirror() {
        MultiblockBuildAssistSnapshot snapshot = currentSnapshot();
        if (snapshot != null) {
            STATE.toggleMirror(snapshot);
        }
    }

    public static void layerUp() {
        MultiblockBuildAssistSnapshot snapshot = currentSnapshot();
        if (snapshot != null) {
            STATE.layerDelta(snapshot, 1);
        }
    }

    public static void layerDown() {
        MultiblockBuildAssistSnapshot snapshot = currentSnapshot();
        if (snapshot != null) {
            STATE.layerDelta(snapshot, -1);
        }
    }

    private static BuildAssistAnchor anchor(Minecraft minecraft, BlockHitResult hit, Identifier definitionId) {
        BlockPos hitPos = hit.getBlockPos();
        if (minecraft.level.getBlockEntity(hitPos) instanceof MultiblockControllerBlockEntity controller
                && definitionId.equals(controller.getMultiblockId())) {
            return BuildAssistAnchor.targetedController(hitPos);
        }
        BlockState hitState = minecraft.level.getBlockState(hitPos);
        if (hitState.getBlock() instanceof MultiblockControllerBlock controller
                && definitionId.equals(controller.defaultDefinitionId())) {
            return BuildAssistAnchor.matchingControllerBlock(hitPos);
        }
        Direction direction = hit.getDirection();
        return BuildAssistAnchor.placement(hitPos.relative(direction == null ? Direction.UP : direction));
    }

    private static CellStatus status(MultiblockBuildAssistCell cell, BlockState state) {
        if (cell.air()) {
            return state.isAir() ? CellStatus.VALID : CellStatus.WRONG;
        }
        if (state.isAir()) {
            return cell.optional() ? CellStatus.OPTIONAL : CellStatus.MISSING;
        }
        if (matches(cell, state)) {
            return CellStatus.VALID;
        }
        return cell.optional() ? CellStatus.OPTIONAL : CellStatus.WRONG;
    }

    private static boolean matches(MultiblockBuildAssistCell cell, BlockState state) {
        if (cell.kind() == StructureBlockRequirement.SlotKind.BLOCK_TAG || cell.expected().startsWith("#")) {
            Identifier tag = Identifier.tryParse(cell.expected().replace("#", "").strip());
            return tag != null && state.is(TagKey.create(Registries.BLOCK, tag));
        }
        if (cell.kind() == StructureBlockRequirement.SlotKind.BLOCK_LIST) {
            return parseList(cell.expected()).stream()
                    .anyMatch(id -> BuiltInRegistries.BLOCK.getOptional(id).filter(state::is).isPresent());
        }
        Identifier id = Identifier.tryParse(cell.expected());
        return id != null && BuiltInRegistries.BLOCK.getOptional(id).filter(state::is).isPresent();
    }

    private static List<Identifier> parseList(String raw) {
        String clean = raw == null ? "" : raw.replace("[", "").replace("]", "");
        List<Identifier> ids = new ArrayList<>();
        for (String part : clean.split(",")) {
            Identifier id = Identifier.tryParse(part.strip());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static int color(MultiblockBuildAssistCell cell, CellStatus status) {
        return switch (status) {
            case VALID -> cell.air() ? COLOR_AIR : COLOR_VALID;
            case OPTIONAL -> COLOR_OPTIONAL;
            case MISSING -> COLOR_MISSING;
            case WRONG -> COLOR_WRONG;
        };
    }

    private static Component layerName(BuildAssistTransform transform) {
        return transform.layer() < 0
                ? Component.translatable("hud.echomultiblockcore.build_assist.layer_all")
                : Component.literal(Integer.toString(transform.layer()));
    }

    private static Component rotationName(Rotation value) {
        return Component.literal(switch (value == null ? Rotation.NONE : value) {
            case NONE -> "0";
            case CLOCKWISE_90 -> "90";
            case CLOCKWISE_180 -> "180";
            case COUNTERCLOCKWISE_90 -> "270";
        });
    }

    private static Component anchorModeName(BuildAssistAnchor.Mode mode) {
        return Component.translatable("hud.echomultiblockcore.build_assist.anchor."
                + switch (mode == null ? BuildAssistAnchor.Mode.PLACEMENT : mode) {
                    case TARGETED_CONTROLLER -> "targeted_controller";
                    case MATCHING_CONTROLLER_BLOCK -> "matching_controller_block";
                    case PLACEMENT -> "placement";
                });
    }

    private static MultiblockBuildAssistSnapshot currentSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ItemStack stack = heldBlueprint(minecraft);
        if (stack.isEmpty()) {
            return null;
        }
        Identifier definitionId = BlueprintItem.definitionId(stack, null);
        MultiblockBuildAssistSnapshot snapshot = MultiblockClientPackets.buildAssist(definitionId);
        if (snapshot != null) {
            STATE.select(definitionId, snapshot);
        }
        return snapshot;
    }

    private static ItemStack heldBlueprint(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.getItem() instanceof BlueprintItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean holdingBlueprint() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !heldBlueprint(minecraft).isEmpty();
    }

    private static boolean enabled() {
        try {
            return Config.ENABLE_PREVIEW_RENDERING.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static int previewMaxRenderCells() {
        try {
            return Math.max(64, Math.min(4096, Config.PREVIEW_MAX_RENDER_CELLS.get()));
        } catch (RuntimeException exception) {
            return DEFAULT_MAX_RENDER_CELLS;
        }
    }

    private static void clearPreviewCache() {
        cachedPreviewKey = null;
        cachedPreview = null;
        cachedPreviewTick = Long.MIN_VALUE;
    }

    private static Component fit(Font font, Component text, int width) {
        String value = text.getString();
        if (font.width(value) <= width) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(value, Math.max(8, width - font.width("..."))) + "...");
    }

    private enum CellStatus {
        VALID,
        MISSING,
        WRONG,
        OPTIONAL
    }

    private record PreviewCacheKey(Identifier definitionId, int snapshotIdentity, BuildAssistTransform transform,
            BuildAssistAnchor anchor, BlockPos playerBlockPos, int maxRenderCells) {
    }

    private record CachedPreview(BuildAssistClientState.PreviewReport report, List<RenderCell> renderCells) {
    }

    private record RenderCell(BlockPos worldPos, MultiblockBuildAssistCell cell, CellStatus status,
            boolean controllerCell) {
    }

    private static final class PreviewTally {
        private int required;
        private int valid;
        private final Map<String, Integer> missing = new LinkedHashMap<>();
        private final Map<String, Integer> wrong = new LinkedHashMap<>();

        void record(MultiblockBuildAssistCell cell, CellStatus status) {
            if (!cell.optional() && !cell.air() && !cell.wildcard()) {
                required++;
                if (status == CellStatus.VALID) {
                    valid++;
                }
            }
            if (status == CellStatus.MISSING) {
                missing.merge(cell.expected(), 1, Integer::sum);
            } else if (status == CellStatus.WRONG) {
                wrong.merge(cell.expected(), 1, Integer::sum);
            }
        }

        BuildAssistClientState.PreviewReport report(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform,
                BuildAssistAnchor anchor, BuildAssistMaterialChecklist checklist, boolean capped, int maxRenderCells) {
            List<BuildAssistPreviewIssue> missingIssues = missing.entrySet().stream()
                    .map(entry -> new BuildAssistPreviewIssue(BuildAssistPreviewIssue.Kind.MISSING, entry.getKey(), entry.getValue()))
                    .toList();
            List<BuildAssistPreviewIssue> wrongIssues = wrong.entrySet().stream()
                    .map(entry -> new BuildAssistPreviewIssue(BuildAssistPreviewIssue.Kind.WRONG, entry.getKey(), entry.getValue()))
                    .toList();
            String warning = capped ? "Preview render capped at " + maxRenderCells + " cells." : "";
            return new BuildAssistClientState.PreviewReport(snapshot.displayName(), warning, transform, anchor,
                    required, valid, missingIssues, wrongIssues, checklist, snapshot.height());
        }
    }
}
