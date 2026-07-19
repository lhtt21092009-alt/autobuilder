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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mo 1 chest/barrel/... tai vi tri cho truoc, quick-move (shift-click) nhung item dang can lay
 * vao tui do (UU TIEN item nao dang con thieu NHIEU NHAT truoc), roi dong lai. Chay theo tung buoc
 * (tick) - moi lan clickSlot cach nhau it nhat "delayTicks" (cfg.itemFetchSpeed).
 *
 * Ghi lai TOAN BO cac loai item thay duoc trong ruong (ke ca khong can lay) qua getAllItemsSeen(),
 * de ItemFetchTask cap nhat vao ChestIndex cho lan sau.
 *
 * Luu y: quick-move lay CA CHONG item trong 1 slot cung luc, nen so luong lay duoc co the nhieu
 * hon can 1 chut - chap nhan du thua nho de don gian hoa logic.
 */
public class ContainerInteraction {
    private enum State { IDLE, WAITING_SCREEN, TRANSFERRING, CLOSING, CLOSE_WAIT, DONE, FAILED }

    private State state = State.IDLE;
    private int waitTicks;
    private int delayCounter;
    private int closeWaitTicks;
    private final Map<Item, Integer> stillNeeded = new HashMap<>();
    private final Set<Item> allItemsSeen = new HashSet<>();
    private boolean gotAnyItem = false;

    private static final int OPEN_TIMEOUT_TICKS = 30; // 1.5 giay cho GUI mo ra
    private static final int CLOSE_WAIT_TICKS = 6;    // ~0.3 giay dem sau khi dong, tranh server con tuong dang mo

    public boolean isIdle() {
        return state == State.IDLE || state == State.DONE || state == State.FAILED;
    }

    public boolean gotAnyItem() {
        return gotAnyItem;
    }

    /** Tat ca cac loai item da thay trong ruong nay (ke ca cai khong lay), dung de cap nhat ChestIndex. */
    public Set<Item> getAllItemsSeen() {
        return allItemsSeen;
    }

    /** Bat dau mo container tai pos va lay cac item trong needed (giam dan so luong can khi lay duoc). */
    public void start(MinecraftClient client, BlockPos pos, Map<Item, Integer> needed) {
        stillNeeded.clear();
        stillNeeded.putAll(needed);
        allItemsSeen.clear();
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
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                            "Auto Builder: khong mo duoc 1 container (qua xa hoac bi chan) - bo qua, sang cai khac."), false);
                }
            }
            case TRANSFERRING -> {
                delayCounter++;
                if (delayCounter < delayTicks) return false;
                delayCounter = 0;

                ScreenHandler handler = client.player.currentScreenHandler;
                if (handler == client.player.playerScreenHandler) {
                    state = State.CLOSING;
                    return false;
                }

                int containerSlotCount = Math.max(0, handler.slots.size() - 36); // 36 = 27 inv + 9 hotbar

                // Ghi nhan toan bo item hien co trong ruong (de cap nhat index), va tim slot co item
                // dang con thieu NHIEU NHAT de uu tien lay truoc.
                int bestSlot = -1;
                int bestNeed = 0;
                for (int i = 0; i < containerSlotCount; i++) {
                    ItemStack stack = handler.getSlot(i).getStack();
                    if (stack.isEmpty()) continue;

                    Item item = stack.getItem();
                    allItemsSeen.add(item);

                    Integer need = stillNeeded.get(item);
                    if (need != null && need > bestNeed) {
                        bestNeed = need;
                        bestSlot = i;
                    }
                }

                if (bestSlot < 0 || allSatisfied()) {
                    // Ruong nay khong (con) co item can lay, hoac da du roi -> dong lai
                    state = State.CLOSING;
                    return false;
                }

                Slot slot = handler.getSlot(bestSlot);
                Item item = slot.getStack().getItem();
                int count = slot.getStack().getCount();

                client.interactionManager.clickSlot(handler.syncId, bestSlot, 0, SlotActionType.QUICK_MOVE, client.player);

                stillNeeded.merge(item, -count, Integer::sum);
                gotAnyItem = true;
            }
            case CLOSING -> {
                client.player.closeHandledScreen();
                if (client.currentScreen != null) {
                    client.setScreen(null); // dam bao chac chan dong ca man hinh GUI (phong khi khong tu dong dong)
                }
                closeWaitTicks = 0;
                state = State.CLOSE_WAIT;
            }
            case CLOSE_WAIT -> {
                // Cho vai tick de goi dong duoc gui/xu ly xong, tranh truong hop server con tuong
                // ruong dang mo va chan khong cho di chuyen ngay sau do.
                closeWaitTicks++;
                if (closeWaitTicks >= CLOSE_WAIT_TICKS) {
                    state = State.DONE;
                }
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
