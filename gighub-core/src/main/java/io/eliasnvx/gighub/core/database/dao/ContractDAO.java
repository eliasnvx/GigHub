package io.eliasnvx.gighub.core.database.dao;

import io.eliasnvx.gighub.core.database.DatabaseManager;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractStatus;
import io.eliasnvx.gighub.core.model.ContractType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DAO для работы с контрактами в БД
 */
public class ContractDAO {
    
    private final DatabaseManager database;
    
    public ContractDAO(DatabaseManager database) {
        this.database = database;
    }
    
    /**
     * Создание контракта
     */
    public CompletableFuture<Contract> create(Contract contract) {
        return database.executeAsync(
            """
            INSERT INTO contracts (
                id, owner_uuid, contractor_uuid, type, title, description,
                reward, status, created_at, deadline, completed_at, auto_verify,
                featured, priority, location_x, location_y, location_z, world
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            stmt -> {
                try {
                    stmt.setString(1, contract.getId().toString());
                    stmt.setString(2, contract.getOwnerUuid().toString());
                    stmt.setString(3, contract.getContractorUuid() != null ? 
                        contract.getContractorUuid().toString() : null);
                    stmt.setString(4, contract.getType().getId());
                    stmt.setString(5, contract.getTitle());
                    stmt.setString(6, contract.getDescription());
                    stmt.setDouble(7, contract.getReward());
                    stmt.setString(8, contract.getStatus().name());
                    stmt.setLong(9, contract.getCreatedAt());
                    stmt.setObject(10, contract.getDeadline());
                    stmt.setObject(11, contract.getCompletedAt());
                    stmt.setBoolean(12, contract.isAutoVerify());
                    stmt.setBoolean(13, contract.isFeatured());
                    stmt.setBoolean(14, contract.isPriority());
                    stmt.setObject(15, contract.getLocationX());
                    stmt.setObject(16, contract.getLocationY());
                    stmt.setObject(17, contract.getLocationZ());
                    stmt.setString(18, contract.getWorld());
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create contract", e);
                }
            }
        ).thenApply(v -> contract);
    }
    
    /**
     * Получение контракта по ID
     */
    public CompletableFuture<Optional<Contract>> findById(UUID id) {
        return database.queryAsync(
            "SELECT * FROM contracts WHERE id = ?",
            stmt -> {
                try {
                    stmt.setString(1, id.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            rs -> {
                try {
                    if (rs.next()) {
                        return Optional.of(mapContract(rs));
                    }
                    return Optional.empty();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to fetch contract", e);
                }
            }
        );
    }
    
    /**
     * Получение всех активных контрактов
     */
    public CompletableFuture<List<Contract>> findActive() {
        return database.queryAsync(
            "SELECT * FROM contracts WHERE status IN ('OPEN', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED') ORDER BY created_at DESC",
            stmt -> {},
            this::mapContracts
        );
    }
    
    /**
     * Получение контрактов игрока как заказчика
     */
    public CompletableFuture<List<Contract>> findByOwner(UUID ownerUuid) {
        return database.queryAsync(
            "SELECT * FROM contracts WHERE owner_uuid = ? ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, ownerUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapContracts
        );
    }
    
    /**
     * Получение контрактов игрока как исполнителя
     */
    public CompletableFuture<List<Contract>> findByContractor(UUID contractorUuid) {
        return database.queryAsync(
            "SELECT * FROM contracts WHERE contractor_uuid = ? ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, contractorUuid.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapContracts
        );
    }
    
    /**
     * Получение контрактов по типу
     */
    public CompletableFuture<List<Contract>> findByType(ContractType type) {
        return database.queryAsync(
            "SELECT * FROM contracts WHERE type = ? AND status = 'OPEN' ORDER BY created_at DESC",
            stmt -> {
                try {
                    stmt.setString(1, type.getId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapContracts
        );
    }
    
    /**
     * Обновление контракта
     */
    public CompletableFuture<Boolean> update(Contract contract) {
        return database.executeAsync(
            """
            UPDATE contracts SET
                contractor_uuid = ?, status = ?, completed_at = ?, auto_verify = ?,
                location_x = ?, location_y = ?, location_z = ?, world = ?
            WHERE id = ?
            """,
            stmt -> {
                try {
                    stmt.setString(1, contract.getContractorUuid() != null ? 
                        contract.getContractorUuid().toString() : null);
                    stmt.setString(2, contract.getStatus().name());
                    stmt.setLong(3, contract.getCompletedAt() != null ? 
                        contract.getCompletedAt() : System.currentTimeMillis());
                    stmt.setBoolean(4, contract.isAutoVerify());
                    stmt.setInt(5, contract.getLocationX() != null ? contract.getLocationX() : 0);
                    stmt.setInt(6, contract.getLocationY() != null ? contract.getLocationY() : 0);
                    stmt.setInt(7, contract.getLocationZ() != null ? contract.getLocationZ() : 0);
                    stmt.setString(8, contract.getWorld());
                    stmt.setString(9, contract.getId().toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Принятие контракта с DB-level optimistic locking
     */
    public CompletableFuture<Boolean> acceptContractOptimistic(UUID contractId, UUID contractorUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                UPDATE contracts 
                SET contractor_uuid = ?, status = 'IN_PROGRESS' 
                WHERE id = ? AND status = 'OPEN' AND contractor_uuid IS NULL AND owner_uuid != ?
                """;
            
            try (var conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, contractorUuid.toString());
                stmt.setString(2, contractId.toString());
                stmt.setString(3, contractorUuid.toString());
                
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to accept contract", e);
            }
        });
    }
    
    /**
     * Удаление контракта
     */
    public CompletableFuture<Boolean> delete(UUID id) {
        return database.executeAsync(
            "DELETE FROM contracts WHERE id = ?",
            stmt -> {
                try {
                    stmt.setString(1, id.toString());
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to delete contract", e);
                }
            }
        ).thenApply(v -> true);
    }
    
    /**
     * Получение количества активных контрактов игрока
     */
    public CompletableFuture<Integer> countActiveByPlayer(UUID playerUuid) {
        return database.queryAsync(
            """
            SELECT COUNT(*) as count FROM contracts 
            WHERE (owner_uuid = ? OR contractor_uuid = ?) 
            AND status IN ('OPEN', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED')
            """,
            stmt -> {
                try {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, playerUuid.toString());
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
     * Получение истекших контрактов
     */
    public CompletableFuture<List<Contract>> findExpired() {
        long now = System.currentTimeMillis();
        return database.queryAsync(
            """
            SELECT * FROM contracts 
            WHERE deadline IS NOT NULL 
            AND deadline < ? 
            AND status IN ('OPEN', 'ACCEPTED', 'IN_PROGRESS')
            """,
            stmt -> {
                try {
                    stmt.setLong(1, now);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            this::mapContracts
        );
    }
    
    /**
     * Маппинг ResultSet в Contract
     */
    private Contract mapContract(ResultSet rs) throws SQLException {
        String contractorUuidStr = rs.getString("contractor_uuid");
        
        // Для nullable полей используем getLong/getInt + wasNull() вместо getObject
        long deadlineValue = rs.getLong("deadline");
        Long deadline = rs.wasNull() ? null : deadlineValue;
        
        long completedAtValue = rs.getLong("completed_at");
        Long completedAt = rs.wasNull() ? null : completedAtValue;
        
        int locationXValue = rs.getInt("location_x");
        Integer locationX = rs.wasNull() ? null : locationXValue;
        
        int locationYValue = rs.getInt("location_y");
        Integer locationY = rs.wasNull() ? null : locationYValue;
        
        int locationZValue = rs.getInt("location_z");
        Integer locationZ = rs.wasNull() ? null : locationZValue;
        
        String world = rs.getString("world");
        
        return Contract.builder()
            .id(UUID.fromString(rs.getString("id")))
            .ownerUuid(UUID.fromString(rs.getString("owner_uuid")))
            .contractorUuid(contractorUuidStr != null ? UUID.fromString(contractorUuidStr) : null)
            .type(ContractType.fromId(rs.getString("type")))
            .title(rs.getString("title"))
            .description(rs.getString("description"))
            .reward(rs.getDouble("reward"))
            .status(ContractStatus.valueOf(rs.getString("status")))
            .createdAt(rs.getLong("created_at"))
            .deadline(deadline)
            .completedAt(completedAt)
            .autoVerify(rs.getBoolean("auto_verify"))
            .featured(rs.getBoolean("featured"))
            .priority(rs.getBoolean("priority"))
            .locationX(locationX)
            .locationY(locationY)
            .locationZ(locationZ)
            .world(world)
            .build();
    }
    
    /**
     * Маппинг ResultSet в список Contract
     */
    private List<Contract> mapContracts(ResultSet rs) {
        List<Contract> contracts = new ArrayList<>();
        try {
            while (rs.next()) {
                contracts.add(mapContract(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map contracts", e);
        }
        return contracts;
    }
}
