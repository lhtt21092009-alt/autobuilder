package com.example.autobuilder;

import com.example.autobuilder.gui.MainScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Phim tat mo/dong GUI, mac dinh la NUM LOCK, co the doi trong Options > Controls > "Auto Builder".
 */
public final class AutoBuilderKeybind {
    public static final KeyBinding TOGGLE_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autobuilder.togglegui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_NUM_LOCK,
            "key.categories.autobuilder"
    ));

    private AutoBuilderKeybind() {}

    /** Goi 1 lan trong onInitializeClient(), CANG SOM CANG TOT (truoc khi GameOptions duoc tao). */
    public static void init() {
        // chi can tham chieu class nay la du de kich hoat static init (dang ky keybinding) o tren
    }

    public static void tick(MinecraftClient client) {
        while (TOGGLE_GUI.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new MainScreen(null));
            } else if (client.currentScreen instanceof MainScreen) {
                client.setScreen(null);
            }
        }
    }
}
