package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSourceScannerTest {
    @Test
    void totalProgressIncludesEveryXZPositionInEachChunkLayer() {
        long total = WaterSourceScanner.totalBlockPositions(-64, 320, 9);

        assertEquals(884_736L, total);
        WaterSourceScanner.ScanStatus quarterComplete = new WaterSourceScanner.ScanStatus(
                true,
                total / 4,
                total,
                123,
                12,
                8,
                false,
                2,
                1,
                9,
                8_000_000,
                3,
                true);
        assertEquals(25, quarterComplete.progressPercent());
        assertEquals(8, quarterComplete.scanDurationMillis());
        assertEquals(123, quarterComplete.inspectedBlocks());
        assertEquals(1, quarterComplete.skippedChunkCount());
        assertEquals(3, quarterComplete.discardedMarkerCount());
        assertTrue(quarterComplete.staleSnapshot());
    }

    @Test
    void invalidDimensionsCannotProduceNegativeProgressTotals() {
        assertEquals(0, WaterSourceScanner.totalBlockPositions(320, -64, 9));
        assertEquals(0, WaterSourceScanner.totalBlockPositions(-64, 320, -1));
    }

    @Test
    void adaptiveDeadlineUsesMillisecondsAndSaturatesOnOverflow() {
        assertEquals(4_000_001L, WaterSourceScanner.scanDeadline(1L, 4));
        assertEquals(Long.MAX_VALUE, WaterSourceScanner.scanDeadline(Long.MAX_VALUE - 10L, 4));
    }

    @Test
    void blockUpdateDebouncePostponesWorkButKeepsAFixedMaximumDelay() {
        WaterSourceScanner.BlockUpdateSchedule first =
                WaterSourceScanner.nextBlockUpdateSchedule(100, false, 0);
        WaterSourceScanner.BlockUpdateSchedule second =
                WaterSourceScanner.nextBlockUpdateSchedule(104, true, first.deadlineTick());
        WaterSourceScanner.BlockUpdateSchedule nearDeadline =
                WaterSourceScanner.nextBlockUpdateSchedule(119, true, first.deadlineTick());

        assertEquals(new WaterSourceScanner.BlockUpdateSchedule(105, 120), first);
        assertEquals(new WaterSourceScanner.BlockUpdateSchedule(109, 120), second);
        assertEquals(new WaterSourceScanner.BlockUpdateSchedule(120, 120), nearDeadline);
    }

    @Test
    void chunkLifecycleBurstsCoalesceIntoOneRescan() {
        WaterSourceScanner.ChunkLifecycleRescanGate gate =
                new WaterSourceScanner.ChunkLifecycleRescanGate();

        for (int event = 0; event < 500; event++) {
            gate.schedule(100);
        }

        assertTrue(gate.pending());
        assertEquals(105, gate.dueTick());
        assertEquals(120, gate.deadlineTick());
        assertFalse(gate.consumeIfDue(104));
        assertTrue(gate.consumeIfDue(105));
        assertFalse(gate.consumeIfDue(105));
    }

    @Test
    void continuousChunkLifecycleChurnCannotPostponeRescanForever() {
        WaterSourceScanner.ChunkLifecycleRescanGate gate =
                new WaterSourceScanner.ChunkLifecycleRescanGate();

        gate.schedule(100);
        for (long tick = 101; tick < 120; tick++) {
            gate.schedule(tick);
            assertFalse(gate.consumeIfDue(tick));
        }

        gate.schedule(120);
        assertEquals(120, gate.dueTick());
        assertTrue(gate.consumeIfDue(120));
        assertFalse(gate.pending());
    }

    @Test
    void clearingChunkLifecycleGateDropsStaleRescan() {
        WaterSourceScanner.ChunkLifecycleRescanGate gate =
                new WaterSourceScanner.ChunkLifecycleRescanGate();

        gate.schedule(Long.MAX_VALUE - 2);
        assertEquals(Long.MAX_VALUE, gate.dueTick());
        assertEquals(Long.MAX_VALUE, gate.deadlineTick());
        gate.clear();

        assertFalse(gate.pending());
        assertFalse(gate.consumeIfDue(Long.MAX_VALUE));
    }
}
