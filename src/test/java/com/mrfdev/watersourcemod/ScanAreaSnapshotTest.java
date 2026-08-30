package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanAreaSnapshotTest {
    @Test
    void pendingAreaContainsOnlyUnknownChunksInStableCoordinateOrder() {
        ScanAreaSnapshot area = ScanAreaSnapshot.pending(10, -4, 1, -64, 320);

        assertFalse(area.isEmpty());
        assertEquals(9, area.chunkCount());
        assertEquals(9, area.chunkXAt(0));
        assertEquals(-5, area.chunkZAt(0));
        assertEquals(11, area.chunkXAt(8));
        assertEquals(-3, area.chunkZAt(8));
        assertTrue(area.isCenterAt(4));
        assertEquals(ScanAreaSnapshot.ChunkState.UNKNOWN, area.stateAt(4));
        assertEquals(0, area.loadedCount());
        assertEquals(0, area.skippedCount());
    }

    @Test
    void completedAreaCopiesPrimitiveStateAndCountsLoadedAndSkippedChunks() {
        byte[] states = {
                ScanAreaSnapshot.ChunkState.LOADED.code(),
                ScanAreaSnapshot.ChunkState.SKIPPED.code(),
                ScanAreaSnapshot.ChunkState.UNKNOWN.code(),
                ScanAreaSnapshot.ChunkState.LOADED.code(),
                ScanAreaSnapshot.ChunkState.LOADED.code(),
                ScanAreaSnapshot.ChunkState.SKIPPED.code(),
                ScanAreaSnapshot.ChunkState.LOADED.code(),
                ScanAreaSnapshot.ChunkState.LOADED.code(),
                ScanAreaSnapshot.ChunkState.SKIPPED.code()
        };
        ScanAreaSnapshot area = ScanAreaSnapshot.completed(0, 0, 1, 0, 128, states);
        states[0] = ScanAreaSnapshot.ChunkState.SKIPPED.code();

        assertEquals(5, area.loadedCount());
        assertEquals(3, area.skippedCount());
        assertEquals(ScanAreaSnapshot.ChunkState.LOADED, area.stateAt(0));
        assertTrue(area.matchesScope(0, 0, 1, 0, 128));
        assertFalse(area.matchesScope(1, 0, 1, 0, 128));
    }
}
