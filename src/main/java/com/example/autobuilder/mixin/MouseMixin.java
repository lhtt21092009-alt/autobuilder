package com.example.autobuilder.mixin;

import com.example.autobuilder.gui.AutoBuilderHud;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bat su kien click chuot khi dang o trong game (khong mo GUI nao) de xac dinh nguoi choi co bam
 * vao nut Start/Stop cua AutoBuilderHud hay khong - cho phep dieu khien Auto Builder MA KHONG can
 * mo man hinh cai dat (nen khong bi khoa di chuyen).
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow private double x;
    @Shadow private double y;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void autobuilder$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        double scaledX = this.x * screenWidth / client.getWindow().getWidth();
        double scaledY = this.y * screenHeight / client.getWindow().getHeight();

        if (AutoBuilderHud.isInside(scaledX, scaledY)) {
            AutoBuilderHud.toggleStartStop();
            ci.cancel();
        }
    }
}
