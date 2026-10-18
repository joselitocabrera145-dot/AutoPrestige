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
    /** Slot del boton verde en el cofre (fila 4, primer slot = 27). */
    public int confirmSlot = 27;
    public String command = "prestige";
    /** Texto que debe contener el titulo del cofre. */
    public String guiTitle = "Prestige";

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                PrestigeConfig c = GSON.fromJson(Files.readString(FILE), PrestigeConfig.class);
                if (c != null) INSTANCE = c;
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
