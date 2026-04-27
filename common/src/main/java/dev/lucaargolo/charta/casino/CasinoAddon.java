package dev.lucaargolo.charta.casino;

import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public final class CasinoAddon {

    public static final String MOD_ID = "charta_casino";

    public static Supplier<GameType<BlackjackGame, BlackjackMenu>> BLACKJACK_GAME;
    public static Supplier<GameType<TexasHoldemGame, TexasHoldemMenu>> TEXAS_HOLDEM_GAME;

    public static Supplier<MenuType<BlackjackMenu>> BLACKJACK_MENU;
    public static Supplier<MenuType<TexasHoldemMenu>> TEXAS_HOLDEM_MENU;

    public static void init() {
    }

    private CasinoAddon() {}
}
