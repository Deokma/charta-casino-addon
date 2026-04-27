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
import dev.lucaargolo.charta.common.menu.AbstractCardMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class CasinoAddonFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CasinoAddon.init();

        GameType<BlackjackGame, BlackjackMenu> blackjack = Registry.register(Games.getRegistry(), ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "blackjack"), BlackjackGame::new);
        GameType<TexasHoldemGame, TexasHoldemMenu> texasHoldem = Registry.register(Games.getRegistry(), ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "texas_holdem"), TexasHoldemGame::new);

        CasinoAddon.BLACKJACK_GAME = () -> blackjack;
        CasinoAddon.TEXAS_HOLDEM_GAME = () -> texasHoldem;

        MenuType<BlackjackMenu> blackJackMenu = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "blackjack"),
                new ExtendedScreenHandlerType<>(BlackjackMenu::new, dev.lucaargolo.charta.common.menu.AbstractCardMenu.Definition.STREAM_CODEC)
        );
        MenuType<TexasHoldemMenu> texasHoldemMenu = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "texas_holdem"),
                new ExtendedScreenHandlerType<>(TexasHoldemMenu::new, dev.lucaargolo.charta.common.menu.AbstractCardMenu.Definition.STREAM_CODEC)
        );

        CasinoAddon.BLACKJACK_MENU = () -> blackJackMenu;
        CasinoAddon.TEXAS_HOLDEM_MENU = () -> texasHoldemMenu;

        PayloadTypeRegistry.playC2S().register(BlackjackActionPayload.TYPE, BlackjackActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TexasHoldemActionPayload.TYPE, TexasHoldemActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TexasHoldemChipsPayload.TYPE, TexasHoldemChipsPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BlackjackActionPayload.TYPE, (payload, context) ->
                BlackjackActionPayload.handleServer(payload, context.player(), context.server()));
        ServerPlayNetworking.registerGlobalReceiver(TexasHoldemActionPayload.TYPE, (payload, context) ->
                TexasHoldemActionPayload.handleServer(payload, context.player(), context.server()));
    }

}
