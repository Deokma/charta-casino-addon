package dev.lucaargolo.charta.casino.client;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.client.ChartaModClient;
import dev.lucaargolo.charta.common.ChartaMod;
import dev.lucaargolo.charta.common.network.ModPacketManager;

/**
 * Client-side initialization for the Casino addon.
 * Call {@link #init()} from your Fabric/NeoForge client initializer.
 */
public final class CasinoAddonClient {

    public static void init() {
        // Register menu screens
        ChartaModClient.getInstance().registerAddonMenuScreen(CasinoAddon.BLACKJACK_MENU.get(),   BlackjackScreen::new);
        ChartaModClient.getInstance().registerAddonMenuScreen(CasinoAddon.TEXAS_HOLDEM_MENU.get(), TexasHoldemScreen::new);

        // Register client-side packet handler for chip sync
        ChartaMod.getPacketManager().registerClientHandler(
                TexasHoldemChipsPayload.class,
                CasinoClientPayloadHandlers::handleTexasHoldemChips);
    }

    private CasinoAddonClient() {}
}
