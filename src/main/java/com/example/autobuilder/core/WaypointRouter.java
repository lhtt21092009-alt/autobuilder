package com.example.autobuilder.core;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Tinh 1 danh sach diem trung gian (waypoint) de bay tu "from" toi "to" ma tranh vuong vat can:
 * neu duong thang khong bi chan, tra ve thang [to]. Neu bi chan, do tim do cao THAP NHAT vua
 * du de bay vong qua (len - ngang - xuong), khong luon bay len dinh cao nhat.
 */
public final class WaypointRouter {
    private WaypointRouter() {}

    public static List<Vec3d> route(ClientWorld world, Vec3d from, Vec3d to, double buildTopHint) {
        List<Vec3d> waypoints = new ArrayList<>();

        if (FlightUtil.isPathClear(world, from, to)) {
            waypoints.add(to);
            return waypoints;
        }

        double baseY = Math.max(from.y, to.y);
        double capY = Math.max(buildTopHint + 5, baseY + 3);

        for (double candidateY = baseY + 2; candidateY <= capY; candidateY += 2) {
            Vec3d ascendPoint = new Vec3d(from.x, candidateY, from.z);
            Vec3d travelPoint = new Vec3d(to.x, candidateY, to.z);

            if (FlightUtil.isPathClear(world, from, ascendPoint)
                    && FlightUtil.isPathClear(world, ascendPoint, travelPoint)
                    && FlightUtil.isPathClear(world, travelPoint, to)) {
                waypoints.add(ascendPoint);
                waypoints.add(travelPoint);
                waypoints.add(to);
                return waypoints;
            }
        }

        // Khong tim duoc do cao nao thap hon thong thoang -> danh phai bay len tan dinh
        Vec3d ascendPoint = new Vec3d(from.x, capY, from.z);
        Vec3d travelPoint = new Vec3d(to.x, capY, to.z);
        waypoints.add(ascendPoint);
        waypoints.add(travelPoint);
        waypoints.add(to);
        return waypoints;
    }
}
