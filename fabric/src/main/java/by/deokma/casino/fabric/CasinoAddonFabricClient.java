package by.deokma.casino.fabric;

import by.deokma.casino.CasinoAddon;
import by.deokma.casino.client.PokerChipRenderer;
import by.deokma.casino.game.blackjack.BlackjackScreen;
import by.deokma.casino.game.texasholdem.TexasHoldemScreen;
import by.deokma.casino.network.TexasHoldemChipsPayload;
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
