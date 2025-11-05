package io.eliasnvx.gighub.core.api;

/**
 * Сервис локализации для использования в core модуле
 */
public interface LocalizationService {
    
    /**
     * Получает локализованное название типа контракта
     */
    String getContractTypeName(String contractTypeId);
    
    /**
     * Получает локализованное название типа контракта для указанной локали
     */
    String getContractTypeName(String locale, String contractTypeId);
}
