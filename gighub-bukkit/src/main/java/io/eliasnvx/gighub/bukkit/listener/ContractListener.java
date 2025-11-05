package io.eliasnvx.gighub.bukkit.listener;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractStatus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Слушатель событий для контрактов
 */
public class ContractListener implements Listener {
    
    private final GigHubPlugin plugin;
    
    public ContractListener(GigHubPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * При входе игрока - проверяем истекшие контракты
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Проверяем контракты игрока асинхронно
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            checkPlayerContracts(player, playerUuid);
        });
    }
    
    /**
     * При выходе игрока - очищаем кеш
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Кеш прав автоматически очищается по таймауту
        // Можно добавить дополнительную логику при необходимости
    }
    
    /**
     * Проверка контрактов игрока
     */
    private void checkPlayerContracts(Player player, UUID playerUuid) {
        // Получаем контракты как владелец
        plugin.getContractManager().getPlayerContracts(playerUuid, true).thenAccept(ownedContracts -> {
            final int[] counts = new int[2]; // [0] = completed, [1] = expired
            
            for (Contract contract : ownedContracts) {
                if (contract.getStatus() == ContractStatus.COMPLETED) {
                    counts[0]++;
                } else if (contract.isExpired() && contract.getStatus() == ContractStatus.ACCEPTED) {
                    counts[1]++;
                }
            }
            
            // Получаем контракты как исполнитель
            plugin.getContractManager().getPlayerContracts(playerUuid, false).thenAccept(contractedContracts -> {
                int inProgress = 0;
                
                for (Contract contract : contractedContracts) {
                    if (contract.getStatus() == ContractStatus.ACCEPTED || 
                        contract.getStatus() == ContractStatus.IN_PROGRESS) {
                        inProgress++;
                    }
                }
                
                final int inProgressCount = inProgress;
                
                // Отправляем уведомления в главном потоке
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (counts[0] > 0) {
                        String message = ChatColor.translateAlternateColorCodes('&', 
                            plugin.getLocaleManager().getMessage("listener-completed-waiting")
                                .replace("{count}", String.valueOf(counts[0]))
                                .replace("{plural}", counts[0] > 1 ? "s" : ""));
                        player.sendMessage(message);
                    }
                    
                    if (counts[1] > 0) {
                        String message = ChatColor.translateAlternateColorCodes('&', 
                            plugin.getLocaleManager().getMessage("listener-expired-warning")
                                .replace("{count}", String.valueOf(counts[1]))
                                .replace("{plural}", counts[1] > 1 ? "s" : "")
                                .replace("{expired}", counts[1] > 1 ? "have expired" : "has expired"));
                        player.sendMessage(message);
                    }
                    
                    if (inProgressCount > 0) {
                        String message = ChatColor.translateAlternateColorCodes('&', 
                            plugin.getLocaleManager().getMessage("listener-in-progress")
                                .replace("{count}", String.valueOf(inProgressCount))
                                .replace("{plural}", inProgressCount > 1 ? "s" : ""));
                        player.sendMessage(message);
                    }
                });
            });
        });
    }
}
