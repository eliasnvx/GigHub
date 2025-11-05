package io.eliasnvx.gighub.core.model;

/**
 * Статусы контракта в жизненном цикле
 */
public enum ContractStatus {
    /**
     * Контракт создан, деньги заморожены
     */
    CREATED,
    
    /**
     * Доступен для принятия
     */
    OPEN,
    
    /**
     * Кто-то взял контракт
     */
    ACCEPTED,
    
    /**
     * Выполняется
     */
    IN_PROGRESS,
    
    /**
     * Выполнен, ожидает подтверждения
     */
    COMPLETED,
    
    /**
     * Подтвержден, оплачен
     */
    VERIFIED,
    
    /**
     * Отменен
     */
    CANCELLED,
    
    /**
     * Истек срок
     */
    EXPIRED;
    
    public boolean isActive() {
        return this == OPEN || this == ACCEPTED || this == IN_PROGRESS || this == COMPLETED;
    }
    
    public String getDisplayName() {
        switch (this) {
            case CREATED:
                return "Created";
            case OPEN:
                return "Open";
            case ACCEPTED:
                return "Accepted";
            case IN_PROGRESS:
                return "In Progress";
            case COMPLETED:
                return "Completed";
            case VERIFIED:
                return "Verified";
            case CANCELLED:
                return "Cancelled";
            case EXPIRED:
                return "Expired";
            default:
                return this.name();
        }
    }
    
    public boolean isFinal() {
        return this == VERIFIED || this == CANCELLED || this == EXPIRED;
    }
}
