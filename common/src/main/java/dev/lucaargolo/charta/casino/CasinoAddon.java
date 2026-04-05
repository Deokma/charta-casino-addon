package dev.lucaargolo.charta.casino;

import dev.lucaargolo.charta.casino.game.blackjack.BlackjackGame;
import dev.lucaargolo.charta.casino.game.blackjack.BlackjackMenu;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemGame;
import dev.lucaargolo.charta.casino.game.texasholdem.TexasHoldemMenu;
import dev.lucaargolo.charta.casino.network.BlackjackActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemActionPayload;
import dev.lucaargolo.charta.casino.network.TexasHoldemChipsPayload;
import dev.lucaargolo.charta.common.addon.ChartaAddonRegistry;
import dev.lucaargolo.charta.common.game.api.game.GameType;
import dev.lucaargolo.charta.common.menu.AbstractCardMenu;
import dev.lucaargolo.charta.common.network.ModPacketManager;
import dev.lucaargolo.charta.common.registry.ModMenuTypeRegistry;
import dev.lucaargolo.charta.common.registry.minecraft.MinecraftEntry;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Supplier;

/**
 * Entry point for the Charta Casino addon.
 * Call {@link #init()} from both Fabric and NeoForge mod initializers.
 * All registrations are deferred — they are applied when ChartaMod.init() runs.
 */
public final class CasinoAddon {

    public static final String MOD_ID = "charta_casino";

    // These holders are populated during ChartaMod.init() — safe to use after that point
    public static ChartaAddonRegistry.AddonEntry<MinecraftEntry<GameType<BlackjackGame, BlackjackMenu>>> BLACKJACK_GAME;
    public static ChartaAddonRegistry.AddonEntry<MinecraftEntry<GameType<TexasHoldemGame, TexasHoldemMenu>>> TEXAS_HOLDEM_GAME;

    public static ChartaAddonRegistry.AddonEntry<ModMenuTypeRegistry.AdvancedMenuTypeEntry<BlackjackMenu, AbstractCardMenu.Definition>> BLACKJACK_MENU;
    public static ChartaAddonRegistry.AddonEntry<ModMenuTypeRegistry.AdvancedMenuTypeEntry<TexasHoldemMenu, AbstractCardMenu.Definition>> TEXAS_HOLDEM_MENU;

    public static void init() {
        BLACKJACK_GAME    = ChartaAddonRegistry.registerGame("blackjack",    () -> BlackjackGame::new);
        TEXAS_HOLDEM_GAME = ChartaAddonRegistry.registerGame("texas_holdem", () -> TexasHoldemGame::new);

        // Supplier wrapping avoids triggering Deck/Suits static init before ChartaMod.instance is set
        Supplier<StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, AbstractCardMenu.Definition>> defCodec =
                () -> AbstractCardMenu.Definition.STREAM_CODEC;

        BLACKJACK_MENU    = ChartaAddonRegistry.registerMenu("blackjack",    BlackjackMenu::new,    defCodec);
        TEXAS_HOLDEM_MENU = ChartaAddonRegistry.registerMenu("texas_holdem", TexasHoldemMenu::new,  defCodec);

        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_SERVER, BlackjackActionPayload.class);
        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_SERVER, TexasHoldemActionPayload.class);
        ChartaAddonRegistry.registerPacket(ModPacketManager.PacketDirection.PLAY_TO_CLIENT, TexasHoldemChipsPayload.class);
    }

    private CasinoAddon() {}
}
