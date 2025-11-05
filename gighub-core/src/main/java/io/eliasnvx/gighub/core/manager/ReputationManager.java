package io.eliasnvx.gighub.core.manager;

import io.eliasnvx.gighub.core.database.dao.ReputationDAO;
import io.eliasnvx.gighub.core.database.dao.ReviewDAO;
import io.eliasnvx.gighub.core.model.ContractReview;
import io.eliasnvx.gighub.core.model.UserReputation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Менеджер для управления репутацией пользователей
 */
public class ReputationManager {
    
    private final Logger logger;
    private final ReputationDAO reputationDAO;
    private final ReviewDAO reviewDAO;
    
    public ReputationManager(Logger logger, ReputationDAO reputationDAO, ReviewDAO reviewDAO) {
        this.logger = logger;
        this.reputationDAO = reputationDAO;
        this.reviewDAO = reviewDAO;
    }
    
    /**
     * Получение репутации игрока
     */
    public CompletableFuture<UserReputation> getReputation(UUID playerUuid) {
        return reputationDAO.getOrCreate(playerUuid);
    }
    
    /**
     * Добавление отзыва
     */
    public CompletableFuture<ContractReview> addReview(
        UUID contractId,
        UUID reviewerUuid,
        UUID revieweeUuid,
        int rating,
        String comment
    ) {
        // Валидация рейтинга
        if (rating < 1 || rating > 5) {
            logger.warning("Invalid rating: " + rating);
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Rating must be between 1 and 5")
            );
        }
        
        // Проверка, не оставлял ли уже отзыв
        return reviewDAO.exists(contractId, reviewerUuid)
            .thenCompose(exists -> {
                if (exists) {
                    logger.warning("Review already exists for contract: " + contractId);
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("Review already exists")
                    );
                }
                
                // Создаем отзыв
                ContractReview review = ContractReview.builder()
                    .contractId(contractId)
                    .reviewerUuid(reviewerUuid)
                    .revieweeUuid(revieweeUuid)
                    .rating(rating)
                    .comment(comment)
                    .createdAt(System.currentTimeMillis())
                    .build();
                
                return reviewDAO.create(review)
                    .thenCompose(created -> {
                        // Обновляем средний рейтинг
                        return updateAverageRating(revieweeUuid)
                            .thenApply(v -> created);
                    })
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            logger.info("Review added: " + reviewerUuid + " -> " + revieweeUuid + " (" + rating + " stars)");
                        }
                    });
            });
    }
    
    /**
     * Получение отзывов о игроке
     */
    public CompletableFuture<List<ContractReview>> getPlayerReviews(UUID playerUuid) {
        return reviewDAO.findByReviewee(playerUuid);
    }
    
    /**
     * Получение отзывов по контракту
     */
    public CompletableFuture<List<ContractReview>> getContractReviews(UUID contractId) {
        return reviewDAO.findByContract(contractId);
    }
    
    /**
     * Обновление среднего рейтинга игрока
     */
    public CompletableFuture<Boolean> updateAverageRating(UUID playerUuid) {
        return reviewDAO.calculateAverageRating(playerUuid)
            .thenCompose(avgRating -> reputationDAO.updateRating(playerUuid, avgRating))
            .whenComplete((result, error) -> {
                if (error == null) {
                    logger.fine("Updated average rating for player: " + playerUuid);
                }
            });
    }
    
    /**
     * Получение статистики репутации
     */
    public CompletableFuture<ReputationStats> getReputationStats(UUID playerUuid) {
        return getReputation(playerUuid)
            .thenCombine(
                reviewDAO.countByReviewee(playerUuid),
                (reputation, reviewCount) -> new ReputationStats(
                    reputation.getRating(),
                    reputation.getTotalCompleted(),
                    reputation.getTotalFailed(),
                    reputation.getTotalCreated(),
                    reputation.getPoints(),
                    reviewCount,
                    reputation.getSuccessRate()
                )
            );
    }
    
    /**
     * Сброс репутации (админ функция)
     */
    public CompletableFuture<Boolean> resetReputation(UUID playerUuid) {
        return reputationDAO.reset(playerUuid)
            .whenComplete((result, error) -> {
                if (error == null) {
                    logger.info("Reputation reset for player: " + playerUuid);
                }
            });
    }
    
    /**
     * Установка рейтинга (админ функция)
     */
    public CompletableFuture<Boolean> setRating(UUID playerUuid, double rating) {
        if (rating < 1.0 || rating > 5.0) {
            logger.warning("Invalid rating: " + rating);
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Rating must be between 1.0 and 5.0")
            );
        }
        
        return reputationDAO.updateRating(playerUuid, rating)
            .whenComplete((result, error) -> {
                if (error == null) {
                    logger.info("Rating set for player: " + playerUuid + " -> " + rating);
                }
            });
    }
    
    /**
     * Проверка минимального количества контрактов для отображения рейтинга
     */
    public CompletableFuture<Boolean> hasMinimumContracts(UUID playerUuid, int minimum) {
        return getReputation(playerUuid)
            .thenApply(reputation -> reputation.hasMinimumContracts(minimum));
    }
    
    /**
     * Класс для статистики репутации
     */
    public static class ReputationStats {
        private final double rating;
        private final int totalCompleted;
        private final int totalFailed;
        private final int totalCreated;
        private final int points;
        private final int reviewCount;
        private final double successRate;
        
        public ReputationStats(
            double rating,
            int totalCompleted,
            int totalFailed,
            int totalCreated,
            int points,
            int reviewCount,
            double successRate
        ) {
            this.rating = rating;
            this.totalCompleted = totalCompleted;
            this.totalFailed = totalFailed;
            this.totalCreated = totalCreated;
            this.points = points;
            this.reviewCount = reviewCount;
            this.successRate = successRate;
        }
        
        public double getRating() { return rating; }
        public int getTotalCompleted() { return totalCompleted; }
        public int getTotalFailed() { return totalFailed; }
        public int getTotalCreated() { return totalCreated; }
        public int getPoints() { return points; }
        public int getReviewCount() { return reviewCount; }
        public double getSuccessRate() { return successRate; }
    }
}
