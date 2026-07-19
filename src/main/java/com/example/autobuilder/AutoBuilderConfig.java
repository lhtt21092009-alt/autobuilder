package com.example.autobuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config cua Auto Builder, ghi ra config/autobuilder.json.
 */
public class AutoBuilderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autobuilder.json");

    // Vung lay nguyen lieu (2 goc doi dien cua 1 khoi hop chua cac ruong/thung/barrel...)
    public double x1, y1, z1;
    public double x2, y2, z2;

    public double flightSpeed = 0.8; // block/tick

    // 1 = tat, 2 = bat (tu thoat game), 3 = dung lai + phat am thanh canh bao
    public int playerDetectMode = 1;

    public double buildSpeed = 4.0;    // so tick cho moi lan dat 1 block (nho hon = nhanh hon)
    public double itemFetchSpeed = 4.0; // so tick cho moi thao tac lay do trong ruong

    public static AutoBuilderConfig INSTANCE = load();

    public static AutoBuilderConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                AutoBuilderConfig cfg = GSON.fromJson(reader, AutoBuilderConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new AutoBuilderConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
