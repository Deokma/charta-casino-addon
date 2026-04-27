package dev.lucaargolo.charta.casino.neoforge;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.client.PokerChipRenderer;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackScreen;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemScreen;
import dev.lucaargolo.charta.casino.network.BlackjackActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.common.game.Games;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(CasinoAddon.MOD_ID)
public class CasinoAddonNeoForge {

    private static final DeferredRegister<GameType<?, ?>> GAME_TYPES = DeferredRegister.create(Games.REGISTRY_KEY, CasinoAddon.MOD_ID);
    private static final Supplier<GameType<BlackjackGame, BlackjackMenu>> BLACKJACK_GAME =
            GAME_TYPES.register("blackjack", () -> BlackjackGame::new);
    private static final Supplier<GameType<TexasHoldemGame, TexasHoldemMenu>> TEXAS_HOLDEM_GAME =
            GAME_TYPES.register("texas_holdem", () -> TexasHoldemGame::new);

    public CasinoAddonNeoForge(IEventBus modBus) {
        CasinoAddon.init();
        GAME_TYPES.register(modBus);
        CasinoAddon.BLACKJACK_GAME = BLACKJACK_GAME;
        CasinoAddon.TEXAS_HOLDEM_GAME = TEXAS_HOLDEM_GAME;

        modBus.addListener(this::registerPayloadHandlers);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(CasinoAddon.BLACKJACK_MENU.get(), BlackjackScreen::new);
                event.register(CasinoAddon.TEXAS_HOLDEM_MENU.get(), TexasHoldemScreen::new);
                PokerChipRenderer.register();
            });
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(BlackjackActionPayload.TYPE, BlackjackActionPayload.STREAM_CODEC, (payload, context) ->
                BlackjackActionPayload.handleServer(payload, (net.minecraft.server.level.ServerPlayer) context.player()));
        registrar.playToServer(TexasHoldemActionPayload.TYPE, TexasHoldemActionPayload.STREAM_CODEC, (payload, context) ->
                TexasHoldemActionPayload.handleServer(payload, (net.minecraft.server.level.ServerPlayer) context.player()));
        registrar.playToClient(TexasHoldemChipsPayload.TYPE, TexasHoldemChipsPayload.STREAM_CODEC, (payload, context) ->
                TexasHoldemChipsPayload.handleClient(payload));
    }
}
