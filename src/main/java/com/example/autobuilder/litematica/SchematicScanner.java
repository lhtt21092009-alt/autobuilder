package com.example.autobuilder.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Doc du lieu tu mod Litematica (chi goi API cong khai, khong dung mixin) de:
 *  - Dem so luong tung loai vat lieu (Item) con thieu, bang cach TU QUET block trong ban ve dang
 *    hien (khong dua vao Material List co san cua Litematica, theo yeu cau).
 *  - Tim danh sach vi tri con thieu, SAP XEP THEO Y TANG DAN (xay tu duoi len) vi khi dat block
 *    can co it nhat 1 mat ke da la block dac trong world that de click vao - xay tu duoi len moi
 *    dam bao luon co diem tua.
 *
 * LUU Y: dung getSubRegionBoxes(...) chu KHONG dung getEclosingBox() - cai sau chi co gia tri khi
 * nguoi dung bat tuy chon hien thi khung bao (mac dinh tat), neu khong luon tra ve null.
 */
public final class SchematicScanner {
    private SchematicScanner() {}

    public static boolean isLitematicaLoaded() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }

    public record ScanResult(List<BlockPos> missingPositionsBottomUp, Map<Item, Integer> materialsNeeded) {}

    /**
     * Lay ra chi nhung vi tri thuoc "HANG DAU TIEN" trong danh sach da sap xep (cung gia tri Z voi
     * phan tu dau tien) - dung de chi lay/xay du cho 1 hang mot, thay vi ca phan con lai cua ban ve
     * (tranh lay qua nhieu do 1 luc).
     */
    public static List<BlockPos> currentRowOnly(List<BlockPos> sortedMissing) {
        List<BlockPos> row = new ArrayList<>();
        if (sortedMissing.isEmpty()) return row;
        int rowZ = sortedMissing.get(0).getZ();
        for (BlockPos pos : sortedMissing) {
            if (pos.getZ() == rowZ) row.add(pos);
        }
        return row;
    }

    /** Tinh bang tong hop vat lieu can dung CHI cho danh sach vi tri duoc truyen vao (vd 1 hang). */
    public static Map<Item, Integer> materialsForPositions(List<BlockPos> positions) {
        Map<Item, Integer> materials = new HashMap<>();
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) return materials;

        for (BlockPos pos : positions) {
            BlockState schemState = schematicWorld.getBlockState(pos);
            if (schemState.isAir()) continue;
            Item item = schemState.getBlock().asItem();
            if (item != null && item != net.minecraft.item.Items.AIR) {
                materials.merge(item, 1, Integer::sum);
            }
        }
        return materials;
    }

    /**
     * Quet toan bo vung dang hien (theo layer range hien tai cua Litematica) mot lan,
     * tra ve danh sach vi tri con thieu (sap xep Y tang dan, roi den khoang cach toi nguoi choi)
     * va bang tong hop vat lieu con thieu.
     */
    public static ScanResult scan(ClientWorld world, Vec3d playerPos, Set<BlockPos> skip, int hardCapCells) {
        List<BlockPos> missing = new ArrayList<>();
        Map<Item, Integer> materials = new HashMap<>();

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null || world == null) return new ScanResult(missing, materials);

        LayerRange layerRange = DataManager.getRenderLayerRange();
        int checked = 0;

        outer:
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (!placement.isEnabled()) continue;

            for (Box box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values()) {
                BlockPos p1 = box.getPos1();
                BlockPos p2 = box.getPos2();
                if (p1 == null || p2 == null) continue;

                int minX = Math.min(p1.getX(), p2.getX());
                int maxX = Math.max(p1.getX(), p2.getX());
                int minZ = Math.min(p1.getZ(), p2.getZ());
                int maxZ = Math.max(p1.getZ(), p2.getZ());
                int minY = Math.max(Math.min(p1.getY(), p2.getY()), layerRange.getLayerMin());
                int maxY = Math.min(Math.max(p1.getY(), p2.getY()), layerRange.getLayerMax());

                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (++checked > hardCapCells) break outer;

                            BlockPos pos = new BlockPos(x, y, z);
                            if (!layerRange.isPositionWithinRange(pos)) continue;

                            BlockState schemState = schematicWorld.getBlockState(pos);
                            if (schemState.isAir()) continue;

                            BlockState realState = world.getBlockState(pos);
                            if (schemState.equals(realState)) continue; // da dat dung roi

                            if (!skip.contains(pos)) {
                                missing.add(pos);
                            }

                            Item item = schemState.getBlock().asItem();
                            if (item != null && item != net.minecraft.item.Items.AIR) {
                                materials.merge(item, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        double px = playerPos.x, py = playerPos.y, pz = playerPos.z;
        // Xay theo "hang" doc truc Z, tu Nam (+Z) sang Bac (-Z): moi hang xay het toan bo truc Y
        // (tu duoi len, luon co diem tua) roi moi chuyen sang hang tiep theo, thay vi nhay lung tung.
        missing.sort((a, b) -> {
            int cmpRow = Integer.compare(b.getZ(), a.getZ()); // Z giam dan = Nam -> Bac
            if (cmpRow != 0) return cmpRow;
            int cmpY = Integer.compare(a.getY(), b.getY()); // trong 1 hang, xay het truc Y truoc
            if (cmpY != 0) return cmpY;
            double da = distSq(a, px, py, pz);
            double db = distSq(b, px, py, pz);
            return Double.compare(da, db);
        });

        return new ScanResult(missing, materials);
    }

    private static double distSq(BlockPos pos, double px, double py, double pz) {
        double dx = (pos.getX() + 0.5) - px;
        double dy = (pos.getY() + 0.5) - py;
        double dz = (pos.getZ() + 0.5) - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Uoc tinh chieu cao dinh cao nhat cua toan bo cac placement dang bat, de bay vong len tren tranh vuong. */
    public static int getHighestEnabledPlacementTop() {
        int top = Integer.MIN_VALUE;
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (!placement.isEnabled()) continue;
            for (Box box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values()) {
                if (box.getPos1() == null || box.getPos2() == null) continue;
                top = Math.max(top, Math.max(box.getPos1().getY(), box.getPos2().getY()));
            }
        }
        return top == Integer.MIN_VALUE ? 0 : top;
    }

    public static boolean isPlacedCorrectly(ClientWorld world, BlockPos pos) {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null || world == null) return true;
        BlockState schemState = schematicWorld.getBlockState(pos);
        if (schemState.isAir()) return true;
        return schemState.equals(world.getBlockState(pos));
    }

    public static BlockState getSchematicBlockState(BlockPos pos) {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        return schematicWorld != null ? schematicWorld.getBlockState(pos) : null;
    }
}
