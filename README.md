# charta-casino-addon

Blackjack and Texas Hold'em addon for [Charta](https://github.com/lucaargolo/charta).

## Getting Started

### Prerequisites

- JDK 21
- Git

### Cloning

This project uses a git submodule for the Charta base mod. After cloning, initialise it:

```bash
git clone https://github.com/your-username/charta-casino-addon.git
cd charta-casino-addon
git submodule update --init --recursive
```

Or clone with submodules in one step:

```bash
git clone --recurse-submodules https://github.com/your-username/charta-casino-addon.git
```

### Building

```bash
./gradlew build
```

Gradle will automatically build the Charta base mod from the submodule at `libs/charta` and use it as a dependency — no manual jar building required.

### Updating Charta

To update the base mod to a newer commit:

```bash
cd libs/charta
git pull origin main
cd ../..
git add libs/charta
git commit -m "Update charta submodule"
```

Then update `charta_version` in `gradle.properties` to match the new version.
