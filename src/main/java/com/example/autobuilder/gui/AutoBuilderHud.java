package com.example.autobuilder.gui;

import com.example.autobuilder.core.AutoBuilderController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Thanh trang thai + nut Start/Stop luon hien thi khi dang choi (khong mo GUI nao), o goc TREN
 * TRAI man hinh. Day KHONG PHAI la Screen nen KHONG khoa di chuyen - nguoi choi van bay/di chuyen
 * binh thuong trong khi no hien. Click duoc phat hien qua MouseMixin (giong nut goc phai cua Auto
 * Flyer truoc day).
 */
public final class AutoBuilderHud {
    public static final int WIDTH = 130;
    public static final int HEIGHT = 16;
    public static final int MARGIN = 6;

    private AutoBuilderHud() {}

    public static int getX() {
        return MARGIN;
    }

    public static int getY() {
        return MARGIN;
    }

    public static boolean isInside(double mouseX, double mouseY) {
        int x = getX();
        int y = getY();
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    public static void toggleStartStop() {
        if (AutoBuilderController.isRunning()) {
            AutoBuilderController.stop();
        } else {
            AutoBuilderController.start();
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return; // chi hien khi khong co GUI nao mo

        int x = getX();
        int y = getY();

        boolean running = AutoBuilderController.isRunning();
        int bg = running ? 0xAA2E7D32 : 0xAA000000;
        context.fill(x, y, x + WIDTH, y + HEIGHT, bg);
        context.drawBorder(x, y, WIDTH, HEIGHT, 0xFFFFFFFF);

        String label = "Auto Builder: " + (running ? "DANG CHAY" : "DA DUNG");
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(label),
                x + WIDTH / 2, y + (HEIGHT - 8) / 2, 0xFFFFFF);
    }
}
