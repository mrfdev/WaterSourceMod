package com.mrfdev.watersourcemod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic handoff between an incremental scan and the render phase.
 *
 * <p>The previous completed snapshot is intentionally retained while a new
 * scan is being assembled, so an automatic refresh cannot make markers blink
 * out for the duration of the scan. A scan may publish only into the generation
 * in which it started, preventing invalidated world or scan-area results from
 * becoming visible later.</p>
 */
final class WaterMarkerSnapshot {
    private final AtomicReference<SnapshotState> state = new AtomicReference<>(
            new SnapshotState(0, List.of(), Set.of(), false));

    List<WaterMarker> currentMarkers() {
        return state.get().markers();
    }

    long generation() {
        return state.get().generation();
    }

    boolean hasPublishedSnapshot() {
        return state.get().published();
    }

    boolean publish(long expectedGeneration, List<WaterMarker> nextMarkers) {
        if (state.get().generation() != expectedGeneration) {
            return false;
        }

        List<WaterMarker> immutableMarkers = List.copyOf(nextMarkers);
        Set<Long> occupiedChunks = occupiedChunks(immutableMarkers);
        while (true) {
            SnapshotState current = state.get();
            if (current.generation() != expectedGeneration) {
                return false;
            }

            SnapshotState replacement = new SnapshotState(
                    expectedGeneration,
                    immutableMarkers,
                    occupiedChunks,
                    true);
            if (state.compareAndSet(current, replacement)) {
                return true;
            }
        }
    }

    long invalidate() {
        while (true) {
            SnapshotState current = state.get();
            SnapshotState invalidated = new SnapshotState(
                    current.generation() + 1,
                    List.of(),
                    Set.of(),
                    false);
            if (state.compareAndSet(current, invalidated)) {
                return invalidated.generation();
            }
        }
    }

    boolean invalidateChunk(int chunkX, int chunkZ) {
        long unloadedChunk = chunkKey(chunkX, chunkZ);
        while (true) {
            SnapshotState current = state.get();
            if (!current.occupiedChunks().contains(unloadedChunk)) {
                SnapshotState invalidated = new SnapshotState(
                        current.generation() + 1,
                        current.markers(),
                        current.occupiedChunks(),
                        current.published());
                if (state.compareAndSet(current, invalidated)) {
                    return false;
                }
                continue;
            }

            List<WaterMarker> filtered = withoutChunk(current.markers(), chunkX, chunkZ);
            Set<Long> retainedChunks = new HashSet<>(current.occupiedChunks());
            retainedChunks.remove(unloadedChunk);
            SnapshotState invalidated = new SnapshotState(
                    current.generation() + 1,
                    filtered,
                    Set.copyOf(retainedChunks),
                    current.published());
            if (state.compareAndSet(current, invalidated)) {
                return filtered != current.markers();
            }
        }
    }

    private static List<WaterMarker> withoutChunk(
            List<WaterMarker> markers,
            int chunkX,
            int chunkZ) {
        List<WaterMarker> filtered = null;
        for (int index = 0; index < markers.size(); index++) {
            WaterMarker marker = markers.get(index);
            if ((marker.x() >> 4) == chunkX && (marker.z() >> 4) == chunkZ) {
                if (filtered == null) {
                    filtered = new ArrayList<>(markers.size() - 1);
                    filtered.addAll(markers.subList(0, index));
                }
            } else if (filtered != null) {
                filtered.add(marker);
            }
        }
        return filtered == null ? markers : List.copyOf(filtered);
    }

    private static Set<Long> occupiedChunks(List<WaterMarker> markers) {
        if (markers.isEmpty()) {
            return Set.of();
        }

        Set<Long> occupiedChunks = new HashSet<>();
        for (WaterMarker marker : markers) {
            occupiedChunks.add(chunkKey(marker.x() >> 4, marker.z() >> 4));
        }
        return Set.copyOf(occupiedChunks);
    }

    static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFF_FFFFL);
    }

    private record SnapshotState(
            long generation,
            List<WaterMarker> markers,
            Set<Long> occupiedChunks,
            boolean published) {
    }
}
