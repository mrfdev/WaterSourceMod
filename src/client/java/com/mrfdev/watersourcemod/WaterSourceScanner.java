package com.mrfdev.watersourcemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Incrementally scans only chunks already available in the client chunk cache.
 * All methods are called on the client thread.
 */
public final class WaterSourceScanner {
    private final Deque<ChunkWork> pendingChunks = new ArrayDeque<>();
    private final List<WaterMarker> sourceMarkers = new ArrayList<>();
    private final List<WaterMarker> flowingMarkers = new ArrayList<>();
    private volatile List<WaterMarker> markerSnapshot = List.of();
    private volatile ScanStatus status = ScanStatus.idle();

    private ClientLevel scannedLevel;
    private ChunkWork activeChunk;
    private boolean enabled;
    private boolean scanRequested;
    private long clientTick;
    private long nextAutomaticScanTick;
    private long processedBlocks;
    private long totalBlocks;
    private int centerChunkX;
    private int centerChunkZ;
    private int lastConfigSignature;
    private int requestedChunkCount;
    private int loadedChunkCount;
    private int worldMinY;
    private int worldMaxY;
    private boolean markerLimitReachedDuringScan;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        } else {
            requestFullScan();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void requestFullScan() {
        if (enabled) {
            scanRequested = true;
        }
    }

    public void onConfigChanged() {
        requestFullScan();
    }

    public void tick(Minecraft client, WaterSourceConfig config) {
        clientTick++;
        if (!enabled) {
            return;
        }

        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            clear();
            return;
        }

        if (scannedLevel != level) {
            scannedLevel = level;
            requestFullScan();
        }

        int playerChunkX = client.player.blockPosition().getX() >> 4;
        int playerChunkZ = client.player.blockPosition().getZ() >> 4;
        if (activeChunk == null && (playerChunkX != centerChunkX || playerChunkZ != centerChunkZ)) {
            requestFullScan();
        }

        int signature = configSignature(config);
        if (signature != lastConfigSignature) {
            lastConfigSignature = signature;
            requestFullScan();
        }

        if (activeChunk == null && pendingChunks.isEmpty() && !scanRequested
                && clientTick >= nextAutomaticScanTick) {
            scanRequested = true;
        }

        if (scanRequested && activeChunk == null) {
            startScan(level, playerChunkX, playerChunkZ, config);
        }

        int budget = config.getScanBudgetPerTick();
        while (budget > 0 && activeChunk != null) {
            int consumed = activeChunk.scan(level, config, budget);
            budget -= Math.max(1, consumed);
            processedBlocks += consumed;
            if (activeChunk.complete()) {
                if (activeChunk.loaded()) {
                    loadedChunkCount++;
                }
                activeChunk = pendingChunks.pollFirst();
                if (activeChunk == null) {
                    finishScan(config);
                }
            }
        }
    }

    public List<WaterMarker> markerSnapshot() {
        return markerSnapshot;
    }

    public ScanStatus status() {
        return status;
    }

    private void startScan(ClientLevel level, int playerChunkX, int playerChunkZ, WaterSourceConfig config) {
        pendingChunks.clear();
        sourceMarkers.clear();
        flowingMarkers.clear();
        markerSnapshot = List.of();
        processedBlocks = 0;
        loadedChunkCount = 0;
        markerLimitReachedDuringScan = false;
        centerChunkX = playerChunkX;
        centerChunkZ = playerChunkZ;
        worldMinY = level.getMinY();
        worldMaxY = level.getMaxY();
        long height = Math.max(0L, (long) worldMaxY - worldMinY);
        int radius = config.getChunkRadius();
        requestedChunkCount = (radius * 2 + 1) * (radius * 2 + 1);
        totalBlocks = height * requestedChunkCount;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                pendingChunks.addLast(new ChunkWork(
                        centerChunkX + dx,
                        centerChunkZ + dz,
                        level.getChunkSource().getChunkNow(centerChunkX + dx, centerChunkZ + dz),
                        worldMinY,
                        worldMaxY));
            }
        }
        scanRequested = false;
        activeChunk = pendingChunks.pollFirst();
        status = new ScanStatus(true, 0, totalBlocks, 0, 0, false, 0, requestedChunkCount);
    }

    private void finishScan(WaterSourceConfig config) {
        int maxMarkers = config.getMaxMarkers();
        List<WaterMarker> combined = new ArrayList<>(Math.min(maxMarkers, sourceMarkers.size() + flowingMarkers.size()));
        int sourceCount = Math.min(sourceMarkers.size(), maxMarkers);
        combined.addAll(sourceMarkers.subList(0, sourceCount));
        int remaining = maxMarkers - combined.size();
        int flowingCount = Math.min(flowingMarkers.size(), remaining);
        combined.addAll(flowingMarkers.subList(0, flowingCount));
        markerSnapshot = List.copyOf(combined);
        boolean markerLimitReached = markerLimitReachedDuringScan
                || sourceMarkers.size() + flowingMarkers.size() > maxMarkers;
        status = new ScanStatus(
                false,
                totalBlocks,
                totalBlocks,
                sourceCount,
                flowingCount,
                markerLimitReached,
                loadedChunkCount,
                requestedChunkCount);
        nextAutomaticScanTick = clientTick + config.getRescanIntervalTicks();
    }

    private void clear() {
        pendingChunks.clear();
        activeChunk = null;
        sourceMarkers.clear();
        flowingMarkers.clear();
        markerSnapshot = List.of();
        status = ScanStatus.idle();
        scanRequested = false;
        scannedLevel = null;
    }

    private static int configSignature(WaterSourceConfig config) {
        int result = config.getChunkRadius();
        result = 31 * result + (config.isShowSources() ? 1 : 0);
        result = 31 * result + (config.isShowFlowing() ? 1 : 0);
        result = 31 * result + (config.isIncludeWaterloggedSources() ? 1 : 0);
        result = 31 * result + config.getMaxMarkers();
        return result;
    }

    public record ScanStatus(
            boolean scanning,
            long processedBlocks,
            long totalBlocks,
            int sourceCount,
            int flowingCount,
            boolean markerLimitReached,
            int loadedChunkCount,
            int requestedChunkCount) {
        private static ScanStatus idle() {
            return new ScanStatus(false, 0, 0, 0, 0, false, 0, 0);
        }

        public int markerCount() {
            return sourceCount + flowingCount;
        }

        public int progressPercent() {
            if (totalBlocks <= 0) {
                return 100;
            }
            return (int) Math.min(100L, processedBlocks * 100L / totalBlocks);
        }
    }

    private final class ChunkWork {
        private final int chunkX;
        private final int chunkZ;
        private final LevelChunk chunk;
        private final int minY;
        private final int maxY;
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private int y;
        private int localX;
        private int localZ;
        private boolean finished;

        private ChunkWork(int chunkX, int chunkZ, LevelChunk chunk, int minY, int maxY) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunk = chunk;
            this.minY = minY;
            this.maxY = maxY;
            this.y = minY;
        }

        private int scan(ClientLevel level, WaterSourceConfig config, int budget) {
            if (chunk == null) {
                finished = true;
                return Math.max(1, (maxY - minY) * 256);
            }

            int consumed = 0;
            while (consumed < budget && !finished) {
                // Set the absolute position once per block; the chunk performs the local lookup.
                position.set((chunkX << 4) + localX, y, (chunkZ << 4) + localZ);
                inspect(config);
                y++;
                consumed++;
                if (y >= maxY) {
                    y = minY;
                    localX++;
                    if (localX >= 16) {
                        localX = 0;
                        localZ++;
                        if (localZ >= 16) {
                            finished = true;
                        }
                    }
                }
            }
            return consumed;
        }

        private void inspect(WaterSourceConfig config) {
            BlockState blockState = chunk.getBlockState(position);
            FluidState fluidState = blockState.getFluidState();
            if (fluidState.getType() != Fluids.WATER && fluidState.getType() != Fluids.FLOWING_WATER) {
                return;
            }

            boolean waterlogged = blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && blockState.getValue(BlockStateProperties.WATERLOGGED);
            if (waterlogged && !config.isIncludeWaterloggedSources()) {
                return;
            }

            boolean source = fluidState.isSource();
            WaterMarker marker = new WaterMarker(
                    position.getX(),
                    position.getY(),
                    position.getZ(),
                    source,
                    waterlogged,
                    fluidState.getOwnHeight());
            if (source) {
                if (config.isShowSources()) {
                    if (sourceMarkers.size() < config.getMaxMarkers()) {
                        sourceMarkers.add(marker);
                    } else {
                        markerLimitReachedDuringScan = true;
                    }
                }
            } else if (config.isShowFlowing()) {
                if (flowingMarkers.size() < config.getMaxMarkers()) {
                    flowingMarkers.add(marker);
                } else {
                    markerLimitReachedDuringScan = true;
                }
            }
        }

        private boolean complete() {
            return finished;
        }

        private boolean loaded() {
            return chunk != null;
        }
    }
}
