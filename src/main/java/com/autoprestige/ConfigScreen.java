package com.autoprestige;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox targetBox, delayBox, maxBox, slotBox;
    private Button modeBtn, toggleBtn;
    private String info = "";

    public ConfigScreen(Screen parent) {
        super(Component.literal("Auto Prestige"));
        this.parent = parent;
    }

    private static int num(EditBox b, int fallback) {
        try {
            return Integer.parseInt(b.getValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    protected void init() {
        PrestigeConfig c = PrestigeConfig.INSTANCE;
        int cx = this.width / 2;
        int boxW = 100, boxX = cx + 20;
        int y = this.height / 2 - 100;

        modeBtn = Button.builder(modeText(), b -> {
            c.useScoreboard = !c.useScoreboard;
            b.setMessage(modeText());
        }).bounds(boxX, y, boxW, 20).build();
        addRenderableWidget(modeBtn);

        y += 28;
        targetBox = box(boxX, y, boxW, String.valueOf(c.targetLevel));
        y += 28;
        delayBox = box(boxX, y, boxW, String.valueOf(c.delayTicks));
        y += 28;
        maxBox = box(boxX, y, boxW, String.valueOf(c.maxPrestiges));
        y += 28;
        slotBox = box(boxX, y, boxW, String.valueOf(c.confirmSlot));

        y += 28;
        addRenderableWidget(Button.builder(Component.literal("DETECTAR"), b -> {
            boolean ok = PrestigeController.detect(Minecraft.getInstance());
            info = ok ? "Nivel y prestige detectados" : "No se pudo leer el scoreboard";
        }).bounds(boxX, y, boxW, 20).build());

        y += 28;
        addRenderableWidget(Button.builder(Component.literal("Guardar configuracion"), b -> {
            apply();
            info = "Configuracion guardada";
        }).bounds(cx - 110, y, 220, 20).build());

        y += 24;
        toggleBtn = Button.builder(toggleText(), b -> {
            apply();
            c.autoEnabled = !c.autoEnabled;
            PrestigeConfig.save();
            b.setMessage(toggleText());
        }).bounds(cx - 110, y, 220, 20).build();
        addRenderableWidget(toggleBtn);

        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Hacer /prestige ahora"), b -> {
            apply();
            onClose();
            PrestigeController.requestNow();
        }).bounds(cx - 110, y, 220, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx - 110, y, 220, 20).build());
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox b = new EditBox(this.font, x, y, w, 20, Component.empty());
        b.setValue(value);
        b.setFilter(s -> s.matches("\\d{0,9}"));
        addRenderableWidget(b);
        return b;
    }

    private Component modeText() {
        return Component.literal("Modo: " + (PrestigeConfig.INSTANCE.useScoreboard ? "SCOREBOARD" : "MANUAL"));
    }

    private Component toggleText() {
        return Component.literal(PrestigeConfig.INSTANCE.autoEnabled
                ? "\u00a7aACTIVADO \u00a7f| Desactivar"
                : "\u00a7cDESACTIVADO \u00a7f| Activar");
    }

    private void apply() {
        PrestigeConfig c = PrestigeConfig.INSTANCE;
        c.targetLevel = num(targetBox, c.targetLevel);
        c.delayTicks = Math.max(1, num(delayBox, c.delayTicks));
        c.maxPrestiges = num(maxBox, c.maxPrestiges);
        c.confirmSlot = num(slotBox, c.confirmSlot);
        PrestigeConfig.save();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int y = this.height / 2 - 100;
        int lx = cx - 110;

        g.drawCenteredString(this.font, this.title, cx, y - 24, 0xFF55FFFF);
        g.drawString(this.font, "Modo de deteccion", lx, y + 6, 0xFFFFFFFF);
        g.drawString(this.font, "Nivel objetivo", lx, y + 34, 0xFFFFFFFF);
        g.drawString(this.font, "Retraso (ticks)", lx, y + 62, 0xFFFFFFFF);
        g.drawString(this.font, "Prestigios maximos", lx, y + 90, 0xFFFFFFFF);
        g.drawString(this.font, "Slot estrella (respaldo)", lx, y + 118, 0xFFFFFFFF);

        String lv = PrestigeController.level >= 0 ? String.valueOf(PrestigeController.level) : "?";
        String pr = PrestigeController.prestige >= 0 ? String.valueOf(PrestigeController.prestige) : "?";
        g.drawString(this.font, "LV: " + lv + "  PRESTIGE: " + pr, lx, y + 146, 0xFF55FFFF);

        int by = y + 146 + 28 + 24 * 3 + 30;
        String line = !info.isEmpty() ? info : PrestigeController.status;
        g.drawString(this.font, line, lx, by, 0xFF55FF55);
        g.drawString(this.font, "Hechos: " + PrestigeController.prestigesDone + "/" + PrestigeConfig.INSTANCE.maxPrestiges
                + "  |  Nivel " + lv + "/" + PrestigeConfig.INSTANCE.targetLevel, lx, by + 12, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        apply();
        this.minecraft.setScreen(parent);
    }
}
