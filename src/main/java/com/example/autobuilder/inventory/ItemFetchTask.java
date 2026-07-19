package com.example.autobuilder.inventory;

import com.example.autobuilder.AutoBuilderConfig;
import com.example.autobuilder.core.FlightUtil;
import com.example.autobuilder.core.WaypointRouter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bay den tung container (chest/barrel/shulker box/...) trong vung lay nguyen lieu (Vi tri 1 - 2),
 * mo ra va lay nhung item con thieu, cho toi khi du hoac het container trong vung.
 */
public class ItemFetchTask {
    private enum Phase { TRAVEL, INTERACT }

    private final Map<Item, Integer> needed;
    private final List<BlockPos> containers = new ArrayList<>();
    private int containerIndex = 0;
    private Phase phase = Phase.TRAVEL;
    private List<Vec3d> currentRoute = null;
    private int routeIndex = 0;
    private final ContainerInteraction interaction = new ContainerInteraction();
    private boolean finished = false;
    private boolean anyItemFound = false;

    public ItemFetchTask(ClientWorld world, AutoBuilderConfig cfg, Map<Item, Integer> needed) {
        this.needed = new HashMap<>(needed);
        scanContainers(world, cfg);
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

    private void scanContainers(ClientWorld world, AutoBuilderConfig cfg) {
        int minX = (int) Math.floor(Math.min(cfg.x1, cfg.x2));
        int maxX = (int) Math.floor(Math.max(cfg.x1, cfg.x2));
        int minY = (int) Math.floor(Math.min(cfg.y1, cfg.y2));
        int maxY = (int) Math.floor(Math.max(cfg.y1, cfg.y2));
        int minZ = (int) Math.floor(Math.min(cfg.z1, cfg.z2));
        int maxZ = (int) Math.floor(Math.max(cfg.z1, cfg.z2));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockEntity(pos) instanceof Inventory) {
                        containers.add(pos);
                    }
                }
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

        if (isSatisfied() || containerIndex >= containers.size()) {
            finished = true;
            return true;
        }

        BlockPos target = containers.get(containerIndex);
        Vec3d standPoint = new Vec3d(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5);

        int delayTicks = (int) Math.max(1, cfg.itemFetchSpeed);

        switch (phase) {
            case TRAVEL -> {
                if (currentRoute == null) {
                    currentRoute = WaypointRouter.route(world, player.getPos(), standPoint, target.getY() + 3);
                    routeIndex = 0;
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
                    containerIndex++;
                    phase = Phase.TRAVEL;
                }
            }
        }

        return false;
    }

    private boolean isSatisfied() {
        for (int v : needed.values()) {
            if (v > 0) return false;
        }
        return true;
    }
}
