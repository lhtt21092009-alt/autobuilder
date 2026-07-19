package com.example.autobuilder;

import com.example.autobuilder.core.AutoBuilderController;
import com.example.autobuilder.gui.AutoBuilderHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class AutoBuilderMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // QUAN TRONG: dang ky keybinding NGAY TU DAU, truoc khi GameOptions duoc tao,
        // neu khong game se crash voi loi "GameOptions has already been initialised"
        AutoBuilderKeybind.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoBuilderController.tick(client);
            AutoBuilderKeybind.tick(client);
        });

        // Thanh trang thai + nut Start/Stop o goc tren trai, khong khoa di chuyen
        HudRenderCallback.EVENT.register((context, tickCounter) -> AutoBuilderHud.render(context));
    }
}
