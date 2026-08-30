package com.mrfdev.watersourcemod.mixin.client;

import com.mrfdev.watersourcemod.WaterSourceModClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Isolated 26.2 hook for the opt-in block-update rescan mode. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN"))
    private void watersourcemod$afterSetBlock(
            BlockPos position,
            BlockState state,
            int flags,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            WaterSourceModClient.onClientBlockUpdated((ClientLevel) (Object) this, position);
        }
    }
}
