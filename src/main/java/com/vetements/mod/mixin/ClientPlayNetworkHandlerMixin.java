package com.vetements.mod.mixin;

import com.vetements.mod.ChatModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void vetements$onMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.getLocation() != MessageType.GAME_INFO) {
            ChatModule.onIncoming(packet.getMessage().getString());
        }
    }
}
