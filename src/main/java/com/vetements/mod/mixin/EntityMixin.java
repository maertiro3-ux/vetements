package com.vetements.mod.mixin;

import com.vetements.mod.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void vetements$freeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (FreeLook.isActive() && (Object) this == MinecraftClient.getInstance().player) {
            FreeLook.rotate(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
