package io.eliasnvx.gighub.core.database.dao;

import io.eliasnvx.gighub.core.database.DatabaseManager;
import io.eliasnvx.gighub.core.model.EscrowTransaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO для работы с эскроу транзакциями
 */
public class EscrowDAO {
    
    private final DatabaseManager database;
    private final Logger logger;
    
    public EscrowDAO(DatabaseManager database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }
    
    /**
     * Создание эскроу транзакции
     */
    public CompletableFuture<EscrowTransaction> create(EscrowTransaction escrow) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO escrow_transactions (
                    contract_id, owner_uuid, amount, commission, status, frozen_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
            
            try (var conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, escrow.getContractId().toString());
                stmt.setString(2, escrow.getOwnerUuid().toString());
                stmt.setDouble(3, escrow.getAmount());
                stmt.setDouble(4, escrow.getCommission());
                stmt.setString(5, escrow.getStatus().name());
                stmt.setLong(6, escrow.getFrozenAt());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating escrow failed, no rows affected.");
                }
                
                // Получаем ID
                try (var generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        escrow.setId(generatedKeys.getLong(1));
                    }
                }
                
                logger.info("Created escrow transaction for contract " + escrow.getContractId());
                return escrow;
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create escrow transaction", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Поиск эскроу по ID контракта
     */
    public CompletableFuture<EscrowTransaction> findByContractId(UUID contractId) {
        return database.queryAsync(
            "SELECT * FROM escrow_transactions WHERE contract_id = ?",
            stmt -> {
                try {
                    stmt.setString(1, contractId.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    return mapEscrows(rs);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        ).thenApply(list -> list.isEmpty() ? null : list.get(0));
    }
    
    /**
     * Обновление статуса эскроу
     */
    public CompletableFuture<Boolean> updateStatus(UUID contractId, EscrowTransaction.EscrowStatus status, long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = String.format("""
                UPDATE escrow_transactions 
                SET status = ?, %s = ? 
                WHERE contract_id = ?
                """, 
                status == EscrowTransaction.EscrowStatus.RELEASED ? "released_at" :
                status == EscrowTransaction.EscrowStatus.REFUNDED ? "refunded_at" : "frozen_at"
            );
            
            try (var conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, status.name());
                stmt.setLong(2, timestamp);
                stmt.setString(3, contractId.toString());
                
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update escrow status", e);
                return false;
            }
        });
    }
    
    /**
     * Удаление эскроу транзакции
     */
    public CompletableFuture<Boolean> delete(UUID contractId) {
        return CompletableFuture.supplyAsync(() -> {
            try (var conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM escrow_transactions WHERE contract_id = ?")) {
                
                stmt.setString(1, contractId.toString());
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to delete escrow transaction", e);
                return false;
            }
        });
    }
    
    /**
     * Получение всех эскроу игрока
     */
    public CompletableFuture<List<EscrowTransaction>> findByOwner(UUID ownerUuid) {
        return database.queryAsync(
            "SELECT * FROM escrow_transactions WHERE owner_uuid = ? ORDER BY frozen_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, ownerUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    return mapEscrows(rs);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
    
    /**
     * Получение всех активных эскроу (статус FROZEN)
     */
    public CompletableFuture<List<EscrowTransaction>> findActive() {
        return database.queryAsync(
            "SELECT * FROM escrow_transactions WHERE status = 'FROZEN' ORDER BY frozen_at DESC",
            stmt -> {},
            rs -> {
                try {
                    return mapEscrows(rs);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
    
    /**
     * Маппинг ResultSet в EscrowTransaction
     */
    private EscrowTransaction mapEscrow(ResultSet rs) throws SQLException {
        // Для nullable полей используем getLong + wasNull() как в ContractDAO
        long releasedAtValue = rs.getLong("released_at");
        Long releasedAt = rs.wasNull() ? null : releasedAtValue;
        
        long refundedAtValue = rs.getLong("refunded_at");
        Long refundedAt = rs.wasNull() ? null : refundedAtValue;
        
        return EscrowTransaction.builder()
            .id(rs.getLong("id"))
            .contractId(UUID.fromString(rs.getString("contract_id")))
            .ownerUuid(UUID.fromString(rs.getString("owner_uuid")))
            .amount(rs.getDouble("amount"))
            .commission(rs.getDouble("commission"))
            .status(EscrowTransaction.EscrowStatus.valueOf(rs.getString("status")))
            .frozenAt(rs.getLong("frozen_at"))
            .releasedAt(releasedAt)
            .refundedAt(refundedAt)
            .build();
    }
    
    /**
     * Маппинг ResultSet в List<EscrowTransaction>
     */
    private List<EscrowTransaction> mapEscrows(ResultSet rs) throws SQLException {
        var escrows = new java.util.ArrayList<EscrowTransaction>();
        while (rs.next()) {
            escrows.add(mapEscrow(rs));
        }
        return escrows;
    }
    
    /**
     * Удаляет временную escrow транзакцию (реальная уже создана системой)
     */
    public CompletableFuture<Boolean> updateContractId(UUID tempContractId, UUID realContractId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM escrow_transactions WHERE contract_id = ?";
            
            try (var conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, tempContractId.toString());
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete temporary escrow transaction", e);
            }
        });
    }
}
