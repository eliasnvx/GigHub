package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.model.ContractType;
import io.eliasnvx.gighub.core.model.ContractStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI для фильтрации контрактов
 */
public class FiltersGUI extends BaseGUI {
    
    private final GigHubPlugin plugin;
    private final GUIManager guiManager;
    private final Map<UUID, ContractFilters> playerFilters = new HashMap<>();
    
    // Позиции элементов
    private static final int BACK_ITEM = 45;
    private static final int APPLY_ITEM = 49;
    private static final int RESET_ITEM = 53;
    
    // Позиции фильтров по типу
    private static final int TYPE_ALL = 10;
    private static final int TYPE_ITEM_REQUEST = 19;
    private static final int TYPE_BUILDING = 28;
    
    // Позиции фильтров по статусу
    private static final int STATUS_ALL = 12;
    private static final int STATUS_OPEN = 21;
    private static final int STATUS_IN_PROGRESS = 30;
    private static final int STATUS_COMPLETED = 39;
    
    public FiltersGUI(GigHubPlugin plugin, GUIManager guiManager) {
        super();
        this.plugin = plugin;
        this.guiManager = guiManager;
    }
    
    @Override
    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lContract Filters");
        
        // Получаем текущие фильтры игрока
        ContractFilters filters = getPlayerFilters(player);
        
        // Заголовок - информация о текущих фильтрах
        inv.setItem(4, createInfoItem(filters));
        
        // Фильтры по типу контракта
        inv.setItem(TYPE_ALL, createTypeFilterItem(null, filters.getType() == null));
        inv.setItem(TYPE_ITEM_REQUEST, createTypeFilterItem(ContractType.ITEM_REQUEST, filters.getType() == ContractType.ITEM_REQUEST));
        inv.setItem(TYPE_BUILDING, createTypeFilterItem(ContractType.BUILDING, filters.getType() == ContractType.BUILDING));
        
        // Фильтры по статусу контракта
        inv.setItem(STATUS_ALL, createStatusFilterItem(null, filters.getStatus() == null));
        inv.setItem(STATUS_OPEN, createStatusFilterItem(ContractStatus.OPEN, filters.getStatus() == ContractStatus.OPEN));
        inv.setItem(STATUS_IN_PROGRESS, createStatusFilterItem(ContractStatus.IN_PROGRESS, filters.getStatus() == ContractStatus.IN_PROGRESS));
        inv.setItem(STATUS_COMPLETED, createStatusFilterItem(ContractStatus.COMPLETED, filters.getStatus() == ContractStatus.COMPLETED));
        
        // Кнопки управления
        inv.setItem(BACK_ITEM, createBackItem());
        inv.setItem(RESET_ITEM, createResetItem());
        inv.setItem(APPLY_ITEM, createApplyItem());
        
        return inv;
    }
    
    @Override
    public void handleClick(Player player, int slot, ClickType clickType) {
        ContractFilters filters = getPlayerFilters(player);
        
        // Фильтры по типу
        if (slot == TYPE_ALL) {
            filters.setType(null);
            refresh(player);
            return;
        }
        
        if (slot == TYPE_ITEM_REQUEST) {
            filters.setType(ContractType.ITEM_REQUEST);
            refresh(player);
            return;
        }
        
        if (slot == TYPE_BUILDING) {
            filters.setType(ContractType.BUILDING);
            refresh(player);
            return;
        }
        
        // Фильтры по статусу
        if (slot == STATUS_ALL) {
            filters.setStatus(null);
            refresh(player);
            return;
        }
        
        if (slot == STATUS_OPEN) {
            filters.setStatus(ContractStatus.OPEN);
            refresh(player);
            return;
        }
        
        if (slot == STATUS_IN_PROGRESS) {
            filters.setStatus(ContractStatus.IN_PROGRESS);
            refresh(player);
            return;
        }
        
        if (slot == STATUS_COMPLETED) {
            filters.setStatus(ContractStatus.COMPLETED);
            refresh(player);
            return;
        }
        
        // Кнопки управления
        if (slot == RESET_ITEM) {
            resetFilters(player);
            refresh(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getLocaleManager().getMessage("gui-filters-reset")));
            return;
        }
        
        if (slot == APPLY_ITEM) {
            // Возвращаемся в главное меню с примененными фильтрами
            guiManager.openGUI(player, new ContractBoardGUI(plugin, guiManager, filters));
            return;
        }
        
        if (slot == BACK_ITEM) {
            // Возвращаемся без применения фильтров
            guiManager.openGUI(player, new ContractBoardGUI(plugin, guiManager));
            return;
        }
    }
    
    private ContractFilters getPlayerFilters(Player player) {
        return playerFilters.computeIfAbsent(player.getUniqueId(), k -> new ContractFilters());
    }
    
    private void resetFilters(Player player) {
        playerFilters.put(player.getUniqueId(), new ContractFilters());
    }
    
    private ItemStack createInfoItem(ContractFilters filters) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§lCurrent Filters");
            
            String typeText = filters.getType() == null ? "All Types" : filters.getType().getDisplayName();
            String statusText = filters.getStatus() == null ? "All Statuses" : filters.getStatus().getDisplayName();
            
            meta.setLore(Arrays.asList(
                "§7Type: §e" + typeText,
                "§7Status: §e" + statusText,
                "",
                "§7Click items below to change filters",
                "§7Click Apply to use these filters"
            ));
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createTypeFilterItem(ContractType type, boolean selected) {
        Material material = selected ? Material.GREEN_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
        String name = selected ? "§a" + (type == null ? "All Types" : type.getDisplayName()) : "§7" + (type == null ? "All Types" : type.getDisplayName());
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (type != null) {
                meta.setLore(Arrays.asList(
                    "§7Filter by " + type.getDisplayName() + " contracts",
                    selected ? "§aCurrently selected" : "§7Click to select"
                ));
            } else {
                meta.setLore(Arrays.asList(
                    "§7Show all contract types",
                    selected ? "§aCurrently selected" : "§7Click to select"
                ));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createStatusFilterItem(ContractStatus status, boolean selected) {
        Material material = selected ? Material.GREEN_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
        String name = selected ? "§a" + (status == null ? "All Statuses" : status.getDisplayName()) : "§7" + (status == null ? "All Statuses" : status.getDisplayName());
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (status != null) {
                meta.setLore(Arrays.asList(
                    "§7Filter by " + status.getDisplayName() + " contracts",
                    selected ? "§aCurrently selected" : "§7Click to select"
                ));
            } else {
                meta.setLore(Arrays.asList(
                    "§7Show all contract statuses",
                    selected ? "§aCurrently selected" : "§7Click to select"
                ));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§cBack");
            meta.setLore(Arrays.asList(
                "§7Return to contract board",
                "§7without applying filters"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§cReset Filters");
            meta.setLore(Arrays.asList(
                "§7Reset all filters to default",
                "§7(All types, All statuses)"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createApplyItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§aApply Filters");
            meta.setLore(Arrays.asList(
                "§7Apply selected filters",
                "§7and return to contract board"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Класс для хранения фильтров игрока
     */
    public static class ContractFilters {
        private ContractType type = null;
        private ContractStatus status = null;
        
        public ContractType getType() {
            return type;
        }
        
        public void setType(ContractType type) {
            this.type = type;
        }
        
        public ContractStatus getStatus() {
            return status;
        }
        
        public void setStatus(ContractStatus status) {
            this.status = status;
        }
        
        public boolean hasActiveFilters() {
            return type != null || status != null;
        }
    }
}
