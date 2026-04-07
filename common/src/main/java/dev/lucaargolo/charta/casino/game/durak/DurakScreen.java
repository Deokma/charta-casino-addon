package dev.lucaargolo.charta.casino.game.durak;

import dev.lucaargolo.charta.casino.network.DurakActionPayload;
import dev.lucaargolo.charta.client.render.screen.GameScreen;
import dev.lucaargolo.charta.common.ChartaMod;
import dev.lucaargolo.charta.common.game.Suits;
import dev.lucaargolo.charta.common.game.api.CardPlayer;
import dev.lucaargolo.charta.common.game.api.card.Card;
import dev.lucaargolo.charta.common.game.api.card.Suit;
import dev.lucaargolo.charta.common.menu.CardSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DurakScreen extends GameScreen<DurakGame, DurakMenu> {

    private static final int BTN_W   = 60;
    private static final int BTN_H   = 20;
    private static final int BTN_GAP = 6;

    private static final int COLOR_ATTACK   = 0xCC2222;
    private static final int COLOR_BEAT     = 0x228844;
    private static final int COLOR_TAKE     = 0xAA6600;
    private static final int COLOR_PASS     = 0x446688;
    private static final int COLOR_INACTIVE = 0x444444;

    /** Index of the attack slot selected for beating (-1 = none). */
    private int selectedAtkSlot = -1;
    /** Index of the hand card selected for attacking or beating (-1 = none). */
    private int selectedHandIdx = -1;

    public DurakScreen(DurakMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 256;
        this.imageHeight = 230;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mx, int my) {
        int bgTop    = 40;
        int bgBottom = height - 63;

        g.fill(0, bgTop, width, bgBottom, 0xFF1B5E20);
        g.fill(2, bgTop + 2, width - 2, bgBottom - 2, 0xFF2E7D32);

        // Trump indicator
        Suit trump = menu.getGame().getTrump();
        if (trump != null) {
            String name = Component.translatable("suit.charta." + Suits.getLocation(trump).getPath()).getString();
            Component tc = Component.literal("Trump: " + name).withStyle(ChatFormatting.GOLD);
            g.drawString(font, tc, width - font.width(tc) - 4, bgTop + 4, 0xFFFFFF);
        }

        // Deck size
        Component dc = Component.literal("Deck: " + menu.getDeckSize()).withStyle(ChatFormatting.WHITE);
        g.drawString(font, dc, 4, bgTop + 4, 0xFFFFFF);

        // Highlight selected attack slot
        if (selectedAtkSlot >= 0 && selectedAtkSlot < DurakGame.MAX_TABLE) {
            var slot = menu.cardSlots.get(getAtkSlotMenuIdx(selectedAtkSlot));
            int sx = leftPos + (int) slot.x - 2;
            int sy = topPos  + (int) slot.y - 2;
            g.fill(sx, sy, sx + (int) CardSlot.getWidth(slot) + 4, sy + (int) CardSlot.getHeight(slot) + 4, 0x88FFFF00);
        }

        // Highlight selected hand card
        if (selectedHandIdx >= 0) {
            // Hand slot is the last card slot
            var handSlot = menu.cardSlots.get(menu.cardSlots.size() - 1);
            List<Card> cards = menu.getGame().getPlayerHand(menu.getCardPlayer()).stream().toList();
            if (selectedHandIdx < cards.size()) {
                float cx = handSlot.x + selectedHandIdx * 30f;
                float cy = handSlot.y;
                g.fill(leftPos + (int) cx - 2, height - 60, leftPos + (int) cx + 27, height - 20, 0x88FFFF00);
            }
        }

        DurakGame.Phase phase = menu.getPhase();
        boolean myTurn = menu.isCurrentPlayer() && menu.isGameReady();
        if (!myTurn) return;

        int bx = (width - (2 * BTN_W + BTN_GAP)) / 2;
        int by = bgBottom - BTN_H - 6;

        if (phase == DurakGame.Phase.ATTACK && menu.isAttacker()) {
            boolean canPass = menu.getTableCount() > 0 && allDefended();
            drawBtn(g, mx, my, bx, by, BTN_W,
                    Component.translatable("button.charta_casino.durak.pass"), canPass, canPass ? COLOR_PASS : COLOR_INACTIVE);
        }

        if (phase == DurakGame.Phase.DEFENSE && menu.isDefender()) {
            drawBtn(g, mx, my, bx, by, BTN_W,
                    Component.translatable("button.charta_casino.durak.take"), true, COLOR_TAKE);
        }
    }

    private boolean allDefended() {
        int tc = menu.getTableCount();
        for (int i = 0; i < tc; i++) {
            if (menu.getGame().defSlots[i].isEmpty()) return false;
        }
        return true;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mx, int my) {
        int cx = width / 2 - leftPos;

        DurakGame.Phase phase = menu.getPhase();
        String phaseStr = switch (phase) {
            case ATTACK   -> "Attack";
            case DEFENSE  -> "Defense";
            case DRAWING  -> "Drawing...";
            case GAME_OVER -> "Game Over";
        };
        Component phaseComp = Component.literal(phaseStr).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, phaseComp, cx - font.width(phaseComp) / 2, 28, 0xFFFFFF);

        if (menu.isGameReady()) {
            CardPlayer current = menu.getCurrentPlayer();
            Component turnComp = menu.isCurrentPlayer()
                    ? Component.translatable("message.charta.your_turn").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.charta.other_turn", current.getName())
                    .withStyle(s -> s.withColor(current.getColor().getTextureDiffuseColor()));
            g.drawString(font, turnComp, cx - font.width(turnComp) / 2, 125, 0xFFFFFF);
        }

        // Show durak result
        int durakIdx = menu.getDurakIdx();
        if (durakIdx >= 0 && durakIdx < menu.getGame().getPlayers().size()) {
            Component durakComp = Component.literal("Durak: " +
                    menu.getGame().getPlayers().get(durakIdx).getName().getString())
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            g.drawString(font, durakComp, cx - font.width(durakComp) / 2, 115, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        DurakGame.Phase phase = menu.getPhase();
        boolean myTurn = menu.isCurrentPlayer() && menu.isGameReady();
        int bgBottom = height - 63;
        int bx = (width - (2 * BTN_W + BTN_GAP)) / 2;
        int by = bgBottom - BTN_H - 6;

        if (myTurn) {
            // Pass button (attacker)
            if (phase == DurakGame.Phase.ATTACK && menu.isAttacker()) {
                if (hit(mx, my, bx, by, BTN_W) && allDefended()) {
                    send(DurakGame.ACT_DONE); selectedAtkSlot = -1; selectedHandIdx = -1; return true;
                }
            }
            // Take button (defender)
            if (phase == DurakGame.Phase.DEFENSE && menu.isDefender()) {
                if (hit(mx, my, bx, by, BTN_W)) {
                    send(DurakGame.ACT_TAKE); selectedAtkSlot = -1; selectedHandIdx = -1; return true;
                }
            }

            // Click on hand card
            int handIdx = getClickedHandCard(mx, my);
            if (handIdx >= 0) {
                if (phase == DurakGame.Phase.ATTACK && menu.isAttacker()) {
                    // Send attack immediately
                    send(DurakGame.encodeAttack(handIdx));
                    selectedHandIdx = -1;
                    return true;
                }
                if (phase == DurakGame.Phase.DEFENSE && menu.isDefender()) {
                    if (selectedAtkSlot >= 0) {
                        // Beat the selected attack slot with this hand card
                        send(DurakGame.encodeDefend(selectedAtkSlot, handIdx));
                        selectedAtkSlot = -1; selectedHandIdx = -1;
                        return true;
                    } else {
                        selectedHandIdx = handIdx;
                        return true;
                    }
                }
            }

            // Click on attack slot (defender selects which to beat)
            if (phase == DurakGame.Phase.DEFENSE && menu.isDefender()) {
                for (int i = 0; i < menu.getTableCount(); i++) {
                    if (!menu.getGame().atkSlots[i].isEmpty() && menu.getGame().defSlots[i].isEmpty()) {
                        var slot = menu.cardSlots.get(getAtkSlotMenuIdx(i));
                        int sx = leftPos + (int) slot.x;
                        int sy = topPos  + (int) slot.y;
                        if (mx >= sx && mx < sx + CardSlot.getWidth(slot) && my >= sy && my < sy + CardSlot.getHeight(slot)) {
                            if (selectedHandIdx >= 0) {
                                // Beat immediately if hand card already selected
                                send(DurakGame.encodeDefend(i, selectedHandIdx));
                                selectedAtkSlot = -1; selectedHandIdx = -1;
                            } else {
                                selectedAtkSlot = (selectedAtkSlot == i) ? -1 : i;
                            }
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Index of attack slot i in menu.cardSlots (after player preview slots). */
    private int getAtkSlotMenuIdx(int i) {
        return menu.getGame().getPlayers().size() + i;
    }

    private int getClickedHandCard(double mx, double my) {
        List<Card> cards = menu.getGame().getPlayerHand(menu.getCardPlayer()).stream().toList();
        if (cards.isEmpty()) return -1;
        float slotW  = CardSlot.getWidth(CardSlot.Type.HORIZONTAL);
        float startX = (width - slotW) / 2f;
        float cardW  = 30f;
        float cardH  = 40f;
        float cardY  = height - 58f;
        for (int i = 0; i < cards.size(); i++) {
            float cx = startX + i * cardW;
            if (mx >= cx && mx < cx + cardW && my >= cardY && my < cardY + cardH) return i;
        }
        return -1;
    }

    private boolean hit(double mx, double my, int ax, int ay, int w) {
        return mx >= ax && mx < ax + w && my >= ay && my < ay + BTN_H;
    }

    private void send(int action) {
        ChartaMod.getPacketManager().sendToServer(new DurakActionPayload(action));
    }

    private void drawBtn(GuiGraphics g, int mx, int my, int ax, int ay, int w,
                         Component label, boolean active, int base) {
        int col = active ? base : COLOR_INACTIVE;
        int ca = 0xFF000000 | col, li = 0xFF000000 | lighten(col), dk = 0xFF000000 | darken(col);
        g.fill(ax + 2, ay + BTN_H - 1, ax + w - 1, ay + BTN_H, dk);
        g.fill(ax + w - 1, ay + 2, ax + w, ay + BTN_H, dk);
        g.fill(ax, ay, ax + w - 1, ay + 1, li);
        g.fill(ax, ay + 1, ax + 1, ay + BTN_H - 1, li);
        g.fill(ax + 1, ay + 1, ax + w - 1, ay + BTN_H - 1, ca);
        g.fill(ax + 1, ay + 1, ax + w - 1, ay + 1 + BTN_H / 3, 0x22FFFFFF);
        int tx = ax + w / 2 - font.width(label) / 2, ty = ay + (BTN_H - 8) / 2;
        g.drawString(font, label, tx + 1, ty + 1, 0x44000000, false);
        g.drawString(font, label, tx, ty, 0xFFFFFFFF, false);
        if (active && mx >= ax && mx < ax + w && my >= ay && my < ay + BTN_H)
            g.fill(ax + 1, ay + 1, ax + w - 1, ay + BTN_H - 1, 0x33FFFFFF);
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 70), g = Math.min(255, ((c >> 8) & 0xFF) + 70), b = Math.min(255, (c & 0xFF) + 70);
        return (r << 16) | (g << 8) | b;
    }
    private static int darken(int c) {
        int r = Math.max(0, ((c >> 16) & 0xFF) - 55), g = Math.max(0, ((c >> 8) & 0xFF) - 55), b = Math.max(0, (c & 0xFF) - 55);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void renderTopBar(@NotNull GuiGraphics g) {
        super.renderTopBar(g);
        List<CardPlayer> players = menu.getGame().getPlayers();
        int n = players.size();
        float slotW = CardSlot.getWidth(CardSlot.Type.PREVIEW) + 28f;
        float total = n * slotW + (n - 1f) * (slotW / 10f);
        for (int i = 0; i < n; i++) {
            float px = width / 2f - total / 2f + i * (slotW + slotW / 10f);
            DyeColor col = players.get(i).getColor();
            g.fill(Mth.floor(px), 28, Mth.ceil(px + slotW), 40, 0x88000000 + col.getTextureDiffuseColor());
            boolean isAtk = i == menu.getAttackerIdx(), isDef = i == menu.getDefenderIdx();
            if (isAtk || isDef) {
                String mark = isAtk ? "A" : "D";
                g.pose().pushPose();
                g.pose().translate(px + 26f, 29f, 0f);
                g.pose().scale(0.5f, 0.5f, 0.5f);
                g.drawString(font, mark, 0, 0, isAtk ? 0xFF4444 : 0x4444FF, true);
                g.pose().popPose();
            }
        }
        g.fill(0, 28, Mth.floor((width - total) / 2f), 40, 0x88000000);
        g.fill(width - Mth.floor((width - total) / 2f), 28, width, 40, 0x88000000);
    }

    @Override
    public void renderBottomBar(@NotNull GuiGraphics g) {
        DyeColor col = menu.getCardPlayer().getColor();
        int tw = Mth.floor(CardSlot.getWidth(CardSlot.Type.HORIZONTAL)) + 10;
        g.fill(0, height - 63, (width - tw) / 2, height, 0x88000000);
        g.fill((width - tw) / 2, height - 63, (width - tw) / 2 + tw, height, 0x88000000 + col.getTextureDiffuseColor());
        g.fill((width - tw) / 2 + tw, height - 63, width, height, 0x88000000);
    }
}
