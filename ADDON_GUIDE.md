# Charta Addon Development Guide

## Overview

Charta exposes a clean API for registering new card games without modifying the base mod.
All addon registration goes through `ChartaAddonRegistry`.

## What addons can register

- **Game types** — new card games (Blackjack, Poker, etc.)
- **Menu types** — the container/screen for each game
- **Network packets** — custom payloads for game-specific client↔server communication

## Addon mod structure

A Charta addon is a standard Fabric/NeoForge mod that declares Charta as a dependency.
It follows the same multiloader layout:

```
my-games-addon/
  common/
    src/main/java/com/example/mygames/
      MyGamesAddon.java          ← calls ChartaAddonRegistry
      game/
        blackjack/
          BlackjackGame.java
          BlackjackMenu.java
        texasholdem/
          TexasHoldemGame.java
          TexasHoldemMenu.java
  fabric/
    src/main/java/com/example/mygames/fabric/
      MyGamesAddonFabric.java
  neoforge/
    src/main/java/com/example/mygames/neoforge/
      MyGamesAddonNeoForge.java
```

## Registration example

```java
// MyGamesAddon.java — called from both Fabric and NeoForge initializers
public class MyGamesAddon {

    public static void init() {
        // 1. Register game type
        ChartaAddonRegistry.registerGame("blackjack", () -> BlackjackGame::new);
        ChartaAddonRegistry.registerGame("texas_holdem", () -> TexasHoldemGame::new);

        // 2. Register menu types
        ChartaAddonRegistry.registerMenu("blackjack",
            BlackjackMenu::new,
            AbstractCardMenu.Definition.STREAM_CODEC);

        ChartaAddonRegistry.registerMenu("texas_holdem",
            TexasHoldemMenu::new,
            AbstractCardMenu.Definition.STREAM_CODEC);

        // 3. Register any custom packets (if needed)
        // ChartaAddonRegistry.registerPacket(
        //     ModPacketManager.PacketDirection.PLAY_TO_BOTH,
        //     MyCustomPayload.class);
    }
}
```

## Implementing a game

Extend `Game<G, M>` and implement the required methods:

```java
public class BlackjackGame extends Game<BlackjackGame, BlackjackMenu> {

    public BlackjackGame(List<CardPlayer> players, Deck deck) {
        super(players, deck);
        // set up your GameSlots here
    }

    @Override
    public ModMenuTypeRegistry.AdvancedMenuTypeEntry<BlackjackMenu, AbstractCardMenu.Definition>
    getMenuType() {
        // Return the entry you registered via ChartaAddonRegistry.registerMenu(...)
        return MyGamesAddon.BLACKJACK_MENU;
    }

    @Override
    public BlackjackMenu createMenu(int containerId, Inventory inv, AbstractCardMenu.Definition def) {
        return new BlackjackMenu(containerId, inv, def);
    }

    @Override
    public Predicate<Deck> getDeckPredicate() {
        return deck -> deck.getCards().size() >= 52
            && Suits.STANDARD.containsAll(deck.getSuits());
    }

    @Override
    public Predicate<Card> getCardPredicate() {
        return card -> Suits.STANDARD.contains(card.suit())
            && Ranks.STANDARD.contains(card.rank());
    }

    @Override public boolean canPlay(CardPlayer player, GamePlay play) { ... }
    @Override public void startGame() { ... }
    @Override public void runGame() { ... }
    @Override public void endGame() { ... }
    @Override public List<GameOption<?>> getOptions() { return List.of(); }
}
```

## Implementing a menu

Extend `AbstractCardMenu<G, M>`:

```java
public class BlackjackMenu extends AbstractCardMenu<BlackjackGame, BlackjackMenu> {

    public BlackjackMenu(int containerId, Inventory inventory, Definition definition) {
        super(MyGamesAddon.BLACKJACK_MENU.get(), containerId, inventory, definition);
        // add card slots via addCardSlot(...)
    }

    @Override
    public GameType<BlackjackGame, BlackjackMenu> getGameType() {
        return MyGamesAddon.BLACKJACK_GAME.get();
    }
}
```

## Gradle dependency (gradle.properties in your addon)

```properties
# Depend on Charta
charta_version=1.3.0
```

```groovy
// build.gradle
dependencies {
    // Fabric
    modImplementation "dev.lucaargolo:charta-fabric-1.21.1:${charta_version}"
    // NeoForge
    implementation "dev.lucaargolo:charta-neoforge-1.21.1:${charta_version}"
}
```

## Initialization order

Charta calls `ChartaMod.init()` during mod loading. Your addon's `init()` must run
**before or during** the same loading phase (mod constructor / static init is fine).
The `ChartaAddonRegistry` queues registrations and applies them at the right time.

## Key classes

| Class | Purpose |
|---|---|
| `ChartaAddonRegistry` | Entry point for all addon registrations |
| `Game<G, M>` | Base class for game logic |
| `AbstractCardMenu<G, M>` | Base class for game menus |
| `GameSlot` | Card pile/hand on the table |
| `DrawSlot` | Draw pile with draw-once logic |
| `PlaySlot` | Play pile that validates via `canPlay()` |
| `CardPlayer` | Abstraction for human/AI players |
| `GameOption` | Configurable game rules (Bool, Number) |
| `Suits` / `Ranks` | Standard card registries (extensible) |
