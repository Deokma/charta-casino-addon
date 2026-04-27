package dev.lucaargolo.charta.casino.fabric;

import dev.lucaargolo.charta.casino.CasinoAddon;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.casino.network.BlackjackActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.common.game.Games;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class CasinoAddonFabric implements ModInitializer {

    @Override
    @SuppressWarnings("unchecked")
    public void onInitialize() {
        CasinoAddon.init();

        Registry<GameType<?, ?>> gameTypeRegistry = (Registry<GameType<?, ?>>) BuiltInRegistries.REGISTRY.get(Games.REGISTRY_KEY.location());
        GameType<BlackjackGame, BlackjackMenu> blackjack = Registry.register(gameTypeRegistry, ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "blackjack"), BlackjackGame::new);
        GameType<TexasHoldemGame, TexasHoldemMenu> texasHoldem = Registry.register(gameTypeRegistry, ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "texas_holdem"), TexasHoldemGame::new);

        CasinoAddon.BLACKJACK_GAME = () -> blackjack;
        CasinoAddon.TEXAS_HOLDEM_GAME = () -> texasHoldem;

        PayloadTypeRegistry.playC2S().register(BlackjackActionPayload.TYPE, BlackjackActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TexasHoldemActionPayload.TYPE, TexasHoldemActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TexasHoldemChipsPayload.TYPE, TexasHoldemChipsPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BlackjackActionPayload.TYPE, (payload, context) ->
                BlackjackActionPayload.handleServer(payload, context.player(), context.server()));
        ServerPlayNetworking.registerGlobalReceiver(TexasHoldemActionPayload.TYPE, (payload, context) ->
                TexasHoldemActionPayload.handleServer(payload, context.player(), context.server()));
    }
}
