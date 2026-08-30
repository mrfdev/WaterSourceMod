package com.mrfdev.watersourcemod;

import java.util.List;

/** Nearest eligible marker and its player-facing HUD information. */
final class NearestWaterMarker {
    private static final NearestWaterMarker NONE = new NearestWaterMarker(null, 0D, Direction.HERE, 0);
    private static final double EIGHTH_TURN = Math.PI / 4D;

    private final WaterMarker marker;
    private final double distanceSquared;
    private final Direction direction;
    private final int verticalOffset;

    private NearestWaterMarker(
            WaterMarker marker,
            double distanceSquared,
            Direction direction,
            int verticalOffset) {
        this.marker = marker;
        this.distanceSquared = distanceSquared;
        this.direction = direction;
        this.verticalOffset = verticalOffset;
    }

    static NearestWaterMarker none() {
        return NONE;
    }

    static NearestWaterMarker find(
            List<WaterMarker> markers,
            double playerX,
            double playerY,
            double playerZ,
            WaterSourceConfig.RenderSettings settings) {
        WaterMarker nearest = null;
        double nearestDistanceSquared = (double) settings.maxRenderDistance() * settings.maxRenderDistance();
        double nearestDx = 0D;
        double nearestDz = 0D;
        double nearestY = playerY;

        for (WaterMarker candidate : markers) {
            if (!settings.isMarkerEnabled(candidate)) {
                continue;
            }
            double markerX = candidate.x() + 0.5D;
            double markerY = candidate.y() + candidate.height() * 0.5D;
            double markerZ = candidate.z() + 0.5D;
            double dx = markerX - playerX;
            double dy = markerY - playerY;
            double dz = markerZ - playerZ;
            double candidateDistanceSquared = dx * dx + dy * dy + dz * dz;
            if (candidateDistanceSquared < nearestDistanceSquared
                    || (nearest == null && candidateDistanceSquared == nearestDistanceSquared)) {
                nearest = candidate;
                nearestDistanceSquared = candidateDistanceSquared;
                nearestDx = dx;
                nearestDz = dz;
                nearestY = markerY;
            }
        }

        if (nearest == null) {
            return NONE;
        }
        return new NearestWaterMarker(
                nearest,
                nearestDistanceSquared,
                Direction.fromOffset(nearestDx, nearestDz),
                (int) Math.round(nearestY - playerY));
    }

    boolean isPresent() {
        return marker != null;
    }

    WaterMarker marker() {
        return marker;
    }

    int roundedDistance() {
        return (int) Math.round(Math.sqrt(distanceSquared));
    }

    Direction direction() {
        return direction;
    }

    int verticalOffset() {
        return verticalOffset;
    }

    boolean matches(WaterMarker candidate) {
        return marker != null
                && marker.x() == candidate.x()
                && marker.y() == candidate.y()
                && marker.z() == candidate.z();
    }

    enum Direction {
        NORTH("north"),
        NORTH_EAST("north_east"),
        EAST("east"),
        SOUTH_EAST("south_east"),
        SOUTH("south"),
        SOUTH_WEST("south_west"),
        WEST("west"),
        NORTH_WEST("north_west"),
        HERE("here");

        private static final Direction[] COMPASS = {
                NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST
        };

        private final String key;

        Direction(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }

        private static Direction fromOffset(double dx, double dz) {
            if (dx * dx + dz * dz < 0.25D) {
                return HERE;
            }
            double clockwiseFromNorth = Math.atan2(dx, -dz);
            int index = Math.floorMod((int) Math.round(clockwiseFromNorth / EIGHTH_TURN), COMPASS.length);
            return COMPASS[index];
        }
    }
}
