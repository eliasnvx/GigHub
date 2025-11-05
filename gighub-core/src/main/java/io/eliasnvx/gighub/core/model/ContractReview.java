package io.eliasnvx.gighub.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Модель отзыва о контракте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractReview {
    
    /**
     * ID отзыва
     */
    private int id;
    
    /**
     * ID контракта
     */
    private UUID contractId;
    
    /**
     * UUID того, кто оставил отзыв
     */
    private UUID reviewerUuid;
    
    /**
     * UUID того, кому оставлен отзыв
     */
    private UUID revieweeUuid;
    
    /**
     * Рейтинг (1-5 звезд)
     */
    private int rating;
    
    /**
     * Комментарий
     */
    private String comment;
    
    /**
     * Время создания (Unix timestamp)
     */
    private long createdAt;
    
    /**
     * Валидация рейтинга
     */
    public boolean isValidRating() {
        return rating >= 1 && rating <= 5;
    }
}
