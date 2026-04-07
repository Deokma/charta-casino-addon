package dev.lucaargolo.charta.casino.game.durak;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.common.block.entity.CardTableBlockEntity;
import dev.lucaargolo.charta.common.game.Ranks;
import dev.lucaargolo.charta.common.game.Suits;
import dev.lucaargolo.charta.common.game.api.CardPlayer;
import dev.lucaargolo.charta.common.game.api.GamePlay;
import dev.lucaargolo.charta.common.game.api.GameSlot;
import dev.lucaargolo.charta.common.game.api.card.Card;
import dev.lucaargolo.charta.common.game.api.card.Deck;
import dev.lucaargolo.charta.common.game.api.card.Rank;
import dev.lucaargolo.charta.common.game.api.card.Suit;
import dev.lucaargolo.charta.common.game.api.game.Game;
import dev.lucaargolo.charta.common.game.api.game.GameOption;
import dev.lucaargolo.charta.common.menu.AbstractCardMenu;
import dev.lucaargolo.charta.common.registry.ModMenuTypeRegistry;
import dev.lucaargolo.charta.common.sound.ModSounds;
import dev.lucaargolo.charta.common.utils.CardImage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.*;
import java.util.function.Predicate;

/**
 * Durak (Дурак) — classic Russian card game.
 *
 * Uses the standard Charta Game API (GameSlot, getPlayerHand, etc.)
 *
 * Actions encoded in GamePlay.slot():
 *   ACT_ATTACK + handCardIndex  — attacker plays a card
 *   ACT_DEFEND + atkSlot*100 + handCardIndex — defender beats attack card
 *   ACT_TAKE   — defender takes all table cards
 *   ACT_DONE   — attacker passes (ends attack round)
 */
public class DurakGame extends Game<DurakGame, DurakMenu> {

    public static final int ACT_ATTACK = 1000;
    public static final int ACT_DEFEND = 2000;
    public static final int ACT_TAKE   = 3000;
    public static final int ACT_DONE   = 4000;

    public static final int MAX_TABLE  = 6;

    // ── Rank ordering for Durak (6 through Ace) ───────────────────────────────
    private static final List<Rank> DURAK_RANKS = List.of(
            Ranks.SIX, Ranks.SEVEN, Ranks.EIGHT, Ranks.NINE, Ranks.TEN,
            Ranks.JACK, Ranks.QUEEN, Ranks.KING, Ranks.ACE
    );

    private static int rankValue(Rank r) { return DURAK_RANKS.indexOf(r); }

    // ── Phases ────────────────────────────────────────────────────────────────
    public enum Phase { ATTACK, DEFENSE, DRAWING, GAME_OVER }

    // ── Synced state ──────────────────────────────────────────────────────────
    public int   phaseOrd     = Phase.ATTACK.ordinal();
    public int   attackerIdx  = 0;
    public int   defenderIdx  = 1;
    public int   trumpOrd     = 0;
    public int   tableCount   = 0;
    public int   durakIdx     = -1;

    // ── Slots ─────────────────────────────────────────────────────────────────
    /** Draw pile — face-down stack. */
    public final GameSlot drawPile;
    /** Discard pile — beaten cards go here. */
    public final GameSlot discardPile;
    /** Attack slots (up to MAX_TABLE). */
    public final GameSlot[] atkSlots = new GameSlot[MAX_TABLE];
    /** Defense slots (one per attack slot). */
    public final GameSlot[] defSlots = new GameSlot[MAX_TABLE];

    // ── Server-side state ─────────────────────────────────────────────────────
    private Suit trump;
    private final boolean[] eliminated;
    private boolean waitingForAction = false;

    public DurakGame(List<CardPlayer> players, Deck deck) {
        super(players, deck);
        eliminated = new boolean[Math.max(players.size(), 2)];

        float tw = CardTableBlockEntity.TABLE_WIDTH;
        float th = CardTableBlockEntity.TABLE_HEIGHT;
        float cw = CardImage.WIDTH, ch = CardImage.HEIGHT;

        // Draw pile — left of centre
        drawPile = addSlot(new GameSlot(new LinkedList<>(),
                tw / 2f - cw - 20f, th / 2f - ch / 2f, 0f, 0f) {
            @Override public boolean canInsertCard(CardPlayer p, List<Card> c, int i) { return false; }
            @Override public boolean canRemoveCard(CardPlayer p, int i)               { return false; }
        });

        // Discard pile — right of centre
        discardPile = addSlot(new GameSlot(new LinkedList<>(),
                tw / 2f + 20f, th / 2f - ch / 2f, 0f, 0f) {
            @Override public boolean canInsertCard(CardPlayer p, List<Card> c, int i) { return false; }
            @Override public boolean canRemoveCard(CardPlayer p, int i)               { return false; }
        });

        // Attack / defense slot pairs spread across the table
        float startX = tw / 2f - (MAX_TABLE * (cw + 4f)) / 2f;
        float atkY   = th / 2f - ch - 6f;
        float defY   = th / 2f + 6f;
        for (int i = 0; i < MAX_TABLE; i++) {
            float x = startX + i * (cw + 4f);
            atkSlots[i] = addSlot(new GameSlot(new LinkedList<>(), x, atkY, 0f, 0f) {
                @Override public boolean canInsertCard(CardPlayer p, List<Card> c, int idx) { return false; }
                @Override public boolean canRemoveCard(CardPlayer p, int idx)               { return false; }
            });
            defSlots[i] = addSlot(new GameSlot(new LinkedList<>(), x, defY, 0f, 0f) {
                @Override public boolean canInsertCard(CardPlayer p, List<Card> c, int idx) { return false; }
                @Override public boolean canRemoveCard(CardPlayer p, int idx)               { return false; }
            });
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Override
    public ModMenuTypeRegistry.AdvancedMenuTypeEntry<DurakMenu, AbstractCardMenu.Definition> getMenuType() {
        return CasinoAddon.DURAK_MENU.get();
    }

    @Override
    public DurakMenu createMenu(int id, Inventory inv, AbstractCardMenu.Definition def) {
        return new DurakMenu(id, inv, def);
    }

    @Override
    public Predicate<Deck> getDeckPredicate() {
        return d -> d.getCards().size() >= 36
                && Suits.STANDARD.containsAll(d.getSuits())
                && d.getSuits().containsAll(Suits.STANDARD);
    }

    @Override
    public Predicate<Card> getCardPredicate() {
        return c -> Suits.STANDARD.contains(c.suit()) && DURAK_RANKS.contains(c.rank());
    }

    @Override public List<GameOption<?>> getOptions() { return List.of(); }
    @Override public int getMinPlayers() { return 2; }
    @Override public int getMaxPlayers() { return 6; }

    // ── startGame ─────────────────────────────────────────────────────────────

    @Override
    public void startGame() {
        isGameReady = false;
        isGameOver  = false;
        Arrays.fill(eliminated, false);
        waitingForAction = false;
        tableCount = 0;
        durakIdx   = -1;

        // Clear all slots and hands
        drawPile.clear();
        discardPile.clear();
        for (GameSlot s : atkSlots) s.clear();
        for (GameSlot s : defSlots) s.clear();
        for (CardPlayer p : players) { getPlayerHand(p).clear(); getCensoredHand(p).clear(); }

        // Shuffle deck into draw pile (face-down)
        List<Card> shuffled = new ArrayList<>(gameDeck);
        Collections.shuffle(shuffled);
        shuffled.forEach(c -> { if (!c.flipped()) c.flip(); drawPile.add(c); });

        // Trump = bottom card (first in pile), flip it face-up so players can see
        Card trumpCard = drawPile.stream().findFirst().orElse(null);
        if (trumpCard != null) {
            if (trumpCard.flipped()) trumpCard.flip();
            trump    = trumpCard.suit();
            trumpOrd = suitOrd(trump);
        }

        // Deal 6 cards to each player
        for (int round = 0; round < 6; round++) {
            for (CardPlayer p : players) {
                scheduledActions.add(() -> {
                    p.playSound(ModSounds.CARD_DRAW.get());
                    dealOneCard(p);
                });
                scheduledActions.add(() -> {});
            }
        }

        // Player with lowest trump leads
        scheduledActions.add(() -> {
            attackerIdx = findLowestTrumpHolder();
            defenderIdx = nextActive(attackerIdx);
            phaseOrd    = Phase.ATTACK.ordinal();
            currentPlayer = players.get(attackerIdx);

            table(Component.translatable("message.charta.game_started"));
            table(Component.literal("Trump: " + Suits.getLocation(trump).getPath().toUpperCase())
                    .withStyle(ChatFormatting.GOLD));
            table(Component.translatable("message.charta.its_player_turn",
                    players.get(attackerIdx).getColoredName()));
        });
    }

    // ── runGame ───────────────────────────────────────────────────────────────

    @Override
    public void runGame() {
        if (isGameOver || waitingForAction) return;

        Phase phase = Phase.values()[phaseOrd];
        if (phase == Phase.DRAWING) {
            doDrawPhase();
            return;
        }

        int actorIdx = (phase == Phase.DEFENSE) ? defenderIdx : attackerIdx;
        currentPlayer = players.get(actorIdx);
        currentPlayer.resetPlay();
        waitingForAction = true;

        currentPlayer.afterPlay(play -> {
            waitingForAction = false;
            if (play != null) handleAction(players.indexOf(currentPlayer), play.slot());
            isGameReady = false;
            scheduledActions.add(() -> {});
        });
    }

    // ── handleAction ──────────────────────────────────────────────────────────

    public void handleAction(int playerIndex, int action) {
        Phase phase = Phase.values()[phaseOrd];

        // TAKE: defender takes all table cards
        if (action == ACT_TAKE && phase == Phase.DEFENSE && playerIndex == defenderIdx) {
            CardPlayer defender = players.get(defenderIdx);
            for (int i = 0; i < MAX_TABLE; i++) {
                for (Card c : atkSlots[i].getCards()) { if (c.flipped()) c.flip(); getPlayerHand(defender).add(c); getCensoredHand(defender).add(new Card()); }
                for (Card c : defSlots[i].getCards()) { if (c.flipped()) c.flip(); getPlayerHand(defender).add(c); getCensoredHand(defender).add(new Card()); }
                atkSlots[i].clear();
                defSlots[i].clear();
            }
            tableCount = 0;
            play(defender, Component.literal("Takes the cards!").withStyle(ChatFormatting.RED));
            // Attacker stays, defender advances
            defenderIdx = nextActive(defenderIdx);
            phaseOrd = Phase.DRAWING.ordinal();
            return;
        }

        // DONE: attacker ends attack (all cards defended)
        if (action == ACT_DONE && phase == Phase.ATTACK && playerIndex == attackerIdx) {
            // Check all attacks are defended
            for (int i = 0; i < tableCount; i++) {
                if (!atkSlots[i].isEmpty() && defSlots[i].isEmpty()) return; // undefended card
            }
            // Discard all table cards
            for (int i = 0; i < MAX_TABLE; i++) {
                atkSlots[i].getCards().forEach(discardPile::add);
                defSlots[i].getCards().forEach(discardPile::add);
                atkSlots[i].clear();
                defSlots[i].clear();
            }
            tableCount = 0;
            play(players.get(attackerIdx), Component.literal("Pass — defense successful!").withStyle(ChatFormatting.GREEN));
            int oldDef = defenderIdx;
            attackerIdx = oldDef;
            defenderIdx = nextActive(attackerIdx);
            phaseOrd = Phase.DRAWING.ordinal();
            return;
        }

        // ATTACK: attacker plays a card
        if (action >= ACT_ATTACK && action < ACT_DEFEND && phase == Phase.ATTACK && playerIndex == attackerIdx) {
            int handIdx = action - ACT_ATTACK;
            CardPlayer attacker = players.get(attackerIdx);
            List<Card> hand = getPlayerHand(attacker).stream().toList();
            if (handIdx < 0 || handIdx >= hand.size()) return;
            Card card = hand.get(handIdx);
            if (!canAttackWith(card)) return;
            if (tableCount >= MAX_TABLE) return;

            getPlayerHand(attacker).remove(card);
            getCensoredHand(attacker).removeLast();
            if (card.flipped()) card.flip();
            atkSlots[tableCount].add(card);
            tableCount++;
            attacker.playSound(ModSounds.CARD_PLAY.get());
            play(attacker, Component.literal("Attacks!"));
            phaseOrd = Phase.DEFENSE.ordinal();
            return;
        }

        // DEFEND: defender beats an attack card
        if (action >= ACT_DEFEND && action < ACT_TAKE && phase == Phase.DEFENSE && playerIndex == defenderIdx) {
            int rem      = action - ACT_DEFEND;
            int atkSlot  = rem / 100;
            int handIdx  = rem % 100;
            if (atkSlot < 0 || atkSlot >= tableCount) return;
            if (!defSlots[atkSlot].isEmpty()) return;

            CardPlayer defender = players.get(defenderIdx);
            List<Card> hand = getPlayerHand(defender).stream().toList();
            if (handIdx < 0 || handIdx >= hand.size()) return;

            Card defCard = hand.get(handIdx);
            Card atkCard = atkSlots[atkSlot].stream().findFirst().orElse(null);
            if (atkCard == null || !canDefend(atkCard, defCard)) return;

            getPlayerHand(defender).remove(defCard);
            getCensoredHand(defender).removeLast();
            if (defCard.flipped()) defCard.flip();
            defSlots[atkSlot].add(defCard);
            defender.playSound(ModSounds.CARD_PLAY.get());
            play(defender, Component.literal("Defends!").withStyle(ChatFormatting.GREEN));

            // If all attacks defended, give attacker chance to add more
            boolean allDefended = true;
            for (int i = 0; i < tableCount; i++) {
                if (defSlots[i].isEmpty()) { allDefended = false; break; }
            }
            if (allDefended) phaseOrd = Phase.ATTACK.ordinal();
        }
    }

    // ── DRAWING phase ─────────────────────────────────────────────────────────

    private void doDrawPhase() {
        // Draw order: attacker first, then clockwise, defender last
        List<Integer> order = new ArrayList<>();
        int idx = attackerIdx;
        for (int i = 0; i < players.size(); i++) {
            if (!eliminated[idx]) order.add(idx);
            idx = (idx + 1) % players.size();
        }
        order.remove(Integer.valueOf(defenderIdx));
        order.add(defenderIdx);

        for (int playerIndex : order) {
            CardPlayer p = players.get(playerIndex);
            int need = 6 - getPlayerHand(p).size();
            for (int i = 0; i < need && !drawPile.isEmpty(); i++) {
                dealOneCard(p);
            }
            // Player wins if hand empty and draw pile empty
            if (getPlayerHand(p).isEmpty() && drawPile.isEmpty() && !eliminated[playerIndex]) {
                eliminated[playerIndex] = true;
                play(p, Component.literal("Leaves the game — not the Durak!").withStyle(ChatFormatting.GREEN));
            }
        }

        // Check if game over
        int remaining = 0, lastIdx = -1;
        for (int i = 0; i < players.size(); i++) {
            if (!eliminated[i]) { remaining++; lastIdx = i; }
        }
        if (remaining <= 1) {
            durakIdx = lastIdx;
            endGame();
            return;
        }

        attackerIdx = nextActive(attackerIdx);
        defenderIdx = nextActive(attackerIdx);
        phaseOrd    = Phase.ATTACK.ordinal();
        currentPlayer = players.get(attackerIdx);
        table(Component.translatable("message.charta.its_player_turn", currentPlayer.getColoredName()));
    }

    // ── canPlay ───────────────────────────────────────────────────────────────

    @Override
    public boolean canPlay(CardPlayer player, GamePlay play) {
        if (!isGameReady || isGameOver) return false;
        if (player != currentPlayer) return false;
        int playerIndex = players.indexOf(player);
        Phase phase = Phase.values()[phaseOrd];
        int action = play.slot();

        if (phase == Phase.ATTACK && playerIndex == attackerIdx) {
            if (action == ACT_DONE) return true;
            if (action >= ACT_ATTACK && action < ACT_DEFEND) {
                int handIdx = action - ACT_ATTACK;
                List<Card> hand = getPlayerHand(player).stream().toList();
                if (handIdx < 0 || handIdx >= hand.size()) return false;
                return canAttackWith(hand.get(handIdx));
            }
        }
        if (phase == Phase.DEFENSE && playerIndex == defenderIdx) {
            if (action == ACT_TAKE) return true;
            if (action >= ACT_DEFEND && action < ACT_TAKE) {
                int rem = action - ACT_DEFEND;
                int atkSlot = rem / 100, handIdx = rem % 100;
                if (atkSlot < 0 || atkSlot >= tableCount) return false;
                if (!defSlots[atkSlot].isEmpty()) return false;
                List<Card> hand = getPlayerHand(player).stream().toList();
                if (handIdx < 0 || handIdx >= hand.size()) return false;
                Card atkCard = atkSlots[atkSlot].stream().findFirst().orElse(null);
                return atkCard != null && canDefend(atkCard, hand.get(handIdx));
            }
        }
        return false;
    }

    // ── endGame ───────────────────────────────────────────────────────────────

    @Override
    public void endGame() {
        if (isGameOver) return;
        isGameOver = true;
        phaseOrd = Phase.GAME_OVER.ordinal();
        scheduledActions.clear();
        players.forEach(p -> p.play(null));

        if (durakIdx >= 0 && durakIdx < players.size()) {
            CardPlayer durak = players.get(durakIdx);
            durak.sendTitle(
                    Component.literal("ДУРАК!").withStyle(ChatFormatting.RED),
                    Component.literal("You are the fool!"));
            for (int i = 0; i < players.size(); i++) {
                if (i != durakIdx) players.get(i).sendTitle(
                        Component.literal("You won!").withStyle(ChatFormatting.GREEN),
                        Component.literal(durak.getName().getString() + " is the Durak!"));
            }
            table(Component.literal("Game over! Durak: " + durak.getColoredName().getString())
                    .withStyle(ChatFormatting.RED));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void dealOneCard(CardPlayer player) {
        if (drawPile.isEmpty()) return;
        Card c = drawPile.removeLast();
        if (c.flipped()) c.flip();
        getPlayerHand(player).add(c);
        getCensoredHand(player).add(new Card());
    }

    private boolean canAttackWith(Card card) {
        if (tableCount == 0) return true; // first card — anything goes
        Rank r = card.rank();
        for (int i = 0; i < tableCount; i++) {
            for (Card c : atkSlots[i].getCards()) if (c.rank() == r) return true;
            for (Card c : defSlots[i].getCards()) if (c.rank() == r) return true;
        }
        return false;
    }

    private boolean canDefend(Card attack, Card defense) {
        boolean atkTrump = attack.suit() == trump;
        boolean defTrump = defense.suit() == trump;
        if (defTrump && !atkTrump) return true;
        if (!defTrump && atkTrump) return false;
        if (defense.suit() != attack.suit()) return false;
        return rankValue(defense.rank()) > rankValue(attack.rank());
    }

    private int findLowestTrumpHolder() {
        int best = 0, bestVal = Integer.MAX_VALUE;
        for (int i = 0; i < players.size(); i++) {
            for (Card c : getPlayerHand(players.get(i)).getCards()) {
                if (c.suit() == trump) {
                    int v = rankValue(c.rank());
                    if (v < bestVal) { bestVal = v; best = i; }
                }
            }
        }
        return best;
    }

    private int nextActive(int from) {
        int n = players.size();
        int cur = (from + 1) % n;
        int tries = 0;
        while (eliminated[cur] && tries < n) { cur = (cur + 1) % n; tries++; }
        return cur;
    }

    private int suitOrd(Suit s) {
        List<Suit> list = List.of(Suits.SPADES, Suits.HEARTS, Suits.CLUBS, Suits.DIAMONDS);
        return list.indexOf(s);
    }

    public Suit getTrump()         { return trump; }
    public boolean isEliminated(int i) { return i >= 0 && i < eliminated.length && eliminated[i]; }
    public Phase getPhase()        { return Phase.values()[phaseOrd]; }
    public int getDrawPileSize()   { return drawPile.size(); }

    public static int encodeAttack(int handIdx)                 { return ACT_ATTACK + handIdx; }
    public static int encodeDefend(int atkSlot, int handIdx)    { return ACT_DEFEND + atkSlot * 100 + handIdx; }
}

