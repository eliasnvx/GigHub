package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Главное меню контрактов
 */
public class ContractBoardGUI extends BaseGUI {
    
    private final GigHubPlugin plugin;
    private final GUIManager guiManager;
    private List<Contract> contracts;
    private int page = 0;
    private static final int CONTRACTS_PER_PAGE = 28;
    private final FiltersGUI.ContractFilters filters;
    
    public ContractBoardGUI(GigHubPlugin plugin, GUIManager guiManager) {
        this(plugin, guiManager, null);
    }
    
    public ContractBoardGUI(GigHubPlugin plugin, GUIManager guiManager, FiltersGUI.ContractFilters filters) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.contracts = new ArrayList<>();
        this.filters = filters;
    }
    
    @Override
    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lContract Board");
        
        // Загружаем контракты асинхронно
        plugin.getContractManager().getActiveContracts().thenAccept(loadedContracts -> {
            this.contracts = applyFilters(loadedContracts);
            sortContractsByPriority(); // Сортируем по priority
            
            // Обновляем GUI в главном потоке
            Bukkit.getScheduler().runTask(plugin, () -> {
                fillInventory(inv, player);
            });
        });
        
        // Пока контракты загружаются, показываем загрузку
        ItemStack loading = createItem(Material.HOPPER, "§eLoading contracts...", "§7Please wait...");
        inv.setItem(22, loading);
        
        return inv;
    }
    
    private void fillInventory(Inventory inv, Player player) {
        inv.clear();
        
        // Проверка пустых результатов фильтрации
        if (contracts.isEmpty() && filters != null && filters.hasActiveFilters()) {
            ItemStack noResults = createItem(Material.BARRIER, "§cNo contracts found", 
                "§7No contracts match your current filters",
                "§7Try adjusting or resetting your filters");
            inv.setItem(22, noResults);
            
            // Добавляем кнопки навигации
            addNavigationButtons(inv, player);
            addActionButtons(inv, player);
            return;
        }
        
        // Контракты (слоты 10-43, пропуская края)
        int startIndex = page * CONTRACTS_PER_PAGE;
        int endIndex = Math.min(startIndex + CONTRACTS_PER_PAGE, contracts.size());
        
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Contract contract = contracts.get(i);
            
            // Пропускаем края инвентаря
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            
            inv.setItem(slot, createContractItem(contract));
            slot++;
        }
        
        // Навигация и кнопки
        addNavigationButtons(inv, player);
        addActionButtons(inv, player);
        
        // Обновляем инвентарь игрока
        player.updateInventory();
    }
    
    private void addNavigationButtons(Inventory inv, Player player) {
        int totalPages = (int) Math.ceil((double) contracts.size() / CONTRACTS_PER_PAGE);
        
        // Предыдущая страница
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§aPrevious Page", 
                "§7Page " + page + "/" + totalPages));
        }
        
        // Следующая страница
        if ((page + 1) * CONTRACTS_PER_PAGE < contracts.size()) {
            inv.setItem(53, createItem(Material.ARROW, "§aNext Page", 
                "§7Page " + (page + 2) + "/" + totalPages));
        }
        
        // Информация
        inv.setItem(49, createItem(Material.BOOK, "§eContract Board", 
            "§7Total Contracts: §f" + contracts.size(),
            "§7Page: §f" + (page + 1) + "/" + Math.max(1, totalPages),
            "",
            "§7Click on a contract to view details"));
    }
    
    private void addActionButtons(Inventory inv, Player player) {
        // Создать контракт
        if (plugin.getPermissionManager().hasPermission(player, "gighub.contract.create")) {
            inv.setItem(46, createItem(Material.WRITABLE_BOOK, "§6Create Contract", 
                "§7Click to create a new contract"));
        }
        
        // Мои контракты
        inv.setItem(48, createItem(Material.CHEST, "§aMy Contracts", 
            "§7View your contracts"));
        
        // Фильтры
        if (filters != null && filters.hasActiveFilters()) {
            inv.setItem(50, createItem(Material.EMERALD, "§aFilters", 
                "§7Filter contracts by type",
                "§aActive filters: §e" + getActiveFilterCount(),
                "§7Click to modify filters"));
        } else {
            inv.setItem(50, createItem(Material.HOPPER, "§eFilters", 
                "§7Filter contracts by type",
                "§7Click to set filters"));
        }
        
        // Закрыть
        inv.setItem(52, createItem(Material.BARRIER, "§cClose", 
            "§7Close this menu"));
    }
    
    private ItemStack createContractItem(Contract contract) {
        Material material = getContractMaterial(contract);
        
        // Заголовок с индикаторами premium функций
        String title = "§6" + contract.getTitle();
        if (contract.isPriority()) {
            title = "§c⚡ §6" + contract.getTitle() + " §c⚡"; // Priority с молниями
        } else if (contract.isFeatured()) {
            title = "§e⭐ §6" + contract.getTitle() + " §e⭐"; // Featured со звездами
        }
        
        List<String> lore = new ArrayList<>();
        
        // Индикаторы premium функций
        if (contract.isPriority()) {
            lore.add("§c§l⚡ PRIORITY CONTRACT ⚡");
            lore.add("");
        } else if (contract.isFeatured()) {
            lore.add("§e§l⭐ FEATURED CONTRACT ⭐");
            lore.add("");
        }
        
        lore.add("§7Type: §f" + contract.getType().name());
        lore.add("§7Reward: §a$" + String.format("%.2f", contract.getReward()));
        lore.add("§7Status: " + getStatusColor(contract.getStatus()));
        lore.add("");
        
        // Описание (первые 50 символов)
        String desc = contract.getDescription();
        if (desc.length() > 50) {
            desc = desc.substring(0, 47) + "...";
        }
        lore.add("§7" + desc);
        lore.add("");
        lore.add("§eClick to view details");
        
        ItemStack item = createItem(material, title, lore.toArray(new String[0]));
        
        // Добавляем enchant glow для featured/priority контрактов
        if (contract.isFeatured() || contract.isPriority()) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }
    
    private Material getContractMaterial(Contract contract) {
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
    
    @Override
    public void handleClick(Player player, int slot, ClickType clickType) {
        // Навигация
        if (slot == 45 && page > 0) {
            page--;
            refresh(player);
            return;
        }
        
        if (slot == 53) {
            int maxPage = (int) Math.ceil((double) contracts.size() / CONTRACTS_PER_PAGE) - 1;
            if (page < maxPage) {
                page++;
                refresh(player);
            }
            return;
        }
        
        // Кнопки действий
        if (slot == 46) { // Создать контракт
            plugin.getGUIManager().openContractCreation(player);
            return;
        }
        
        if (slot == 48) { // Мои контракты
            guiManager.openGUI(player, new MyContractsGUI(plugin, guiManager));
            return;
        }
        
        if (slot == 50) { // Фильтры
            guiManager.openGUI(player, new FiltersGUI(plugin, guiManager));
            return;
        }
        
        if (slot == 52) { // Закрыть
            player.closeInventory();
            return;
        }
        
        // Клик на контракт
        int contractIndex = getContractIndex(slot);
        if (contractIndex >= 0 && contractIndex < contracts.size()) {
            Contract contract = contracts.get(contractIndex);
            guiManager.openGUI(player, new ContractDetailsGUI(plugin, guiManager, contract));
        }
    }
    
    private int getContractIndex(int slot) {
        // Конвертируем слот в индекс контракта
        int row = slot / 9;
        int col = slot % 9;
        
        // Пропускаем края
        if (col == 0 || col == 8 || row < 1 || row > 4) {
            return -1;
        }
        
        int positionInPage = (row - 1) * 7 + (col - 1);
        return page * CONTRACTS_PER_PAGE + positionInPage;
    }
    
    private List<Contract> applyFilters(List<Contract> contracts) {
        if (filters == null || !filters.hasActiveFilters()) {
            return contracts;
        }
        
        List<Contract> filtered = contracts.stream()
            .filter(contract -> filters.getType() == null || contract.getType() == filters.getType())
            .filter(contract -> filters.getStatus() == null || contract.getStatus() == filters.getStatus())
            .collect(Collectors.toList());
        
        // Reset page to 0 when filters are applied
        if (page > 0 && filtered.size() <= page * CONTRACTS_PER_PAGE) {
            page = 0;
        }
        
        return filtered;
    }
    
    private int getActiveFilterCount() {
        if (filters == null) return 0;
        int count = 0;
        if (filters.getType() != null) count++;
        if (filters.getStatus() != null) count++;
        return count;
    }
    
    /**
     * Сортирует контракты по priority (priority контракты показываются первыми)
     */
    private void sortContractsByPriority() {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        
        contracts.sort((c1, c2) -> {
            // Priority контракты идут первыми
            if (c1.isPriority() && !c2.isPriority()) return -1;
            if (!c1.isPriority() && c2.isPriority()) return 1;
            
            // Featured контракты идут вторыми
            if (c1.isFeatured() && !c2.isFeatured()) return -1;
            if (!c1.isFeatured() && c2.isFeatured()) return 1;
            
            // Остальные по времени создания (новые первыми)
            return Long.compare(c2.getCreatedAt(), c1.getCreatedAt());
        });
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
