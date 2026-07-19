package com.example.autobuilder.inventory;

import com.example.autobuilder.AutoBuilderConfig;
import com.example.autobuilder.core.FlightUtil;
import com.example.autobuilder.core.WaypointRouter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bay den tung container (chest/barrel/shulker box/...) trong vung lay nguyen lieu (Vi tri 1 - 2),
 * mo ra va lay nhung item con thieu, cho toi khi du hoac het container trong vung.
 *
 * Thu tu tham quan: uu tien nhung vi tri DA BIET tu truoc (ChestIndex, luu tu lan chay truoc) co
 * chua item dang can NHIEU NHAT truoc, sau do moi den cac container CHUA TUNG mo (de kham pha/cap
 * nhat index). Sau khi mo xong 1 ruong, index se duoc cap nhat lai (ghi nho ca nhung item khong lay).
 */
public class ItemFetchTask {
    private enum Phase { TRAVEL, INTERACT }

    private final Map<Item, Integer> needed;
    private final List<BlockPos> visitOrder = new ArrayList<>();
    private int containerIndex = 0;
    private Phase phase = Phase.TRAVEL;
    private List<Vec3d> currentRoute = null;
    private int routeIndex = 0;
    private final ContainerInteraction interaction = new ContainerInteraction();
    private boolean finished = false;
    private boolean anyItemFound = false;
    private BlockPos currentTargetPos;

    public ItemFetchTask(ClientWorld world, AutoBuilderConfig cfg, Map<Item, Integer> needed) {
        this.needed = new HashMap<>(needed);
        buildVisitOrder(world, cfg);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean foundAnyItem() {
        return anyItemFound;
    }

    public Map<Item, Integer> remainingNeeded() {
        return needed;
    }

    private void buildVisitOrder(ClientWorld world, AutoBuilderConfig cfg) {
        int minX = (int) Math.floor(Math.min(cfg.x1, cfg.x2));
        int maxX = (int) Math.floor(Math.max(cfg.x1, cfg.x2));
        int minY = (int) Math.floor(Math.min(cfg.y1, cfg.y2));
        int maxY = (int) Math.floor(Math.max(cfg.y1, cfg.y2));
        int minZ = (int) Math.floor(Math.min(cfg.z1, cfg.z2));
        int maxZ = (int) Math.floor(Math.max(cfg.z1, cfg.z2));

        Set<BlockPos> allContainers = new LinkedHashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockEntity(pos) instanceof Inventory) {
                        allContainers.add(pos);
                    }
                }
            }
        }

        // 1) Uu tien vi tri DA BIET tu ChestIndex, sap xep theo item dang can NHIEU NHAT truoc.
        List<Item> itemsByNeedDesc = new ArrayList<>(needed.keySet());
        itemsByNeedDesc.sort(Comparator.comparingInt((Item it) -> needed.getOrDefault(it, 0)).reversed());

        Set<BlockPos> added = new LinkedHashSet<>();
        for (Item item : itemsByNeedDesc) {
            if (needed.getOrDefault(item, 0) <= 0) continue;
            for (BlockPos known : ChestIndex.getKnownPositions(item)) {
                if (allContainers.contains(known) && added.add(known)) {
                    visitOrder.add(known);
                }
            }
        }

        // 2) Sau do them cac container CHUA co trong danh sach da them (bao gom ca cai chua tung
        // catalog, de kham pha va cap nhat index cho lan sau).
        for (BlockPos pos : allContainers) {
            if (added.add(pos)) {
                visitOrder.add(pos);
            }
        }
    }

    /** Tra ve true khi da xong (du item hoac het container de kiem tra). */
    public boolean tick(MinecraftClient client, AutoBuilderConfig cfg) {
        if (finished) return true;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            finished = true;
            return true;
        }

        if (isSatisfied() || containerIndex >= visitOrder.size()) {
            finished = true;
            ChestIndex.save();
            return true;
        }

        BlockPos target = visitOrder.get(containerIndex);
        int delayTicks = (int) Math.max(1, cfg.itemFetchSpeed);

        switch (phase) {
            case TRAVEL -> {
                if (currentRoute == null) {
                    Vec3d standPoint = findStandPoint(world, target);
                    currentRoute = WaypointRouter.route(world, player.getPos(), standPoint, target.getY() + 3);
                    routeIndex = 0;
                    currentTargetPos = target;
                }
                Vec3d wp = currentRoute.get(routeIndex);
                if (FlightUtil.flyToward(player, wp, cfg.flightSpeed)) {
                    routeIndex++;
                    if (routeIndex >= currentRoute.size()) {
                        currentRoute = null;
                        FlightUtil.lookAt(player, new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5));
                        interaction.start(client, target, needed);
                        phase = Phase.INTERACT;
                    }
                }
            }
            case INTERACT -> {
                if (interaction.tick(client, delayTicks)) {
                    if (interaction.gotAnyItem()) anyItemFound = true;
                    ChestIndex.recordContents(currentTargetPos, interaction.getAllItemsSeen());
                    containerIndex++;
                    phase = Phase.TRAVEL;
                }
            }
        }

        return false;
    }

    /**
     * Tim 1 diem dung MO (khong bi vat can) ngay canh container de tuong tac, thay vi luon dung
     * y+1.2 ngay tren no (co the bi ket neu tren dau ruong co block khac, vd tran nha thap).
     */
    private Vec3d findStandPoint(ClientWorld world, BlockPos containerPos) {
        Direction[] tryOrder = {
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
        };
        for (Direction d : tryOrder) {
            BlockPos side = containerPos.offset(d, 2);
            BlockState state = world.getBlockState(side);
            if (state.isAir() || state.getCollisionShape(world, side).isEmpty()) {
                return new Vec3d(
                        containerPos.getX() + 0.5 + d.getOffsetX() * 1.5,
                        containerPos.getY() + 0.3 + Math.max(0, d.getOffsetY()) * 1.2,
                        containerPos.getZ() + 0.5 + d.getOffsetZ() * 1.5
                );
            }
        }
        // Khong tim duoc huong nao thoang - danh phai dung tam tren, nhung lui ra xa hon truoc
        return new Vec3d(containerPos.getX() + 0.5, containerPos.getY() + 1.5, containerPos.getZ() + 0.5);
    }

    private boolean isSatisfied() {
        for (int v : needed.values()) {
            if (v > 0) return false;
        }
        return true;
    }
}
