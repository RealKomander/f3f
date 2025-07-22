# F3F - Render Distance Control Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.7-green.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Latest-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](#license)

A Fabric mod that restores the classic **F3+F** render distance controls removed from Minecraft 1.19+, with enhanced server-client synchronization and permission management.


## 📦 Installation

### Requirements
- **Minecraft**: 1.21.7
- **Fabric Loader**: 0.16.14+
- **Fabric API**: 0.129.0+1.21.7
- **LuckPerms** (Optional): For permission management

### Client & Server Installation
1. Download the latest release from [Releases](https://github.com/yourusername/f3f/releases)
2. Place `f3f-VERSION.jar` in your `mods/` folder **on both client an server**
3. Install [Fabric API](https://modrinth.com/mod/fabric-api) if not already installed
4. (Optional) Install [LuckPerms](https://luckperms.net/) for permission management


## 🎯 Usage

### Key Combinations
- **F3 + F**: Increase render distance by 1 chunk
- **Shift + F3 + F**: Decrease render distance by 1 chunk

### Automatic Features
- **Login Sync**: Your client render distance is synced to the server when you join
- **Settings Sync**: Changes in Video Settings → Render Distance automatically sync to the server
- **Permission-Based**: All features respect the configured permission system

## 🔐 Permissions

### LuckPerms Setup

```
f3f.change - allows managing server render distance

/lp user <username> permission set f3f.change true
```

- **Without LuckPerms**, all players can change render distance

### Chat Messages
When you change render distance, you'll see messages like:

```
[Debug]: Render Distance: 12
[Debug]: Unable to change render distance; no permission
```

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
- Easily extensible for additional languages

## 🛠️ Development

### Building from Source
```
git clone https://github.com/RealKomander/f3f.git
cd f3f
./gradlew build
```

## 🐛 Troubleshooting

### Common Issues

**F3+F doesn't work:**
- Check if you have the `f3f.change` permission
- Verify LuckPerms is properly installed and configured
- Check server logs for error messages

**Render distance not syncing:**
- Ensure `enableAutoSync` is `true` in config
- Check that you have the required permission
- Verify both client and server have the mod installed



## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Adding Translations
To add a new language:
1. Create `src/main/resources/assets/f3f/lang/<locale>.json`
2. Copy the structure from `en_us.json`
3. Translate all strings
4. Test with the new locale

## 📋 Changelog

### Version 1.0.0
- Initial release
- F3+F key combinations for render distance control
- LuckPerms integration
- Automatic client-server synchronization
- Configurable settings
- Multi-language support (English, Russian)
- Intelligent conflict prevention

## 📄 License

All Rights Reserved.

This mod is proprietary software. You may not distribute, modify, or use this software except as permitted by the author.

## 🙏 Acknowledgments

- **Perplexity AI** - For the opportunity to use Claude Sonnet for only 1$ a month
- **Claude AI** - For bringing this mod to life
- **DristMine community** - For pushing me to develop SpeedrunPVP, which inspired this mod

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/RealKomander/f3f/issues)
- **Discord**: [DristMine server](https://discord.gg/9QsZBCyNRJ)

---

⭐ If you found this mod helpful, please consider starring the repository!

