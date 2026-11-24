package com.vetements.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

public final class Fullbright {
    private static final double BRIGHT = 16.0D;
    private static boolean enabled;
    private static double savedGamma = 1.0D;

    private Fullbright() {
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (enabled) {
            savedGamma = client.options.gamma;
            client.options.gamma = BRIGHT;
        } else {
            client.options.gamma = savedGamma;
        }
        if (client.player != null) {
            client.player.sendMessage(new LiteralText("Fullbright: " + (enabled ? "§aвкл" : "§cвыкл")), true);
        }
    }

    public static void tick(MinecraftClient client) {
        if (enabled && client.options.gamma < BRIGHT) {
            client.options.gamma = BRIGHT;
        }
    }

    /** Puts the normal brightness back (used on game exit so options.txt is not polluted). */
    public static void restore(MinecraftClient client) {
        if (enabled) {
            client.options.gamma = savedGamma;
            enabled = false;
        }
    }
}
