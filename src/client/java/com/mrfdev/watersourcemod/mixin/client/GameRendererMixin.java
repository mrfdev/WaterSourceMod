package com.mrfdev.watersourcemod.mixin.client;

import com.mrfdev.watersourcemod.WaterSourceModClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {
    @Inject(method = "close", at = @At("RETURN"))
    private void watersourcemod$closeMarkerBuffer(CallbackInfo callbackInfo) {
        WaterSourceModClient.closeRenderer();
    }
}
