package com.vetements.mod.mixin;

import com.vetements.mod.FreeLook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0)
    )
    private void vetements$freeLookRotation(Args args) {
        if (FreeLook.isActive()) {
            args.set(0, FreeLook.yaw());
            args.set(1, FreeLook.pitch());
        }
    }
}
