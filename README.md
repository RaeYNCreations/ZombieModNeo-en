# Zombie Mode - NeoForge 1.21.1 Minecraft Mod

A zombie survival mod for Minecraft with a wave system, points, weapon crates, gun range feature and team respawn.

## Features

### 🎮 Gameplay System
- **Start Countdown**: 60 seconds to join before the start
- **Zombie Waves**: Increasing difficulty (6 + wave × 6 zombies)
- **Points**: 100 points per zombie killed, retained after death
- **Respawn System**: Automatic respawn at the end of each wave
- **Game Over**: When all active players are dead

### 💀 Zombies
- The last 32 zombies in each wave glow
- Random spawn at configurable points
- Real-time counter of remaining zombies

### 🎁 Weapon Crates
- **3 predefined presets**: Starter (500 points), Advanced (1500 points), Legendary (5000 points)
- **Weighted Loot System**: Rare weapons Harder to obtain
- **Enchanted Weapons**: From simple bows to legendary weapons
- **Vanilla Chests**: Use double chests for crates

### 👥 Player System
- **Active Players**: In-game, can earn points and make purchases
- **Waiting Players**: Join at the end of the current wave
- **Spectators**: The undead wait for the end of the wave in spectator mode

## Commands

### Game Commands
- `/zombiestart` - Starts a game with a 60-second countdown
- `/zombiejoin` - Join the current game
- `/zombieleave` - Leave the game
- `/zombiestatus` - Display information (wave, players, points)

### Configuration
- `/zombiespawn` - **(Admin)** Add a zombie spawn point
- `/zombiespawn list` - **(Admin)** List all spawn points
- `/zombiespawn clear` - **(Admin)** Delete all spawn points
- `/zombierespawn` - **(Admin)** Set the team spawn point
- `/zombierespawn show` - **(Admin)** Show the spawn point

### Weapon Crates
- `/weaponcrate create <cost>` - **(Admin)** Create a crate (view a chest)
- `/weaponcrate addweapon <item> <count> <weight> <name>` - **(Admin)** Add a custom weapon
- `/weaponcrate preset starter` - **(Admin)** Create Starter crate (500 points)
- `/weaponcrate preset advanced` - **(Admin)** Create Advanced crate (1500 points)
- `/weaponcrate preset legendary` - **(Admin)** Create Legendary crate (5000 points)

## Installation

1. **Prerequisites**:

- Minecraft 1.21.1

- NeoForge 21.1.77+

- Java 21

2. **Build the mod**:

```bash

./gradlew build
```
The JAR file will be in `build/libs/`

3. **Installation**:

- Place the JAR in the `mods` folder of your NeoForge server/client

- Restart the server/client

## Setting up a game

### 1. Prepare the arena
1. Set the respawn point: `/zombierespawn`
2. Place zombie spawn points: `/zombiespawn` (minimum 3-4 points recommended)
3. Place Transform chests into crates:

```
/weaponcrate preset starter
/weaponcrate preset advanced
/weaponcrate preset legendary
```

### 2. Starting the Game
1. An admin starts: `/zombiestart`
2. A 60-second countdown begins
3. Players join: `/zombiejoin`
4. Wave 1 starts automatically

### 3. During the Game
- Kill zombies to earn points
- Buy weapons from crates by clicking on them
- Survive as long as possible!

## Crate Presets

### 🟢 Starter (500 points)
- Survival Knife (40%)
- Basic Sword (30%)
- Iron Blade (20%)
- Simple Bow + Infinity (10%)

### 🔵 Advanced (1500 points)
- Diamond Sword + Sharpness III (30%)
- Mighty Bow + Power IV + Infinity (25%)
- Rapid Crossbow + Quick Charge III (25%)
- Trident + Loyalty III (20%)

### 🔴 Legendary (5000 points)
- **INFERNAL BLADE** - Netherite Sword + Sharpness V + Fire Aspect II + Looting III (50%)
- **DIVINE BOW** - Bow + Power V + Flame + Infinity (30%)
- **POSEIDON'S TRIDENT** - Trident + Loyalty III + Impaling V + Channeling (20%)

## HUD

The HUD displays the following in real time:
- **Current Wave** (top left)
- **Remaining Zombies** (top left)
- **Player Points** (top left, if active)
- **Countdown** between waves (center of screen)
- **Status** (waiting, etc.)

## Important Behaviors

### Joining a Match in Progress
- **During the countdown (60s)**: Join immediately in Adventure Mode
- **During a Wave**: Spectator Mode, join at the end of the wave
- **Between Waves**: Join immediately in Survival Mode

### Death and Respawn
- **Died during a wave**: Spectator Mode until the end
- **End of Wave**: Automatic respawn with restored health/hunger
- **Points are never lost**: Points are never lost

### Game Over
- All active players are dead
- Displays The wave reached
- Everyone becomes a spectator
- Option to restart with `/zombiestart`

## Project Structure
```
src/main/java/com/zombiemod/
├── ZombieMod.java # Main Class
├── manager/
│ ├── GameManager.java # Player and State Management
│ ├── WaveManager.java # Wave and Zombie Management
│ ├── PointsManager.java # Point Management
│ └── RespawnManager.java # Respawn Management
├── system/
│ └── WeaponCrateManager.java # Crate System
├── event/
│ ├── ZombieEventHandler.java
│ ├── ChestInteractionHandler.java
│ └── PlayerDeathHandler.java
├── command/
│ ├── GameCommands.java
│ ├── SpawnCommand.java
│ ├── RespawnCommand.java
│ └── WeaponCrateCommand.java
└── client/
└── ZombieHUD.java # HUD Display
```

## Developement

- **Minecraft** : 1.21.1
- **NeoForge** : 21.1.77
- **Java** : 21
- **Gradle** : 8.x

## Licence

All Rights Reserved

## Authors

RaeYNCraft
