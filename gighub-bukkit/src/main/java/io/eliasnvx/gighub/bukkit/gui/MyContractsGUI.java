package io.eliasnvx.gighub.bukkit.gui;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.model.Contract;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI "Мои контракты"
 */
public class MyContractsGUI extends BaseGUI {
    
    private final GigHubPlugin plugin;
    private final GUIManager guiManager;
    private List<Contract> ownedContracts = new ArrayList<>();
    private List<Contract> contractedContracts = new ArrayList<>();
    
    public MyContractsGUI(GigHubPlugin plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }
    
    @Override
    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§a§lMy Contracts");
        
        // Сразу добавляем кнопку "Back" чтобы она всегда была доступна
        inv.setItem(49, createItem(Material.ARROW, "§cBack", "§7Return to Contract Board"));
        
        // Загружаем контракты
        plugin.getContractManager().getPlayerContracts(player.getUniqueId(), true).thenAccept(owned -> {
            this.ownedContracts = owned;
            
            plugin.getContractManager().getPlayerContracts(player.getUniqueId(), false).thenAccept(contracted -> {
                this.contractedContracts = contracted;
                
                Bukkit.getScheduler().runTask(plugin, () -> fillInventory(inv, player));
            });
        });
        
        // Загрузка
        inv.setItem(22, createItem(Material.HOPPER, "§eLoading...", "§7Please wait..."));
        
        return inv;
    }
    
    private void fillInventory(Inventory inv, Player player) {
        inv.clear();
        
        // Заголовки
        inv.setItem(10, createItem(Material.EMERALD_BLOCK, "§aAs Owner", 
            "§7Contracts you created",
            "§7Total: §f" + ownedContracts.size()));
        
        inv.setItem(16, createItem(Material.DIAMOND_BLOCK, "§bAs Contractor", 
            "§7Contracts you're working on",
            "§7Total: §f" + contractedContracts.size()));
        
        // Контракты как владелец (левая сторона)
        int slot = 19;
        for (int i = 0; i < Math.min(ownedContracts.size(), 12); i++) {
            if (slot % 9 >= 4) slot = (slot / 9 + 1) * 9; // Переход на следующую строку
            inv.setItem(slot, createContractItem(ownedContracts.get(i), true));
            slot++;
        }
        
        // Контракты как исполнитель (правая сторона)
        slot = 23;
        for (int i = 0; i < Math.min(contractedContracts.size(), 12); i++) {
            if (slot % 9 >= 8) slot = (slot / 9 + 1) * 9 + 5; // Переход на следующую строку
            inv.setItem(slot, createContractItem(contractedContracts.get(i), false));
            slot++;
        }
        
        // Кнопка назад (нижний ряд, центр)
        inv.setItem(49, createItem(Material.ARROW, "§cBack", "§7Return to Contract Board"));
        
        // Обновляем инвентарь игрока
        player.updateInventory();
    }
    
    private ItemStack createContractItem(Contract contract, boolean asOwner) {
        Material material = asOwner ? Material.PAPER : Material.WRITABLE_BOOK;
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Type: §f" + contract.getType().name());
        lore.add("§7Reward: §a$" + String.format("%.2f", contract.getReward()));
        lore.add("§7Status: " + getStatusColor(contract.getStatus()));
        lore.add("");
        lore.add("§eClick to view details");
        
        return createItem(material, "§6" + contract.getTitle(), lore.toArray(new String[0]));
    }
    
    @Override
    public void handleClick(Player player, int slot, ClickType clickType) {
        if (slot == 49) { // Назад
            guiManager.openGUI(player, new ContractBoardGUI(plugin, guiManager));
            return;
        }
        
        // Клик на контракт
        Contract contract = getContractAtSlot(slot);
        if (contract != null) {
            guiManager.openGUI(player, new ContractDetailsGUI(plugin, guiManager, contract));
        }
    }
    
    private Contract getContractAtSlot(int slot) {
        // Левая сторона (owned)
        if (slot >= 19 && slot < 46 && slot % 9 < 4) {
            int row = (slot - 19) / 9;
            int col = slot % 9;
            int index = row * 4 + col;
            if (index < ownedContracts.size()) {
                return ownedContracts.get(index);
            }
        }
        
        // Правая сторона (contracted)
        if (slot >= 23 && slot < 46 && slot % 9 >= 5) {
            int row = (slot - 23) / 9;
            int col = (slot % 9) - 5;
            int index = row * 3 + col;
            if (index < contractedContracts.size()) {
                return contractedContracts.get(index);
            }
        }
        
        return null;
    }
    
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
