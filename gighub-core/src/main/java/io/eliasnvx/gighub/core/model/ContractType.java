package io.eliasnvx.gighub.core.model;

import io.eliasnvx.gighub.core.api.LocalizationService;

/**
 * Типы контрактов в GigHub
 */
public enum ContractType {
    /**
     * Заказ предметов - автоверификация
     */
    ITEM_REQUEST("item_request", true),
    
    /**
     * Строительные работы - ручная верификация
     */
    BUILDING("building", false),
    
    /**
     * Боевые задачи (убийство мобов) - автоверификация
     */
    COMBAT("combat", true),
    
    /**
     * Добыча ресурсов - автоверификация
     */
    MINING("mining", true),
    
    /**
     * Сбор урожая - автоверификация
     */
    FARMING("farming", true),
    
    /**
     * Кастомные задачи
     */
    CUSTOM("custom", false);
    
    private final String id;
    private final boolean autoVerify;
    private static LocalizationService localizationService;
    
    ContractType(String id, boolean autoVerify) {
        this.id = id;
        this.autoVerify = autoVerify;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isAutoVerify() {
        return autoVerify;
    }
    
    /**
     * Устанавливает сервис локализации (должен вызываться при инициализации плагина)
     */
    public static void setLocalizationService(LocalizationService service) {
        localizationService = service;
    }
    
    public String getDisplayName() {
        if (localizationService != null) {
            return localizationService.getContractTypeName(id);
        }
        // Fallback на английские названия если локализация не доступна
        switch (this) {
            case ITEM_REQUEST:
                return "Item Request";
            case BUILDING:
                return "Building";
            case COMBAT:
                return "Combat";
            case MINING:
                return "Mining";
            case FARMING:
                return "Farming";
            case CUSTOM:
                return "Custom";
            default:
                return this.name();
        }
    }
    
    /**
     * Получает локализованное название для указанной локали
     */
    public String getDisplayName(String locale) {
        if (localizationService != null) {
            return localizationService.getContractTypeName(locale, id);
        }
        return getDisplayName();
    }
    
    public static ContractType fromId(String id) {
        for (ContractType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
