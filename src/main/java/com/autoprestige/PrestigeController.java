package com.autoprestige;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrestigeController {
    private enum State { IDLE, WAIT_GUI, WAIT_CLICK, CLOSING, COOLDOWN }

    private static final Pattern LEVEL = Pattern.compile("(?i)level\\D{0,8}?(\\d[\\d.,]*)");
    private static final Pattern PRESTIGE = Pattern.compile("(?i)prestige\\D{0,8}?(\\d[\\d.,]*)");
    private static final Pattern LEVEL_UP = Pattern.compile("(?i)level\\s+\\d[\\d.,]*\\D{1,4}(\\d[\\d.,]*)");

    // Letras en versalitas (small caps) -> letras normales
    private static final char[] SC_FROM = {
            '\u1D00', '\u0299', '\u1D04', '\u1D05', '\u1D07', '\uA730', '\u0262', '\u029C', '\u026A',
            '\u1D0A', '\u1D0B', '\u029F', '\u1D0D', '\u0274', '\u1D0F', '\u1D18', '\u01EB', '\u0280',
            '\uA731', '\u1D1B', '\u1D1C', '\u1D20', '\u1D21', '\u028F', '\u1D22'
    };
    private static final char[] SC_TO = "abcdefghijklmnopqrstuvwyz".toCharArray();

    private static State state = State.IDLE;
    private static int timer = 0;
    private static boolean manualRequest = false;
    private static boolean hooked = false;

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

    /** Quita colores, pasa letras raras a normales y deja solo texto legible. */
    private static String norm(String s) {
        if (s == null) return "";
        s = ChatFormatting.stripFormatting(s);
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            for (int k = 0; k < SC_FROM.length; k++) {
                if (ch == SC_FROM[k]) {
                    ch = SC_TO[k];
                    break;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static void hookChat() {
        if (hooked) return;
        hooked = true;
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            try {
                String text = norm(message.getString());
                Matcher m = LEVEL_UP.matcher(text);
                if (m.find()) {
                    int lv = toInt(m.group(1));
                    if (lv >= 0) level = lv;
                }
            } catch (Throwable ignored) {
            }
        });
    }

    /** true si hay un menu de cofre/servidor abierto (no el inventario propio). */
    private static boolean serverMenuOpen(Minecraft mc) {
        return mc.screen instanceof AbstractContainerScreen<?>
                && !(mc.screen instanceof InventoryScreen);
    }

    private static void forceClose(Minecraft mc) {
        mc.player.closeContainer();
        if (serverMenuOpen(mc)) {
            mc.setScreen(null);
        }
    }

    /** Busca la estrella (nether star) en el menu; si no hay, usa el slot configurado. */
    private static int findStarSlot(AbstractContainerScreen<?> screen, int configured) {
        List<Slot> slots = screen.getMenu().slots;
        int top = Math.max(0, slots.size() - 36); // quita el inventario del jugador
        for (int i = 0; i < top; i++) {
            if (slots.get(i).getItem().is(Items.NETHER_STAR)) {
                return i;
            }
        }
        return configured;
    }

    public static void tick(Minecraft mc) {
        hookChat();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            state = State.IDLE;
            manualRequest = false;
            return;
        }
        PrestigeConfig cfg = PrestigeConfig.INSTANCE;

        if (cfg.useScoreboard && mc.player.tickCount % 5 == 0) {
            scan(mc, false);
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
                if (timer < Math.max(1, cfg.delayTicks)) return;

                AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
                int slot = findStarSlot(screen, cfg.confirmSlot);
                if (slot < 0 || slot >= screen.getMenu().slots.size()) {
                    status = "Slot invalido: " + slot;
                    forceClose(mc);
                    startCooldown(60);
                    return;
                }
                // Esperar a que carguen los objetos del menu (maximo 2 segundos)
                boolean hasItem = !screen.getMenu().slots.get(slot).getItem().isEmpty();
                if (!hasItem && timer < 40) return;

                mc.gameMode.handleInventoryMouseClick(
                        screen.getMenu().containerId, slot, 0, ClickType.PICKUP, mc.player);
                prestigesDone++;
                status = "Prestige confirmado (" + prestigesDone + ") slot " + slot;
                forceClose(mc); // cerrar justo despues del clic
                state = State.CLOSING;
                timer = 0;
            }
            case CLOSING -> {
                // Durante ~2 segundos cierra cualquier menu del servidor que aparezca
                timer++;
                if (serverMenuOpen(mc)) {
                    forceClose(mc);
                }
                if (timer >= 40) {
                    level = -1; // forzar nueva lectura
                    startCooldown(20);
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
        String title = norm(s.getTitle().getString()).toLowerCase();
        return title.contains(PrestigeConfig.INSTANCE.guiTitle.toLowerCase());
    }

    /** Boton DETECTAR: escanea y escribe en el chat lo que encontro. */
    public static boolean detect(Minecraft mc) {
        if (mc.level == null) return false;
        scan(mc, true);
        return level >= 0;
    }

    /** Revisa TODOS los scoreboards (no solo el de la barra lateral normal). */
    private static void scan(Minecraft mc, boolean debug) {
        try {
            Scoreboard sb = mc.level.getScoreboard();
            int lv = -1, pr = -1, lines = 0;
            StringBuilder dbg = new StringBuilder();
            for (Objective obj : sb.getObjectives()) {
                for (PlayerScoreEntry e : sb.listPlayerScores(obj)) {
                    PlayerTeam team = sb.getPlayersTeam(e.owner());
                    Component c = PlayerTeam.formatNameForTeam(team, e.ownerName());
                    String text = norm(c.getString());
                    lines++;
                    if (debug && lines <= 15) dbg.append("[").append(text).append("] ");
                    Matcher m = LEVEL.matcher(text);
                    if (m.find()) lv = toInt(m.group(1));
                    m = PRESTIGE.matcher(text);
                    if (m.find()) pr = toInt(m.group(1));
                }
            }
            if (lv >= 0) level = lv;
            if (pr >= 0) prestige = pr;
            if (debug) {
                msg("Objetivos: " + sb.getObjectives().size() + ", lineas: " + lines
                        + " | nivel=" + level + " prestige=" + prestige);
                if (dbg.length() > 0) msg(dbg.toString());
            }
        } catch (Throwable t) {
            if (debug) msg("Error leyendo scoreboard: " + t);
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
