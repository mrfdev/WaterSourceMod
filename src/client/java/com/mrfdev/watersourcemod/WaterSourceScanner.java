package com.mrfdev.watersourcemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Incrementally scans only chunks already available in the client chunk cache.
 * All methods are called on the client thread.
 */
public final class WaterSourceScanner {
    private static final int CHUNK_WIDTH = 16;
    private static final int BLOCKS_PER_CHUNK_LAYER = CHUNK_WIDTH * CHUNK_WIDTH;
    private static final int TIME_CHECK_SLICE = 256;
    private static final long BLOCK_UPDATE_DEBOUNCE_TICKS = 5;
    private static final long BLOCK_UPDATE_MAX_DELAY_TICKS = 20;
    private static final long CHUNK_LIFECYCLE_DEBOUNCE_TICKS = 5;
    private static final long CHUNK_LIFECYCLE_MAX_DELAY_TICKS = 20;

    private final Deque<ChunkWork> pendingChunks = new ArrayDeque<>();
    private final Set<Long> pendingChunkKeys = new HashSet<>();
    private final ChunkLifecycleRescanGate chunkLifecycleRescan = new ChunkLifecycleRescanGate();
    private final BlockPos.MutableBlockPos scanPosition = new BlockPos.MutableBlockPos();
    private final WaterMarkerSnapshot markerSnapshot = new WaterMarkerSnapshot();
    private volatile ScanStatus status = ScanStatus.idle();
    private volatile ScanAreaSnapshot scanAreaSnapshot = ScanAreaSnapshot.empty();

    private ClientLevel scannedLevel;
    private ChunkWork activeChunk;
    private WaterMarkerAccumulator scanMarkers;
    private byte[] scanChunkStates;
    private WaterSourceConfig observedConfig;
    private WaterSourceConfig.ScanSettings scanSettings;
    private WaterSourceConfig.RescanMode rescanMode = WaterSourceConfig.RescanMode.AUTOMATIC;
    private int observedRescanIntervalTicks = 80;
    private boolean renderScanArea;
    private volatile boolean enabled;
    private boolean scanRequested;
    private boolean hasScanCenter;
    private boolean blockUpdatePending;
    private long activeSnapshotGeneration;
    private long clientTick;
    private long nextAutomaticScanTick;
    private long blockUpdateDueTick;
    private long blockUpdateDeadlineTick;
    private long processedBlocks;
    private long inspectedBlocks;
    private long totalBlocks;
    private long scanStartNanos;
    private long completedScanSequence;
    private int centerChunkX;
    private int centerChunkZ;
    private int centerBlockY;
    private int requestedChunkCount;
    private int loadedChunkCount;
    private int skippedChunkCount;
    private int worldMinY;
    private int worldMaxY;

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

    public void onLevelChanged(Minecraft client, ClientLevel level) {
        // The callback runs before another render can legitimately reuse the old
        // world's marker positions. The next tick establishes the new scan.
        clear();
    }

    /** Cancels partial work and starts a fresh scan on the next scanner tick. */
    public void requestFullScan() {
        if (enabled) {
            blockUpdatePending = false;
            chunkLifecycleRescan.clear();
            scanRequested = true;
            cancelActiveScan();
        }
    }

    /** Debounces relevant client-world updates in the opt-in block-driven mode. */
    public void onBlockUpdated(ClientLevel level, BlockPos position) {
        if (!enabled
                || rescanMode != WaterSourceConfig.RescanMode.BLOCK_UPDATES
                || scannedLevel != level
                || !hasScanCenter
                || scanSettings == null
                || position.getY() < worldMinY
                || position.getY() >= worldMaxY
                || !isWithinCurrentScan(position.getX() >> 4, position.getZ() >> 4)) {
            return;
        }

        BlockUpdateSchedule schedule = nextBlockUpdateSchedule(
                clientTick,
                blockUpdatePending,
                blockUpdateDeadlineTick);
        blockUpdatePending = true;
        blockUpdateDueTick = schedule.dueTick();
        blockUpdateDeadlineTick = schedule.deadlineTick();
    }

    public void onChunkLoaded(ClientLevel level, LevelChunk chunk) {
        if (!enabled || scannedLevel != level || !hasScanCenter || scanSettings == null) {
            return;
        }

        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        if (isWithinCurrentScan(chunkX, chunkZ) && !hasPendingWorkFor(chunkX, chunkZ)) {
            // This pass already skipped the chunk, so include it in a follow-up pass.
            chunkLifecycleRescan.schedule(clientTick);
        }
    }

    public void onChunkUnloaded(ClientLevel level, LevelChunk chunk) {
        if (!enabled || scannedLevel != level) {
            return;
        }

        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        if (!hasScanCenter || scanSettings == null || !isWithinCurrentScan(chunkX, chunkZ)) {
            return;
        }

        // Removing a loaded chunk changes the snapshot's validity even when it
        // contained no markers, so invalidate any in-flight publication too.
        markerSnapshot.invalidateChunk(chunkX, chunkZ);
        scanAreaSnapshot = ScanAreaSnapshot.empty();
        chunkLifecycleRescan.schedule(clientTick);
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
            resetForLevel(level);
        }

        observeConfig(config);

        int playerChunkX = client.player.blockPosition().getX() >> 4;
        int playerChunkZ = client.player.blockPosition().getZ() >> 4;
        int playerY = client.player.blockPosition().getY();
        if (hasScanCenter && (playerChunkX != centerChunkX
                || playerChunkZ != centerChunkZ
                || scanSettings.verticalRange().shouldRecenter(playerY, centerBlockY))) {
            // A snapshot centered elsewhere is not valid for the configured scan radius.
            markerSnapshot.invalidate();
            scanAreaSnapshot = ScanAreaSnapshot.empty();
            hasScanCenter = false;
            scanRequested = true;
            blockUpdatePending = false;
            chunkLifecycleRescan.clear();
            cancelActiveScan();
        } else if (!hasScanCenter) {
            scanRequested = true;
        }

        if (chunkLifecycleRescan.consumeIfDue(clientTick)) {
            blockUpdatePending = false;
            scanRequested = true;
            cancelActiveScan();
        }

        if (blockUpdatePending
                && rescanMode == WaterSourceConfig.RescanMode.BLOCK_UPDATES
                && clientTick >= blockUpdateDueTick) {
            blockUpdatePending = false;
            chunkLifecycleRescan.clear();
            scanRequested = true;
            cancelActiveScan();
        }

        if (activeChunk == null && pendingChunks.isEmpty() && !scanRequested
                && rescanMode == WaterSourceConfig.RescanMode.AUTOMATIC
                && clientTick >= nextAutomaticScanTick) {
            scanRequested = true;
        }

        if (scanRequested && activeChunk == null && scanSettings != null) {
            startScan(
                    level,
                    playerChunkX,
                    playerChunkZ,
                    playerY,
                    scanSettings,
                    renderScanArea);
        }

        int budget = config.getScanBudgetPerTick();
        long deadline = scanDeadline(System.nanoTime(), config.getScanTimeBudgetMillis());
        boolean didWork = false;
        while (budget > 0
                && activeChunk != null
                && (!didWork || System.nanoTime() < deadline)) {
            int sliceBudget = Math.min(budget, TIME_CHECK_SLICE);
            ScanSlice slice = activeChunk.scan(level, scanSettings, scanMarkers, sliceBudget);
            budget -= Math.max(1, slice.budgetUsed());
            didWork = true;
            processedBlocks = Math.min(totalBlocks, processedBlocks + slice.positionsProcessed());
            inspectedBlocks = Math.min(totalBlocks, inspectedBlocks + slice.blocksInspected());
            if (activeChunk.complete()) {
                pendingChunkKeys.remove(chunkKey(activeChunk.chunkX, activeChunk.chunkZ));
                if (activeChunk.loaded()) {
                    loadedChunkCount++;
                    if (scanChunkStates != null) {
                        scanChunkStates[activeChunk.index()] = ScanAreaSnapshot.ChunkState.LOADED.code();
                    }
                } else {
                    skippedChunkCount++;
                    if (scanChunkStates != null) {
                        scanChunkStates[activeChunk.index()] = ScanAreaSnapshot.ChunkState.SKIPPED.code();
                    }
                }
                activeChunk.release();
                activeChunk = pendingChunks.pollFirst();
                if (activeChunk == null) {
                    finishScan(config);
                }
            }
        }

        if (activeChunk != null) {
            publishProgress();
        }
    }

    public List<WaterMarker> markerSnapshot() {
        return markerSnapshot.currentMarkers();
    }

    public ScanStatus status() {
        return status;
    }

    ScanAreaSnapshot scanAreaSnapshot() {
        return scanAreaSnapshot;
    }

    long completedScanSequence() {
        return completedScanSequence;
    }

    private void observeConfig(WaterSourceConfig config) {
        if (observedConfig == config) {
            return;
        }

        WaterSourceConfig.RescanMode previousRescanMode = rescanMode;
        int previousRescanIntervalTicks = observedRescanIntervalTicks;
        observedConfig = config;
        rescanMode = config.getRescanMode();
        observedRescanIntervalTicks = config.getRescanIntervalTicks();
        boolean updatedRenderScanArea = config.getScanBoundaryMode()
                != WaterSourceConfig.ScanBoundaryMode.OFF;
        boolean renderScanAreaChanged = renderScanArea != updatedRenderScanArea;
        renderScanArea = updatedRenderScanArea;
        if (rescanMode != previousRescanMode
                || (rescanMode == WaterSourceConfig.RescanMode.AUTOMATIC
                && observedRescanIntervalTicks != previousRescanIntervalTicks)) {
            blockUpdatePending = false;
            nextAutomaticScanTick = rescanMode == WaterSourceConfig.RescanMode.AUTOMATIC
                    ? clientTick + observedRescanIntervalTicks
                    : Long.MAX_VALUE;
        }
        WaterSourceConfig.ScanSettings updatedSettings = config.scanSettings();
        if (!updatedSettings.equals(scanSettings) || renderScanAreaChanged) {
            if (scanSettings != null
                    && (updatedSettings.chunkRadius() != scanSettings.chunkRadius()
                    || updatedSettings.verticalRange() != scanSettings.verticalRange())) {
                // A completed snapshot from a wider radius may contain markers that
                // no longer belong to the configured search area.
                markerSnapshot.invalidate();
                scanAreaSnapshot = ScanAreaSnapshot.empty();
                hasScanCenter = false;
            }
            if (!renderScanArea) {
                scanAreaSnapshot = ScanAreaSnapshot.empty();
            }
            scanSettings = updatedSettings;
            scanRequested = true;
            blockUpdatePending = false;
            chunkLifecycleRescan.clear();
            cancelActiveScan();
        }
    }

    private void resetForLevel(ClientLevel level) {
        chunkLifecycleRescan.clear();
        cancelActiveScan();
        if (scannedLevel != null || !markerSnapshot.currentMarkers().isEmpty()) {
            markerSnapshot.invalidate();
        }
        scanAreaSnapshot = ScanAreaSnapshot.empty();
        scannedLevel = level;
        hasScanCenter = false;
        scanRequested = true;
    }

    private void startScan(
            ClientLevel level,
            int playerChunkX,
            int playerChunkZ,
            int playerY,
            WaterSourceConfig.ScanSettings settings,
            boolean renderScanArea) {
        pendingChunks.clear();
        pendingChunkKeys.clear();
        processedBlocks = 0;
        inspectedBlocks = 0;
        loadedChunkCount = 0;
        skippedChunkCount = 0;
        scanStartNanos = System.nanoTime();
        centerChunkX = playerChunkX;
        centerChunkZ = playerChunkZ;
        centerBlockY = playerY;
        hasScanCenter = true;
        WaterSourceConfig.ScanHeight scanHeight = settings.verticalRange().bounds(
                playerY,
                level.getMinY(),
                level.getMaxY());
        worldMinY = scanHeight.minY();
        worldMaxY = scanHeight.maxY();
        int radius = settings.chunkRadius();
        requestedChunkCount = (radius * 2 + 1) * (radius * 2 + 1);
        scanChunkStates = renderScanArea ? new byte[requestedChunkCount] : null;
        totalBlocks = totalBlockPositions(worldMinY, worldMaxY, requestedChunkCount);
        scanRequested = false;
        if (!settings.showSources() && !settings.showFlowing() && !renderScanArea) {
            markerSnapshot.invalidate();
            scanAreaSnapshot = ScanAreaSnapshot.empty();
            scanMarkers = null;
            scanChunkStates = null;
            activeChunk = null;
            status = ScanStatus.idle();
            nextAutomaticScanTick = Long.MAX_VALUE;
            return;
        }

        activeSnapshotGeneration = markerSnapshot.generation();
        scanMarkers = new WaterMarkerAccumulator(settings.maxMarkers());
        if (renderScanArea && !scanAreaSnapshot.matchesScope(
                centerChunkX,
                centerChunkZ,
                radius,
                worldMinY,
                worldMaxY)) {
            scanAreaSnapshot = ScanAreaSnapshot.pending(
                    centerChunkX,
                    centerChunkZ,
                    radius,
                    worldMinY,
                    worldMaxY);
        }
        int chunkIndex = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                pendingChunks.addLast(new ChunkWork(
                        chunkIndex++,
                        centerChunkX + dx,
                        centerChunkZ + dz,
                        worldMinY,
                        worldMaxY));
                pendingChunkKeys.add(chunkKey(centerChunkX + dx, centerChunkZ + dz));
            }
        }
        activeChunk = pendingChunks.pollFirst();
        status = new ScanStatus(
                true,
                0,
                totalBlocks,
                0,
                0,
                0,
                false,
                0,
                0,
                requestedChunkCount,
                0,
                0,
                markerSnapshot.hasPublishedSnapshot());
    }

    private void publishProgress() {
        WaterMarkerAccumulator markers = scanMarkers;
        status = new ScanStatus(
                true,
                processedBlocks,
                totalBlocks,
                inspectedBlocks,
                markers == null ? 0 : markers.sourceCount(),
                markers == null ? 0 : markers.flowingCount(),
                markers != null && markers.limitReached(),
                loadedChunkCount,
                skippedChunkCount,
                requestedChunkCount,
                elapsedScanNanos(),
                markers == null ? 0 : markers.discardedCount(),
                markerSnapshot.hasPublishedSnapshot());
    }

    private void finishScan(WaterSourceConfig config) {
        WaterMarkerAccumulator markers = scanMarkers;
        if (markers == null) {
            return;
        }

        if (!markerSnapshot.publish(activeSnapshotGeneration, markers.markersWithSourcesFirst())) {
            scanMarkers = null;
            scanChunkStates = null;
            scanRequested = false;
            status = ScanStatus.idle();
            nextAutomaticScanTick = Long.MAX_VALUE;
            chunkLifecycleRescan.schedule(clientTick);
            return;
        }
        if (scanChunkStates != null) {
            scanAreaSnapshot = ScanAreaSnapshot.completed(
                    centerChunkX,
                    centerChunkZ,
                    scanSettings.chunkRadius(),
                    worldMinY,
                    worldMaxY,
                    scanChunkStates);
        }
        completedScanSequence++;
        long durationNanos = elapsedScanNanos();
        status = new ScanStatus(
                false,
                totalBlocks,
                totalBlocks,
                inspectedBlocks,
                markers.sourceCount(),
                markers.flowingCount(),
                markers.limitReached(),
                loadedChunkCount,
                skippedChunkCount,
                requestedChunkCount,
                durationNanos,
                markers.discardedCount(),
                false);
        scanMarkers = null;
        scanChunkStates = null;
        nextAutomaticScanTick = rescanMode == WaterSourceConfig.RescanMode.AUTOMATIC
                ? clientTick + config.getRescanIntervalTicks()
                : Long.MAX_VALUE;
    }

    private void cancelActiveScan() {
        pendingChunks.clear();
        pendingChunkKeys.clear();
        if (activeChunk != null) {
            activeChunk.release();
            activeChunk = null;
        }
        scanMarkers = null;
        scanChunkStates = null;
        processedBlocks = 0;
        inspectedBlocks = 0;
        totalBlocks = 0;
        requestedChunkCount = 0;
        loadedChunkCount = 0;
        skippedChunkCount = 0;
        scanStartNanos = 0;
        status = ScanStatus.idle();
    }

    private void clear() {
        boolean hadSnapshotScope = scannedLevel != null || !markerSnapshot.currentMarkers().isEmpty();
        cancelActiveScan();
        if (hadSnapshotScope) {
            markerSnapshot.invalidate();
        }
        scanAreaSnapshot = ScanAreaSnapshot.empty();
        scanRequested = false;
        scannedLevel = null;
        observedConfig = null;
        scanSettings = null;
        rescanMode = WaterSourceConfig.RescanMode.AUTOMATIC;
        observedRescanIntervalTicks = 80;
        renderScanArea = false;
        hasScanCenter = false;
        blockUpdatePending = false;
        chunkLifecycleRescan.clear();
        nextAutomaticScanTick = 0;
    }

    private long elapsedScanNanos() {
        if (scanStartNanos == 0) {
            return 0;
        }
        return Math.max(0L, System.nanoTime() - scanStartNanos);
    }

    private boolean isWithinCurrentScan(int chunkX, int chunkZ) {
        int radius = scanSettings.chunkRadius();
        return Math.abs((long) chunkX - centerChunkX) <= radius
                && Math.abs((long) chunkZ - centerChunkZ) <= radius;
    }

    private boolean hasPendingWorkFor(int chunkX, int chunkZ) {
        return pendingChunkKeys.contains(chunkKey(chunkX, chunkZ));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return WaterMarkerSnapshot.chunkKey(chunkX, chunkZ);
    }

    static long totalBlockPositions(int minY, int maxY, int chunkCount) {
        long height = Math.max(0L, (long) maxY - minY);
        return height * BLOCKS_PER_CHUNK_LAYER * Math.max(0, chunkCount);
    }

    static long scanDeadline(long startNanos, int budgetMillis) {
        long durationNanos = Math.max(1L, budgetMillis) * 1_000_000L;
        try {
            return Math.addExact(startNanos, durationNanos);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    static BlockUpdateSchedule nextBlockUpdateSchedule(
            long currentTick,
            boolean alreadyPending,
            long existingDeadlineTick) {
        long deadlineTick = alreadyPending
                ? existingDeadlineTick
                : currentTick + BLOCK_UPDATE_MAX_DELAY_TICKS;
        long dueTick = Math.min(currentTick + BLOCK_UPDATE_DEBOUNCE_TICKS, deadlineTick);
        return new BlockUpdateSchedule(dueTick, deadlineTick);
    }

    record BlockUpdateSchedule(long dueTick, long deadlineTick) {
    }

    static final class ChunkLifecycleRescanGate {
        private boolean pending;
        private long dueTick;
        private long deadlineTick;

        void schedule(long currentTick) {
            if (!pending) {
                pending = true;
                deadlineTick = saturatingAdd(currentTick, CHUNK_LIFECYCLE_MAX_DELAY_TICKS);
            }
            dueTick = Math.min(
                    saturatingAdd(currentTick, CHUNK_LIFECYCLE_DEBOUNCE_TICKS),
                    deadlineTick);
        }

        boolean consumeIfDue(long currentTick) {
            if (!pending || currentTick < dueTick) {
                return false;
            }
            clear();
            return true;
        }

        void clear() {
            pending = false;
            dueTick = 0;
            deadlineTick = 0;
        }

        boolean pending() {
            return pending;
        }

        long dueTick() {
            return dueTick;
        }

        long deadlineTick() {
            return deadlineTick;
        }

        private static long saturatingAdd(long value, long increment) {
            try {
                return Math.addExact(value, increment);
            } catch (ArithmeticException ignored) {
                return Long.MAX_VALUE;
            }
        }
    }

    public record ScanStatus(
            boolean scanning,
            long processedBlocks,
            long totalBlocks,
            long inspectedBlocks,
            int sourceCount,
            int flowingCount,
            boolean markerLimitReached,
            int loadedChunkCount,
            int skippedChunkCount,
            int requestedChunkCount,
            long scanDurationNanos,
            long discardedMarkerCount,
            boolean staleSnapshot) {
        private static ScanStatus idle() {
            return new ScanStatus(false, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0, false);
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

        public long scanDurationMillis() {
            return scanDurationNanos / 1_000_000L;
        }
    }

    private record ScanSlice(int budgetUsed, long positionsProcessed, long blocksInspected) {
    }

    private final class ChunkWork {
        private final int index;
        private final int chunkX;
        private final int chunkZ;
        private final int minY;
        private final int maxY;
        private LevelChunk chunk;
        private int y;
        private int localX;
        private int localZ;
        private boolean resolved;
        private boolean loaded;
        private boolean finished;

        private ChunkWork(int index, int chunkX, int chunkZ, int minY, int maxY) {
            this.index = index;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.minY = minY;
            this.maxY = maxY;
            this.y = minY;
        }

        private ScanSlice scan(
                ClientLevel level,
                WaterSourceConfig.ScanSettings settings,
                WaterMarkerAccumulator markers,
                int budget) {
            if (!resolved) {
                chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                resolved = true;
                loaded = chunk != null;
            }

            if (chunk == null || minY >= maxY) {
                finished = true;
                long skippedPositions = Math.max(0L, (long) maxY - minY) * BLOCKS_PER_CHUNK_LAYER;
                return new ScanSlice(1, skippedPositions, 0);
            }

            if (!settings.showSources() && !settings.showFlowing()) {
                finished = true;
                long skippedPositions = Math.max(0L, (long) maxY - minY) * BLOCKS_PER_CHUNK_LAYER;
                return new ScanSlice(1, skippedPositions, 0);
            }

            int consumed = 0;
            while (consumed < budget && !finished) {
                scanPosition.set((chunkX << 4) + localX, y, (chunkZ << 4) + localZ);
                inspect(settings, markers);
                y++;
                consumed++;
                if (y >= maxY) {
                    y = minY;
                    localX++;
                    if (localX >= CHUNK_WIDTH) {
                        localX = 0;
                        localZ++;
                        if (localZ >= CHUNK_WIDTH) {
                            finished = true;
                        }
                    }
                }
            }
            return new ScanSlice(consumed, consumed, consumed);
        }

        private void inspect(WaterSourceConfig.ScanSettings settings, WaterMarkerAccumulator markers) {
            BlockState blockState = chunk.getBlockState(scanPosition);
            FluidState fluidState = blockState.getFluidState();
            if (fluidState.getType() != Fluids.WATER && fluidState.getType() != Fluids.FLOWING_WATER) {
                return;
            }

            boolean source = fluidState.isSource();
            if ((source && !settings.showSources()) || (!source && !settings.showFlowing())) {
                return;
            }

            boolean waterlogged = blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && blockState.getValue(BlockStateProperties.WATERLOGGED);
            if (waterlogged && !settings.includeWaterloggedSources()) {
                return;
            }

            markers.add(
                    scanPosition.getX(),
                    scanPosition.getY(),
                    scanPosition.getZ(),
                    source,
                    waterlogged,
                    fluidState.getOwnHeight());
        }

        private boolean complete() {
            return finished;
        }

        private int index() {
            return index;
        }

        private boolean loaded() {
            return loaded;
        }

        private void release() {
            chunk = null;
        }
    }
}
