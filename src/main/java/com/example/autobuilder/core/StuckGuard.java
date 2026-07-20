package com.example.autobuilder.core;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Phat hien "ket" khi dang bay toi 1 diem (khong nhuc nhich duoc trong 1 khoang thoi gian), va
 * dua ra hanh dong: RETRY (nhun len + de caller tinh lai duong bay) hoac GIVE_UP (sau vai lan thu
 * van khong duoc thi bo cuoc, chuyen sang muc tieu khac) - tranh treo vinh vien.
 *
 * Day la nguyen nhan chinh gay ra loi "bay ra roi dung im khong lam gi nua": WaypointRouter chi
 * kiem tra DUONG DI co bi chan khong, khong kiem tra chinh DIEM DUNG cuoi cung co nam trong 1 khoi
 * dac hay khong (vd do tinh toan lech 1 chut) - neu diem do khong the toi duoc, truoc day se cho
 * mai mai ma khong co gioi han thoi gian nao ca.
 */
public class StuckGuard {
    private Vec3d lastPos;
    private int stuckTicks;
    private int retries;

    private static final int STUCK_TIMEOUT_TICKS = 24; // ~1.2 giay khong nhuc nhich
    private static final int MAX_RETRIES = 3;

    public enum Result { OK, RETRY, GIVE_UP }

    public Result check(Vec3d currentPos, ClientPlayerEntity player) {
        if (lastPos != null && currentPos.squaredDistanceTo(lastPos) < 0.02 * 0.02) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPos = currentPos;

        if (stuckTicks > STUCK_TIMEOUT_TICKS) {
            stuckTicks = 0;
            retries++;

            if (retries > MAX_RETRIES) {
                retries = 0;
                return Result.GIVE_UP;
            }

            // Nhun tuc thi de thoat ra khoi cho ket, cho lan tinh lai duong bay tiep theo co co hoi khac
            player.setVelocity(0, 0.4, 0);
            player.velocityModified = true;
            return Result.RETRY;
        }

        return Result.OK;
    }

    public void reset() {
        lastPos = null;
        stuckTicks = 0;
        retries = 0;
    }
}
