package dev.lucaargolo.charta.casino.fabric;

import dev.lucaargolo.charta.casino.CasinoAddon;
import net.fabricmc.api.ModInitializer;

public class CasinoAddonFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CasinoAddon.init();
    }
}
