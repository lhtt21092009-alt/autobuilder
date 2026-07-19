package com.example.autobuilder.gui;

import com.example.autobuilder.AutoBuilderConfig;
import com.example.autobuilder.core.AutoBuilderController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * GUI chinh cua Auto Builder:
 *  - Vi tri 1 / Vi tri 2 (x,y,z + Set here + Clear): vung chua ruong/thung de lay nguyen lieu.
 *  - Toc do bay.
 *  - Che do an toan khi phat hien nguoi choi khac (Tat / Thoat game / Dung + am thanh).
 *  - Toc do dat block (build) va toc do lay do trong ruong.
 *  - Start / Stop.
 */
public class MainScreen extends Screen {
    private final Screen parent;
    private final AutoBuilderConfig cfg = AutoBuilderConfig.INSTANCE;

    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget flightSpeedField;
    private TextFieldWidget buildSpeedField;
    private TextFieldWidget itemFetchSpeedField;
    private ButtonWidget playerDetectButton;
    private ButtonWidget startStopButton;

    public MainScreen(Screen parent) {
        super(Text.literal("Auto Builder"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 70;
        int fieldHeight = 20;
        int gap = 6;

        int row1Y = 40;
        int row2Y = 110;
        int row3Y = 180; // toc do bay
        int row4Y = 210; // che do an toan
        int row5Y = 245; // toc do build + lay do
        int row7Y = this.height - 34; // start/stop

        // ---- Vi tri 1 ----
        int startX1 = centerX - (fieldWidth * 3 + gap * 2) / 2;
        x1Field = new TextFieldWidget(this.textRenderer, startX1, row1Y, fieldWidth, fieldHeight, Text.literal("X"));
        y1Field = new TextFieldWidget(this.textRenderer, startX1 + fieldWidth + gap, row1Y, fieldWidth, fieldHeight, Text.literal("Y"));
        z1Field = new TextFieldWidget(this.textRenderer, startX1 + (fieldWidth + gap) * 2, row1Y, fieldWidth, fieldHeight, Text.literal("Z"));
        setNumericOnly(x1Field);
        setNumericOnly(y1Field);
        setNumericOnly(z1Field);
        x1Field.setText(fmt(cfg.x1));
        y1Field.setText(fmt(cfg.y1));
        z1Field.setText(fmt(cfg.z1));
        addDrawableChild(x1Field);
        addDrawableChild(y1Field);
        addDrawableChild(z1Field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Set here"), b -> setHere(1))
                .dimensions(centerX - 130, row1Y + 28, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> clear(1))
                .dimensions(centerX + 10, row1Y + 28, 120, 20).build());

        // ---- Vi tri 2 ----
        x2Field = new TextFieldWidget(this.textRenderer, startX1, row2Y, fieldWidth, fieldHeight, Text.literal("X"));
        y2Field = new TextFieldWidget(this.textRenderer, startX1 + fieldWidth + gap, row2Y, fieldWidth, fieldHeight, Text.literal("Y"));
        z2Field = new TextFieldWidget(this.textRenderer, startX1 + (fieldWidth + gap) * 2, row2Y, fieldWidth, fieldHeight, Text.literal("Z"));
        setNumericOnly(x2Field);
        setNumericOnly(y2Field);
        setNumericOnly(z2Field);
        x2Field.setText(fmt(cfg.x2));
        y2Field.setText(fmt(cfg.y2));
        z2Field.setText(fmt(cfg.z2));
        addDrawableChild(x2Field);
        addDrawableChild(y2Field);
        addDrawableChild(z2Field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Set here"), b -> setHere(2))
                .dimensions(centerX - 130, row2Y + 28, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> clear(2))
                .dimensions(centerX + 10, row2Y + 28, 120, 20).build());

        // ---- Toc do bay ----
        flightSpeedField = new TextFieldWidget(this.textRenderer, centerX - 40, row3Y, 80, fieldHeight, Text.literal("Toc do bay"));
        flightSpeedField.setMaxLength(6);
        flightSpeedField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d*(\\.\\d*)?"));
        flightSpeedField.setText(String.valueOf(cfg.flightSpeed));
        addDrawableChild(flightSpeedField);

        // ---- Che do an toan (bam de doi vong 1 -> 2 -> 3) ----
        playerDetectButton = ButtonWidget.builder(Text.literal(playerDetectLabel()), b -> cyclePlayerDetectMode())
                .dimensions(centerX - 150, row4Y, 300, 20).build();
        addDrawableChild(playerDetectButton);

        // ---- Toc do build + toc do lay do ----
        buildSpeedField = new TextFieldWidget(this.textRenderer, centerX - 90, row5Y, 70, fieldHeight, Text.literal("Toc do xay"));
        buildSpeedField.setMaxLength(6);
        buildSpeedField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d*(\\.\\d*)?"));
        buildSpeedField.setText(String.valueOf(cfg.buildSpeed));
        addDrawableChild(buildSpeedField);

        itemFetchSpeedField = new TextFieldWidget(this.textRenderer, centerX + 20, row5Y, 70, fieldHeight, Text.literal("Toc do lay do"));
        itemFetchSpeedField.setMaxLength(6);
        itemFetchSpeedField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d*(\\.\\d*)?"));
        itemFetchSpeedField.setText(String.valueOf(cfg.itemFetchSpeed));
        addDrawableChild(itemFetchSpeedField);

        // ---- Start / Stop ----
        startStopButton = ButtonWidget.builder(
                Text.literal(AutoBuilderController.isRunning() ? "Stop" : "Start"),
                b -> toggleStartStop()
        ).dimensions(centerX - 60, row7Y, 120, 20).build();
        addDrawableChild(startStopButton);
    }

    private String playerDetectLabel() {
        return switch (cfg.playerDetectMode) {
            case 2 -> "Phat hien nguoi choi: BAT (tu thoat game)";
            case 3 -> "Phat hien nguoi choi: DUNG LAI + am thanh";
            default -> "Phat hien nguoi choi: TAT";
        };
    }

    private void cyclePlayerDetectMode() {
        cfg.playerDetectMode = cfg.playerDetectMode >= 3 ? 1 : cfg.playerDetectMode + 1;
        playerDetectButton.setMessage(Text.literal(playerDetectLabel()));
        cfg.save();
    }

    private void setNumericOnly(TextFieldWidget field) {
        field.setMaxLength(16);
        field.setTextPredicate(s -> s.isEmpty() || s.matches("-?\\d*(\\.\\d*)?"));
    }

    private String fmt(double v) {
        return v == 0 ? "" : String.valueOf(v);
    }

    private double parse(TextFieldWidget field) {
        try {
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setHere(int point) {
        if (this.client == null || this.client.player == null) return;
        var pos = this.client.player.getPos();
        if (point == 1) {
            x1Field.setText(fmt(pos.x));
            y1Field.setText(fmt(pos.y));
            z1Field.setText(fmt(pos.z));
        } else {
            x2Field.setText(fmt(pos.x));
            y2Field.setText(fmt(pos.y));
            z2Field.setText(fmt(pos.z));
        }
    }

    private void clear(int point) {
        if (point == 1) {
            x1Field.setText("");
            y1Field.setText("");
            z1Field.setText("");
        } else {
            x2Field.setText("");
            y2Field.setText("");
            z2Field.setText("");
        }
    }

    private void toggleStartStop() {
        saveFieldsToConfig();
        if (AutoBuilderController.isRunning()) {
            AutoBuilderController.stop();
        } else {
            AutoBuilderController.start();
        }
        startStopButton.setMessage(Text.literal(AutoBuilderController.isRunning() ? "Stop" : "Start"));
    }

    private void saveFieldsToConfig() {
        cfg.x1 = parse(x1Field);
        cfg.y1 = parse(y1Field);
        cfg.z1 = parse(z1Field);
        cfg.x2 = parse(x2Field);
        cfg.y2 = parse(y2Field);
        cfg.z2 = parse(z2Field);
        cfg.flightSpeed = flightSpeedField != null && !flightSpeedField.getText().isEmpty()
                ? Double.parseDouble(flightSpeedField.getText()) : 0.8;
        cfg.buildSpeed = buildSpeedField != null && !buildSpeedField.getText().isEmpty()
                ? Double.parseDouble(buildSpeedField.getText()) : 4.0;
        cfg.itemFetchSpeed = itemFetchSpeedField != null && !itemFetchSpeedField.getText().isEmpty()
                ? Double.parseDouble(itemFetchSpeedField.getText()) : 4.0;
        cfg.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Vi tri 1 (vung lay do)"), this.width / 2, 25, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Vi tri 2 (vung lay do)"), this.width / 2, 95, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        saveFieldsToConfig();
        if (this.client != null) this.client.setScreen(parent);
    }
}
