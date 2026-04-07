package dev.lucaargolo.charta.casino.fabric;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.client.CasinoClientPayloadHandlers;
import dev.lucaargolo.charta.casino.client.PokerChipRenderer;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.durak.DurakScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.common.ChartaMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CasinoAddonFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register screens directly — Fabric allows this at any time
        MenuScreens.register(CasinoAddon.BLACKJACK_MENU.get().get(),    BlackjackScreen::new);
        MenuScreens.register(CasinoAddon.TEXAS_HOLDEM_MENU.get().get(), TexasHoldemScreen::new);
        MenuScreens.register(CasinoAddon.DURAK_MENU.get().get(),        DurakScreen::new);

        // Register 3D chip renderer for poker tables
        PokerChipRenderer.register();

        // Client packet handler
        ChartaMod.getPacketManager().registerClientHandler(
                TexasHoldemChipsPayload.class,
                CasinoClientPayloadHandlers::handleTexasHoldemChips);
    }
}
