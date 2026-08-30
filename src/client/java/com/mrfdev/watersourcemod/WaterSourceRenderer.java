package com.mrfdev.watersourcemod;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Renders immutable scan data using Minecraft 26.2's extraction and render phases. */
public final class WaterSourceRenderer implements AutoCloseable {
    private static final RenderPipeline DEPTH_AWARE_PIPELINE = RenderPipelines.DEBUG_FILLED_BOX;

    /*
     * The exact 26.2 client exposes this position/color pipeline publicly. It is
     * used only after the player explicitly opts in to through-wall rendering.
     */
    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.GUI;
    /* Position/color vertices are 16 bytes, keeping 200k vertices below the 4 MiB big buffer. */
    private static final int MAX_VERTICES_PER_FRAME = 200_000;
    private static final int BOX_VERTICES = 24;
    private static final int OUTLINED_BOX_VERTICES = BOX_VERTICES * 2;
    private static final int HORIZONTAL_FRAME_VERTICES = BOX_VERTICES * 4;
    private static final int HOLLOW_BOX_VERTICES = BOX_VERTICES * 12;
    private static final int DASHED_FRAME_VERTICES = BOX_VERTICES * 16;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1F, 1F, 1F, 1F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final WaterSourceConfig.RenderColor CURRENT_BOUNDARY_COLOR =
            new WaterSourceConfig.RenderColor(1F, 0.82F, 0.22F);
    private static final WaterSourceConfig.RenderColor LOADED_BOUNDARY_COLOR =
            new WaterSourceConfig.RenderColor(0.18F, 0.92F, 0.55F);
    private static final WaterSourceConfig.RenderColor SKIPPED_BOUNDARY_COLOR =
            new WaterSourceConfig.RenderColor(1F, 0.30F, 0.34F);
    private static final WaterSourceConfig.RenderColor UNKNOWN_BOUNDARY_COLOR =
            new WaterSourceConfig.RenderColor(0.62F, 0.68F, 0.76F);
    private static final WaterSourceConfig.RenderColor NEAREST_COLOR =
            new WaterSourceConfig.RenderColor(1F, 1F, 0.78F);

    private final StagedVertexBuffer stagedBuffer = new StagedVertexBuffer(
            () -> "WaterSourceMod marker buffer",
            RenderType.BIG_BUFFER_SIZE);
    private final BlockPos.MutableBlockPos cullingPosition = new BlockPos.MutableBlockPos();
    private final WaterMarkerCuller markerCuller = new WaterMarkerCuller();

    private volatile List<WaterMarker> renderMarkers = List.of();
    private volatile ScanAreaSnapshot renderScanArea = ScanAreaSnapshot.empty();
    private volatile NearestWaterMarker renderNearest = NearestWaterMarker.none();
    private volatile float animationTime;
    private volatile long renderedMetrics;
    private volatile boolean closed;
    private boolean registered;

    public void register() {
        if (registered) {
            return;
        }
        if (closed) {
            throw new IllegalStateException("Cannot register a closed water marker renderer");
        }
        LevelExtractionEvents.END_EXTRACTION.register(this::extractMarkers);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::renderAndDrawMarkers);
        registered = true;
    }

    private void extractMarkers(LevelExtractionContext context) {
        if (closed) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        WaterSourceScanner scanner = WaterSourceModClient.scanner();
        if (client.level == null || client.level != context.level() || !scanner.isEnabled()) {
            clearExtractedState();
            return;
        }

        renderScanArea = scanner.scanAreaSnapshot();
        renderNearest = WaterSourceModClient.nearestMarker();
        animationTime = context.level().getGameTime()
                + context.deltaTracker().getGameTimeDeltaPartialTick(false);
        // Publish markers last so this volatile write also publishes the matching data above.
        renderMarkers = scanner.markerSnapshot();
    }

    private void renderAndDrawMarkers(LevelRenderContext context) {
        List<WaterMarker> markers = renderMarkers;
        ScanAreaSnapshot scanArea = renderScanArea;
        if (closed || !WaterSourceModClient.scanner().isEnabled()) {
            renderedMetrics = 0;
            return;
        }

        WaterSourceConfig.RenderSettings settings = WaterSourceModClient.renderSettings();
        boolean hasMarkers = settings.hasEnabledMarkers() && !markers.isEmpty();
        boolean hasBoundary = settings.scanBoundaryMode() != WaterSourceConfig.ScanBoundaryMode.OFF
                && !scanArea.isEmpty();
        if (!settings.hasRenderableGeometry() || (!hasMarkers && !hasBoundary)) {
            renderedMetrics = 0;
            return;
        }

        RenderPipeline pipeline = settings.throughWalls() ? THROUGH_WALL_PIPELINE : DEPTH_AWARE_PIPELINE;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) {
            renderedMetrics = 0;
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = stagedBuffer.appendDraw(
                format,
                primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);
        try {
            long frameMetrics = renderGeometry(
                    context,
                    draw,
                    markers,
                    scanArea,
                    renderNearest,
                    settings,
                    animationTime);
            renderedMetrics = frameMetrics;
            if (unpackVertexCount(frameMetrics) == 0) {
                return;
            }

            stagedBuffer.upload();
            StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);
            if (info != null) {
                draw(Minecraft.getInstance(), info, pipeline);
            }
        } finally {
            stagedBuffer.endFrame();
        }
    }

    private long renderGeometry(
            LevelRenderContext context,
            StagedVertexBuffer.Draw draw,
            List<WaterMarker> markers,
            ScanAreaSnapshot scanArea,
            NearestWaterMarker nearest,
            WaterSourceConfig.RenderSettings settings,
            float frameAnimationTime) {
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) {
            return 0;
        }

        markerCuller.prepareFrustum(
                context.levelState().cameraRenderState.projectionMatrix,
                context.levelState().cameraRenderState.viewRotationMatrix,
                camera);
        float renderDistance = settings.maxRenderDistance();
        float cameraRenderDistance = context.levelState().cameraRenderState.depthFar;
        if (Float.isFinite(cameraRenderDistance) && cameraRenderDistance > 0F) {
            renderDistance = Math.min(renderDistance, cameraRenderDistance);
        }

        VertexConsumer builder = stagedBuffer.getVertexBuilder(draw);
        int vertexCount = 0;
        int visibleMarkerCount = 0;
        int remainingMarkers = settings.maxVisibleMarkers();
        long previousSection = 0;
        boolean hasPreviousSection = false;
        boolean previousSectionRenderable = false;

        matrices.pushPose();
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4fc positionMatrix = matrices.last().pose();
            if (settings.scanBoundaryMode() != WaterSourceConfig.ScanBoundaryMode.OFF) {
                vertexCount += renderScanBoundary(
                        positionMatrix,
                        builder,
                        scanArea,
                        settings,
                        camera,
                        renderDistance,
                        MAX_VERTICES_PER_FRAME - vertexCount);
            }

            for (WaterMarker marker : markers) {
                if (remainingMarkers <= 0 || vertexCount >= MAX_VERTICES_PER_FRAME) {
                    break;
                }
                if (!settings.isMarkerEnabled(marker)
                        || !markerCuller.isWithinDistance(marker, camera, renderDistance)) {
                    continue;
                }

                long section = SectionPos.asLong(marker.x() >> 4, marker.y() >> 4, marker.z() >> 4);
                if (!hasPreviousSection || section != previousSection) {
                    hasPreviousSection = true;
                    previousSection = section;
                    previousSectionRenderable = isInRenderableSection(
                            context,
                            marker,
                            settings.throughWalls());
                }
                if (!previousSectionRenderable || !markerCuller.intersectsFrustum(marker)) {
                    continue;
                }

                WaterSourceConfig.RenderColor markerColor = settings.colorFor(marker);
                float alpha = settings.baseAlpha() * markerCuller.distanceFadeAlpha(
                        marker,
                        camera,
                        renderDistance,
                        settings.fadeStartFraction());
                if (alpha <= 0F) {
                    continue;
                }
                if (settings.pulse()) {
                    alpha *= 0.90F + 0.10F * (float) Math.sin(
                            frameAnimationTime * 0.13F + marker.x() * 0.07F + marker.z() * 0.03F);
                }

                boolean highlightNearest = settings.nearestMarkerMode().showsWorld() && nearest.matches(marker);
                int estimatedVertices = estimateMarkerVertices(marker, settings, highlightNearest);
                if (vertexCount + estimatedVertices > MAX_VERTICES_PER_FRAME) {
                    break;
                }
                vertexCount += renderMarker(
                        positionMatrix,
                        builder,
                        marker,
                        settings,
                        markerColor,
                        alpha,
                        highlightNearest);
                visibleMarkerCount++;
                remainingMarkers--;
            }
        } finally {
            matrices.popPose();
        }
        return packMetrics(visibleMarkerCount, vertexCount);
    }

    private int renderScanBoundary(
            Matrix4fc positionMatrix,
            VertexConsumer builder,
            ScanAreaSnapshot area,
            WaterSourceConfig.RenderSettings settings,
            Vec3 camera,
            float renderDistance,
            int vertexBudget) {
        if (area.isEmpty() || vertexBudget < HORIZONTAL_FRAME_VERTICES) {
            return 0;
        }

        float boundaryY = (float) Math.floor(camera.y) + 0.035F;
        if (area.maxY() > area.minY()) {
            boundaryY = Math.max(
                    area.minY() + 0.035F,
                    Math.min(area.maxY() - 0.035F, boundaryY));
        }
        int emitted = 0;
        double maxDistanceSquared = (double) renderDistance * renderDistance;
        for (int index = 0; index < area.chunkCount(); index++) {
            boolean center = area.isCenterAt(index);
            if (settings.scanBoundaryMode() == WaterSourceConfig.ScanBoundaryMode.CURRENT && !center) {
                continue;
            }

            int chunkX = area.chunkXAt(index);
            int chunkZ = area.chunkZAt(index);
            float minX = (chunkX << 4) + 0.08F;
            float minZ = (chunkZ << 4) + 0.08F;
            float maxX = minX + 15.84F;
            float maxZ = minZ + 15.84F;
            if (horizontalDistanceSquared(camera, minX, minZ, maxX, maxZ) > maxDistanceSquared
                    || !markerCuller.intersectsFrustum(
                    minX,
                    boundaryY - 0.08F,
                    minZ,
                    maxX,
                    boundaryY + 0.10F,
                    maxZ)) {
                continue;
            }

            ScanAreaSnapshot.ChunkState state = area.stateAt(index);
            boolean dashed = !center && state != ScanAreaSnapshot.ChunkState.LOADED;
            int estimate = dashed ? DASHED_FRAME_VERTICES : HORIZONTAL_FRAME_VERTICES;
            if (emitted + estimate > vertexBudget) {
                break;
            }

            WaterSourceConfig.RenderColor color = center
                    ? CURRENT_BOUNDARY_COLOR
                    : switch (state) {
                        case LOADED -> LOADED_BOUNDARY_COLOR;
                        case SKIPPED -> SKIPPED_BOUNDARY_COLOR;
                        case UNKNOWN -> UNKNOWN_BOUNDARY_COLOR;
                    };
            float alpha = settings.baseAlpha() * (center ? 0.92F : 0.72F)
                    * boundaryDistanceFade(
                    camera,
                    minX,
                    minZ,
                    maxX,
                    maxZ,
                    renderDistance,
                    settings.fadeStartFraction());
            if (alpha <= 0F) {
                continue;
            }
            float thickness = center ? 0.10F : 0.065F;
            if (dashed) {
                emitted += drawDashedHorizontalFrame(
                        positionMatrix,
                        builder,
                        minX,
                        boundaryY,
                        minZ,
                        maxX,
                        maxZ,
                        thickness,
                        color,
                        alpha);
            } else {
                emitted += drawHorizontalFrame(
                        positionMatrix,
                        builder,
                        minX,
                        boundaryY,
                        minZ,
                        maxX,
                        maxZ,
                        thickness,
                        color,
                        alpha);
            }
        }
        return emitted;
    }

    private boolean isInRenderableSection(
            LevelRenderContext context,
            WaterMarker marker,
            boolean throughWalls) {
        cullingPosition.set(marker.x(), marker.y(), marker.z());
        if (!throughWalls) {
            return context.levelRenderer().isSectionCompiledAndVisible(cullingPosition);
        }

        ViewArea viewArea = context.levelRenderer().viewArea();
        return viewArea != null && viewArea.getRenderSectionAt(cullingPosition) != null;
    }

    private static int estimateMarkerVertices(
            WaterMarker marker,
            WaterSourceConfig.RenderSettings settings,
            boolean highlightNearest) {
        int vertices = switch (settings.styleFor(marker)) {
            case BOX, PILLAR -> OUTLINED_BOX_VERTICES;
            case BEACON -> OUTLINED_BOX_VERTICES * 2;
            case HOLLOW -> HOLLOW_BOX_VERTICES;
            case STRIPES -> BOX_VERTICES * 3;
            case DOTS -> BOX_VERTICES * 4;
        };
        if (marker.waterlogged()) {
            vertices += switch (settings.waterloggedIndicator()) {
                case OFF -> 0;
                case CAP -> BOX_VERTICES;
                case CROSS -> BOX_VERTICES * 2;
            };
        }
        if (!marker.source() && settings.fluidLevelVisualization().usesGauge()) {
            vertices += HORIZONTAL_FRAME_VERTICES;
        }
        if (highlightNearest) {
            vertices += HORIZONTAL_FRAME_VERTICES * 2;
        }
        return vertices;
    }

    private static int renderMarker(
            Matrix4fc positionMatrix,
            VertexConsumer builder,
            WaterMarker marker,
            WaterSourceConfig.RenderSettings settings,
            WaterSourceConfig.RenderColor markerColor,
            float alpha,
            boolean highlightNearest) {
        float centerX = marker.x() + 0.5F;
        float centerZ = marker.z() + 0.5F;
        float baseY = marker.y();
        float displayHeight = marker.source()
                ? 1.0F
                : settings.fluidLevelVisualization().usesHeight() ? marker.height() : 0.65F;
        float topY = baseY + Math.max(0.18F, displayHeight);
        float border = settings.outlineBorder();
        WaterSourceConfig.RenderColor outlineColor = settings.outlineColor();
        float outlineAlpha = Math.min(1F, alpha * 1.05F);
        int emitted = 0;

        emitted += switch (settings.styleFor(marker)) {
            case PILLAR -> {
                float halfWidth = marker.source() ? 0.20F : 0.14F;
                yield drawOutlinedBox(positionMatrix, builder,
                        centerX - halfWidth, baseY + 0.02F, centerZ - halfWidth,
                        centerX + halfWidth, Math.max(baseY + 0.35F, topY), centerZ + halfWidth,
                        border,
                        outlineColor, outlineAlpha,
                        markerColor, alpha);
            }
            case BEACON -> drawOutlinedBox(positionMatrix, builder,
                    centerX - 0.38F, baseY + 0.02F, centerZ - 0.38F,
                    centerX + 0.38F, baseY + 0.18F, centerZ + 0.38F,
                    border,
                    outlineColor, outlineAlpha,
                    markerColor, alpha)
                    + drawOutlinedBox(positionMatrix, builder,
                    centerX - 0.11F, baseY + 0.16F, centerZ - 0.11F,
                    centerX + 0.11F, baseY + 0.72F + displayHeight * 0.48F, centerZ + 0.11F,
                    border,
                    outlineColor, outlineAlpha,
                    markerColor, alpha);
            case HOLLOW -> drawHollowBox(positionMatrix, builder,
                    marker.x() + 0.10F, baseY + 0.04F, marker.z() + 0.10F,
                    marker.x() + 0.90F, topY, marker.z() + 0.90F,
                    Math.max(0.035F, border * 1.25F),
                    markerColor, alpha);
            case STRIPES -> drawStripePattern(
                    positionMatrix,
                    builder,
                    centerX,
                    topY,
                    centerZ,
                    markerColor,
                    alpha);
            case DOTS -> drawDotPattern(
                    positionMatrix,
                    builder,
                    centerX,
                    topY,
                    centerZ,
                    markerColor,
                    alpha);
            case BOX -> drawOutlinedBox(positionMatrix, builder,
                    marker.x() + 0.07F, baseY + 0.03F, marker.z() + 0.07F,
                    marker.x() + 0.93F, topY, marker.z() + 0.93F,
                    border,
                    outlineColor, outlineAlpha,
                    markerColor, alpha);
        };

        if (marker.waterlogged()) {
            emitted += switch (settings.waterloggedIndicator()) {
                case OFF -> 0;
                case CAP -> drawBox(positionMatrix, builder,
                        centerX - 0.19F, topY + 0.025F, centerZ - 0.19F,
                        centerX + 0.19F, topY + 0.085F, centerZ + 0.19F,
                        outlineColor, outlineAlpha);
                case CROSS -> drawBox(positionMatrix, builder,
                        centerX - 0.30F, topY + 0.025F, centerZ - 0.055F,
                        centerX + 0.30F, topY + 0.085F, centerZ + 0.055F,
                        outlineColor, outlineAlpha)
                        + drawBox(positionMatrix, builder,
                        centerX - 0.055F, topY + 0.025F, centerZ - 0.30F,
                        centerX + 0.055F, topY + 0.085F, centerZ + 0.30F,
                        outlineColor, outlineAlpha);
            };
        }

        if (!marker.source() && settings.fluidLevelVisualization().usesGauge()) {
            float gaugeY = baseY + marker.height() + 0.015F;
            emitted += drawHorizontalFrame(positionMatrix, builder,
                    marker.x() + 0.035F, gaugeY, marker.z() + 0.035F,
                    marker.x() + 0.965F, marker.z() + 0.965F,
                    0.035F,
                    outlineColor,
                    outlineAlpha);
        }

        if (highlightNearest) {
            emitted += drawHorizontalFrame(positionMatrix, builder,
                    marker.x() - 0.08F, baseY - 0.015F, marker.z() - 0.08F,
                    marker.x() + 1.08F, marker.z() + 1.08F,
                    0.055F,
                    NEAREST_COLOR,
                    Math.min(1F, alpha * 1.18F));
            emitted += drawHorizontalFrame(positionMatrix, builder,
                    marker.x() - 0.08F, Math.max(baseY + 1.06F, topY + 0.13F), marker.z() - 0.08F,
                    marker.x() + 1.08F, marker.z() + 1.08F,
                    0.055F,
                    NEAREST_COLOR,
                    Math.min(1F, alpha * 1.18F));
        }
        return emitted;
    }

    private static int drawOutlinedBox(
            Matrix4fc positionMatrix,
            VertexConsumer builder,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float border,
            WaterSourceConfig.RenderColor outline,
            float outlineAlpha,
            WaterSourceConfig.RenderColor fill,
            float fillAlpha) {
        float smallestDimension = Math.min(maxX - minX, Math.min(maxY - minY, maxZ - minZ));
        float effectiveBorder = Math.min(border, Math.max(0F, smallestDimension * 0.45F));
        return drawBox(positionMatrix, builder,
                minX - effectiveBorder, minY - effectiveBorder, minZ - effectiveBorder,
                maxX + effectiveBorder, maxY + effectiveBorder, maxZ + effectiveBorder,
                outline, outlineAlpha)
                + drawBox(positionMatrix, builder,
                minX + effectiveBorder, minY + effectiveBorder, minZ + effectiveBorder,
                maxX - effectiveBorder, maxY - effectiveBorder, maxZ - effectiveBorder,
                fill, fillAlpha);
    }

    private static int drawHollowBox(
            Matrix4fc matrix,
            VertexConsumer builder,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float thickness,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        float edge = Math.min(thickness, Math.min(maxX - minX, maxZ - minZ) * 0.20F);
        int emitted = drawHorizontalFrame(matrix, builder,
                minX, minY, minZ, maxX, maxZ, edge, color, alpha);
        emitted += drawHorizontalFrame(matrix, builder,
                minX, maxY - edge, minZ, maxX, maxZ, edge, color, alpha);
        emitted += drawBox(matrix, builder,
                minX, minY + edge, minZ,
                minX + edge, maxY - edge, minZ + edge,
                color, alpha);
        emitted += drawBox(matrix, builder,
                maxX - edge, minY + edge, minZ,
                maxX, maxY - edge, minZ + edge,
                color, alpha);
        emitted += drawBox(matrix, builder,
                minX, minY + edge, maxZ - edge,
                minX + edge, maxY - edge, maxZ,
                color, alpha);
        emitted += drawBox(matrix, builder,
                maxX - edge, minY + edge, maxZ - edge,
                maxX, maxY - edge, maxZ,
                color, alpha);
        return emitted;
    }

    private static int drawStripePattern(
            Matrix4fc matrix,
            VertexConsumer builder,
            float centerX,
            float topY,
            float centerZ,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        int emitted = 0;
        for (int stripe = -1; stripe <= 1; stripe++) {
            float stripeX = centerX + stripe * 0.26F;
            emitted += drawBox(matrix, builder,
                    stripeX - 0.055F, topY, centerZ - 0.40F,
                    stripeX + 0.055F, topY + 0.07F, centerZ + 0.40F,
                    color, alpha);
        }
        return emitted;
    }

    private static int drawDotPattern(
            Matrix4fc matrix,
            VertexConsumer builder,
            float centerX,
            float topY,
            float centerZ,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        int emitted = 0;
        for (int xSign = -1; xSign <= 1; xSign += 2) {
            for (int zSign = -1; zSign <= 1; zSign += 2) {
                float x = centerX + xSign * 0.23F;
                float z = centerZ + zSign * 0.23F;
                emitted += drawBox(matrix, builder,
                        x - 0.075F, topY, z - 0.075F,
                        x + 0.075F, topY + 0.12F, z + 0.075F,
                        color, alpha);
            }
        }
        return emitted;
    }

    private static int drawHorizontalFrame(
            Matrix4fc matrix,
            VertexConsumer builder,
            float minX,
            float y,
            float minZ,
            float maxX,
            float maxZ,
            float thickness,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        float half = thickness * 0.5F;
        return drawBox(matrix, builder,
                minX, y - half, minZ,
                maxX, y + half, minZ + thickness,
                color, alpha)
                + drawBox(matrix, builder,
                minX, y - half, maxZ - thickness,
                maxX, y + half, maxZ,
                color, alpha)
                + drawBox(matrix, builder,
                minX, y - half, minZ + thickness,
                minX + thickness, y + half, maxZ - thickness,
                color, alpha)
                + drawBox(matrix, builder,
                maxX - thickness, y - half, minZ + thickness,
                maxX, y + half, maxZ - thickness,
                color, alpha);
    }

    private static int drawDashedHorizontalFrame(
            Matrix4fc matrix,
            VertexConsumer builder,
            float minX,
            float y,
            float minZ,
            float maxX,
            float maxZ,
            float thickness,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        float half = thickness * 0.5F;
        int emitted = 0;
        for (float offset = 0F; offset < maxX - minX; offset += 4F) {
            float segmentMaxX = Math.min(maxX, minX + offset + 2F);
            emitted += drawBox(matrix, builder,
                    minX + offset, y - half, minZ,
                    segmentMaxX, y + half, minZ + thickness,
                    color, alpha);
            emitted += drawBox(matrix, builder,
                    minX + offset, y - half, maxZ - thickness,
                    segmentMaxX, y + half, maxZ,
                    color, alpha);
        }
        for (float offset = 0F; offset < maxZ - minZ; offset += 4F) {
            float segmentMaxZ = Math.min(maxZ, minZ + offset + 2F);
            emitted += drawBox(matrix, builder,
                    minX, y - half, minZ + offset,
                    minX + thickness, y + half, segmentMaxZ,
                    color, alpha);
            emitted += drawBox(matrix, builder,
                    maxX - thickness, y - half, minZ + offset,
                    maxX, y + half, segmentMaxZ,
                    color, alpha);
        }
        return emitted;
    }

    private static int drawBox(
            Matrix4fc positionMatrix,
            VertexConsumer buffer,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            WaterSourceConfig.RenderColor color,
            float alpha) {
        float red = color.red();
        float green = color.green();
        float blue = color.blue();
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        return BOX_VERTICES;
    }

    private static double horizontalDistanceSquared(
            Vec3 camera,
            float minX,
            float minZ,
            float maxX,
            float maxZ) {
        double dx = distanceOutside(camera.x, minX, maxX);
        double dz = distanceOutside(camera.z, minZ, maxZ);
        return dx * dx + dz * dz;
    }

    private static float boundaryDistanceFade(
            Vec3 camera,
            float minX,
            float minZ,
            float maxX,
            float maxZ,
            float renderDistance,
            float fadeStartFraction) {
        if (fadeStartFraction >= 1F) {
            return 1F;
        }
        double distance = Math.sqrt(horizontalDistanceSquared(camera, minX, minZ, maxX, maxZ));
        double fadeStart = renderDistance * Math.max(0F, fadeStartFraction);
        if (distance <= fadeStart) {
            return 1F;
        }
        if (distance >= renderDistance) {
            return 0F;
        }
        return (float) (1D - (distance - fadeStart) / (renderDistance - fadeStart));
    }

    private static double distanceOutside(double value, double minimum, double maximum) {
        if (value < minimum) {
            return minimum - value;
        }
        if (value > maximum) {
            return value - maximum;
        }
        return 0D;
    }

    private static long packMetrics(int visibleMarkers, int vertices) {
        return ((long) visibleMarkers << 32) | (vertices & 0xFFFF_FFFFL);
    }

    private static int unpackVertexCount(long metrics) {
        return (int) metrics;
    }

    int visibleMarkerCount() {
        return (int) (renderedMetrics >>> 32);
    }

    int renderedVertexCount() {
        return unpackVertexCount(renderedMetrics);
    }

    private static void draw(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(),
                COLOR_MODULATOR,
                MODEL_OFFSET,
                TEXTURE_MATRIX);
        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) {
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "WaterSourceMod marker rendering",
                        colorTexture,
                        Optional.empty(),
                        mainTarget.getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    private void clearExtractedState() {
        renderScanArea = ScanAreaSnapshot.empty();
        renderNearest = NearestWaterMarker.none();
        renderedMetrics = 0;
        renderMarkers = List.of();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            clearExtractedState();
            stagedBuffer.close();
        }
    }
}
