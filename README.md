# GigHub - Player-to-Player Contract Marketplace

<div align="center">

![GigHub Logo](https://img.shields.io/badge/GoigHub-Contract%20Marketplace-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4+-orange?style=for-the-badge)
![License](https://img.shields.io/badge/LoLicense-MIT-blue?style=for-the-badge)

**A comprehensive Minecraft plugin that creates a dynamic marketplace for players to create, accept, and complete contracts with escrow-based payment protection.**

[![Features](#-features)] [![Installation](#-installation)] [![Configuration](#-configuration)] [![API](#-api)] [![Contributing](#-contributing)]

</div>

## 🌟 Features

### 💼 Contract Management
- **Create Contracts**: Players can create various types of contracts with custom requirements
- **Accept & Complete**: Accept contracts from other players and mark them as completed
- **Escrow System**: Secure payment handling with automatic escrow management
- **Verification Process**: Contract completion verification with manual/auto-verify options

### 🎯 Contract Types
- **Item Requests**: Gather and deliver specific items
- **Building**: Construction projects with location requirements
- **Combat Tasks**: Combat-related objectives
- **Resource Mining**: Mining and resource collection
- **Crop Harvesting**: Farming and agriculture tasks
- **Custom Tasks**: Fully customizable contract types

### 🛡️ Security & Economy
- **Vault Integration**: Full economy plugin support
- **Reputation System**: Player reputation tracking
- **Fraud Protection**: Escrow-based payment security
- **Contract Limits**: Configurable limits per player

### 🌐 Localization
- **Multi-language Support**: English, Russian, and more
- **Dynamic Locale Switching**: Players can choose their preferred language
- **Easy Translation**: Simple YAML-based translation files

### 🎨 Modern GUI
- **Intuitive Interface**: Clean, modern inventory-based GUIs
- **Interactive Elements**: Click-based contract management
- **Real-time Updates**: Live contract status updates
- **Mobile-friendly**: Responsive design for all screen sizes

## 📋 Requirements

- **Minecraft**: 1.20.4 or higher
- **Java**: 17 or higher
- **Vault**: Required for economy integration
- **Compatible Economy Plugin**: EssentialsX, CMI, etc.

## 🚀 Installation

1. **Download the latest release** from the [Releases page](https://github.com/eliasnvx/GigHub/releases)
2. **Place the JAR file** in your server's `plugins/` directory
3. **Install Vault** if not already present
4. **Restart your server**
5. **Configure** the plugin to your needs (see Configuration section)

### Quick Setup

```bash
# Download and install
wget https://github.com/eliasnvx/GigHub/releases/latest/download/GigHub.jar
cp GigHub.jar /path/to/your/server/plugins/
```

## ⚙️ Configuration

### Basic Configuration (`config.yml`)

```yaml
# Economy settings
economy:
  min-reward: 100.0
  max-reward: 1000000.0
  escrow-fee: 0.05  # 5% fee

# Contract limits
contracts:
  max-active-per-player: 5
  max-title-length: 100
  max-description-length: 500

# Database settings
database:
  type: sqlite  # sqlite, mysql, postgresql
  host: localhost
  port: 3306
  database: gighub
  username: root
  password: password
```

### Localization

Add new languages by creating files in `plugins/GigHub/lang/`:

```yaml
# lang/en_US.yml
messages:
  prefix: "&6[GigHub] "
  contract-created: "&aContract created successfully!"
  # ... more messages
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/gig` | `gighub.use` | Opens main contract menu |
| `/gig create` | `gighub.create` | Create new contract |
| `/gig list` | `gighub.list` | Browse available contracts |
| `/gig accept <id>` | `gighub.accept` | Accept a contract |
| `/gig my` | `gighub.my` | View your contracts |
| `/gig admin` | `gighub.admin` | Administrative commands |

## 🔧 API Integration

### Maven Dependency

```xml
<dependency>
    <groupId>io.eliasnvx</groupId>
    <artifactId>gighub-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Example Usage

```java
// Get GigHub API
GigHubAPI api = GigHubPlugin.getAPI();

// Create contract programmatically
Contract contract = api.createContract()
    .type(ContractType.ITEM_REQUEST)
    .title("Diamond Collection")
    .description("Need 64 diamonds")
    .reward(5000.0)
    .creator(player)
    .build();

// Listen to contract events
@EventHandler
public void onContractAccepted(ContractAcceptedEvent event) {
    Player player = event.getPlayer();
    Contract contract = event.getContract();
    // Handle contract acceptance
}
```

## 🏗️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/eliasnvx/GigHub.git
cd GigHub

# Build with Maven
./mvnw clean compile

# Package plugin
./mvnw package

# Development build
./quick-dev.sh build
```

### Project Structure

```
GigHub/
├── gighub-api/          # Public API
├── gighub-core/         # Core business logic
├── gighub-bukkit/       # Bukkit/Spigot implementation
├── gighub-sponge/       # Sponge implementation (planned)
├── docs/                # Documentation
└── test-server/         # Development testing server
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Code Style

- Follow Java 17+ conventions
- Use meaningful variable names
- Add comprehensive Javadoc comments
- Include unit tests for new features

### Development Setup

```bash
# Clone your fork
git clone https://github.com/yourusername/GigHub.git
cd GigHub

# Set up development environment
./quick-dev.sh setup

# Start test server
./quick-dev.sh server
```

## 📊 Statistics

- **Active Contracts**: Real-time contract tracking
- **Player Reputation**: Dynamic reputation system
- **Economy Impact**: Monitor marketplace activity
- **Performance Metrics**: Built-in performance monitoring

## 🐛 Bug Reports & Feature Requests

- **Bug Reports**: [Open an issue](https://github.com/eliasnvx/GigHub/issues/new?template=bug_report.md)
- **Feature Requests**: [Open an issue](https://github.com/eliasnvx/GigHub/issues/new?template=feature_request.md)
- **Support**: [Join our Discord](https://discord.gg/gighub)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [EliasNVX](https://github.com/eliasnvx)
- **Contributors**: [All contributors](https://github.com/eliasnvx/GigHub/graphs/contributors)
- **Special Thanks**: The Minecraft plugin development community

## 📈 Roadmap

- [ ] **v1.1**: Advanced filtering system
- [ ] **v1.2**: Contract templates
- [ ] **v1.3**: API improvements
- [ ] **v2.0**: Sponge platform support
- [ ] **v2.1**: Cross-server contracts

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ for the Minecraft community

</div>
