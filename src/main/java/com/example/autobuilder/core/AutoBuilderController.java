package com.example.autobuilder.core;

import com.example.autobuilder.AutoBuilderConfig;
import com.example.autobuilder.inventory.ItemFetchTask;
import com.example.autobuilder.litematica.SchematicScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dieu phoi toan bo qua trinh: quet nguyen lieu con thieu -> bay di lay do (ItemFetchTask) ->
 * xay (BuildTask) -> neu con thieu nguyen lieu quay lai lay tiep -> lap lai cho toi khi xay xong
 * toan bo lop dang hien, hoac het nguyen lieu 2 lan lien tiep thi dung va bao loi.
 */
public class AutoBuilderController {
    private enum Phase { IDLE, FETCHING, BUILDING, PAUSED_SAFETY }

    private static Phase phase = Phase.IDLE;
    private static Phase phaseBeforePause = Phase.IDLE;
    private static ItemFetchTask fetchTask;
    private static BuildTask buildTask;
    private static final Set<BlockPos> buildFailSkip = new HashSet<>();
    private static int consecutiveEmptyFetches = 0;

    public static boolean isRunning() {
        return phase != Phase.IDLE;
    }

    public static void start() {
        if (!SchematicScanner.isLitematicaLoaded()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Auto Builder: khong tim thay mod Litematica dang cai."), false);
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        if (player != null && !player.getAbilities().allowFlying) {
            player.sendMessage(Text.literal("Auto Builder: canh bao - ban khong duoc phep bay tren server nay."), false);
        }

        buildFailSkip.clear();
        consecutiveEmptyFetches = 0;
        PlayerWatch.resetAlertCooldown();
        beginFetchPhase(client, world, player);
    }

    public static void stop() {
        phase = Phase.IDLE;
        fetchTask = null;
        buildTask = null;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) player.setVelocity(0, 0, 0);
    }

    public static void tick(MinecraftClient client) {
        if (phase == Phase.IDLE) return;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            stop();
            return;
        }

        AutoBuilderConfig cfg = AutoBuilderConfig.INSTANCE;

        PlayerWatch.Action safety = PlayerWatch.check(client, cfg.playerDetectMode);
        if (safety == PlayerWatch.Action.PAUSE) {
            if (phase != Phase.PAUSED_SAFETY) {
                phaseBeforePause = phase;
                phase = Phase.PAUSED_SAFETY;
            }
            player.setVelocity(0, 0, 0);
            return;
        } else if (phase == Phase.PAUSED_SAFETY) {
            phase = phaseBeforePause;
        }

        switch (phase) {
            case FETCHING -> {
                if (fetchTask.tick(client, cfg)) {
                    if (fetchTask.foundAnyItem()) {
                        consecutiveEmptyFetches = 0;
                    } else {
                        consecutiveEmptyFetches++;
                    }

                    if (consecutiveEmptyFetches >= 2) {
                        player.sendMessage(Text.literal("Auto Builder: het nguyen lieu trong vung lay do - dung lai."), false);
                        stop();
                        return;
                    }

                    beginBuildPhase(world, player);
                }
            }
            case BUILDING -> {
                if (buildTask.tick(client, cfg, buildFailSkip)) {
                    if (buildTask.isMissingMaterial()) {
                        beginFetchPhase(client, world, player);
                    } else {
                        // Xong 1 vong build -> quet lai xem con block nao khac (vd truoc chua co diem tua)
                        var rescan = SchematicScanner.scan(world, player.getPos(), buildFailSkip, 400_000);
                        if (rescan.missingPositionsBottomUp().isEmpty()) {
                            player.sendMessage(Text.literal("Auto Builder: xay xong toan bo lop dang hien!"), false);
                            playDoneSound(client);
                            stop();
                        } else {
                            buildTask = new BuildTask(rescan.missingPositionsBottomUp());
                        }
                    }
                }
            }
            default -> {}
        }
    }

    private static void beginFetchPhase(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        var scan = SchematicScanner.scan(world, player.getPos(), buildFailSkip, 400_000);
        if (scan.missingPositionsBottomUp().isEmpty()) {
            player.sendMessage(Text.literal("Auto Builder: khong con block nao con thieu trong lop dang hien - da xong!"), false);
            playDoneSound(client);
            stop();
            return;
        }

        Map<Item, Integer> needed = new HashMap<>(scan.materialsNeeded());
        subtractInventory(player, needed);

        if (needed.isEmpty() || allZero(needed)) {
            // Da du do trong tui roi, khong can di lay - vao xay luon
            buildTask = new BuildTask(scan.missingPositionsBottomUp());
            phase = Phase.BUILDING;
            return;
        }

        fetchTask = new ItemFetchTask(world, AutoBuilderConfig.INSTANCE, needed);
        phase = Phase.FETCHING;
    }

    private static void beginBuildPhase(ClientWorld world, ClientPlayerEntity player) {
        var scan = SchematicScanner.scan(world, player.getPos(), buildFailSkip, 400_000);
        if (scan.missingPositionsBottomUp().isEmpty()) {
            player.sendMessage(Text.literal("Auto Builder: khong con block nao con thieu trong lop dang hien - da xong!"), false);
            playDoneSound(MinecraftClient.getInstance());
            stop();
            return;
        }
        buildTask = new BuildTask(scan.missingPositionsBottomUp());
        phase = Phase.BUILDING;
    }

    private static void subtractInventory(ClientPlayerEntity player, Map<Item, Integer> needed) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            Item item = inv.getStack(i).getItem();
            Integer need = needed.get(item);
            if (need != null && need > 0) {
                int has = inv.getStack(i).getCount();
                needed.put(item, Math.max(0, need - has));
            }
        }
    }

    private static boolean allZero(Map<Item, Integer> map) {
        for (int v : map.values()) {
            if (v > 0) return false;
        }
        return true;
    }

    private static void playDoneSound(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        client.world.playSound(client.player, client.player.getBlockPos(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
    }
}
