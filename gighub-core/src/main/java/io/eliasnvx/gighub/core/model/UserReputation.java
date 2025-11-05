package io.eliasnvx.gighub.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Модель репутации пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReputation {
    
    /**
     * UUID игрока
     */
    private UUID uuid;
    
    /**
     * Рейтинг (1.0 - 5.0 звезд)
     */
    @Builder.Default
    private double rating = 5.0;
    
    /**
     * Количество завершенных контрактов
     */
    @Builder.Default
    private int totalCompleted = 0;
    
    /**
     * Количество проваленных контрактов
     */
    @Builder.Default
    private int totalFailed = 0;
    
    /**
     * Количество созданных контрактов
     */
    @Builder.Default
    private int totalCreated = 0;
    
    /**
     * Очки репутации
     */
    @Builder.Default
    private int points = 0;
    
    /**
     * Последнее обновление (Unix timestamp)
     */
    private long lastUpdated;
    
    /**
     * Добавление успешно выполненного контракта
     */
    public void addSuccess(int bonusPoints) {
        totalCompleted++;
        points += bonusPoints;
        lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Добавление проваленного контракта
     */
    public void addFailure(int penaltyPoints) {
        totalFailed++;
        points = Math.max(0, points - penaltyPoints);
        lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Обновление рейтинга
     */
    public void updateRating(double newRating) {
        this.rating = Math.max(1.0, Math.min(5.0, newRating));
        lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Получение процента успеха
     */
    public double getSuccessRate() {
        int total = totalCompleted + totalFailed;
        if (total == 0) {
            return 100.0;
        }
        return (double) totalCompleted / total * 100.0;
    }
    
    /**
     * Проверка минимального количества контрактов для рейтинга
     */
    public boolean hasMinimumContracts(int minimum) {
        return (totalCompleted + totalFailed) >= minimum;
    }
}
