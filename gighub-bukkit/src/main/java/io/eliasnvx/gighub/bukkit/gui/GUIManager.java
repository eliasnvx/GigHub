package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.bukkit.manager.EscrowManager;
import io.eliasnvx.gighub.core.manager.ContractManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер GUI для отслеживания открытых меню
 */
public class GUIManager implements Listener {
    
    private final GigHubPlugin plugin;
    private final ContractManager contractManager;
    private final EscrowManager escrowManager;
    
    private final Map<UUID, BaseGUI> openGUIs = new HashMap<>();
    private final Map<UUID, ContractCreationGUI> contractCreationGUIs = new HashMap<>();
    
    public GUIManager(GigHubPlugin plugin, ContractManager contractManager, EscrowManager escrowManager) {
        this.plugin = plugin;
        this.contractManager = contractManager;
        this.escrowManager = escrowManager;
    }
    
    /**
     * Открытие GUI для игрока
     */
    public void openGUI(Player player, BaseGUI gui) {
        Inventory inventory = gui.createInventory(player);
        openGUIs.put(player.getUniqueId(), gui);
        player.openInventory(inventory);
    }
    
    /**
     * Открытие GUI создания контракта
     */
    public void openContractCreation(Player player) {
        ContractCreationGUI existingGUI = contractCreationGUIs.get(player.getUniqueId());
        ContractCreationGUI.ContractBuilder existingBuilder = null;
        
        // Сохраняем существующий builder если есть
        if (existingGUI != null) {
            existingBuilder = existingGUI.getBuilder(player.getUniqueId());
            plugin.getLogger().info("[GUIManager] Found existing builder for " + player.getName());
            if (existingBuilder != null) {
                plugin.getLogger().info("[GUIManager] Builder data - Type: " + existingBuilder.getType() + 
                        ", Title: " + existingBuilder.getTitle() + 
                        ", Deadline: " + existingBuilder.getDeadline());
            }
            org.bukkit.event.HandlerList.unregisterAll(existingGUI);
        }
        
        // Создаем новый GUI для создания контракта
        ContractCreationGUI gui = new ContractCreationGUI(plugin, player);
        contractCreationGUIs.put(player.getUniqueId(), gui);
        
        // Всегда открываем GUI (внутри open() регистрируются обработчики событий)
        gui.open();

        // Если есть сохраненный builder, восстанавливаем его ПОСЛЕ открытия
        if (existingBuilder != null) {
            plugin.getLogger().info("[GUIManager] Restoring builder data...");
            gui.setBuilderData(existingBuilder);
        }
    }
    
    /**
     * Получить существующий GUI создания контракта
     */
    public ContractCreationGUI getContractCreationGUI(Player player) {
        return contractCreationGUIs.get(player.getUniqueId());
    }
    
    /**
     * Закрытие GUI создания контракта
     */
    public void closeContractCreation(Player player) {
        ContractCreationGUI gui = contractCreationGUIs.remove(player.getUniqueId());
        if (gui != null) {
            gui.cleanup();
        }
    }
    
    /**
     * Закрытие GUI игрока
     */
    public void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
        player.closeInventory();
    }
    
    /**
     * Получение открытого GUI игрока
     */
    public BaseGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    /**
     * Проверка есть ли у игрока открытый GUI
     */
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * Обновление GUI для игрока
     */
    public void updateGUI(Player player, BaseGUI gui) {
        if (openGUIs.containsKey(player.getUniqueId())) {
            Inventory inventory = gui.createInventory(player);
            player.openInventory(inventory);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        
        if (gui != null) {
            event.setCancelled(true); // Отменяем все клики в GUI
            
            // Проверяем что клик в верхнем инвентаре (GUI)
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().equals(event.getView().getTopInventory())) {
                gui.handleClick(player, event.getSlot(), event.getClick());
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        
        if (gui != null) {
            // Задержка перед удалением - даем время на открытие нового GUI
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("GigHub"),
                () -> {
                    // Проверяем что игрок не открыл новое GUI
                    if (player.getOpenInventory().getTopInventory().getSize() == player.getInventory().getSize()) {
                        gui.onClose(player);
                        openGUIs.remove(player.getUniqueId());
                    }
                },
                1L // 1 tick задержка
            );
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Очищаем BaseGUI
        openGUIs.remove(player.getUniqueId());
        
        // Очищаем ContractCreationGUI
        closeContractCreation(player);
    }
    
    /**
     * Получает ContractCreationGUI для игрока
     */
    public ContractCreationGUI getContractCreationGUI(UUID playerUuid) {
        return contractCreationGUIs.get(playerUuid);
    }
}
