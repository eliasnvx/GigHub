package io.eliasnvx.gighub.bukkit.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Базовый класс для всех GUI
 */
public abstract class BaseGUI {
    
    /**
     * Создание инвентаря для GUI
     */
    public abstract Inventory createInventory(Player player);
    
    /**
     * Обработка клика в GUI
     */
    public abstract void handleClick(Player player, int slot, ClickType clickType);
    
    /**
     * Вызывается при закрытии GUI
     */
    public void onClose(Player player) {
        // По умолчанию ничего не делаем
    }
    
    /**
     * Обновление GUI
     */
    public void refresh(Player player) {
        Inventory newInventory = createInventory(player);
        player.getOpenInventory().getTopInventory().setContents(newInventory.getContents());
    }
}
