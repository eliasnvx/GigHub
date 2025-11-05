package io.eliasnvx.gighub.bukkit.command;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.manager.ContractManager;
import io.eliasnvx.gighub.core.manager.ReputationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Админская команда /gigadmin
 */
public class GigAdminCommand implements CommandExecutor, TabCompleter {
    
    private final GigHubPlugin plugin;
    private final ContractManager contractManager;
    private final ReputationManager reputationManager;
    
    /**
     * Безопасно получает OfflinePlayer по имени
     * Для админских команд необходимо получать игрока по имени,
     * поэтому используем deprecated метод как лучший доступный вариант
     */
    @SuppressWarnings("deprecation")
    private OfflinePlayer getOfflinePlayerSafely(String playerName) {
        // Сначала пробуем найти онлайн игрока
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }
        
        // Если онлайн нет, используем устаревший метод (необходимо для админских команд)
        return Bukkit.getOfflinePlayer(playerName);
    }
    
    public GigAdminCommand(GigHubPlugin plugin) {
        this.plugin = plugin;
        this.contractManager = plugin.getContractManager();
        this.reputationManager = plugin.getReputationManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("gighub.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-no-permission")));
            return true;
        }
        
        if (args.length == 0) {
            return handleHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            
            case "stats":
                return handleStats(sender);
            
            case "cancel":
                return handleCancel(sender, args);
            
            case "verify":
                return handleVerify(sender, args);
            
            case "setreputation":
            case "setrep":
                return handleSetReputation(sender, args);
            
            case "resetreputation":
            case "resetrep":
                return handleResetReputation(sender, args);
            
            case "cleanup":
                return handleCleanup(sender);
            
            case "testdata":
                return handleTestData(sender, args);
            
            case "cleardata":
                return handleClearData(sender);
            
            case "help":
                return handleHelp(sender);
            
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-unknown-command")));
                return true;
        }
    }
    
    /**
     * /gigadmin reload - Перезагрузка конфига
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("gighub.admin.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-reloading")));
        
        try {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-reload-success")));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-reload-failed")
                    .replace("{error}", e.getMessage())));
            plugin.getLogger().severe("Config reload failed: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * /gigadmin stats - Статистика плагина
     */
    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("gighub.admin.stats")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-loading-stats")));
        
        contractManager.getActiveContracts().thenAccept(activeContracts -> {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-stats-header")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-active-contracts")
                    .replace("{count}", String.valueOf(activeContracts.size()))));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-database-status")
                    .replace("{status}", plugin.getDatabaseManager().isConnected() ? "§aConnected" : "§cDisconnected")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-economy-status")
                    .replace("{status}", plugin.getEscrowManager().isVaultEnabled() ? "§aEnabled" : "§cDisabled")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-permissions-status")
                    .replace("{plugin}", plugin.getPermissionManager().getPermissionPluginName())));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("footer")));
        });
        
        return true;
    }
    
    /**
     * /gigadmin cancel <id> - Отмена контракта
     */
    private boolean handleCancel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gighub.admin.contract.delete")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-usage-cancel")));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-cancelling")));
            
            contractManager.cancelContract(contractId).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-cancel-success")));
                    plugin.getLogger().info(sender.getName() + " cancelled contract " + contractId);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-cancel-failed")));
                }
            });
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gigadmin verify <id> - Принудительная верификация
     */
    private boolean handleVerify(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gighub.admin.contract.force")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-usage-verify")));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            int successBonus = plugin.getConfig().getInt("reputation.success-bonus", 10);
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-verifying")));
            
            contractManager.verifyContract(contractId, successBonus).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-verify-success")));
                    plugin.getLogger().info(sender.getName() + " force-verified contract " + contractId);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-verify-failed")));
                }
            });
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gigadmin setreputation <player> <rating> - Установка рейтинга
     */
    private boolean handleSetReputation(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gighub.admin.reputation.set")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-usage-setreputation")));
            return true;
        }
        
        OfflinePlayer target = getOfflinePlayerSafely(args[1]);
        
        try {
            double rating = Double.parseDouble(args[2]);
            
            if (rating < 1.0 || rating > 5.0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-rating-range")));
                return true;
            }
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-setting-reputation")));
            
            reputationManager.setRating(target.getUniqueId(), rating).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-setreputation-success")
                    .replace("{rating}", String.valueOf(rating))
                    .replace("{player}", target.getName())));
                    plugin.getLogger().info(sender.getName() + " set reputation for " + target.getName() + " to " + rating);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-setreputation-failed")));
                }
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-invalid-rating")));
        }
        
        return true;
    }
    
    /**
     * /gigadmin resetreputation <player> - Сброс репутации
     */
    private boolean handleResetReputation(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gighub.admin.reputation.reset")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-usage-resetreputation")));
            return true;
        }
        
        OfflinePlayer target = getOfflinePlayerSafely(args[1]);
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-resetting-reputation")));
        
        reputationManager.resetReputation(target.getUniqueId()).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-resetreputation-success")
                    .replace("{player}", target.getName())));
                plugin.getLogger().info(sender.getName() + " reset reputation for " + target.getName());
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-resetreputation-failed")));
            }
        });
        
        return true;
    }
    
    /**
     * /gigadmin cleanup - Очистка истекших контрактов
     */
    private boolean handleCleanup(CommandSender sender) {
        if (!sender.hasPermission("gighub.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-starting-cleanup")));
        
        int failurePenalty = plugin.getConfig().getInt("reputation.failure-penalty", 5);
        
        contractManager.cleanupExpiredContracts(failurePenalty).thenAccept(count -> {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-cleanup-success")
                    .replace("{count}", String.valueOf(count))));
            plugin.getLogger().info(sender.getName() + " ran cleanup, removed " + count + " contracts");
        });
        
        return true;
    }
    
    /**
     * /gigadmin testdata - Создание тестовых данных
     */
    private boolean handleTestData(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gighub.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        int count = 5; // По умолчанию 5 контрактов
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
                if (count < 1 || count > 20) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-count-range")));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-invalid-number")));
                return true;
            }
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-creating-testdata")
                    .replace("{count}", String.valueOf(count))));
        
        final int finalCount = count;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            createTestContracts(sender, finalCount);
        });
        
        return true;
    }
    
    /**
     * Создание тестовых контрактов
     */
    private void createTestContracts(CommandSender sender, int count) {
        // Используем фиксированный UUID для владельца, чтобы все контракты были от одного "игрока"
        java.util.UUID testOwner = java.util.UUID.nameUUIDFromBytes("TestOwner".getBytes());
        
        String[] titles = {
            "Need 64 Diamonds", "Build a Castle", "Kill 100 Zombies",
            "Farm 1000 Wheat", "Mine 500 Stone", "Deliver Enchanted Gear",
            "Collect Rare Items", "Escort Mission", "Gather Resources",
            "Complete Dungeon", "Trade Goods", "Craft Equipment"
        };
        
        String[] descriptions = {
            "I need diamonds for my project. Will pay well!",
            "Looking for a skilled builder to construct a medieval castle.",
            "Help me clear the area of zombies. Combat experience required.",
            "Need wheat for my farm. Bulk order available.",
            "Mining job available. Bring your own pickaxe.",
            "Looking for enchanted diamond gear. Must be high quality."
        };
        
        io.eliasnvx.gighub.core.model.ContractType[] types = io.eliasnvx.gighub.core.model.ContractType.values();
        
        // Создаем все контракты асинхронно
        java.util.List<java.util.concurrent.CompletableFuture<io.eliasnvx.gighub.core.model.Contract>> futures = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String title = titles[i % titles.length] + " #" + (i + 1);
            String description = descriptions[i % descriptions.length];
            io.eliasnvx.gighub.core.model.ContractType type = types[i % types.length];
            double reward = 100.0 + (i * 50.0);
            
            java.util.UUID contractId = java.util.UUID.randomUUID();
            io.eliasnvx.gighub.core.model.Contract contract = io.eliasnvx.gighub.core.model.Contract.builder()
                .id(contractId)
                .ownerUuid(testOwner)
                .title(title)
                .description(description)
                .type(type)
                .reward(reward)
                .status(io.eliasnvx.gighub.core.model.ContractStatus.OPEN)
                .createdAt(System.currentTimeMillis())
                .build();
            
            // Используем DAO напрямую, чтобы избежать проблем с репутацией
            futures.add(plugin.getContractDAO().create(contract));
        }
        
        // Ждем завершения всех операций
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .thenApply(v -> {
                // Подсчитываем успешные
                return (int) futures.stream()
                    .filter(f -> !f.isCompletedExceptionally())
                    .count();
            })
            .thenAccept(created -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("admin-testdata-success")
                        .replace("{created}", String.valueOf(created))));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("admin-testdata-hint")));
                });
            })
            .exceptionally(e -> {
                plugin.getLogger().severe("Failed to create test contracts: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("admin-testdata-failed")));
                });
                return null;
            });
    }
    
    /**
     * /gigadmin cleardata - Очистка всех данных
     */
    private boolean handleClearData(CommandSender sender) {
        if (!sender.hasPermission("gighub.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("no-permission")));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin-recreating-schema")));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabaseManager().recreateSchema();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("admin-schema-success")));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to recreate schema: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("admin-schema-failed")));
                });
            }
        });
        
        return true;
    }
    
    /**
     * /gigadmin help - Помощь
     */
    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-header")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-reload")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-stats")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-cancel")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-verify")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-setreputation")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-resetreputation")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-cleanup")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-testdata")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("admin.help-cleardata")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("footer")));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("gighub.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("reload", "stats", "cancel", "verify", "setreputation", "resetreputation", "cleanup", "testdata", "cleardata", "help")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("setreputation") || args[0].equalsIgnoreCase("resetreputation"))) {
            return null; // Bukkit автоматически предложит имена игроков
        }
        
        return new ArrayList<>();
    }
}
