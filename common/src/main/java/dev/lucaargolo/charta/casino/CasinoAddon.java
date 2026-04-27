package dev.lucaargolo.charta.casino;

import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.common.ChartaMod;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import dev.lucaargolo.charta.common.menu.AbstractCardMenu;
import dev.lucaargolo.charta.common.registry.ModMenuTypeRegistry;

import java.util.function.Supplier;

public final class CasinoAddon {

    public static final String MOD_ID = "charta_casino";
    public static final ModMenuTypeRegistry MENU_REGISTRY = ChartaMod.menuTypeRegistry();

    public static Supplier<GameType<BlackjackGame, BlackjackMenu>> BLACKJACK_GAME;
    public static Supplier<GameType<TexasHoldemGame, TexasHoldemMenu>> TEXAS_HOLDEM_GAME;

    public static final ModMenuTypeRegistry.AdvancedMenuTypeEntry<BlackjackMenu, AbstractCardMenu.Definition> BLACKJACK_MENU =
            MENU_REGISTRY.register("blackjack", BlackjackMenu::new, AbstractCardMenu.Definition.STREAM_CODEC);
    public static final ModMenuTypeRegistry.AdvancedMenuTypeEntry<TexasHoldemMenu, AbstractCardMenu.Definition> TEXAS_HOLDEM_MENU =
            MENU_REGISTRY.register("texas_holdem", TexasHoldemMenu::new, AbstractCardMenu.Definition.STREAM_CODEC);

    public static void init() {
        MENU_REGISTRY.init();
    }

    private CasinoAddon() {}
}
