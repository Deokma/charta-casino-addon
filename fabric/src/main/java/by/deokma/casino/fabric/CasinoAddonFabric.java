package by.deokma.casino.fabric;

import by.deokma.casino.CasinoAddon;
import by.deokma.casino.game.blackjack.BlackjackGame;
import by.deokma.casino.game.blackjack.BlackjackMenu;
import by.deokma.casino.game.durak.DurakGame;
import by.deokma.casino.game.durak.DurakMenu;
import by.deokma.casino.game.texasholdem.TexasHoldemGame;
import by.deokma.casino.game.texasholdem.TexasHoldemMenu;
import by.deokma.casino.network.BlackjackActionPayload;
import by.deokma.casino.network.DurakActionPayload;
import by.deokma.casino.network.TexasHoldemActionPayload;
import by.deokma.casino.network.TexasHoldemChipsPayload;
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
        GameType<DurakGame, DurakMenu> durak = Registry.register(Games.getRegistry(), ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "durak"), DurakGame::new);

        CasinoAddon.BLACKJACK_GAME = () -> blackjack;
        CasinoAddon.TEXAS_HOLDEM_GAME = () -> texasHoldem;
        CasinoAddon.DURAK_GAME = () -> durak;

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
        MenuType<DurakMenu> durakMenu = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CasinoAddon.MOD_ID, "durak"),
                new ExtendedScreenHandlerType<>(DurakMenu::new, dev.lucaargolo.charta.common.menu.AbstractCardMenu.Definition.STREAM_CODEC)
        );

        CasinoAddon.BLACKJACK_MENU = () -> blackJackMenu;
        CasinoAddon.TEXAS_HOLDEM_MENU = () -> texasHoldemMenu;
        CasinoAddon.DURAK_MENU = () -> durakMenu;

        PayloadTypeRegistry.playC2S().register(BlackjackActionPayload.TYPE, BlackjackActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TexasHoldemActionPayload.TYPE, TexasHoldemActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DurakActionPayload.TYPE, DurakActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TexasHoldemChipsPayload.TYPE, TexasHoldemChipsPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BlackjackActionPayload.TYPE, (payload, context) ->
                BlackjackActionPayload.handleServer(payload, context.player(), context.server()));
        ServerPlayNetworking.registerGlobalReceiver(TexasHoldemActionPayload.TYPE, (payload, context) ->
                TexasHoldemActionPayload.handleServer(payload, context.player(), context.server()));
        ServerPlayNetworking.registerGlobalReceiver(DurakActionPayload.TYPE, (payload, context) ->
                DurakActionPayload.handleServer(payload, context.player(), context.server()));
    }

}
