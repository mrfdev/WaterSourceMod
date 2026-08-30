package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterMarkerSnapshotTest {
    @Test
    void keepsLastCompletedMarkersVisibleUntilReplacementIsPublished() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        List<WaterMarker> firstScan = List.of(new WaterMarker(12, 72, 24, true, false, 1.0F));
        List<WaterMarker> secondScan = List.of(new WaterMarker(13, 72, 24, false, false, 0.6F));
        long generation = snapshot.generation();

        assertFalse(snapshot.hasPublishedSnapshot());
        assertTrue(snapshot.publish(generation, firstScan));
        assertTrue(snapshot.hasPublishedSnapshot());

        // This is the user-visible regression: a rescan must not blank the overlay.
        assertEquals(firstScan, snapshot.currentMarkers());

        assertTrue(snapshot.publish(generation, secondScan));
        assertEquals(secondScan, snapshot.currentMarkers());
    }

    @Test
    void rejectsPublicationFromAnInvalidatedGeneration() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        WaterMarker original = new WaterMarker(12, 72, 24, true, false, 1.0F);
        WaterMarker stale = new WaterMarker(13, 72, 24, false, false, 0.6F);
        long originalGeneration = snapshot.generation();

        assertTrue(snapshot.publish(originalGeneration, List.of(original)));
        long replacementGeneration = snapshot.invalidate();

        assertEquals(List.of(), snapshot.currentMarkers());
        assertFalse(snapshot.hasPublishedSnapshot());
        assertFalse(snapshot.publish(originalGeneration, List.of(stale)));
        assertEquals(List.of(), snapshot.currentMarkers());
        assertTrue(snapshot.publish(replacementGeneration, List.of(original)));
        assertEquals(List.of(original), snapshot.currentMarkers());
        assertTrue(snapshot.hasPublishedSnapshot());
    }

    @Test
    void removesOnlyMarkersBelongingToAnUnloadedChunk() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        WaterMarker negativeChunk = new WaterMarker(-1, 64, -16, true, false, 1.0F);
        WaterMarker targetChunk = new WaterMarker(31, 64, 16, false, false, 0.8F);
        WaterMarker retained = new WaterMarker(32, 64, 16, true, false, 1.0F);
        long firstGeneration = snapshot.generation();
        assertTrue(snapshot.publish(firstGeneration, List.of(negativeChunk, targetChunk, retained)));

        assertTrue(snapshot.invalidateChunk(1, 1));
        assertTrue(snapshot.hasPublishedSnapshot());
        long secondGeneration = snapshot.generation();
        assertTrue(secondGeneration > firstGeneration);
        assertEquals(List.of(negativeChunk, retained), snapshot.currentMarkers());
        assertFalse(snapshot.publish(firstGeneration, List.of(targetChunk)));
        assertFalse(snapshot.invalidateChunk(1, 1));
        assertTrue(snapshot.generation() > secondGeneration);

        assertTrue(snapshot.invalidateChunk(-1, -1));
        assertEquals(List.of(retained), snapshot.currentMarkers());
    }

    @Test
    void markerlessChunkInvalidationAdvancesGenerationWithoutReplacingMarkers() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        WaterMarker retained = new WaterMarker(32, 64, 16, true, false, 1.0F);
        long scanGeneration = snapshot.generation();
        assertTrue(snapshot.publish(scanGeneration, List.of(retained)));
        List<WaterMarker> publishedMarkers = snapshot.currentMarkers();

        assertFalse(snapshot.invalidateChunk(7, -9));

        assertSame(publishedMarkers, snapshot.currentMarkers());
        assertTrue(snapshot.hasPublishedSnapshot());
        assertNotEquals(scanGeneration, snapshot.generation());
        assertFalse(snapshot.publish(scanGeneration, List.of(
                new WaterMarker(112, 64, -144, false, false, 0.5F))));
    }

    @Test
    void rebuildingSnapshotAlsoRebuildsOccupiedChunkIndex() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        WaterMarker first = new WaterMarker(16, 64, 16, true, false, 1.0F);
        long firstGeneration = snapshot.generation();
        assertTrue(snapshot.publish(firstGeneration, List.of(first)));
        assertTrue(snapshot.invalidateChunk(1, 1));

        WaterMarker replacement = new WaterMarker(-1, 70, -16, false, false, 0.8F);
        long replacementGeneration = snapshot.generation();
        assertTrue(snapshot.publish(replacementGeneration, List.of(replacement)));

        assertFalse(snapshot.invalidateChunk(1, 1));
        assertTrue(snapshot.invalidateChunk(-1, -1));
        assertEquals(List.of(), snapshot.currentMarkers());
    }

    @Test
    void chunkKeysDoNotAliasAtSignedCoordinateExtremes() {
        assertNotEquals(
                WaterMarkerSnapshot.chunkKey(Integer.MIN_VALUE, Integer.MAX_VALUE),
                WaterMarkerSnapshot.chunkKey(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertNotEquals(
                WaterMarkerSnapshot.chunkKey(-1, 0),
                WaterMarkerSnapshot.chunkKey(0, -1));
    }
}
