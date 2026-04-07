package dev.lucaargolo.charta.casino;

import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.durak.DurakGame;
import dev.lucaargolo.charta.casino.game.durak.DurakMenu;
import dev.lucaargolo.charta.casino.network.*;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.casino.network.BlackjackActionPayload;
import dev.lucaargolo.charta.common.addon.ChartaAddonRegistry;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import dev.lucaargolo.charta.common.menu.AbstractCardMenu;
import dev.lucaargolo.charta.common.network.ModPacketManager;
import dev.lucaargolo.charta.common.registry.ModMenuTypeRegistry;
import dev.lucaargolo.charta.common.registry.minecraft.MinecraftEntry;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Supplier;

public final class CasinoAddon {

    public static final String MOD_ID = "charta_casino";

    public static ChartaAddonRegistry.AddonEntry<MinecraftEntry<GameType<BlackjackGame, BlackjackMenu>>>  BLACKJACK_GAME;
    public static ChartaAddonRegistry.AddonEntry<MinecraftEntry<GameType<TexasHoldemGame, TexasHoldemMenu>>> TEXAS_HOLDEM_GAME;
    public static ChartaAddonRegistry.AddonEntry<MinecraftEntry<GameType<DurakGame, DurakMenu>>> DURAK_GAME;

    public static ChartaAddonRegistry.AddonEntry<ModMenuTypeRegistry.AdvancedMenuTypeEntry<BlackjackMenu, AbstractCardMenu.Definition>>  BLACKJACK_MENU;
    public static ChartaAddonRegistry.AddonEntry<ModMenuTypeRegistry.AdvancedMenuTypeEntry<TexasHoldemMenu, AbstractCardMenu.Definition>> TEXAS_HOLDEM_MENU;
    public static ChartaAddonRegistry.AddonEntry<ModMenuTypeRegistry.AdvancedMenuTypeEntry<DurakMenu, AbstractCardMenu.Definition>> DURAK_MENU;

    public static void init() {
        BLACKJACK_GAME    = ChartaAddonRegistry.registerGame("blackjack",    () -> BlackjackGame::new);
        TEXAS_HOLDEM_GAME = ChartaAddonRegistry.registerGame("texas_holdem", () -> TexasHoldemGame::new);
        DURAK_GAME        = ChartaAddonRegistry.registerGame("durak",        () -> DurakGame::new);

        Supplier<StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, AbstractCardMenu.Definition>> defCodec =
                () -> AbstractCardMenu.Definition.STREAM_CODEC;

        BLACKJACK_MENU    = ChartaAddonRegistry.registerMenu("blackjack",    BlackjackMenu::new,    defCodec);
        TEXAS_HOLDEM_MENU = ChartaAddonRegistry.registerMenu("texas_holdem", TexasHoldemMenu::new,  defCodec);
        DURAK_MENU        = ChartaAddonRegistry.registerMenu("durak",        DurakMenu::new,        defCodec);

        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_SERVER, BlackjackActionPayload.class);
        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_SERVER, TexasHoldemActionPayload.class);
        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_CLIENT, TexasHoldemChipsPayload.class);
        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_SERVER, DurakActionPayload.class);
    }

    private CasinoAddon() {}
}
