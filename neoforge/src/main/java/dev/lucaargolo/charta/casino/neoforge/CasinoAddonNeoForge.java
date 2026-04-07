package dev.lucaargolo.charta.casino.neoforge;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.client.CasinoClientPayloadHandlers;
import dev.lucaargolo.charta.casino.client.PokerChipRenderer;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.durak.DurakScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.common.ChartaMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(CasinoAddon.MOD_ID)
public class CasinoAddonNeoForge {

    public CasinoAddonNeoForge(IEventBus modBus) {
        CasinoAddon.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(CasinoAddon.BLACKJACK_MENU.get().get(),    BlackjackScreen::new);
                event.register(CasinoAddon.TEXAS_HOLDEM_MENU.get().get(), TexasHoldemScreen::new);
                event.register(CasinoAddon.DURAK_MENU.get().get(),        DurakScreen::new);

                PokerChipRenderer.register();

                ChartaMod.getPacketManager().registerClientHandler(
                        TexasHoldemChipsPayload.class,
                        CasinoClientPayloadHandlers::handleTexasHoldemChips);
            });
        }
    }
}
