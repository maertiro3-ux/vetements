package com.vetements.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VetementsMod implements ClientModInitializer {
    private static KeyBinding fullbrightKey;
    private static KeyBinding freelookKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        fullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vetements.fullbright", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.vetements"));
        freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vetements.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.vetements"));

        ClientTickEvents.END_CLIENT_TICK.register(VetementsMod::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(Fullbright::restore);
    }

    private static void tick(MinecraftClient client) {
        while (fullbrightKey.wasPressed()) {
            Fullbright.toggle(client);
        }
        Fullbright.tick(client);
        FreeLook.tick(client, freelookKey.isPressed());
        ChatModule.tick(client);
    }
}
