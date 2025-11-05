package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI деталей контракта
 */
public class ContractDetailsGUI extends BaseGUI {
    
    private final GigHubPlugin plugin;
    private final GUIManager guiManager;
    private final Contract contract;
    
    public ContractDetailsGUI(GigHubPlugin plugin, GUIManager guiManager, Contract contract) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.contract = contract;
    }
    
    @Override
    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§6§lContract Details");
        
        fillInventory(inv, player);
        
        return inv;
    }
    
    private void fillInventory(Inventory inv, Player player) {
        // Информация о контракте (центр)
        inv.setItem(13, createContractInfoItem());
        
        // Владелец
        inv.setItem(11, createItem(Material.PLAYER_HEAD, "§eOwner", 
            "§7UUID: §f" + contract.getOwnerUuid().toString().substring(0, 8) + "...",
            "",
            "§7Click to view profile"));
        
        // Исполнитель
        if (contract.getContractorUuid() != null) {
            inv.setItem(15, createItem(Material.PLAYER_HEAD, "§bContractor", 
                "§7UUID: §f" + contract.getContractorUuid().toString().substring(0, 8) + "...",
                "",
                "§7Click to view profile"));
        } else {
            inv.setItem(15, createItem(Material.BARRIER, "§7No Contractor", 
                "§7This contract hasn't been accepted yet"));
        }
        
        // Кнопки действий
        addActionButtons(inv, player);
        
        // Навигация
        inv.setItem(40, createItem(Material.ARROW, "§cBack", "§7Return to previous menu"));
    }
    
    private void addActionButtons(Inventory inv, Player player) {
        UUID playerUuid = player.getUniqueId();
        boolean isOwner = contract.getOwnerUuid().equals(playerUuid);
        boolean isContractor = contract.getContractorUuid() != null && 
                               contract.getContractorUuid().equals(playerUuid);
        
        // Принять контракт
        if (contract.canBeAccepted() && !isOwner && 
            plugin.getPermissionManager().hasPermission(player, "gighub.contract.accept")) {
            inv.setItem(29, createItem(Material.EMERALD, "§aAccept Contract", 
                "§7Click to accept this contract",
                "",
                "§7You will start working on it"));
        }
        
        // Завершить контракт
        if (contract.canBeCompleted() && isContractor) {
            inv.setItem(31, createItem(Material.DIAMOND, "§bComplete Contract", 
                "§7Click to mark as completed",
                "",
                "§7Waiting for owner verification"));
        }
        
        // Верифицировать контракт
        if (contract.getStatus() == ContractStatus.COMPLETED && isOwner) {
            inv.setItem(31, createItem(Material.EMERALD_BLOCK, "§2Verify Contract", 
                "§7Click to verify completion",
                "",
                "§7Payment will be sent to contractor"));
        }
        
        // Отменить контракт
        if (contract.canBeCancelled() && (isOwner || isContractor) &&
            plugin.getPermissionManager().hasPermission(player, "gighub.contract.cancel")) {
            inv.setItem(33, createItem(Material.REDSTONE_BLOCK, "§cCancel Contract", 
                "§7Click to cancel this contract",
                "",
                "§cThis action cannot be undone!"));
        }
    }
    
    private ItemStack createContractInfoItem() {
        Material material = getContractMaterial();
        
        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + contract.getId().toString().substring(0, 8) + "...");
        lore.add("§7Type: §f" + contract.getType().name());
        lore.add("§7Status: " + getStatusColor(contract.getStatus()));
        lore.add("");
        lore.add("§7Reward: §a$" + String.format("%.2f", contract.getReward()));
        
        if (contract.getDeadline() != null) {
            long timeLeft = contract.getTimeRemaining();
            if (timeLeft > 0) {
                lore.add("§7Time Left: §f" + formatTime(timeLeft));
            } else {
                lore.add("§7Time Left: §cExpired");
            }
        } else {
            lore.add("§7Deadline: §7No deadline");
        }
        
        lore.add("");
        lore.add("§7Description:");
        
        // Разбиваем описание на строки
        String desc = contract.getDescription();
        int maxLength = 40;
        for (int i = 0; i < desc.length(); i += maxLength) {
            int end = Math.min(i + maxLength, desc.length());
            lore.add("§7" + desc.substring(i, end));
        }
        
        return createItem(material, "§6" + contract.getTitle(), lore.toArray(new String[0]));
    }
    
    @Override
    public void handleClick(Player player, int slot, ClickType clickType) {
        UUID playerUuid = player.getUniqueId();
        
        switch (slot) {
            case 29: // Accept
                if (contract.canBeAccepted()) {
                    player.closeInventory();
                    plugin.getContractManager().acceptContract(contract.getId(), playerUuid)
                        .thenAccept(success -> {
                            if (success) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-contract-accepted")));
                                
                                // Уведомляем создателя контракта
                                plugin.getContractManager().getContract(contract.getId()).thenAccept(contractOpt -> {
                                    if (contractOpt != null && contractOpt.isPresent()) {
                                        Contract acceptedContract = contractOpt.get();
                                        Player owner = Bukkit.getPlayer(acceptedContract.getOwnerUuid());
                                        if (owner != null && owner.isOnline()) {
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-accepted-owner")));
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-accepted-owner-contractor")
                                                .replace("{contractor}", player.getName())));
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-accepted-owner-title")
                                                .replace("{title}", acceptedContract.getTitle())));
                                        }
                                    }
                                });
                            } else {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-accept-failed")));
                            }
                        });
                }
                break;
            
            case 31: // Complete or Verify
                if (contract.canBeCompleted() && contract.getContractorUuid().equals(playerUuid)) {
                    player.closeInventory();
                    plugin.getContractManager().completeContract(contract.getId())
                        .thenAccept(success -> {
                            if (success) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-contract-completed")));
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-waiting-verification")));
                                
                                // Уведомляем заказчика что исполнитель завершил работу
                                plugin.getContractManager().getContract(contract.getId()).thenAccept(contractOpt -> {
                                    if (contractOpt != null && contractOpt.isPresent()) {
                                        Contract completedContract = contractOpt.get();
                                        Player owner = Bukkit.getPlayer(completedContract.getOwnerUuid());
                                        if (owner != null && owner.isOnline()) {
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-completed-owner")));
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-completed-owner-contractor")
                                                .replace("{contractor}", player.getName())));
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-completed-owner-title")
                                                .replace("{title}", completedContract.getTitle())));
                                            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-completed-owner-confirm")
                                                .replace("{id}", completedContract.getId().toString().substring(0, 8))));
                                        }
                                    }
                                });
                            } else {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-complete-failed")));
                            }
                        });
                } else if (contract.getStatus() == ContractStatus.COMPLETED && 
                           contract.getOwnerUuid().equals(playerUuid)) {
                    player.closeInventory();
                    int bonus = plugin.getConfig().getInt("reputation.success-bonus", 10);
                    plugin.getContractManager().verifyContract(contract.getId(), bonus)
                        .thenAccept(success -> {
                            if (success) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-contract-verified")));
                                
                                // Уведомляем исполнителя что заказчик подтвердил и деньги переведены
                                plugin.getContractManager().getContract(contract.getId()).thenAccept(contractOpt -> {
                                    if (contractOpt != null && contractOpt.isPresent()) {
                                        Contract verifiedContract = contractOpt.get();
                                        Player contractor = Bukkit.getPlayer(verifiedContract.getContractorUuid());
                                        if (contractor != null && contractor.isOnline()) {
                                            contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-verified-contractor")));
                                            contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-verified-contractor-owner")
                                                .replace("{owner}", player.getName())));
                                            contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-verified-contractor-title")
                                                .replace("{title}", verifiedContract.getTitle())));
                                            contractor.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                            plugin.getLocaleManager().getMessage("gui-contract-verified-payment")));
                                        }
                                    }
                                });
                            } else {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-verify-failed")));
                            }
                        });
                }
                break;
            
            case 33: // Cancel
                if (contract.canBeCancelled()) {
                    player.closeInventory();
                    plugin.getContractManager().cancelContract(contract.getId())
                        .thenAccept(cancelSuccess -> {
                            if (cancelSuccess) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-contract-cancelled")));
                            } else {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                        plugin.getLocaleManager().getMessage("gui-cancel-failed")));
                            }
                        });
                }
                break;
            
            case 40: // Back
                guiManager.openGUI(player, new ContractBoardGUI(plugin, guiManager));
                break;
        }
    }
    
    private Material getContractMaterial() {
        return switch (contract.getType()) {
            case ITEM_REQUEST -> Material.DIAMOND;
            case BUILDING -> Material.BRICKS;
            case COMBAT -> Material.DIAMOND_SWORD;
            case MINING -> Material.IRON_PICKAXE;
            case FARMING -> Material.WHEAT;
            default -> Material.PAPER;
        };
    }
    
    private String getStatusColor(ContractStatus status) {
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
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
