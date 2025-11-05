package io.eliasnvx.gighub.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Модель контракта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    
    /**
     * Уникальный ID контракта
     */
    private UUID id;
    
    /**
     * UUID заказчика
     */
    private UUID ownerUuid;
    
    /**
     * UUID исполнителя (nullable)
     */
    private UUID contractorUuid;
    
    /**
     * Тип контракта
     */
    private ContractType type;
    
    /**
     * Название контракта (до 100 символов)
     */
    private String title;
    
    /**
     * Описание (до 500 символов по умолчанию)
     */
    private String description;
    
    /**
     * Награда за выполнение
     */
    private double reward;
    
    /**
     * Текущий статус
     */
    private ContractStatus status;
    
    /**
     * Время создания (Unix timestamp)
     */
    private long createdAt;
    
    /**
     * Дедлайн (Unix timestamp, nullable)
     */
    private Long deadline;
    
    /**
     * Время завершения (Unix timestamp, nullable)
     */
    private Long completedAt;
    
    /**
     * Автоверификация включена
     */
    private boolean autoVerify;
    
    /**
     * Featured контракт (выделяется в списке)
     */
    @Builder.Default
    private boolean featured = false;
    
    /**
     * Priority контракт (показывается выше в списке)
     */
    @Builder.Default
    private boolean priority = false;
    
    /**
     * Координаты локации (для building контрактов)
     */
    private Integer locationX;
    private Integer locationY;
    private Integer locationZ;
    
    /**
     * Мир (для building контрактов)
     */
    private String world;
    
    /**
     * Метаданные контракта (requirements, NBT и т.д.)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Проверка истечения срока
     */
    public boolean isExpired() {
        if (deadline == null) {
            return false;
        }
        return System.currentTimeMillis() > deadline && status.isActive();
    }
    
    /**
     * Проверка возможности принять контракт
     */
    public boolean canBeAccepted() {
        return status == ContractStatus.OPEN && !isExpired();
    }
    
    /**
     * Проверка возможности завершить контракт
     */
    public boolean canBeCompleted() {
        return status == ContractStatus.IN_PROGRESS && !isExpired();
    }
    
    /**
     * Проверка возможности отменить контракт
     */
    public boolean canBeCancelled() {
        return status.isActive() && !status.isFinal();
    }
    
    /**
     * Получение оставшегося времени в миллисекундах
     */
    public long getTimeRemaining() {
        if (deadline == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, deadline - System.currentTimeMillis());
    }
}
