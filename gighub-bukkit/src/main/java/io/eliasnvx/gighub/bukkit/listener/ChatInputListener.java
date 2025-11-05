package io.eliasnvx.gighub.bukkit.listener;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.bukkit.gui.ContractCreationGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for handling chat input during contract creation
 */
public class ChatInputListener implements Listener {
    
    private final GigHubPlugin plugin;
    // Store UUIDs of players in input mode (for fast checking)
    private static final Set<UUID> inputMode = new HashSet<>();
    
    public ChatInputListener(GigHubPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Enable input mode for player
     */
    public static void enableInputMode(UUID playerUuid) {
        inputMode.add(playerUuid);
    }
    
    /**
     * Disable input mode for player
     */
    public static void disableInputMode(UUID playerUuid) {
        inputMode.remove(playerUuid);
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // DEBUG: Log all chat events
        plugin.getLogger().info("[ChatInput] Chat event from " + player.getName() + ": " + event.getMessage());
        plugin.getLogger().info("[ChatInput] Input mode active: " + inputMode.contains(player.getUniqueId()));
        plugin.getLogger().info("[ChatInput] Input mode set size: " + inputMode.size());
        
        // Fast check - is player in input mode?
        if (!inputMode.contains(player.getUniqueId())) {
            plugin.getLogger().info("[ChatInput] Player not in input mode, ignoring");
            return; // Player not in input mode
        }
        
        plugin.getLogger().info("[ChatInput] Player IS in input mode! Cancelling event...");
        
        // Save original message BEFORE clearing!
        String message = event.getMessage().trim();
        plugin.getLogger().info("[ChatInput] Processing message: '" + message + "'");
        
        // CRITICAL: Cancel event IMMEDIATELY
        event.setCancelled(true);
        event.getRecipients().clear();
        event.setMessage(""); // Clear message
        
        // Check builder - get from GUIManager
        ContractCreationGUI gui = plugin.getGUIManager().getContractCreationGUI(player.getUniqueId());
        if (gui == null) {
            plugin.getLogger().info("[ChatInput] GUI not found for player " + player.getName());
            return;
        }
        ContractCreationGUI.ContractBuilder builder = gui.getBuilder(player.getUniqueId());
        
        if (builder == null || builder.getInputStep() == null) {
            plugin.getLogger().info("[ChatInput] Builder is null or step is null - returning");
            disableInputMode(player.getUniqueId());
            return;
        }
        
        // Handle cancellation
        if (message.equalsIgnoreCase("cancel")) {
            builder.setInputStep(null);
            disableInputMode(player.getUniqueId()); // Disable input mode
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLocaleManager().getMessage("chat-input.cancelled")));
            
            // Reopen GUI with current data
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    // Get the existing GUI and refresh it directly
                    ContractCreationGUI contractGui = plugin.getGUIManager().getContractCreationGUI(player);
                    if (contractGui != null) {
                        ContractCreationGUI.ContractBuilder currentBuilder = contractGui.getBuilder(player.getUniqueId());
                        if (currentBuilder != null) {
                            plugin.getLogger().info("[ChatInput] Refreshing GUI after cancellation...");
                            contractGui.fillInventory(currentBuilder);
                            player.openInventory(contractGui.getInventory());
                        }
                    } else {
                        plugin.getLogger().warning("[ChatInput] No existing GUI found after cancellation!");
                    }
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error reopening GUI after cancellation: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            return;
        }
        
        // Process input based on step
        ContractCreationGUI.InputStep step = builder.getInputStep();
        plugin.getLogger().info("[ChatInput] Current input step: " + step);
        
        try {
            plugin.getLogger().info("[ChatInput] Entering switch for step: " + step);
            switch (step) {
                case TITLE -> {
                    plugin.getLogger().info("[ChatInput] Processing TITLE case");
                    if (message.length() > 100) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.title-too-long")));
                        return;
                    }
                    builder.setTitle(message);
                    builder.setInputStep(null);
                    disableInputMode(player.getUniqueId()); // Disable input mode
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLocaleManager().getMessage("chat-input.title-set")
                                    .replace("{title}", message)));
                    plugin.getLogger().info("[ChatInput] TITLE case completed, breaking");
                    break;
                }
                
                case DESCRIPTION -> {
                    plugin.getLogger().info("[ChatInput] Processing DESCRIPTION case");
                    int maxLength = plugin.getConfig().getInt("contracts.max-description-length", 500);
                    if (message.length() > maxLength) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.description-too-long")
                                        .replace("{max}", String.valueOf(maxLength))));
                        return;
                    }
                    builder.setDescription(message);
                    builder.setInputStep(null);
                    disableInputMode(player.getUniqueId()); // Disable input mode
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLocaleManager().getMessage("chat-input.description-set")));
                    plugin.getLogger().info("[ChatInput] DESCRIPTION case completed, breaking");
                    break;
                }
                
                case REWARD -> {
                    plugin.getLogger().info("[ChatInput] Processing REWARD case");
                    try {
                        double reward = Double.parseDouble(message);
                        double minReward = plugin.getConfig().getDouble("contracts.min-reward", 100);
                        double maxReward = plugin.getConfig().getDouble("contracts.max-reward", 1000000);
                        
                        plugin.getLogger().info("[ChatInput] Parsed reward: " + reward);
                        
                        if (reward < minReward || reward > maxReward) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    plugin.getLocaleManager().getMessage("chat-input.reward-range")
                                            .replace("{min}", String.format("%.2f", minReward))
                                            .replace("{max}", String.format("%.2f", maxReward))));
                            return;
                        }
                        
                        builder.setReward(reward);
                        builder.setInputStep(null);
                        disableInputMode(player.getUniqueId()); // Disable input mode
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.reward-set")
                                        .replace("{amount}", String.format("%.2f", reward))));
                        plugin.getLogger().info("[ChatInput] REWARD case completed, breaking");
                        break;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.reward-invalid")));
                        return;
                    }
                }
                
                case DEADLINE -> {
                    plugin.getLogger().info("[ChatInput] Processing DEADLINE case");
                    try {
                        int hours = Integer.parseInt(message);
                        
                        if (hours < 1 || hours > 168) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    plugin.getLocaleManager().getMessage("chat-input.deadline-range")));
                            return;
                        }
                        
                        builder.setDeadlineHours(hours);
                        builder.setInputStep(null);
                        disableInputMode(player.getUniqueId()); // Disable input mode
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.deadline-set")
                                        .replace("{hours}", String.valueOf(hours))));
                        plugin.getLogger().info("[ChatInput] DEADLINE case completed, breaking");
                        break;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getLocaleManager().getMessage("chat-input.deadline-invalid")));
                        return;
                    }
                }
            }
            
            // Reopen GUI after successful input
            plugin.getLogger().info("[ChatInput] Scheduling GUI open for player " + player.getName());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    plugin.getLogger().info("[ChatInput] Inside scheduler - opening GUI for " + player.getName());
                    
                    // Check if player is still online
                    if (!player.isOnline()) {
                        plugin.getLogger().warning("[ChatInput] Player went offline, cannot open GUI");
                        return;
                    }
                    
                    // Get the updated GUI and refresh it with current builder data
                    ContractCreationGUI contractGui = plugin.getGUIManager().getContractCreationGUI(player);
                    if (contractGui != null) {
                        ContractCreationGUI.ContractBuilder currentBuilder = contractGui.getBuilder(player.getUniqueId());
                        if (currentBuilder != null) {
                            plugin.getLogger().info("[ChatInput] Refreshing GUI with updated data...");
                            contractGui.fillInventory(currentBuilder);
                            player.openInventory(contractGui.getInventory());
                        }
                    }
                    plugin.getLogger().info("[ChatInput] GUI opened successfully for " + player.getName());
                } catch (Exception ex) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLocaleManager().getMessage("chat-input.gui-error")
                                    .replace("{error}", ex.getMessage())));
                    plugin.getLogger().severe("Error opening GUI after input: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLocaleManager().getMessage("chat-input.error")
                            .replace("{error}", e.getMessage())));
            builder.setInputStep(null);
            disableInputMode(player.getUniqueId());
            plugin.getLogger().severe("Error processing chat input: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear input mode when player quits
        disableInputMode(event.getPlayer().getUniqueId());
    }
}
