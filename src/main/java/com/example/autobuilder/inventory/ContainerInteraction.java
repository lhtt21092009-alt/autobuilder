package com.example.autobuilder.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Mo 1 chest/barrel/... tai vi tri cho truoc, quick-move (shift-click) nhung item dang can lay
 * vao tui do, roi dong lai. Chay theo tung buoc (tick) - moi lan clickSlot cach nhau it nhat
 * "delayTicks" (cfg.itemFetchSpeed) de khong lam nhanh bat thuong.
 *
 * Luu y: quick-move lay CA CHONG item trong 1 slot cung luc (khong lay le tung item mot), nen so
 * luong lay duoc co the nhieu hon can 1 chut - chap nhan du thua nho de don gian hoa logic.
 */
public class ContainerInteraction {
    private enum State { IDLE, WAITING_SCREEN, TRANSFERRING, CLOSING, DONE, FAILED }

    private State state = State.IDLE;
    private int waitTicks;
    private int delayCounter;
    private final Map<Item, Integer> stillNeeded = new HashMap<>();
    private boolean gotAnyItem = false;

    private static final int OPEN_TIMEOUT_TICKS = 30; // 1.5 giay cho GUI mo ra

    public boolean isIdle() {
        return state == State.IDLE || state == State.DONE || state == State.FAILED;
    }

    public boolean gotAnyItem() {
        return gotAnyItem;
    }

    /** Bat dau mo container tai pos va lay cac item trong needed (giam dan so luong can khi lay duoc). */
    public void start(MinecraftClient client, BlockPos pos, Map<Item, Integer> needed) {
        stillNeeded.clear();
        stillNeeded.putAll(needed);
        gotAnyItem = false;
        waitTicks = 0;
        delayCounter = 0;

        if (client.player == null || client.world == null) {
            state = State.FAILED;
            return;
        }

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);

        state = State.WAITING_SCREEN;
    }

    /** Goi moi client tick. Tra ve true khi da xong (DONE hoac FAILED) va co the chuyen sang chest khac. */
    public boolean tick(MinecraftClient client, int delayTicks) {
        if (client.player == null) {
            state = State.FAILED;
            return true;
        }

        switch (state) {
            case WAITING_SCREEN -> {
                waitTicks++;
                if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
                    state = State.TRANSFERRING;
                } else if (waitTicks > OPEN_TIMEOUT_TICKS) {
                    state = State.FAILED; // khong mo duoc (qua xa, bi chan, khong phai container...)
                }
            }
            case TRANSFERRING -> {
                delayCounter++;
                if (delayCounter < delayTicks) return false;
                delayCounter = 0;

                if (allSatisfied() || client.player.currentScreenHandler == client.player.playerScreenHandler) {
                    state = State.CLOSING;
                    return false;
                }

                ScreenHandler handler = client.player.currentScreenHandler;
                int containerSlotCount = Math.max(0, handler.slots.size() - 36); // 36 = 27 inv + 9 hotbar

                int slotToTake = -1;
                for (int i = 0; i < containerSlotCount; i++) {
                    Slot slot = handler.getSlot(i);
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty()) continue;

                    Item item = stack.getItem();
                    Integer need = stillNeeded.get(item);
                    if (need != null && need > 0) {
                        slotToTake = i;
                        break;
                    }
                }

                if (slotToTake < 0) {
                    // Ruong nay khong (con) co item can lay -> dong lai, chuyen sang ruong khac
                    state = State.CLOSING;
                    return false;
                }

                Slot slot = handler.getSlot(slotToTake);
                Item item = slot.getStack().getItem();
                int count = slot.getStack().getCount();

                client.interactionManager.clickSlot(handler.syncId, slotToTake, 0, SlotActionType.QUICK_MOVE, client.player);

                stillNeeded.merge(item, -count, Integer::sum);
                gotAnyItem = true;
            }
            case CLOSING -> {
                client.player.closeHandledScreen();
                state = State.DONE;
            }
            default -> {
                return true;
            }
        }

        return state == State.DONE || state == State.FAILED;
    }

    private boolean allSatisfied() {
        for (int v : stillNeeded.values()) {
            if (v > 0) return false;
        }
        return true;
    }
}
