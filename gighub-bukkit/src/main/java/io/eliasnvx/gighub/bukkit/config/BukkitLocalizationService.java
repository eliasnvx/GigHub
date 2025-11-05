package io.eliasnvx.gighub.bukkit.config;

import io.eliasnvx.gighub.core.api.LocalizationService;

/**
 * Реализация сервиса локализации для Bukkit
 */
public class BukkitLocalizationService implements LocalizationService {
    private final LocaleManager localeManager;
    
    public BukkitLocalizationService(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }
    
    @Override
    public String getContractTypeName(String contractTypeId) {
        return localeManager.getContractTypeName(contractTypeId);
    }
    
    @Override
    public String getContractTypeName(String locale, String contractTypeId) {
        return localeManager.getContractTypeName(locale, contractTypeId);
    }
}
