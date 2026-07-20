package com.example.autobuilder.core;

import com.example.autobuilder.AutoBuilderConfig;
import com.example.autobuilder.litematica.SchematicScanner;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Xay tung block con thieu (danh sach da duoc SchematicScanner sap xep tu duoi len - Y tang dan).
 * Voi moi vi tri:
 *  1) Tim 1 mat ke da la block dac trong world THAT de click vao (uu tien mat duoi truoc, vi xay
 *     tu duoi len se luon co diem tua o day).
 *  2) Bay toi gan mat do, xoay mat nhin dung huong.
 *  3) Dam bao dang cam dung item (chuyen tu trong tui do vao thanh hotbar neu can).
 *  4) Right-click (interactBlock) vao mat do - Litematica se tu dieu chinh dung huong/trang thai
 *     block qua tinh nang "Easy Place" (neu ban da bat trong Litematica).
 *  5) Kiem tra da dat dung chua, neu khong thi bo qua (tranh ket mai 1 cho) va chuyen tiep.
 */
public class BuildTask {
    private enum Phase { TRAVEL, PREPARE, PLACE, VERIFY }

    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private Phase phase = Phase.TRAVEL;
    private BlockPos currentTarget;
    private Direction currentFace;
    private BlockPos currentNeighbor;
    private List<Vec3d> currentRoute;
    private int routeIndex;
    private int actionDelay;
    private int verifyTicks;
    private boolean finished = false;
    private boolean missingMaterial = false;
    private int placedCount = 0;
    private int noSupportCount = 0;
    private final StuckGuard travelGuard = new StuckGuard();

    private static final int VERIFY_TIMEOUT_TICKS = 10;
    private static final Direction[] FACE_PRIORITY = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    public BuildTask(List<BlockPos> missingPositionsBottomUp) {
        queue.addAll(missingPositionsBottomUp);
    }

    public boolean isFinished() {
        return finished;
    }

    public int getPlacedCount() {
        return placedCount;
    }

    /** True neu ket thuc ma khong dat duoc block nao ca - vd cong trinh dang lo lung khong co diem tua. */
    public boolean madeNoProgress() {
        return finished && !missingMaterial && placedCount == 0;
    }

    /** True neu dung lai giua chung vi thieu vat lieu (can quay lai fetch them). */
    public boolean isMissingMaterial() {
        return missingMaterial;
    }

    public boolean tick(MinecraftClient client, AutoBuilderConfig cfg, Set<BlockPos> failSkip) {
        if (finished) return true;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            finished = true;
            return true;
        }

        if (currentTarget == null) {
            currentTarget = pollNext(world, failSkip);
            if (currentTarget == null) {
                finished = true;
                return true;
            }

            Direction[] facePick = findUsableFace(world, currentTarget);
            if (facePick == null) {
                // Khong co mat nao de click (chua co diem tua) -> tam bo qua, thu lai sau (khi cac
                // block xung quanh da duoc dat)
                failSkip.add(currentTarget);
                noSupportCount++;
                if (client.player != null && noSupportCount == 1) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                            "Auto Builder: mot so vi tri chua co diem tua (block ke ben) de dat, se thu lai sau."), false);
                }
                currentTarget = null;
                return false;
            }
            currentFace = pickedFaceFrom(facePick);
            currentNeighbor = currentTarget.offset(currentFace.getOpposite());
            phase = Phase.TRAVEL;
            currentRoute = null;
        }

        int delayTicks = (int) Math.max(1, cfg.buildSpeed);

        switch (phase) {
            case TRAVEL -> {
                Vec3d hitVec = faceCenter(currentNeighbor, currentFace);
                Vec3d normal = new Vec3d(currentFace.getOffsetX(), currentFace.getOffsetY(), currentFace.getOffsetZ());
                Vec3d standPoint = hitVec.subtract(normal.multiply(2.5)).add(0, 0.3, 0);

                if (currentRoute == null) {
                    currentRoute = WaypointRouter.route(world, player.getPos(), standPoint,
                            SchematicScanner.getHighestEnabledPlacementTop() + 3);
                    routeIndex = 0;
                    travelGuard.reset();
                }
                Vec3d wp = currentRoute.get(routeIndex);
                if (FlightUtil.flyToward(player, wp, cfg.flightSpeed)) {
                    routeIndex++;
                    if (routeIndex >= currentRoute.size()) {
                        currentRoute = null;
                        phase = Phase.PREPARE;
                    }
                } else {
                    StuckGuard.Result result = travelGuard.check(player.getPos(), player);
                    if (result == StuckGuard.Result.RETRY) {
                        currentRoute = null; // tinh lai duong bay tu vi tri moi (sau khi nhun len)
                    } else if (result == StuckGuard.Result.GIVE_UP) {
                        // Khong toi duoc diem dung cho vi tri nay du thu nhieu lan -> bo qua, sang block khac
                        failSkip.add(currentTarget);
                        currentTarget = null;
                        currentRoute = null;
                    }
                }
            }
            case PREPARE -> {
                actionDelay++;
                if (actionDelay < delayTicks) return false;
                actionDelay = 0;

                BlockState schemState = SchematicScanner.getSchematicBlockState(currentTarget);
                if (schemState == null || schemState.isAir()) {
                    // Da bi thay doi/khong con hop le -> bo qua
                    currentTarget = null;
                    return false;
                }
                Item neededItem = schemState.getBlock().asItem();

                if (!ensureItemInHand(client, neededItem)) {
                    missingMaterial = true;
                    finished = true; // dung lai, de controller quay lai fetch them do
                    return true;
                }

                Vec3d hitVec = faceCenter(currentNeighbor, currentFace);
                FlightUtil.lookAt(player, hitVec);
                phase = Phase.PLACE;
            }
            case PLACE -> {
                actionDelay++;
                if (actionDelay < delayTicks) return false;
                actionDelay = 0;

                Vec3d hitVec = faceCenter(currentNeighbor, currentFace);
                BlockHitResult hitResult = new BlockHitResult(hitVec, currentFace, currentNeighbor, false);
                client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);

                verifyTicks = 0;
                phase = Phase.VERIFY;
            }
            case VERIFY -> {
                verifyTicks++;
                if (SchematicScanner.isPlacedCorrectly(world, currentTarget)) {
                    placedCount++;
                    currentTarget = null; // xong, sang block tiep theo
                } else if (verifyTicks > VERIFY_TIMEOUT_TICKS) {
                    // Khong dat duoc (sai huong, khong du reach, khong bat Easy Place...) -> bo qua
                    failSkip.add(currentTarget);
                    if (placedCount == 0 && failSkip.size() == 1 && client.player != null) {
                        client.player.sendMessage(net.minecraft.text.Text.literal(
                                "Auto Builder: dat block khong thanh cong. Kiem tra da bat 'Easy Place' trong Litematica chua, va da co du item trong tui chua."), false);
                    }
                    currentTarget = null;
                }
            }
        }

        return false;
    }

    private BlockPos pollNext(ClientWorld world, Set<BlockPos> failSkip) {
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (failSkip.contains(pos)) continue;
            if (SchematicScanner.isPlacedCorrectly(world, pos)) continue; // da co san roi (vd fetch xong quay lai)
            return pos;
        }
        return null;
    }

    private Direction[] findUsableFace(ClientWorld world, BlockPos target) {
        for (Direction d : FACE_PRIORITY) {
            BlockPos neighbor = target.offset(d.getOpposite());
            BlockState state = world.getBlockState(neighbor);
            if (!state.isAir() && !state.getCollisionShape(world, neighbor).isEmpty()) {
                return new Direction[]{d};
            }
        }
        return null;
    }

    private Direction pickedFaceFrom(Direction[] arr) {
        return arr[0];
    }

    private Vec3d faceCenter(BlockPos neighbor, Direction face) {
        return new Vec3d(
                neighbor.getX() + 0.5 + face.getOffsetX() * 0.5,
                neighbor.getY() + 0.5 + face.getOffsetY() * 0.5,
                neighbor.getZ() + 0.5 + face.getOffsetZ() * 0.5
        );
    }

    /** Dam bao dang cam item dung loai o hotbar. Tra ve false neu khong co item nay trong tui do. */
    private boolean ensureItemInHand(MinecraftClient client, Item neededItem) {
        ClientPlayerEntity player = client.player;
        var inv = player.getInventory();

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == neededItem) {
                inv.setSelectedSlot(i);
                return true;
            }
        }

        int hotbarSlot = inv.selectedSlot;
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == neededItem) {
                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        i, hotbarSlot, SlotActionType.SWAP, player
                );
                return true;
            }
        }

        return false;
    }
}
