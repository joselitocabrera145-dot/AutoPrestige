package com.autoprestige;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrestigeController {
    private enum State { IDLE, WAIT_GUI, WAIT_CLICK, WAIT_CLOSE, COOLDOWN }

    private static final Pattern LEVEL = Pattern.compile("(?i)\\blevel\\s*:?\\s*([\\d.,]+)");
    private static final Pattern PRESTIGE = Pattern.compile("(?i)\\bprestige\\s*:?\\s*([\\d.,]+)");

    private static State state = State.IDLE;
    private static int timer = 0;
    private static boolean manualRequest = false;

    public static int level = -1;
    public static int prestige = -1;
    public static int prestigesDone = 0;
    public static String status = "";

    public static void requestNow() {
        manualRequest = true;
    }

    public static boolean isBusy() {
        return state != State.IDLE;
    }

    public static void toggleAuto() {
        PrestigeConfig.INSTANCE.autoEnabled = !PrestigeConfig.INSTANCE.autoEnabled;
        PrestigeConfig.save();
        msg(PrestigeConfig.INSTANCE.autoEnabled ? "ACTIVADO" : "DESACTIVADO");
    }

    public static void resetCounter() {
        prestigesDone = 0;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            state = State.IDLE;
            manualRequest = false;
            return;
        }
        PrestigeConfig cfg = PrestigeConfig.INSTANCE;

        if (cfg.useScoreboard && mc.player.tickCount % 5 == 0) {
            detect(mc);
        }

        switch (state) {
            case IDLE -> {
                boolean auto = cfg.autoEnabled
                        && cfg.useScoreboard
                        && prestigesDone < cfg.maxPrestiges
                        && level >= cfg.targetLevel;
                if (manualRequest || auto) {
                    manualRequest = false;
                    mc.player.connection.sendCommand(cfg.command);
                    state = State.WAIT_GUI;
                    timer = 0;
                    status = "Abriendo /" + cfg.command + "...";
                }
            }
            case WAIT_GUI -> {
                timer++;
                if (isPrestigeScreen(mc)) {
                    state = State.WAIT_CLICK;
                    timer = 0;
                } else if (timer > 100) {
                    status = "El menu no se abrio (timeout)";
                    startCooldown(60);
                }
            }
            case WAIT_CLICK -> {
                if (!isPrestigeScreen(mc)) {
                    status = "Menu cerrado antes de confirmar";
                    startCooldown(40);
                    return;
                }
                timer++;
                if (timer >= Math.max(1, cfg.delayTicks)) {
                    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
                    int slot = cfg.confirmSlot;
                    if (slot < 0 || slot >= screen.getMenu().slots.size()) {
                        status = "Slot invalido: " + slot;
                        mc.player.closeContainer();
                        startCooldown(60);
                        return;
                    }
                    mc.gameMode.handleInventoryMouseClick(
                            screen.getMenu().containerId, slot, 0, ClickType.PICKUP, mc.player);
                    prestigesDone++;
                    status = "Prestige confirmado (" + prestigesDone + ")";
                    state = State.WAIT_CLOSE;
                    timer = 0;
                }
            }
            case WAIT_CLOSE -> {
                timer++;
                if (timer >= Math.max(1, cfg.delayTicks)) {
                    if (mc.screen instanceof AbstractContainerScreen<?>) {
                        mc.player.closeContainer();
                    }
                    level = -1; // forzar nueva lectura del scoreboard
                    startCooldown(40);
                }
            }
            case COOLDOWN -> {
                timer--;
                if (timer <= 0) state = State.IDLE;
            }
        }
    }

    private static void startCooldown(int ticks) {
        state = State.COOLDOWN;
        timer = ticks;
    }

    private static boolean isPrestigeScreen(Minecraft mc) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> s)) return false;
        String title = s.getTitle().getString().toLowerCase();
        return title.contains(PrestigeConfig.INSTANCE.guiTitle.toLowerCase());
    }

    /** Lee "LEVEL: x" y "PRESTIGE: y" de la barra lateral (scoreboard). */
    public static boolean detect(Minecraft mc) {
        try {
            Scoreboard sb = mc.level.getScoreboard();
            Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (obj == null) return false;
            int lv = -1, pr = -1;
            for (PlayerScoreEntry e : sb.listPlayerScores(obj)) {
                PlayerTeam team = sb.getPlayersTeam(e.owner());
                Component c = PlayerTeam.formatNameForTeam(team, e.ownerName());
                String text = ChatFormatting.stripFormatting(c.getString());
                if (text == null) continue;
                Matcher m = LEVEL.matcher(text);
                if (m.find()) lv = toInt(m.group(1));
                m = PRESTIGE.matcher(text);
                if (m.find()) pr = toInt(m.group(1));
            }
            if (lv >= 0) level = lv;
            if (pr >= 0) prestige = pr;
            return lv >= 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    public static void msg(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[AutoPrestige] " + text), false);
        }
    }
}
