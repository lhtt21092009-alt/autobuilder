package com.example.autobuilder.core;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Ham dung chung cho viec bay (dieu khien van toc) va kiem tra duong bay co bi vuong khong.
 * Tach rieng ra day de dung chung cho ca ItemFetchTask va BuildTask.
 */
public final class FlightUtil {
    private static final double PLAYER_RADIUS = 0.35;

    private FlightUtil() {}

    /** Bay ve phia target voi toc do cho truoc. Tra ve true neu da toi noi (trong pham vi 0.4 block). */
    public static boolean flyToward(ClientPlayerEntity player, Vec3d target, double speed) {
        Vec3d current = player.getPos();
        Vec3d diff = target.subtract(current);
        double distance = diff.length();

        if (distance < 0.4) {
            player.setVelocity(0, 0, 0);
            return true;
        }

        if (!player.getAbilities().flying && player.getAbilities().allowFlying) {
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
        }

        double moveAmount = Math.min(speed, distance);
        Vec3d direction = diff.normalize();
        player.setVelocity(direction.multiply(moveAmount));

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.y));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.velocityModified = true;

        return false;
    }

    /** Xoay mat nhin ve phia 1 diem, khong di chuyen (dung khi dung yen truoc mat chest/block de tuong tac). */
    public static void lookAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d eyePos = player.getEyePos();
        Vec3d diff = target.subtract(eyePos);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, horizontalDist));
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    /**
     * Kiem tra doan thang tu 'from' toi 'to' co bi block dac chan khong, co xet be rong nguoi choi
     * (4 diem lech sang 2 ben) va ca chieu cao (chan/dau), tranh truong hop tia don bo sot va cham.
     */
    public static boolean isPathClear(ClientWorld world, Vec3d from, Vec3d to) {
        double distance = from.distanceTo(to);
        if (distance < 0.1) return true;

        int steps = (int) Math.ceil(distance / 0.5);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;

            if (isSolidNear(world, x, y, z) || isSolidNear(world, x, y + 1.5, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSolidNear(ClientWorld world, double x, double y, double z) {
        double[][] offsets = {
                {0, 0}, {PLAYER_RADIUS, 0}, {-PLAYER_RADIUS, 0}, {0, PLAYER_RADIUS}, {0, -PLAYER_RADIUS}
        };
        for (double[] off : offsets) {
            BlockPos pos = BlockPos.ofFloored(x + off[0], y, z + off[1]);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(world, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
