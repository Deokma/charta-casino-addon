# Charta Casino Addon

A reference addon for [Charta](https://github.com/lucaargolo/charta) that adds **Blackjack** and **Texas Hold'em** as standalone card games using the Charta Addon API.

## Games

- **Blackjack** — bet, hit, stand, double down. Dealer auto-plays at 17+.
- **Texas Hold'em** — full poker with blinds, betting rounds (pre-flop → flop → turn → river → showdown), side pots, and 3D chip stacks on the table block.

## Requirements

- Minecraft 1.21.1
- [Charta](https://github.com/lucaargolo/charta) 1.3.0+
- Fabric Loader 0.17.3+ **or** NeoForge 21.1.212+

## Building

```bash
# 1. Build the base Charta mod first
cd /path/to/charta
./gradlew :common:jar :neoforge:jar :fabric:remapJar

# 2. Build this addon
cd /path/to/charta-casino-addon
./gradlew build
```

The built jars will be in `neoforge/build/libs/` and `fabric/build/libs/`.

## How it works

This addon uses the `ChartaAddonRegistry` API introduced in Charta 1.3.0:

```java
// CasinoAddon.java
ChartaAddonRegistry.registerGame("blackjack",    () -> BlackjackGame::new);
ChartaAddonRegistry.registerGame("texas_holdem", () -> TexasHoldemGame::new);

ChartaAddonRegistry.registerMenu("blackjack",    BlackjackMenu::new,
    () -> AbstractCardMenu.Definition.STREAM_CODEC);
ChartaAddonRegistry.registerMenu("texas_holdem", TexasHoldemMenu::new,
    () -> AbstractCardMenu.Definition.STREAM_CODEC);
```

See [ADDON_GUIDE.md](https://github.com/lucaargolo/charta/blob/main/ADDON_GUIDE.md) for full documentation.

## License

MPL-2.0
