package com.autoprestige;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PrestigeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autoprestige.json");

    public static PrestigeConfig INSTANCE = new PrestigeConfig();

    /** Si esta activo, hace prestige solo cuando el nivel >= targetLevel. */
    public boolean autoEnabled = false;
    /** true = lee nivel/prestige del scoreboard; false = solo manual (tecla / boton). */
    public boolean useScoreboard = true;
    public int targetLevel = 1150;
    /** Ticks de espera entre pasos (20 ticks = 1 segundo). */
    public int delayTicks = 4;
    public int maxPrestiges = 350;
    /** Slot de la estrella (por si no la encuentra): fila 2, columna del medio = 13. */
    public int confirmSlot = 13;
    public String command = "prestige";
    /** Texto que debe contener el titulo del cofre. */
    public String guiTitle = "Prestige";

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                PrestigeConfig c = GSON.fromJson(Files.readString(FILE), PrestigeConfig.class);
                if (c != null) INSTANCE = c;
                // Migracion: 27 era el slot del panel verde (version vieja)
                if (INSTANCE.confirmSlot == 27) {
                    INSTANCE.confirmSlot = 13;
                    save();
                }
            } else {
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
