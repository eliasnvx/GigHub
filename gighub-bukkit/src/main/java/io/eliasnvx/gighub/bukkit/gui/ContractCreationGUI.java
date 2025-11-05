package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.bukkit.listener.ChatInputListener;
import io.eliasnvx.gighub.core.manager.ContractManager;
import io.eliasnvx.gighub.core.model.ContractType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

/**
 * GUI для создания контрактов
 */
public class ContractCreationGUI implements Listener, InventoryHolder {
    
    private final GigHubPlugin plugin;
    private final ContractManager contractManager;
    private final Economy economy;
    private final Player player;
    private final Inventory inventory;
    
    private final Map<UUID, ContractBuilder> contractBuilders = new HashMap<>();
    
    public ContractCreationGUI(GigHubPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.contractManager = plugin.getContractManager();
        this.economy = plugin.getEscrowManager().getEconomy();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.title")));
        
        // Store builder for player
        contractBuilders.put(player.getUniqueId(), new ContractBuilder());
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Opens the GUI
     */
    public void open() {
        // Unregister old events to prevent duplicates
        HandlerList.unregisterAll(this);
        
        fillInventory(new ContractBuilder());
        player.openInventory(inventory);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Fills inventory with items
     */
    public void fillInventory(ContractBuilder builder) {
        plugin.getLogger().info("[ContractCreationGUI] fillInventory called with builder:");
        plugin.getLogger().info("[ContractCreationGUI] - Title: " + (builder.getTitle() != null ? "'" + builder.getTitle() + "'" : "null"));
        plugin.getLogger().info("[ContractCreationGUI] - Description: " + (builder.getDescription() != null ? "'" + builder.getDescription() + "'" : "null"));
        plugin.getLogger().info("[ContractCreationGUI] - Reward: " + (builder.getReward() > 0 ? builder.getReward() : "0"));
        plugin.getLogger().info("[ContractCreationGUI] - Deadline: " + (builder.getDeadline() != null && builder.getDeadline() > 0 ? builder.getDeadline() + "h" : "null"));
        plugin.getLogger().info("[ContractCreationGUI] - Type: " + (builder.getType() != null ? builder.getType().name() : "null"));
        
        inventory.clear();
        
        // Header
        ItemStack header = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.title")));
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, header);
        }
        
        // Contract type
        ItemStack typeItem = createGuiItem(
            Material.PAPER, 
            ChatColor.translateAlternateColorCodes('&', 
                builder.getType() != null ? 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.type")
                        .replace("{type}", builder.getType().getDisplayName()) :
                    plugin.getLocaleManager().getMessage("gui.contract-creation.type-not-selected")),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.click-title"))
        );
        inventory.setItem(10, typeItem);
        
        // Title
        String titleDisplay = builder.getTitle() != null && !builder.getTitle().isEmpty() ? 
            plugin.getLocaleManager().getMessage("gui.contract-creation.title")
                .replace("{title}", builder.getTitle()) :
            plugin.getLocaleManager().getMessage("gui.contract-creation.title-not-set");
        plugin.getLogger().info("[ContractCreationGUI] Creating title item with display: '" + titleDisplay + "'");
        ItemStack titleItem = createGuiItem(
            Material.NAME_TAG,
            ChatColor.translateAlternateColorCodes('&', titleDisplay),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.click-title"))
        );
        inventory.setItem(12, titleItem);
        
        // Description
        String descDisplay = builder.getDescription() != null && !builder.getDescription().isEmpty() ? 
            plugin.getLocaleManager().getMessage("gui.contract-creation.description")
                .replace("{description}", builder.getDescription().substring(0, Math.min(20, builder.getDescription().length())) + "...") :
            plugin.getLocaleManager().getMessage("gui.contract-creation.description-not-set");
        plugin.getLogger().info("[ContractCreationGUI] Creating description item with display: '" + descDisplay + "'");
        ItemStack descItem = createGuiItem(
            Material.WRITABLE_BOOK,
            ChatColor.translateAlternateColorCodes('&', descDisplay),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.click-description"))
        );
        inventory.setItem(14, descItem);
        
        // Reward
        ItemStack rewardItem = createGuiItem(
            Material.GOLD_INGOT,
            ChatColor.translateAlternateColorCodes('&', 
                builder.getReward() > 0 ? 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.reward")
                        .replace("{reward}", "&6$" + builder.getReward()) :
                    plugin.getLocaleManager().getMessage("gui.contract-creation.reward-not-set")),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.click-reward"))
        );
        inventory.setItem(16, rewardItem);
        
        // Deadline
        ItemStack deadlineItem = createGuiItem(
            Material.CLOCK,
            ChatColor.translateAlternateColorCodes('&', 
                builder.getDeadline() != null ? 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.deadline")
                        .replace("{deadline}", builder.getDeadline() + " hours") :
                    plugin.getLocaleManager().getMessage("gui.contract-creation.deadline-not-set")),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.click-deadline"))
        );
        inventory.setItem(19, deadlineItem);
        
        // Location (for BUILDING contracts)
        if (builder.getType() == ContractType.BUILDING) {
            ItemStack locationItem = createGuiItem(
                Material.COMPASS,
                ChatColor.translateAlternateColorCodes('&', 
                    builder.getLocation() != null ? 
                        plugin.getLocaleManager().getMessage("gui.contract-creation.location")
                            .replace("{location}", "&aSet") :
                        plugin.getLocaleManager().getMessage("gui.contract-creation.location-not-set")),
                ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.click-location"))
            );
            inventory.setItem(21, locationItem);
        }
        
        // Submit button
        if (builder.canSubmit()) {
            ItemStack submitItem = createGuiItem(
                Material.GREEN_CONCRETE,
                ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.submit")),
                ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.submit-ready"))
            );
            inventory.setItem(40, submitItem);
        } else {
            ItemStack submitItem = createGuiItem(
                Material.RED_CONCRETE,
                ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.submit-disabled")),
                ChatColor.translateAlternateColorCodes('&', 
                    plugin.getLocaleManager().getMessage("gui.contract-creation.submit-not-ready"))
            );
            inventory.setItem(40, submitItem);
        }
        
        // Cancel button
        ItemStack cancelItem = createGuiItem(
            Material.BARRIER,
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.cancel")),
            ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.cancel-lore"))
        );
        inventory.setItem(44, cancelItem);
        
        // Dividers
        ItemStack divider = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 41, 42, 43, 45, 46, 47, 48, 49, 50, 51, 52, 53}) {
            inventory.setItem(i, divider);
        }
    }
    
    /**
     * Creates GUI item
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;
        
        // Check if this is type selection inventory
        String title = event.getView().getTitle();
        String typeSelectionTitle = ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.select-type"));
        
        if (title.equals(typeSelectionTitle)) {
            event.setCancelled(true);
            handleTypeSelection(clicker, event.getSlot());
            return;
        }
        
        // Check if this is main creation inventory
        if (event.getInventory() != inventory) return;
        if (event.getClickedInventory() != inventory) return;
        
        event.setCancelled(true);
        
        ContractBuilder builder = contractBuilders.get(player.getUniqueId());
        if (builder == null) {
            builder = new ContractBuilder();
            contractBuilders.put(player.getUniqueId(), builder);
        }
        
        int slot = event.getSlot();
        
        // Handle clicks
        switch (slot) {
            case 10: // Contract type
                openTypeSelection();
                break;
            case 12: // Title
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.enter-title")));
                builder.setInputStep(InputStep.TITLE);
                ChatInputListener.enableInputMode(clicker.getUniqueId());
                break;
            case 14: // Description
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.enter-description")));
                builder.setInputStep(InputStep.DESCRIPTION);
                ChatInputListener.enableInputMode(clicker.getUniqueId());
                break;
            case 16: // Reward
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.enter-reward")));
                builder.setInputStep(InputStep.REWARD);
                ChatInputListener.enableInputMode(clicker.getUniqueId());
                break;
            case 19: // Deadline
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.enter-deadline")));
                builder.setInputStep(InputStep.DEADLINE);
                ChatInputListener.enableInputMode(clicker.getUniqueId());
                break;
            case 21: // Location
                if (builder.getType() == ContractType.BUILDING) {
                    builder.setLocation(player.getLocation());
                    fillInventory(builder);
                    clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLocaleManager().getMessage("gui.contract-creation.location-set")
                                    .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                                    .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                                    .replace("{z}", String.valueOf(player.getLocation().getBlockZ()))));
                }
                break;
            case 40: // Submit
                plugin.getLogger().info("[ContractCreationGUI] Submit button clicked by " + clicker.getName());
                if (builder.canSubmit()) {
                    submitContract(builder);
                } else {
                    plugin.getLogger().info("[ContractCreationGUI] Cannot submit - builder not ready");
                }
                break;
            case 44: // Cancel
                cancelContractCreation();
                break;
        }
    }
    
    /**
     * Opens contract type selection GUI
     */
    private void openTypeSelection() {
        Inventory typeInv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui.contract-creation.select-type")));
        
        // Add all contract types
        int slot = 0;
        for (ContractType type : ContractType.values()) {
            if (slot >= 27) break;
            
            String autoVerifyText = plugin.getLocaleManager().getMessage("contract-types.auto-verify") + " " +
                    (type.isAutoVerify() ? 
                        plugin.getLocaleManager().getMessage("common.affirmative") : 
                        plugin.getLocaleManager().getMessage("common.negative"));
            
            ItemStack typeItem = createGuiItem(
                getMaterialForType(type),
                "&e" + type.getDisplayName(),
                autoVerifyText,
                "&7ID: " + type.getId()
            );
            
            typeInv.setItem(slot, typeItem);
            slot++;
        }
        
        player.openInventory(typeInv);
    }
    
    /**
     * Handles type selection from type selection GUI
     */
    private void handleTypeSelection(Player player, int slot) {
        ContractBuilder builder = contractBuilders.get(player.getUniqueId());
        if (builder == null) {
            builder = new ContractBuilder();
            contractBuilders.put(player.getUniqueId(), builder);
        }
        
        // Get contract type by slot
        ContractType[] types = ContractType.values();
        if (slot >= 0 && slot < types.length) {
            ContractType selectedType = types[slot];
            builder.setType(selectedType);
            
            // Close type selection and refresh current GUI with updated type
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Get current builder and update GUI in-place
                ContractBuilder currentBuilder = contractBuilders.get(player.getUniqueId());
                if (currentBuilder != null) {
                    fillInventory(currentBuilder);
                    player.openInventory(inventory);
                } else {
                    // Fallback to opening new GUI if no builder exists
                    plugin.getGUIManager().openContractCreation(player);
                }
            });
        }
    }
    
    /**
     * Gets material for contract type
     */
    private Material getMaterialForType(ContractType type) {
        switch (type) {
            case ITEM_REQUEST: return Material.DIAMOND;
            case BUILDING: return Material.BRICKS;
            case COMBAT: return Material.IRON_SWORD;
            case MINING: return Material.IRON_PICKAXE;
            case FARMING: return Material.WHEAT;
            case CUSTOM: return Material.NETHER_STAR;
            default: return Material.PAPER;
        }
    }
    
    /**
     * Submits contract for creation
     */
    private void submitContract(ContractBuilder builder) {
        if (!builder.canSubmit()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLocaleManager().getMessage("gui.contract-creation.fill-required")));
            return;
        }
        
        // Check BUILDING contracts for location - USE LOCALIZATION!
        if (builder.getType() == ContractType.BUILDING && builder.getLocation() == null) {
            String message = plugin.getLocaleManager().getMessage("errors.building-requires-location");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        // Check player balance
        double totalCost = builder.getReward() + (builder.getReward() * plugin.getEscrowManager().getCommissionPercentage() / 100);
        if (economy != null && economy.getBalance(player) < totalCost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLocaleManager().getMessage("gui.contract-creation.insufficient-funds")
                            .replace("{amount}", String.format("%.2f", totalCost))));
            return;
        }
        
        // Create contract asynchronously
        contractManager.createContract(
            player.getUniqueId(),
            builder.getType(),
            builder.getTitle(),
            builder.getDescription(),
            builder.getReward(),
            builder.getDeadline()
        ).thenAccept(contract -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.success")
                                .replace("{id}", contract.getId().toString().substring(0, 8))));
                player.closeInventory();
                cancelContractCreation();
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLocaleManager().getMessage("gui.contract-creation.error")
                                .replace("{error}", throwable.getMessage())));
            });
            return null;
        });
    }
    
    /**
     * Cancels contract creation
     */
    private void cancelContractCreation() {
        contractBuilders.remove(player.getUniqueId());
        player.closeInventory();
        HandlerList.unregisterAll(this);
    }
    
    /**
     * Cleanup GUI
     */
    public void cleanup() {
        contractBuilders.remove(player.getUniqueId());
        HandlerList.unregisterAll(this);
    }
    
    /**
     * Gets builder for player
     */
    public ContractBuilder getBuilder(UUID playerUuid) {
        return contractBuilders.get(playerUuid);
    }
    
    /**
     * Sets builder data and reopens GUI
     */
    public void setBuilderData(ContractBuilder builder) {
        contractBuilders.put(player.getUniqueId(), builder);
        fillInventory(builder);
        player.openInventory(inventory);
    }
    
        
    /**
     * Input steps
     */
    public enum InputStep {
        TITLE, DESCRIPTION, REWARD, DEADLINE, LOCATION
    }
    
    /**
     * Builder for contract creation
     */
    public static class ContractBuilder {
        private ContractType type;
        private String title;
        private String description;
        private double reward;
        private Long deadline;
        private Location location;
        private InputStep inputStep = InputStep.TITLE;
        
        public ContractType getType() { return type; }
        public void setType(ContractType type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getReward() { return reward; }
        public void setReward(double reward) { this.reward = reward; }
        
        public Long getDeadline() { return deadline; }
        public void setDeadline(Long deadline) { this.deadline = deadline; }
        
        public void setDeadlineHours(int hours) {
            this.deadline = (long) hours;
        }
        
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        
        public InputStep getInputStep() { return inputStep; }
        public void setInputStep(InputStep inputStep) { this.inputStep = inputStep; }
        
        public boolean canSubmit() {
            return type != null && 
                   title != null && !title.trim().isEmpty() && 
                   description != null && !description.trim().isEmpty() && 
                   reward > 0 && 
                   deadline != null && deadline > 0 &&
                   (type != ContractType.BUILDING || location != null);
        }
    }
}
