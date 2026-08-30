package com.mrfdev.watersourcemod;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterMarkerCullerTest {
    private final WaterMarkerCuller culler = new WaterMarkerCuller();

    @Test
    void distanceCheckUsesTheMarkerBoundsRatherThanOnlyItsCenter() {
        WaterMarker marker = new WaterMarker(0, 0, 0, true, false, 1.0F);

        assertTrue(culler.isWithinDistance(marker, new Vec3(-0.05, 0, 0), 0.01F));
        assertFalse(culler.isWithinDistance(marker, new Vec3(10, 0, 0), 5F));
        assertTrue(culler.isWithinDistance(marker, new Vec3(10, 0, 0), Float.NaN));
    }

    @Test
    void frustumCheckRejectsMarkersOutsideTheClipVolume() {
        culler.prepareFrustum(new Matrix4f(), new Matrix4f(), Vec3.ZERO);

        assertTrue(culler.intersectsFrustum(new WaterMarker(0, 0, 1, true, false, 1.0F)));
        assertTrue(culler.intersectsFrustum(new WaterMarker(0, 0, 0, true, false, 1.0F)));
        assertFalse(culler.intersectsFrustum(new WaterMarker(10, 10, 10, true, false, 1.0F)));
    }

    @Test
    void frustumCheckIsRelativeToTheCameraPosition() {
        culler.prepareFrustum(new Matrix4f(), new Matrix4f(), new Vec3(100, 50, -100));

        assertTrue(culler.intersectsFrustum(new WaterMarker(100, 50, -100, true, false, 1.0F)));
        assertFalse(culler.intersectsFrustum(new WaterMarker(0, 0, 0, true, false, 1.0F)));
    }

    @Test
    void genericBoundsUseTheSameCameraRelativeFrustumForScanAreas() {
        culler.prepareFrustum(new Matrix4f(), new Matrix4f(), new Vec3(100, 50, -100));

        assertTrue(culler.intersectsFrustum(99.5, 49.5, -100.5, 100.5, 50.5, -99.5));
        assertFalse(culler.intersectsFrustum(-10, -10, -10, -9, -9, -9));
    }

    @Test
    void distanceFadeIsFullBeforeItsStartAndZeroAtTheLimit() {
        WaterMarker near = new WaterMarker(0, 0, 0, true, false, 1F);
        WaterMarker fading = new WaterMarker(8, 0, 0, true, false, 1F);
        WaterMarker far = new WaterMarker(10, 0, 0, true, false, 1F);
        Vec3 camera = Vec3.ZERO;

        assertEquals(1F, culler.distanceFadeAlpha(near, camera, 10F, 0.5F));
        float fadedAlpha = culler.distanceFadeAlpha(fading, camera, 10F, 0.5F);
        assertTrue(fadedAlpha > 0F && fadedAlpha < 1F);
        assertEquals(0F, culler.distanceFadeAlpha(far, camera, 9F, 0.5F));
        assertEquals(1F, culler.distanceFadeAlpha(far, camera, 10F, 1F));
    }
}
