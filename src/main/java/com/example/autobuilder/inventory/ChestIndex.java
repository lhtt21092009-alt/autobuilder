package com.example.autobuilder.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Luu lai vi tri (BlockPos) cua tung ruong da tung mo, kem theo cac loai item da thay trong do,
 * de lan chay sau khong can mo lai tat ca ruong - chi can den thang cho da biet co san item can.
 * Ghi ra config/autobuilder_chestindex.json, ton tai qua nhieu lan choi.
 */
public final class ChestIndex {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autobuilder_chestindex.json");

    // item id (vd "minecraft:oak_planks") -> danh sach "x,y,z" cac vi tri ruong tung co item nay
    private static Map<String, List<String>> itemToPositions = load();

    // vi tri ruong nao da tung duoc mo/catalog roi (tranh ghi de lung tung, chi can biet "da biet" hay chua)
    private static final Set<String> cataloguedPositions = new LinkedHashSet<>();

    private ChestIndex() {}

    private static Map<String, List<String>> load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                Map<String, List<String>> data = GSON.fromJson(reader, new TypeToken<Map<String, List<String>>>() {}.getType());
                if (data != null) return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(itemToPositions, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos parsePos(String key) {
        String[] parts = key.split(",");
        return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /** Ghi nhan: tai vi tri nay, cac item sau da duoc tim thay (goi sau moi lan mo/kiem tra 1 ruong). */
    public static void recordContents(BlockPos pos, Set<Item> itemsFound) {
        String posStr = posKey(pos);
        cataloguedPositions.add(posStr);

        for (Item item : itemsFound) {
            String itemId = Registries.ITEM.getId(item).toString();
            List<String> positions = itemToPositions.computeIfAbsent(itemId, k -> new ArrayList<>());
            if (!positions.contains(posStr)) {
                positions.add(posStr);
            }
        }
    }

    /** Vi tri nay da tung duoc mo/catalog trong phien lam viec nay chua. */
    public static boolean isCatalogued(BlockPos pos) {
        return cataloguedPositions.contains(posKey(pos));
    }

    /** Danh sach vi tri ruong DA BIET (tu lan truoc) co chua item nay. */
    public static List<BlockPos> getKnownPositions(Item item) {
        String itemId = Registries.ITEM.getId(item).toString();
        List<String> positions = itemToPositions.get(itemId);
        if (positions == null) return new ArrayList<>();

        List<BlockPos> result = new ArrayList<>();
        for (String p : positions) {
            try {
                result.add(parsePos(p));
            } catch (Exception ignored) {
                // du lieu hong, bo qua
            }
        }
        return result;
    }
}
