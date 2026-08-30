package com.mrfdev.watersourcemod;

import java.util.Arrays;

/** Immutable loaded/skipped state for the chunks in one scan area. */
final class ScanAreaSnapshot {
    private static final ScanAreaSnapshot EMPTY = new ScanAreaSnapshot(0, 0, -1, 0, 0, new byte[0]);

    private final int centerChunkX;
    private final int centerChunkZ;
    private final int radius;
    private final int minY;
    private final int maxY;
    private final int diameter;
    private final byte[] states;
    private final int loadedCount;
    private final int skippedCount;

    private ScanAreaSnapshot(
            int centerChunkX,
            int centerChunkZ,
            int radius,
            int minY,
            int maxY,
            byte[] sourceStates) {
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.radius = radius;
        this.minY = minY;
        this.maxY = maxY;
        diameter = radius < 0 ? 0 : radius * 2 + 1;
        int expectedLength = diameter * diameter;
        if (sourceStates.length != expectedLength) {
            throw new IllegalArgumentException("Chunk state count does not match scan radius");
        }
        states = Arrays.copyOf(sourceStates, sourceStates.length);

        int loaded = 0;
        int skipped = 0;
        for (byte state : states) {
            if (state == ChunkState.LOADED.code) {
                loaded++;
            } else if (state == ChunkState.SKIPPED.code) {
                skipped++;
            }
        }
        loadedCount = loaded;
        skippedCount = skipped;
    }

    static ScanAreaSnapshot empty() {
        return EMPTY;
    }

    static ScanAreaSnapshot pending(
            int centerChunkX,
            int centerChunkZ,
            int radius,
            int minY,
            int maxY) {
        int diameter = radius * 2 + 1;
        return new ScanAreaSnapshot(
                centerChunkX,
                centerChunkZ,
                radius,
                minY,
                maxY,
                new byte[diameter * diameter]);
    }

    static ScanAreaSnapshot completed(
            int centerChunkX,
            int centerChunkZ,
            int radius,
            int minY,
            int maxY,
            byte[] states) {
        return new ScanAreaSnapshot(centerChunkX, centerChunkZ, radius, minY, maxY, states);
    }

    boolean isEmpty() {
        return states.length == 0;
    }

    int centerChunkX() {
        return centerChunkX;
    }

    int centerChunkZ() {
        return centerChunkZ;
    }

    int radius() {
        return radius;
    }

    int minY() {
        return minY;
    }

    int maxY() {
        return maxY;
    }

    int chunkCount() {
        return states.length;
    }

    int loadedCount() {
        return loadedCount;
    }

    int skippedCount() {
        return skippedCount;
    }

    int chunkXAt(int index) {
        return centerChunkX + index % diameter - radius;
    }

    int chunkZAt(int index) {
        return centerChunkZ + index / diameter - radius;
    }

    boolean isCenterAt(int index) {
        return index == radius * diameter + radius;
    }

    ChunkState stateAt(int index) {
        return ChunkState.fromCode(states[index]);
    }

    boolean matchesScope(
            int otherCenterChunkX,
            int otherCenterChunkZ,
            int otherRadius,
            int otherMinY,
            int otherMaxY) {
        return centerChunkX == otherCenterChunkX
                && centerChunkZ == otherCenterChunkZ
                && radius == otherRadius
                && minY == otherMinY
                && maxY == otherMaxY;
    }

    enum ChunkState {
        UNKNOWN((byte) 0),
        LOADED((byte) 1),
        SKIPPED((byte) 2);

        private final byte code;

        ChunkState(byte code) {
            this.code = code;
        }

        byte code() {
            return code;
        }

        private static ChunkState fromCode(byte code) {
            return switch (code) {
                case 1 -> LOADED;
                case 2 -> SKIPPED;
                default -> UNKNOWN;
            };
        }
    }
}
