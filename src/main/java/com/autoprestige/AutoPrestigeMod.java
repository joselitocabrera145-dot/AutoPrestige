package com.autoprestige;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class AutoPrestigeMod implements ClientModInitializer {
    public static KeyMapping RUN_KEY;
    public static KeyMapping TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        PrestigeConfig.load();

        RUN_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autoprestige.run", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"));
        TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autoprestige.toggle", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (RUN_KEY.consumeClick()) {
                PrestigeController.requestNow();
            }
            while (TOGGLE_KEY.consumeClick()) {
                PrestigeController.toggleAuto();
            }
            PrestigeController.tick(mc);
        });
    }
}
