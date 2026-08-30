package com.mrfdev.watersourcemod;

import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Allocation-free visibility checks performed before marker vertex generation. */
final class WaterMarkerCuller {
    private static final double MIN_HORIZONTAL_OFFSET = -0.05;
    private static final double MAX_HORIZONTAL_OFFSET = 1.05;
    private static final double MIN_VERTICAL_OFFSET = -0.12;
    private static final double MAX_VERTICAL_OFFSET = 1.35;

    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f frustumMatrix = new Matrix4f();
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private boolean frustumReady;

    void prepareFrustum(Matrix4fc projectionMatrix, Matrix4fc viewRotationMatrix, Vec3 camera) {
        projectionMatrix.mul(viewRotationMatrix, frustumMatrix);
        frustum.set(frustumMatrix);
        cameraX = camera.x;
        cameraY = camera.y;
        cameraZ = camera.z;
        frustumReady = true;
    }

    boolean isWithinDistance(WaterMarker marker, Vec3 camera, float renderDistance) {
        if (!Float.isFinite(renderDistance) || renderDistance <= 0F) {
            return true;
        }
        return distanceSquared(marker, camera) <= (double) renderDistance * renderDistance;
    }

    float distanceFadeAlpha(
            WaterMarker marker,
            Vec3 camera,
            float renderDistance,
            float fadeStartFraction) {
        if (!Float.isFinite(renderDistance) || renderDistance <= 0F || fadeStartFraction >= 1F) {
            return 1F;
        }

        float clampedStart = Math.max(0F, fadeStartFraction);
        double distance = Math.sqrt(distanceSquared(marker, camera));
        double fadeStart = renderDistance * clampedStart;
        if (distance <= fadeStart) {
            return 1F;
        }
        if (distance >= renderDistance) {
            return 0F;
        }
        return (float) (1D - (distance - fadeStart) / (renderDistance - fadeStart));
    }

    boolean intersectsFrustum(WaterMarker marker) {
        return intersectsFrustum(
                marker.x() + MIN_HORIZONTAL_OFFSET,
                marker.y() + MIN_VERTICAL_OFFSET,
                marker.z() + MIN_HORIZONTAL_OFFSET,
                marker.x() + MAX_HORIZONTAL_OFFSET,
                marker.y() + MAX_VERTICAL_OFFSET,
                marker.z() + MAX_HORIZONTAL_OFFSET);
    }

    boolean intersectsFrustum(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        if (!frustumReady) {
            return true;
        }

        return frustum.testAab(
                (float) (minX - cameraX),
                (float) (minY - cameraY),
                (float) (minZ - cameraZ),
                (float) (maxX - cameraX),
                (float) (maxY - cameraY),
                (float) (maxZ - cameraZ));
    }

    private static double distanceOutside(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0;
    }

    private static double distanceSquared(WaterMarker marker, Vec3 camera) {
        double minX = marker.x() + MIN_HORIZONTAL_OFFSET;
        double minY = marker.y() + MIN_VERTICAL_OFFSET;
        double minZ = marker.z() + MIN_HORIZONTAL_OFFSET;
        double maxX = marker.x() + MAX_HORIZONTAL_OFFSET;
        double maxY = marker.y() + MAX_VERTICAL_OFFSET;
        double maxZ = marker.z() + MAX_HORIZONTAL_OFFSET;
        double dx = distanceOutside(camera.x, minX, maxX);
        double dy = distanceOutside(camera.y, minY, maxY);
        double dz = distanceOutside(camera.z, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}
