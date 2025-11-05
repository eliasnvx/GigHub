package io.eliasnvx.gighub.bukkit.manager;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Менеджер прав через Vault
 */
public class PermissionManager {
    
    private final GigHubPlugin plugin;
    private final Logger logger;
    private Permission vaultPermission = null;
    private boolean vaultEnabled = false;
    
    // Кеш проверок прав (UUID -> permission -> result)
    private final Map<UUID, Map<String, Boolean>> permissionCache = new HashMap<>();
    private static final long CACHE_DURATION = 60000; // 1 минута
    private final Map<UUID, Long> cacheTimestamps = new HashMap<>();
    
    public PermissionManager(GigHubPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        setupVault();
    }
    
    /**
     * Настройка интеграции с Vault
     */
    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Vault не найден! Используется Bukkit SuperPerms");
            vaultEnabled = false;
            return;
        }
        
        RegisteredServiceProvider<Permission> rsp = 
            Bukkit.getServicesManager().getRegistration(Permission.class);
        
        if (rsp == null) {
            logger.warning("Vault найден, но Permission провайдер недоступен");
            vaultEnabled = false;
            return;
        }
        
        vaultPermission = rsp.getProvider();
        vaultEnabled = true;
        logger.info("Vault интеграция активирована: " + vaultPermission.getName());
    }
    
    /**
     * Проверка права у игрока
     */
    public boolean hasPermission(Player player, String permission) {
        // Проверяем кеш
        UUID uuid = player.getUniqueId();
        if (isCacheValid(uuid)) {
            Map<String, Boolean> cache = permissionCache.get(uuid);
            if (cache != null && cache.containsKey(permission)) {
                return cache.get(permission);
            }
        }
        
        boolean result;
        if (vaultEnabled && vaultPermission != null) {
            result = vaultPermission.has(player, permission);
        } else {
            // Fallback на Bukkit SuperPerms
            result = player.hasPermission(permission);
        }
        
        // Кешируем результат
        cachePermission(uuid, permission, result);
        
        return result;
    }
    
    /**
     * Проверка права у оффлайн игрока (только через Vault)
     */
    public boolean hasPermission(String world, String playerName, String permission) {
        if (vaultEnabled && vaultPermission != null) {
            return vaultPermission.playerHas(world, playerName, permission);
        }
        // Для оффлайн игроков без Vault - возвращаем false
        return false;
    }
    
    /**
     * Получение лимита контрактов для игрока
     */
    public int getContractLimit(Player player, int defaultLimit) {
        // Проверяем безлимит
        if (hasPermission(player, "gighub.limit.contracts.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        // Проверяем группы прав из конфига
        if (plugin.getConfig().contains("contracts.limits.groups")) {
            var groups = plugin.getConfig().getConfigurationSection("contracts.limits.groups");
            if (groups != null) {
                // Проверяем группы от большего лимита к меньшему
                String[] groupOrder = {"owner", "admin", "premium", "vip", "user"};
                for (String group : groupOrder) {
                    if (hasPermission(player, "gighub.group." + group)) {
                        int limit = groups.getInt(group, -1);
                        if (limit == -1) return Integer.MAX_VALUE;
                        if (limit > 0) return limit;
                    }
                }
            }
        }
        
        // Проверяем числовые лимиты (обратная совместимость)
        int[] limits = {100, 50, 25, 20, 15, 10, 5, 3};
        for (int limit : limits) {
            if (hasPermission(player, "gighub.limit.contracts." + limit)) {
                return limit;
            }
        }
        
        // Дефолтный лимит из конфига
        int configDefault = plugin.getConfig().getInt("contracts.limits.default", defaultLimit);
        return configDefault;
    }
    
    /**
     * Проверка, в какой группе игрок
     */
    public String getPrimaryGroup(Player player) {
        if (vaultEnabled && vaultPermission != null) {
            return vaultPermission.getPrimaryGroup(player);
        }
        return "default";
    }
    
    /**
     * Получение всех групп игрока
     */
    public String[] getPlayerGroups(Player player) {
        if (vaultEnabled && vaultPermission != null) {
            return vaultPermission.getPlayerGroups(player);
        }
        return new String[]{"default"};
    }
    
    /**
     * Проверка доступности Vault
     */
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    /**
     * Получение имени используемого плагина прав
     */
    public String getPermissionPluginName() {
        if (vaultEnabled && vaultPermission != null) {
            return vaultPermission.getName();
        }
        return "Bukkit SuperPerms";
    }
    
    /**
     * Инвалидация кеша для игрока
     */
    public void invalidateCache(UUID uuid) {
        permissionCache.remove(uuid);
        cacheTimestamps.remove(uuid);
    }
    
    /**
     * Очистка всего кеша
     */
    public void clearCache() {
        permissionCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Кеширование проверки права
     */
    private void cachePermission(UUID uuid, String permission, boolean result) {
        permissionCache.computeIfAbsent(uuid, k -> new HashMap<>())
            .put(permission, result);
        cacheTimestamps.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * Проверка валидности кеша
     */
    private boolean isCacheValid(UUID uuid) {
        Long timestamp = cacheTimestamps.get(uuid);
        if (timestamp == null) {
            return false;
        }
        return (System.currentTimeMillis() - timestamp) < CACHE_DURATION;
    }
    
    /**
     * Очистка устаревшего кеша (вызывать периодически)
     */
    public void cleanupCache() {
        long now = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > CACHE_DURATION
        );
        
        // Удаляем кеш для игроков, чьи timestamps были удалены
        permissionCache.keySet().removeIf(uuid -> 
            !cacheTimestamps.containsKey(uuid)
        );
    }
}
