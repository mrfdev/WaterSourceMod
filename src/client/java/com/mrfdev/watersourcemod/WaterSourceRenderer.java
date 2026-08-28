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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Renders the immutable marker snapshot using Minecraft 26.2's render phases. */
public final class WaterSourceRenderer implements AutoCloseable {
    private static final RenderPipeline DEPTH_AWARE_PIPELINE = RenderPipelines.DEBUG_FILLED_BOX;

    /*
     * The exact 26.2 client exposes the no-depth GUI position/color pipeline as
     * a public built-in pipeline, while DEBUG_FILLED_SNIPPET is private. It has
     * the same position/color quad binding needed here and is used only after
     * the player explicitly enables through-wall visibility.
     */
    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.GUI;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1F, 1F, 1F, 1F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED_BUFFER = new StagedVertexBuffer(
            () -> "WaterSourceMod marker buffer",
            RenderType.BIG_BUFFER_SIZE);

    private volatile List<WaterMarker> renderMarkers = List.of();
    private volatile WaterSourceConfig renderConfig = WaterSourceConfig.defaults();
    private volatile float animationTime;
    private volatile boolean closed;

    public void register() {
        LevelExtractionEvents.END_EXTRACTION.register(this::extractMarkers);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::renderAndDrawMarkers);
    }

    private void extractMarkers(LevelExtractionContext context) {
        if (closed) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.level != context.level() || !WaterSourceModClient.scanner().isEnabled()) {
            renderMarkers = List.of();
            return;
        }

        renderMarkers = WaterSourceModClient.scanner().markerSnapshot();
        renderConfig = WaterSourceModClient.config().copy();
        animationTime = context.level().getGameTime() + context.deltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private void renderAndDrawMarkers(LevelRenderContext context) {
        if (closed || renderMarkers.isEmpty()) {
            return;
        }

        WaterSourceConfig config = renderConfig;
        RenderPipeline pipeline = config.isThroughWalls() ? THROUGH_WALL_PIPELINE : DEPTH_AWARE_PIPELINE;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) {
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                format,
                primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);
        renderMarkers(context, draw, config);
        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null) {
            draw(Minecraft.getInstance(), info, pipeline);
        }
        STAGED_BUFFER.endFrame();
    }

    private void renderMarkers(LevelRenderContext context, StagedVertexBuffer.Draw draw, WaterSourceConfig config) {
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4fc positionMatrix = matrices.last().pose();
        for (WaterMarker marker : renderMarkers) {
            float[] color = color(marker.source() ? config.getSourceColor() : config.getFlowingColor());
            float alpha = config.getOpacityPercent() / 100F;
            if (config.isPulse()) {
                alpha *= 0.90F + 0.10F * (float) Math.sin(animationTime * 0.13F + marker.x() * 0.07F + marker.z() * 0.03F);
            }
            renderMarker(positionMatrix, builder, marker, config, color, alpha);
        }
        matrices.popPose();
    }

    private static void renderMarker(
            Matrix4fc positionMatrix,
            VertexConsumer builder,
            WaterMarker marker,
            WaterSourceConfig config,
            float[] color,
            float alpha) {
        float centerX = marker.x() + 0.5F;
        float centerZ = marker.z() + 0.5F;
        float baseY = marker.y();
        float waterHeight = marker.source() ? 1.0F : marker.height();
        float border = config.getOutlineThickness() * 0.018F;
        float[] outline = color(config.getOutlineColor());
        float outlineAlpha = Math.min(1F, alpha * 1.05F);

        switch (config.getMarkerStyle()) {
            case PILLAR -> {
                float halfWidth = marker.source() ? 0.20F : 0.14F;
                float top = baseY + Math.max(0.35F, waterHeight);
                drawOutlinedBox(positionMatrix, builder,
                        centerX - halfWidth, baseY + 0.02F, centerZ - halfWidth,
                        centerX + halfWidth, top, centerZ + halfWidth,
                        border, outline, outlineAlpha, color, alpha);
            }
            case BEACON -> {
                drawOutlinedBox(positionMatrix, builder,
                        centerX - 0.38F, baseY + 0.02F, centerZ - 0.38F,
                        centerX + 0.38F, baseY + 0.18F, centerZ + 0.38F,
                        border, outline, outlineAlpha, color, alpha);
                drawOutlinedBox(positionMatrix, builder,
                        centerX - 0.11F, baseY + 0.16F, centerZ - 0.11F,
                        centerX + 0.11F, baseY + 0.92F + waterHeight * 0.28F, centerZ + 0.11F,
                        border, outline, outlineAlpha, color, alpha);
            }
            case BOX -> drawOutlinedBox(positionMatrix, builder,
                    marker.x() + 0.07F, baseY + 0.03F, marker.z() + 0.07F,
                    marker.x() + 0.93F, baseY + Math.max(0.18F, waterHeight), marker.z() + 0.93F,
                    border, outline, outlineAlpha, color, alpha);
        }
    }

    private static void drawOutlinedBox(
            Matrix4fc positionMatrix,
            VertexConsumer builder,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float border,
            float[] outline,
            float outlineAlpha,
            float[] color,
            float alpha) {
        drawBox(positionMatrix, builder,
                minX - border, minY - border, minZ - border,
                maxX + border, maxY + border, maxZ + border,
                outline[0], outline[1], outline[2], outlineAlpha);
        drawBox(positionMatrix, builder,
                minX + border, minY + border, minZ + border,
                maxX - border, maxY - border, maxZ - border,
                color[0], color[1], color[2], alpha);
    }

    private static void drawBox(
            Matrix4fc positionMatrix,
            VertexConsumer buffer,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float red,
            float green,
            float blue,
            float alpha) {
        // Front face
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        // Back face
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        // Left face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // Right face
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        // Top face
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // Bottom face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    private static float[] color(int rgb) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255F,
                ((rgb >> 8) & 0xFF) / 255F,
                (rgb & 0xFF) / 255F};
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

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            STAGED_BUFFER.close();
        }
    }
}
