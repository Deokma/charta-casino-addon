package dev.lucaargolo.charta.casino.fabric;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.client.PokerChipRenderer;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class CasinoAddonFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(CasinoAddon.BLACKJACK_MENU.get(), BlackjackScreen::new);
        MenuScreens.register(CasinoAddon.TEXAS_HOLDEM_MENU.get(), TexasHoldemScreen::new);

        PokerChipRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(TexasHoldemChipsPayload.TYPE, (payload, context) ->
                TexasHoldemChipsPayload.handleClient(payload, context.client()));
    }
}
