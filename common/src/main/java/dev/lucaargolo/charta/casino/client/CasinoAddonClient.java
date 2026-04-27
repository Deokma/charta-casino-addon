package dev.lucaargolo.charta.casino.client;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.client.ChartaModClient;

public final class CasinoAddonClient {

    public static void init() {
        ChartaModClient.getInstance().registerAddonMenuScreen(CasinoAddon.BLACKJACK_MENU, BlackjackScreen::new);
        ChartaModClient.getInstance().registerAddonMenuScreen(CasinoAddon.TEXAS_HOLDEM_MENU, TexasHoldemScreen::new);
    }

    private CasinoAddonClient() {}
}
