# F3F - Render Distance Control Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1:1.21.7-green.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Latest-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](#license)

## 🎯 Features

### Key Combinations
- **F3 + F**: Increase render distance by 1 chunk
- **Shift + F3 + F**: Decrease render distance by 1 chunk

### Client & Server Installation
1. Download the latest release from [Releases](https://github.com/RealKomander/f3f/releases)
2. Place `f3f-VERSION.jar` in your `mods/` folder **on both client an server**
3. Install [Fabric API](https://modrinth.com/mod/fabric-api) if not already installed
4. (Optional) Install [LuckPerms](https://luckperms.net/) for permission management

## 🔐 Permissions

### LuckPerms Setup

```
f3f.change - allows managing server render distance

/lp user <username> permission set f3f.change true
```

- **Without LuckPerms**, all players can change render distance

## ⚙️ Configuration

The mod automatically creates `config/f3f.json`.


### Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| `permissionNode` | LuckPerms permission required to change render distance | `f3f.change` |
| `minRenderDistance` | Minimum allowed render distance (chunks) | `2` |
| `maxRenderDistance` | Maximum allowed render distance (chunks) | `32` |
| `enableAutoSync` | Enable automatic sync when render distance changes in settings | `true` |
| `enableF3FKeys` | Enable F3+F key combinations | `true` |
| `f3fCooldown` | Cooldown after F3+F usage (milliseconds) | `1000` |
| `serverUpdateCooldown` | Cooldown after server updates (milliseconds) | `1000` |

### 🌍 Localization
- **English** (`en_us`)
- **Russian** (`ru_ru`)

## 🛠️ Development

### Building from Source
```
git clone https://github.com/RealKomander/f3f.git
cd f3f
./gradlew build
```

## 📋 Changelog

### Version 1.1.0
- Now supports Minecraft 1.20.1-1.21.8
- Better server-client sync when several players change render distance
- Now works in client-only environments

### Version 1.0.0
- Initial release
- F3+F key combinations for render distance control
- LuckPerms integration
- Automatic client-server synchronization
- Configurable settings
- Multi-language support (English, Russian)
- Intelligent conflict prevention

