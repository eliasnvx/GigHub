package io.eliasnvx.gighub.core.database.dao;

import io.eliasnvx.gighub.core.database.DatabaseManager;
import io.eliasnvx.gighub.core.model.ContractReview;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DAO для работы с отзывами о контрактах
 */
public class ReviewDAO {
    
    private final DatabaseManager database;
    
    public ReviewDAO(DatabaseManager database) {
        this.database = database;
    }
    
    /**
     * Создание отзыва
     */
    public CompletableFuture<ContractReview> create(ContractReview review) {
        return database.queryAsync(
            """
            INSERT INTO contract_reviews (
                contract_id, reviewer_uuid, reviewee_uuid, 
                rating, comment, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            stmt -> {
                try {
                    stmt.setString(1, review.getContractId().toString());
                    stmt.setString(2, review.getReviewerUuid().toString());
                    stmt.setString(3, review.getRevieweeUuid().toString());
                    stmt.setInt(4, review.getRating());
                    stmt.setString(5, review.getComment());
                    stmt.setLong(6, review.getCreatedAt());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create review", e);
                }
            },
            rs -> {
                // Возвращаем созданный отзыв с ID
                return review;
            }
        );
    }
    
    /**
     * Получение отзывов по контракту
     */
    public CompletableFuture<List<ContractReview>> findByContract(UUID contractId) {
        return database.queryAsync(
            "SELECT * FROM contract_reviews WHERE contract_id = ? ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, contractId.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapReviews
        );
    }
    
    /**
     * Получение отзывов о игроке (как reviewee)
     */
    public CompletableFuture<List<ContractReview>> findByReviewee(UUID revieweeUuid) {
        return database.queryAsync(
            "SELECT * FROM contract_reviews WHERE reviewee_uuid = ? ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, revieweeUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapReviews
        );
    }
    
    /**
     * Получение отзывов от игрока (как reviewer)
     */
    public CompletableFuture<List<ContractReview>> findByReviewer(UUID reviewerUuid) {
        return database.queryAsync(
            "SELECT * FROM contract_reviews WHERE reviewer_uuid = ? ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, reviewerUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapReviews
        );
    }
    
    /**
     * Расчет среднего рейтинга игрока
     */
    public CompletableFuture<Double> calculateAverageRating(UUID revieweeUuid) {
        return database.queryAsync(
            "SELECT AVG(rating) as avg_rating FROM contract_reviews WHERE reviewee_uuid = ?",
            stmt -> {
                try {
                    stmt.setString(1, revieweeUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    if (rs.next()) {
                        double avg = rs.getDouble("avg_rating");
                        return rs.wasNull() ? 5.0 : avg; // Дефолтный рейтинг 5.0
                    }
                    return 5.0;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
    
    /**
     * Получение количества отзывов о игроке
     */
    public CompletableFuture<Integer> countByReviewee(UUID revieweeUuid) {
        return database.queryAsync(
            "SELECT COUNT(*) as count FROM contract_reviews WHERE reviewee_uuid = ?",
            stmt -> {
                try {
                    stmt.setString(1, revieweeUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                    return 0;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
    
    /**
     * Проверка существования отзыва
     */
    public CompletableFuture<Boolean> exists(UUID contractId, UUID reviewerUuid) {
        return database.queryAsync(
            "SELECT COUNT(*) as count FROM contract_reviews WHERE contract_id = ? AND reviewer_uuid = ?",
            stmt -> {
                try {
                    stmt.setString(1, contractId.toString());
                    stmt.setString(2, reviewerUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("count") > 0;
                    }
                    return false;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
    
    /**
     * Удаление отзыва
     */
    public CompletableFuture<Boolean> delete(int reviewId) {
        return database.executeAsync(
            "DELETE FROM contract_reviews WHERE id = ?",
            stmt -> {
                try {
                    stmt.setInt(1, reviewId);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to delete review", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Маппинг ResultSet в ContractReview
     */
    private ContractReview mapReview(ResultSet rs) throws SQLException {
        return ContractReview.builder()
            .id(rs.getInt("id"))
            .contractId(UUID.fromString(rs.getString("contract_id")))
            .reviewerUuid(UUID.fromString(rs.getString("reviewer_uuid")))
            .revieweeUuid(UUID.fromString(rs.getString("reviewee_uuid")))
            .rating(rs.getInt("rating"))
            .comment(rs.getString("comment"))
            .createdAt(rs.getLong("created_at"))
            .build();
    }
    
    /**
     * Маппинг ResultSet в список ContractReview
     */
    private List<ContractReview> mapReviews(ResultSet rs) {
        List<ContractReview> reviews = new ArrayList<>();
        try {
            while (rs.next()) {
                reviews.add(mapReview(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map reviews", e);
        }
        return reviews;
    }
}
