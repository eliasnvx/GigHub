package io.eliasnvx.gighub.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Модель эскроу транзакции
 */
@Data
@Builder
public class EscrowTransaction {
    private Long id;
    private UUID contractId;      // ID контракта
    private UUID ownerUuid;       // Владелец средств
    private Double amount;        // Сумма
    private Double commission;    // Комиссия
    private EscrowStatus status;  // Статус
    private Long frozenAt;        // Время заморозки
    private Long releasedAt;      // Время выпуска
    private Long refundedAt;      // Время возврата
    
    /**
     * Статусы эскроу транзакции
     */
    public enum EscrowStatus {
        FROZEN,     // Средства заморожены
        RELEASED,   // Выплачено исполнителю
        REFUNDED    // Возвращено владельцу
    }
    
    /**
     * Проверка, активна ли транзакция
     */
    public boolean isActive() {
        return status == EscrowStatus.FROZEN;
    }
    
    /**
     * Получение суммы к выплате (с вычетом комиссии)
     */
    public double getPayoutAmount() {
        return amount - commission;
    }
}
