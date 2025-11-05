package io.eliasnvx.gighub.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Модель отзыва о контракте
 */
@Data
@Builder
public class Review {
    private UUID id;
    private UUID contractId;      // ID контракта
    private UUID reviewerId;      // Кто оставил отзыв
    private UUID targetId;        // О ком отзыв
    private int rating;           // Рейтинг 1-5
    private String comment;       // Комментарий (опционально)
    private long createdAt;       // Timestamp создания
}
