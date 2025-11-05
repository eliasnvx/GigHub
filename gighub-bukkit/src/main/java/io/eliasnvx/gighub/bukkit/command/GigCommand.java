package io.eliasnvx.gighub.bukkit.command;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.bukkit.gui.ContractBoardGUI;
import io.eliasnvx.gighub.bukkit.manager.PermissionManager;
import io.eliasnvx.gighub.core.manager.ContractManager;
import io.eliasnvx.gighub.core.manager.ReputationManager;
import io.eliasnvx.gighub.core.model.Contract;
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
 * Главная команда /gig
 */
public class GigCommand implements CommandExecutor, TabCompleter {
    
    private final GigHubPlugin plugin;
    private final ContractManager contractManager;
    private final ReputationManager reputationManager;
    private final PermissionManager permissionManager;
    
    public GigCommand(GigHubPlugin plugin) {
        this.plugin = plugin;
        this.contractManager = plugin.getContractManager();
        this.reputationManager = plugin.getReputationManager();
        this.permissionManager = plugin.getPermissionManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Только для игроков
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Проверка базового права
        if (!permissionManager.hasPermission(player, "gighub.use")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        // Если нет аргументов - открываем GUI
        if (args.length == 0) {
            plugin.getGUIManager().openGUI(player, new ContractBoardGUI(plugin, plugin.getGUIManager()));
            return true;
        }
        
        // Обработка подкоманд
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            
            case "list":
                return handleList(player, args);
            
            case "my":
                return handleMy(player, args);
            
            case "accept":
                return handleAccept(player, args);
            
            case "complete":
                return handleComplete(player, args);
            
            case "confirm":
                return handleConfirm(player, args);
            
            case "cancel":
                return handleCancel(player, args);
            
            case "info":
                return handleInfo(player, args);
            
            case "reputation":
            case "rep":
                return handleReputation(player, args);
            
            case "help":
                return handleHelp(player);
            
            default:
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("messages.unknown-command")));
                return true;
        }
    }
    
    /**
     * /gig create - Создание контракта
     */
    private boolean handleCreate(Player player, String[] args) {
        if (!permissionManager.hasPermission(player, "gighub.contract.create")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        // TODO: Открыть GUI создания контракта
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.contract-creation-gui")));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-usage")
                    .replace("{usage}", "/gig create <type> <title> <reward> [deadline]")));
        });
        return true;
    }
    
    /**
     * /gig list - Список контрактов с пагинацией
     */
    private boolean handleList(Player player, String[] args) {
        // Парсим номер страницы (по умолчанию 1)
        // args[0] = "list", args[1] = page number
        int pageValue = 1;
        if (args.length > 1) {
            try {
                int parsedPage = Integer.parseInt(args[1]);
                if (parsedPage < 1) parsedPage = 1;
                pageValue = parsedPage;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("messages.invalid-usage")
                        .replace("{usage}", "/gig list [page]")));
                return true;
            }
        }
        final int page = pageValue;
        
        // Отправляем сообщение о загрузке
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getLocaleManager().getMessage("list.loading")));
        
        contractManager.getActiveContracts().thenAccept(contracts -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (contracts.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("list.no-contracts")));
                    return;
                }
                
                // Настройки пагинации
                final int CONTRACTS_PER_PAGE = 10;
                int totalPages = (int) Math.ceil((double) contracts.size() / CONTRACTS_PER_PAGE);
                int currentPage = page;
                if (currentPage > totalPages) currentPage = totalPages;
                
                int startIndex = (currentPage - 1) * CONTRACTS_PER_PAGE;
                int endIndex = Math.min(startIndex + CONTRACTS_PER_PAGE, contracts.size());
                
                // Заголовок
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("list.header")));
                
                // Информация о странице
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("list.page-info")
                        .replace("{current}", String.valueOf(currentPage))
                        .replace("{total}", String.valueOf(totalPages))
                        .replace("{count}", String.valueOf(endIndex - startIndex))));
                
                // Список контрактов
                for (int i = startIndex; i < endIndex; i++) {
                    Contract contract = contracts.get(i);
                    String status = getStatusColor(contract.getStatus());
                    
                    String contractMessage = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("list.contract-entry")
                            .replace("{id}", contract.getId().toString().substring(0, 8))
                            .replace("{title}", contract.getTitle())
                            .replace("{reward:.2f}", String.format("%.2f", contract.getReward()))
                            .replace("{status}", status));
                    
                    player.sendMessage(contractMessage);
                }
                
                // Нижняя граница
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("list.footer")));
                
                // Навигация
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("list.use-info")));
                
                // Кнопки навигации
                if (totalPages > 1) {
                    StringBuilder nav = new StringBuilder("§7");
                    if (currentPage > 1) {
                        nav.append("§e/gig list ").append(currentPage - 1).append(" §7◀ ");
                    } else {
                        nav.append("§7◀ ");
                    }
                    nav.append("§fСтраница ").append(currentPage).append("§7/");
                    nav.append(totalPages);
                    if (currentPage < totalPages) {
                        nav.append(" §7▶ §e/gig list ").append(currentPage + 1);
                    } else {
                        nav.append(" §7▶");
                    }
                    player.sendMessage(nav.toString());
                }
            });
        }).exceptionally(e -> {
            plugin.getLogger().severe("Error fetching contracts: " + e.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("messages.error")));
            });
            return null;
        });
        return true;
    }
    
    /**
     * /gig my - Мои контракты
     */
    private boolean handleMy(Player player, String[] args) {
        UUID playerUuid = player.getUniqueId();
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getLocaleManager().getMessage("notifications.loading-contracts")));
        
        // Получаем контракты как заказчик
        contractManager.getPlayerContracts(playerUuid, true).thenAccept(ownedContracts -> {
            // Получаем контракты как исполнитель
            contractManager.getPlayerContracts(playerUuid, false).thenAccept(contractedContracts -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.my-contracts-header")));
                
                if (!ownedContracts.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.as-owner")));
                    for (Contract contract : ownedContracts) {
                        String status = getStatusColor(contract.getStatus());
                        player.sendMessage(String.format(
                            "  §7#%s §f%s §7- %s",
                            contract.getId().toString().substring(0, 8),
                            contract.getTitle(),
                            status
                        ));
                    }
                }
                
                if (!contractedContracts.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.as-contractor")));
                    for (Contract contract : contractedContracts) {
                        String status = getStatusColor(contract.getStatus());
                        player.sendMessage(String.format(
                            "  §7#%s §f%s §7- %s",
                            contract.getId().toString().substring(0, 8),
                            contract.getTitle(),
                            status
                        ));
                    }
                }
                
                if (ownedContracts.isEmpty() && contractedContracts.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.no-active-contracts")));
                }
                
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.footer")));
            });
        });
        
        return true;
    }
    
    /**
     * /gig accept <id> - Принять контракт
     */
    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getLocaleManager().getMessage("usage.accept")));
            return true;
        }
        
        if (!permissionManager.hasPermission(player, "gighub.contract.accept")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("operation.accepting")));
            
            contractManager.acceptContract(contractId, player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.contract-accepted-success")));
                    
                    // Уведомляем создателя контракта
                    contractManager.getContract(contractId).thenAccept(contractOpt -> {
                        if (contractOpt != null && contractOpt.isPresent()) {
                            Contract contract = contractOpt.get();
                            Player client = Bukkit.getPlayer(contract.getOwnerUuid());
                            if (client != null && client.isOnline()) {
                                client.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-accepted-owner")));
                                client.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contractor-info")
                                        .replace("{player}", player.getName())));
                                client.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-title-info")
                                        .replace("{title}", contract.getTitle())));
                            }
                        }
                    });
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("operation-failed.accept")));
                }
            });
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gig complete <id> - Завершить контракт
     */
    private boolean handleComplete(Player player, String[] args) {
        if (args.length < 2) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getLocaleManager().getMessage("usage.complete")));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("operation.completing")));
            
            contractManager.completeContract(contractId).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-completed-success")));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.waiting-verification")));
                    
                    // Уведомляем заказчика что исполнитель завершил работу
                    contractManager.getContract(contractId).thenAccept(contractOpt -> {
                        if (contractOpt != null && contractOpt.isPresent()) {
                            Contract contract = contractOpt.get();
                            Player owner = Bukkit.getPlayer(contract.getOwnerUuid());
                            if (owner != null && owner.isOnline()) {
                                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-completed-owner")));
                                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contractor-info")
                                        .replace("{player}", player.getName())));
                                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-title-info")
                                        .replace("{title}", contract.getTitle())));
                                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.confirm-instruction")
                                        .replace("{id}", contractId.toString().substring(0, 8))));
                            }
                        }
                    });
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("operation-failed.complete")));
                }
            });
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gig confirm <id> - Подтвердить выполнение контракта и выплатить деньги
     */
    private boolean handleConfirm(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("usage.confirm")));
            return true;
        }
        
        if (!permissionManager.hasPermission(player, "gighub.contract.confirm")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("operation.confirming")));
            
            contractManager.verifyContract(contractId, 0).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.contract-confirmed-success")));
                    
                    // Уведомляем исполнителя что заказчик подтвердил и деньги переведены
                    contractManager.getContract(contractId).thenAccept(contractOpt -> {
                        if (contractOpt != null && contractOpt.isPresent()) {
                            Contract contract = contractOpt.get();
                            Player contractor = Bukkit.getPlayer(contract.getContractorUuid());
                            if (contractor != null && contractor.isOnline()) {
                                contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-confirmed-contractor")));
                                contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.client-info")
                                        .replace("{player}", player.getName())));
                                contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.contract-title-info")
                                        .replace("{title}", contract.getTitle())));
                                contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                    plugin.getLocaleManager().getMessage("notifications.payment-sent")));
                            }
                        }
                    });
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("operation-failed.confirm")));
                }
            });
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gig cancel <id> - Отменить контракт
     */
    private boolean handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("usage.cancel")));
            return true;
        }
        
        if (!permissionManager.hasPermission(player, "gighub.contract.cancel")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        try {
            UUID contractId = UUID.fromString(args[1]);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("operation.cancelling")));
            
            contractManager.cancelContract(contractId).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.contract-cancelled-success")));
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("operation-failed.cancel")));
                }
            });
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.invalid-contract-id")));
        }
        
        return true;
    }
    
    /**
     * /gig info <id> - Информация о контракте
     */
    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("usage.info")));
            return true;
        }
        
        // Поддержка короткого ID (первые 8 символов)
        String idArg = args[1].replace("#", "");
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getLocaleManager().getMessage("operation.loading-info")));
        
        contractManager.getActiveContracts().thenAccept(contracts -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Contract contract = null;
                
                // Поиск по полному или короткому ID
                for (Contract c : contracts) {
                    String fullId = c.getId().toString();
                    if (fullId.equals(idArg) || fullId.startsWith(idArg)) {
                        contract = c;
                        break;
                    }
                }
                
                if (contract == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("messages.contract-not-found")));
                    return;
                }
                
                // Детальная информация
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.contract-details-header")));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-id")
                            .replace("{id}", contract.getId().toString().substring(0, 8))));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-title")
                            .replace("{title}", contract.getTitle())));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-description")
                            .replace("{description}", contract.getDescription() != null ? contract.getDescription() : "No description")));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-type")
                            .replace("{type}", contract.getType().name())));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-reward")
                            .replace("{reward}", String.format("%.2f", contract.getReward()))));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-status")
                            .replace("{status}", getStatusColor(contract.getStatus()))));
                
                // Владелец
                OfflinePlayer owner = plugin.getServer().getOfflinePlayer(contract.getOwnerUuid());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-owner")
                            .replace("{owner}", owner.getName() != null ? owner.getName() : "Unknown")));
                
                // Исполнитель (если есть)
                if (contract.getContractorUuid() != null) {
                    OfflinePlayer contractor = plugin.getServer().getOfflinePlayer(contract.getContractorUuid());
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-contractor")
                            .replace("{contractor}", contractor.getName() != null ? contractor.getName() : "Unknown")));
                }
                
                // Дедлайн (если есть)
                if (contract.getDeadline() != null) {
                    long hoursLeft = (contract.getDeadline() - System.currentTimeMillis()) / (1000 * 60 * 60);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getLocaleManager().getMessage("notifications.contract-deadline")
                            .replace("{deadline}", String.valueOf(hoursLeft))));
                }
                
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.footer")));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("notifications.contract-use-accept")
                        .replace("{id}", contract.getId().toString().substring(0, 8))));
            });
        }).exceptionally(e -> {
            plugin.getLogger().severe("Error fetching contract info: " + e.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.failed-to-load-contract")));
            });
            return null;
        });
        
        return true;
    }
    
    /**
     * /gig reputation [player] - Репутация игрока
     */
    private boolean handleReputation(Player player, String[] args) {
        String targetName = args.length > 1 ? args[1] : player.getName();
        Player target = plugin.getServer().getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("messages.player-not-found")));
            return true;
        }
        
        reputationManager.getReputationStats(target.getUniqueId()).thenAccept(stats -> {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-header")));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-player")
                    .replace("{player}", target.getName())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-rating")
                    .replace("{rating}", String.format("%.1f", stats.getRating()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-completed")
                    .replace("{completed}", String.valueOf(stats.getTotalCompleted()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-failed")
                    .replace("{failed}", String.valueOf(stats.getTotalFailed()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-success-rate")
                    .replace("{successRate}", String.format("%.1f%%", stats.getSuccessRate()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-points")
                    .replace("{points}", String.valueOf(stats.getPoints()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.reputation-reviews")
                    .replace("{reviews}", String.valueOf(stats.getReviewCount()))));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("notifications.footer")));
        });
        
        return true;
    }
    
    /**
     * /gig help - Помощь
     */
    private boolean handleHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.header")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.help")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.create")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.list")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.my")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.accept")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.complete")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.confirm")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.cancel")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.info")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("help.reputation")));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "list", "my", "accept", "complete", "cancel", "info", "reputation", "help")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("reputation")) {
            return null; // Bukkit автоматически предложит имена игроков
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Получение цвета статуса
     */
    private String getStatusColor(io.eliasnvx.gighub.core.model.ContractStatus status) {
        return switch (status) {
            case OPEN -> "§aOpen";
            case ACCEPTED -> "§eAccepted";
            case IN_PROGRESS -> "§6In Progress";
            case COMPLETED -> "§bCompleted";
            case VERIFIED -> "§2Verified";
            case CANCELLED -> "§cCancelled";
            case EXPIRED -> "§4Expired";
            default -> "§7" + status.name();
        };
    }
    
    /**
     * Получение сообщения из конфига
     */
    private String getMessage(String key) {
        // TODO: Загрузка из lang файлов
        return switch (key) {
            case "player-only" -> "§c[GigHub] This command can only be used by players!";
            case "no-permission" -> "§c[GigHub] You don't have permission to do that!";
            default -> "§c[GigHub] Unknown message: " + key;
        };
    }
}
