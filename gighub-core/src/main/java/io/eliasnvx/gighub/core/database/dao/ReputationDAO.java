package io.eliasnvx.gighub.core.database.dao;

import io.eliasnvx.gighub.core.database.DatabaseManager;
import io.eliasnvx.gighub.core.model.UserReputation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DAO для работы с репутацией пользователей
 */
public class ReputationDAO {
    
    private final DatabaseManager database;
    
    public ReputationDAO(DatabaseManager database) {
        this.database = database;
    }
    
    /**
     * Создание или обновление репутации
     */
    public CompletableFuture<UserReputation> save(UserReputation reputation) {
        return database.executeAsync(
            """
            INSERT INTO user_reputation (
                uuid, rating, total_completed, total_failed, 
                total_created, points, last_updated
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                rating = excluded.rating,
                total_completed = excluded.total_completed,
                total_failed = excluded.total_failed,
                total_created = excluded.total_created,
                points = excluded.points,
                last_updated = excluded.last_updated
            """,
            stmt -> {
                try {
                    stmt.setString(1, reputation.getUuid().toString());
                    stmt.setDouble(2, reputation.getRating());
                    stmt.setInt(3, reputation.getTotalCompleted());
                    stmt.setInt(4, reputation.getTotalFailed());
                    stmt.setInt(5, reputation.getTotalCreated());
                    stmt.setInt(6, reputation.getPoints());
                    stmt.setLong(7, reputation.getLastUpdated());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to save reputation", e);
                }
            }
        ).thenApply(v -> reputation);
    }
    
    /**
     * Получение репутации игрока
     */
    public CompletableFuture<Optional<UserReputation>> findByUuid(UUID uuid) {
        return database.queryAsync(
            "SELECT * FROM user_reputation WHERE uuid = ?",
            stmt -> {
                try {
                    stmt.setString(1, uuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    if (rs.next()) {
                        return Optional.of(mapReputation(rs));
                    }
                    return Optional.empty();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to fetch reputation", e);
                }
            }
        );
    }
    
    /**
     * Получение или создание репутации (если не существует)
     */
    public CompletableFuture<UserReputation> getOrCreate(UUID uuid) {
        return findByUuid(uuid).thenCompose(optional -> {
            if (optional.isPresent()) {
                return CompletableFuture.completedFuture(optional.get());
            }
            
            // Создаем новую репутацию
            UserReputation newReputation = UserReputation.builder()
                .uuid(uuid)
                .rating(5.0)
                .totalCompleted(0)
                .totalFailed(0)
                .totalCreated(0)
                .points(0)
                .lastUpdated(System.currentTimeMillis())
                .build();
            
            return save(newReputation);
        });
    }
    
    /**
     * Увеличение счетчика завершенных контрактов
     */
    public CompletableFuture<Boolean> incrementCompleted(UUID uuid, int bonusPoints) {
        return database.executeAsync(
            """
            UPDATE user_reputation SET
                total_completed = total_completed + 1,
                points = points + ?,
                last_updated = ?
            WHERE uuid = ?
            """,
            stmt -> {
                try {
                    stmt.setInt(1, bonusPoints);
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.setString(3, uuid.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to increment completed", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Увеличение счетчика проваленных контрактов
     */
    public CompletableFuture<Boolean> incrementFailed(UUID uuid, int penaltyPoints) {
        return database.executeAsync(
            """
            UPDATE user_reputation SET
                total_failed = total_failed + 1,
                points = CASE 
                    WHEN points - ? < 0 THEN 0 
                    ELSE points - ? 
                END,
                last_updated = ?
            WHERE uuid = ?
            """,
            stmt -> {
                try {
                    stmt.setInt(1, penaltyPoints);
                    stmt.setInt(2, penaltyPoints);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, uuid.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to increment failed", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Увеличение счетчика созданных контрактов
     */
    public CompletableFuture<Boolean> incrementCreated(UUID uuid) {
        return database.executeAsync(
            """
            UPDATE user_reputation SET
                total_created = total_created + 1,
                last_updated = ?
            WHERE uuid = ?
            """,
            stmt -> {
                try {
                    stmt.setLong(1, System.currentTimeMillis());
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to increment created", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Обновление рейтинга
     */
    public CompletableFuture<Boolean> updateRating(UUID uuid, double newRating) {
        return database.executeAsync(
            """
            UPDATE user_reputation SET
                rating = ?,
                last_updated = ?
            WHERE uuid = ?
            """,
            stmt -> {
                try {
                    stmt.setDouble(1, Math.max(1.0, Math.min(5.0, newRating)));
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.setString(3, uuid.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update rating", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Сброс репутации
     */
    public CompletableFuture<Boolean> reset(UUID uuid) {
        return database.executeAsync(
            """
            UPDATE user_reputation SET
                rating = 5.0,
                total_completed = 0,
                total_failed = 0,
                total_created = 0,
                points = 0,
                last_updated = ?
            WHERE uuid = ?
            """,
            stmt -> {
                try {
                    stmt.setLong(1, System.currentTimeMillis());
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to reset reputation", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Маппинг ResultSet в UserReputation
     */
    private UserReputation mapReputation(ResultSet rs) throws SQLException {
        return UserReputation.builder()
            .uuid(UUID.fromString(rs.getString("uuid")))
            .rating(rs.getDouble("rating"))
            .totalCompleted(rs.getInt("total_completed"))
            .totalFailed(rs.getInt("total_failed"))
            .totalCreated(rs.getInt("total_created"))
            .points(rs.getInt("points"))
            .lastUpdated(rs.getLong("last_updated"))
            .build();
    }
}
