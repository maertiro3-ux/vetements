package com.vetements.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class FreeLook {
    private static boolean active;
    private static float yaw;
    private static float pitch;

    private FreeLook() {
    }

    public static boolean isActive() {
        return active;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    /** Called instead of the vanilla head rotation while FreeLook is active. */
    public static void rotate(double cursorDeltaX, double cursorDeltaY) {
        pitch = MathHelper.clamp(pitch + (float) cursorDeltaY * 0.15F, -90.0F, 90.0F);
        yaw += (float) cursorDeltaX * 0.15F;
    }

    public static void tick(MinecraftClient client, boolean keyDown) {
        if (client.player == null) {
            active = false;
            return;
        }
        if (keyDown && !active && client.currentScreen == null) {
            yaw = client.player.yaw;
            pitch = client.player.pitch;
            active = true;
        } else if (!keyDown && active) {
            active = false;
        }
    }
}
