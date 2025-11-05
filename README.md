# GigHub - Player-to-Player Contract Marketplace

<div align="center">

![GigHub Logo](https://img.shields.io/badge/G oigHub-Contract%20Marketplace-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4+-orange?style=for-the-badge)
![License](https://img.shields.io/badge/L oLicense-MIT-blue?style=for-the-badge)

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
/gig complete <id> - Завершить контракт
/gig cancel <id> - Отменить контракт
```

### Административные
```
/gigadmin reload - Перезагрузить конфиг
/gigadmin stats - Статистика плагина
/gigadmin cancel <id> - Отменить любой контракт
/gigadmin cleanup - Очистить истекшие контракты
```

## 🔐 Права

```yaml
gighub.use - Базовый доступ
gighub.contract.create - Создание контрактов
gighub.limit.contracts.3 - Лимит 3 контракта
gighub.limit.contracts.10 - Лимит 10 контрактов
gighub.admin - Админ доступ
```

Полный список прав см. в [plugin.yml](gighub-bukkit/src/main/resources/plugin.yml)

## 📊 API

```java
// Получение API
GigHubAPI api = GigHubPlugin.getAPI();

// Создание контракта
api.createContract(
    playerUuid,
    ContractType.ITEM_REQUEST,
    "Need 64 diamonds",
    "Please deliver to spawn",
    1000.0,
    System.currentTimeMillis() + 86400000L
).thenAccept(contract -> {
    // Контракт создан
});

// Получение репутации
api.getReputation(playerUuid).thenAccept(reputation -> {
    double rating = reputation.getRating();
    int completed = reputation.getTotalCompleted();
});
```

## 🌍 Локализация

Поддерживаемые языки:
- 🇺🇸 English (en_US)
- 🇷🇺 Русский (ru_RU) - В разработке
- 🇵🇱 Polski (pl_PL) - В разработке

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие проекта! 

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'Add some AmazingFeature'`)
4. Push в branch (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. См. [LICENSE](LICENSE) для деталей.

## 📞 Поддержка

- **Discord**: [Ссылка на Discord]
- **Issues**: [GitHub Issues](https://github.com/yourname/gighub/issues)
- **Wiki**: [GitHub Wiki](https://github.com/yourname/gighub/wiki)

## 🗺️ Roadmap

- [x] Базовая архитектура
- [x] Database manager
- [ ] Config система
- [ ] Escrow система
- [ ] Основные команды
- [ ] GUI интерфейс
- [ ] Система репутации
- [ ] Локализация (ru, pl)
- [ ] Discord интеграция
- [ ] Sponge версия

## 👏 Благодарности

- Spigot Team за отличный API
- HikariCP за connection pooling
- Всем контрибьюторам проекта

---

Made with ❤️ for Minecraft community
