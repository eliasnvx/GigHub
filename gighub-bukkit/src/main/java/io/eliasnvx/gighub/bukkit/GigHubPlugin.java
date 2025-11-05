package io.eliasnvx.gighub.bukkit;

import io.eliasnvx.gighub.bukkit.command.GigAdminCommand;
import io.eliasnvx.gighub.bukkit.command.GigCommand;
import io.eliasnvx.gighub.bukkit.config.BukkitLocalizationService;
import io.eliasnvx.gighub.bukkit.config.LocaleManager;
import io.eliasnvx.gighub.bukkit.gui.GUIManager;
import io.eliasnvx.gighub.bukkit.listener.ContractListener;
import io.eliasnvx.gighub.bukkit.manager.EscrowManager;
import io.eliasnvx.gighub.bukkit.manager.PermissionManager;
import io.eliasnvx.gighub.core.database.DatabaseConfig;
import io.eliasnvx.gighub.core.database.DatabaseManager;
import io.eliasnvx.gighub.core.database.DatabaseType;
import io.eliasnvx.gighub.core.database.dao.ContractDAO;
import io.eliasnvx.gighub.core.database.dao.EscrowDAO;
import io.eliasnvx.gighub.core.database.dao.ReputationDAO;
import io.eliasnvx.gighub.core.database.dao.ReviewDAO;
import io.eliasnvx.gighub.core.manager.ContractManager;
import io.eliasnvx.gighub.core.manager.ReputationManager;
import io.eliasnvx.gighub.core.model.ContractType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

/**
 * Главный класс плагина GigHub для Bukkit/Spigot
 */
public class GigHubPlugin extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private LocaleManager localeManager;
    
    // DAOs
    private ContractDAO contractDAO;
    private ReputationDAO reputationDAO;
    private ReviewDAO reviewDAO;
    private EscrowDAO escrowDAO;
    
    // Managers
    private ContractManager contractManager;
    private ReputationManager reputationManager;
    private EscrowManager escrowManager;
    private PermissionManager permissionManager;
    private GUIManager guiManager;
    
    @Override
    public void onEnable() {
        // Цветной вывод в консоль
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";
        
        getLogger().info(GREEN + "╔═══════════════════════════════════╗" + RESET);
        getLogger().info(GREEN + "║       GigHub v" + getDescription().getVersion() + "           ║" + RESET);
        getLogger().info(GREEN + "║  Player-to-Player Marketplace     ║" + RESET);
        getLogger().info(GREEN + "╚═══════════════════════════════════╝" + RESET);
        
        // Создание конфигурации
        saveDefaultConfig();
        
        // Инициализация локализации
        localeManager = new LocaleManager(this);
        
        // Инициализация базы данных
        if (!initializeDatabase()) {
            getLogger().severe("Failed to initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Инициализация DAOs
        initializeDAOs();
        
        // Инициализация менеджеров
        initializeManagers();
        
        // Регистрация команд
        registerCommands();
        
        // Регистрация слушателей
        registerListeners();
        
        String BOLD = "\u001B[1m";
        getLogger().info(GREEN + BOLD + "✓ GigHub enabled successfully!" + RESET);
    }
    
    @Override
    public void onDisable() {
        // Очистка всех GUI
        if (guiManager != null) {
            // Закрываем все ContractCreationGUIs
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                guiManager.closeContractCreation(player);
            }
        }

        // Закрытие БД
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("GigHub disabled!");
    }

    /**
     * Инициализация базы данных
     */
    private boolean initializeDatabase() {
        try {
            String dbType = getConfig().getString("database.type", "SQLITE");
            DatabaseType type = DatabaseType.valueOf(dbType.toUpperCase());

            DatabaseConfig.DatabaseConfigBuilder configBuilder = DatabaseConfig.builder()
                .type(type)
                .poolSize(getConfig().getInt("database.pool.size", 10))
                .connectionTimeout(getConfig().getLong("database.pool.timeout", 30000))
                .maxLifetime(getConfig().getLong("database.pool.max-lifetime", 1800000));

            // Настройка в зависимости от типа БД
            switch (type) {
                case SQLITE:
                    File dataFolder = getDataFolder();
                    if (!dataFolder.exists()) {
                        dataFolder.mkdirs();
                    }
                    String sqliteFilename = getConfig().getString("database.sqlite-filename", "gighub.db");
                    File dbFile = new File(dataFolder, sqliteFilename);
                    configBuilder.file(dbFile.getAbsolutePath());
                    getLogger().info("Using SQLite database: " + dbFile.getAbsolutePath());
                    break;

                case MYSQL:
                case POSTGRESQL:
                    configBuilder
                        .host(getConfig().getString("database.host", "localhost"))
                        .port(getConfig().getInt("database.port", 3306))
                        .database(getConfig().getString("database.database", "gighub"))
                        .username(getConfig().getString("database.username", "root"))
                        .password(getConfig().getString("database.password", "password"));
                    break;
            }

            DatabaseConfig dbConfig = configBuilder.build();
            databaseManager = new DatabaseManager(getLogger(), dbConfig);
            databaseManager.initialize();

            return true;
        } catch (SQLException e) {
            getLogger().severe("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            getLogger().severe("Unexpected error during database initialization: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Инициализация DAOs
     */
    private void initializeDAOs() {
        getLogger().info("Initializing DAOs...");
        contractDAO = new ContractDAO(databaseManager);
        escrowDAO = new EscrowDAO(databaseManager, getLogger());
        reputationDAO = new ReputationDAO(databaseManager);
        reviewDAO = new ReviewDAO(databaseManager);
        getLogger().info("DAOs initialized!");
    }

    /**
     * Инициализация менеджеров
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");

        // Настройка локализации для ContractType
        BukkitLocalizationService localizationService = new BukkitLocalizationService(localeManager);
        ContractType.setLocalizationService(localizationService);

        // Bukkit-specific managers (сначала создаем EscrowManager)
        double commissionPercentage = getConfig().getDouble("economy.commission-percentage", 5.0);
        escrowManager = new EscrowManager(getLogger(), commissionPercentage, escrowDAO);

        // Core managers (передаем EscrowManager в ContractManager)
        contractManager = new ContractManager(getLogger(), contractDAO, reputationDAO, escrowManager);
        reputationManager = new ReputationManager(getLogger(), reputationDAO, reviewDAO);
        permissionManager = new PermissionManager(this, getLogger());
        guiManager = new GUIManager(this, contractManager, escrowManager);

        getLogger().info("Managers initialized!");
        getLogger().info("  - Economy: " + (escrowManager.isVaultEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  - Permissions: " + permissionManager.getPermissionPluginName());
    }

    /**
     * Регистрация команд
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        // Главная команда /gig
        GigCommand gigCommand = new GigCommand(this);
        getCommand("gig").setExecutor(gigCommand);
        getCommand("gig").setTabCompleter(gigCommand);

        // Админская команда /gigadmin
        GigAdminCommand gigAdminCommand = new GigAdminCommand(this);
        getCommand("gigadmin").setExecutor(gigAdminCommand);
        getCommand("gigadmin").setTabCompleter(gigAdminCommand);

        getLogger().info("Commands registered!");
    }

    /**
     * Регистрация слушателей
     */
    private void registerListeners() {
        getLogger().info("Registering listeners...");

        // GUI Manager
        getServer().getPluginManager().registerEvents(guiManager, this);

        // Contract Listener
        getServer().getPluginManager().registerEvents(new ContractListener(this), this);

        // Chat Input Listener (для ввода данных в чат при создании контракта)
        getServer().getPluginManager().registerEvents(new io.eliasnvx.gighub.bukkit.listener.ChatInputListener(this), this);

        getLogger().info("Listeners registered!");
    }

    // Getters

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ContractDAO getContractDAO() {
        return contractDAO;
    }

    public ReputationDAO getReputationDAO() {
        return reputationDAO;
    }

    public ReviewDAO getReviewDAO() {
        return reviewDAO;
    }

    public ContractManager getContractManager() {
        return contractManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    public EscrowManager getEscrowManager() {
        return escrowManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }
}
