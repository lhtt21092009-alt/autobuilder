package com.example.autobuilder.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Kiem tra dinh ky co nguoi choi khac xuat hien trong tam nhin (dang duoc client tai/hien thi -
 * tuc la trong pham vi render/tracking cua server) khong. Theo che do da chon trong config:
 *  1 = TAT: khong lam gi.
 *  2 = BAT (thoat game): tu dong disconnect ngay lap tuc.
 *  3 = DUNG LAI + AM THANH: bao AutoBuilderController dung lai, phat am thanh + chat canh bao,
 *      lien tuc nhac lai moi vai giay cho toi khi nguoi choi tu xu ly (bam Stop hoac roi di).
 */
public final class PlayerWatch {
    private PlayerWatch() {}

    private static int alertCooldown = 0;

    public enum Action { NONE, PAUSE }

    public static Action check(MinecraftClient client, int mode) {
        if (mode == 1) return Action.NONE;
        if (client.player == null || client.world == null) return Action.NONE;

        boolean otherPlayerVisible = false;
        for (AbstractClientPlayerEntity other : client.world.getPlayers()) {
            if (other == client.player) continue;
            otherPlayerVisible = true;
            break;
        }

        if (!otherPlayerVisible) return Action.NONE;

        if (mode == 2) {
            client.execute(client::disconnect);
            return Action.PAUSE;
        }

        // mode == 3
        if (alertCooldown <= 0) {
            client.player.sendMessage(Text.literal("Auto Builder: PHAT HIEN NGUOI CHOI KHAC - da tu dung lai!"), false);
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1.0f, 1.0f);
            alertCooldown = 60; // nhac lai moi 3 giay neu van con nguoi o gan
        } else {
            alertCooldown--;
        }
        return Action.PAUSE;
    }

    public static void resetAlertCooldown() {
        alertCooldown = 0;
    }
}
