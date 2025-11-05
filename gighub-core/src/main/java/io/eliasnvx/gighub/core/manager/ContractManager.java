package io.eliasnvx.gighub.core.manager;

import io.eliasnvx.gighub.core.api.EscrowService;
import io.eliasnvx.gighub.core.database.dao.ContractDAO;
import io.eliasnvx.gighub.core.database.dao.ReputationDAO;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractStatus;
import io.eliasnvx.gighub.core.model.ContractType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Менеджер для управления контрактами
 */
public class ContractManager {
    
    private final Logger logger;
    private final ContractDAO contractDAO;
    private final ReputationDAO reputationDAO;
    private final EscrowService escrowService;
    
    public ContractManager(Logger logger, ContractDAO contractDAO, ReputationDAO reputationDAO, EscrowService escrowService) {
        this.logger = logger;
        this.contractDAO = contractDAO;
        this.reputationDAO = reputationDAO;
        this.escrowService = escrowService;
    }
    
    /**
     * Создание нового контракта с эскроу
     */
    public CompletableFuture<Contract> createContract(
        UUID ownerUuid,
        ContractType type,
        String title,
        String description,
        double reward,
        Long deadline
    ) {
        UUID contractId = UUID.randomUUID();
        
        // Фаза 1: Создаем контракт в БД (escrow уже создан в GUI)
        Contract contract = Contract.builder()
            .id(contractId)
            .ownerUuid(ownerUuid)
            .type(type)
            .title(title)
            .description(description)
            .reward(reward)
            .status(ContractStatus.OPEN)
            .createdAt(System.currentTimeMillis())
            .deadline(deadline)
            .autoVerify(type.isAutoVerify())
            .build();
        
        // Фаза 2: Сохраняем контракт и обновляем репутацию
        return contractDAO.create(contract)
            .thenCompose(created -> {
                // Обновляем репутацию
                return reputationDAO.getOrCreate(ownerUuid)
                    .thenCompose(rep -> reputationDAO.incrementCreated(ownerUuid))
                    .thenApply(v -> created);
            })
            .exceptionally(error -> {
                // Rollback: возвращаем средства если создание контракта не удалось
                logger.severe("Contract creation failed, refunding: " + error.getMessage());
                escrowService.refundFunds(contractId, ownerUuid, reward);
                throw new RuntimeException("Failed to create contract", error);
            })
            .whenComplete((result, error) -> {
                if (error != null) {
                    logger.severe("Failed to create contract: " + error.getMessage());
                } else {
                    logger.info("Contract created with escrow: " + result.getId() + " by " + ownerUuid);
                }
            });
    }
    
    /**
     * Получение контракта по ID
     */
    public CompletableFuture<Optional<Contract>> getContract(UUID contractId) {
        return contractDAO.findById(contractId);
    }
    
    /**
     * Получение всех активных контрактов
     */
    public CompletableFuture<List<Contract>> getActiveContracts() {
        return contractDAO.findActive();
    }
    
    /**
     * Получение контрактов по типу
     */
    public CompletableFuture<List<Contract>> getContractsByType(ContractType type) {
        return contractDAO.findByType(type);
    }
    
    /**
     * Получение контрактов игрока
     */
    public CompletableFuture<List<Contract>> getPlayerContracts(UUID playerUuid, boolean asOwner) {
        if (asOwner) {
            return contractDAO.findByOwner(playerUuid);
        } else {
            return contractDAO.findByContractor(playerUuid);
        }
    }
    
    /**
     * Принятие контракта с DB-level optimistic locking
     */
    public CompletableFuture<Boolean> acceptContract(UUID contractId, UUID contractorUuid) {
        return contractDAO.acceptContractOptimistic(contractId, contractorUuid)
            .thenCompose(success -> {
                if (!success) {
                    logger.warning("Contract cannot be accepted (race condition or invalid): " + contractId);
                    return CompletableFuture.completedFuture(false);
                }
                
                logger.info("Contract accepted: " + contractId + " by " + contractorUuid);
                return CompletableFuture.completedFuture(true);
            });
    }
    
    /**
     * Завершение контракта с выплатой через эскроу
     */
    public CompletableFuture<Boolean> completeContract(UUID contractId) {
        return contractDAO.findById(contractId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                Contract contract = optional.get();
                
                if (!contract.canBeCompleted()) {
                    logger.warning("Contract cannot be completed: " + contractId);
                    return CompletableFuture.completedFuture(false);
                }
                
                // Обновляем статус контракта
                contract.setStatus(ContractStatus.COMPLETED);
                contract.setCompletedAt(System.currentTimeMillis());
                
                return contractDAO.update(contract)
                    .thenCompose(updated -> {
                        if (!updated) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // Выплачиваем средства через эскроу
                        UUID contractorUuid = contract.getContractorUuid();
                        if (contractorUuid != null) {
                            return escrowService.releaseFunds(contractId, contractorUuid, contract.getReward())
                                .whenComplete((success, error) -> {
                                    if (error != null) {
                                        logger.severe("Failed to release escrow funds: " + error.getMessage());
                                    } else if (success) {
                                        logger.info("Contract completed and funds released: " + contractId);
                                    }
                                });
                        }
                        
                        return CompletableFuture.completedFuture(true);
                    });
            });
    }
    
    /**
     * Верификация контракта (подтверждение заказчиком)
     */
    public CompletableFuture<Boolean> verifyContract(UUID contractId, int successBonus) {
        return contractDAO.findById(contractId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                Contract contract = optional.get();
                
                if (contract.getStatus() != ContractStatus.COMPLETED) {
                    logger.warning("Contract not completed: " + contractId);
                    return CompletableFuture.completedFuture(false);
                }
                
                contract.setStatus(ContractStatus.VERIFIED);
                
                // Обновляем репутацию исполнителя
                UUID contractorUuid = contract.getContractorUuid();
                if (contractorUuid != null) {
                    return contractDAO.update(contract)
                        .thenCompose(v -> reputationDAO.getOrCreate(contractorUuid))
                        .thenCompose(rep -> reputationDAO.incrementCompleted(contractorUuid, successBonus))
                        .whenComplete((result, error) -> {
                            if (error == null) {
                                logger.info("Contract verified: " + contractId);
                            }
                        });
                }
                
                return contractDAO.update(contract);
            });
    }
    
    /**
     * Отмена контракта с возвратом средств через эскроу
     */
    public CompletableFuture<Boolean> cancelContract(UUID contractId) {
        return contractDAO.findById(contractId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                Contract contract = optional.get();
                
                if (!contract.canBeCancelled()) {
                    logger.warning("Contract cannot be cancelled: " + contractId);
                    return CompletableFuture.completedFuture(false);
                }
                
                // Обновляем статус контракта
                contract.setStatus(ContractStatus.CANCELLED);
                
                return contractDAO.update(contract)
                    .thenCompose(updated -> {
                        if (!updated) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // Возвращаем средства через эскроу
                        return escrowService.refundFunds(contractId, contract.getOwnerUuid(), contract.getReward())
                            .whenComplete((success, error) -> {
                                if (error != null) {
                                    logger.severe("Failed to refund escrow funds: " + error.getMessage());
                                } else if (success) {
                                    logger.info("Contract cancelled and funds refunded: " + contractId);
                                }
                            });
                    });
            });
    }
    
    /**
     * Провал контракта с возвратом средств через эскроу
     */
    public CompletableFuture<Boolean> failContract(UUID contractId, int failurePenalty) {
        return contractDAO.findById(contractId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                Contract contract = optional.get();
                contract.setStatus(ContractStatus.EXPIRED);
                
                return contractDAO.update(contract)
                    .thenCompose(updated -> {
                        if (!updated) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // Возвращаем средства через эскроу
                        return escrowService.refundFunds(contractId, contract.getOwnerUuid(), contract.getReward())
                            .thenCompose(refundSuccess -> {
                                // Штраф для исполнителя, если он был назначен
                                UUID contractorUuid = contract.getContractorUuid();
                                if (contractorUuid != null) {
                                    return reputationDAO.getOrCreate(contractorUuid)
                                        .thenCompose(rep -> reputationDAO.incrementFailed(contractorUuid, failurePenalty))
                                        .thenApply(v -> refundSuccess);
                                }
                                return CompletableFuture.completedFuture(refundSuccess);
                            })
                            .whenComplete((success, error) -> {
                                if (error != null) {
                                    logger.severe("Failed to process contract failure: " + error.getMessage());
                                } else {
                                    logger.info("Contract failed and funds refunded: " + contractId);
                                }
                            });
                    });
            });
    }
    
    /**
     * Получение количества активных контрактов игрока
     */
    public CompletableFuture<Integer> getActiveContractCount(UUID playerUuid) {
        return contractDAO.countActiveByPlayer(playerUuid);
    }
    
    /**
     * Очистка истекших контрактов
     */
    public CompletableFuture<Integer> cleanupExpiredContracts(int failurePenalty) {
        return contractDAO.findExpired()
            .thenCompose(expiredContracts -> {
                if (expiredContracts.isEmpty()) {
                    return CompletableFuture.completedFuture(0);
                }
                
                logger.info("Found " + expiredContracts.size() + " expired contracts");
                
                // Обрабатываем каждый истекший контракт
                List<CompletableFuture<Boolean>> futures = expiredContracts.stream()
                    .map(contract -> failContract(contract.getId(), failurePenalty))
                    .toList();
                
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> expiredContracts.size());
            })
            .whenComplete((count, error) -> {
                if (error == null && count > 0) {
                    logger.info("Cleaned up " + count + " expired contracts");
                }
            });
    }
    
    /**
     * Удаление контракта (админ функция)
     */
    public CompletableFuture<Boolean> deleteContract(UUID contractId) {
        return contractDAO.delete(contractId)
            .whenComplete((result, error) -> {
                if (error == null) {
                    logger.info("Contract deleted: " + contractId);
                }
            });
    }
    
    /**
     * Проверка, может ли игрок создать новый контракт
     */
    public CompletableFuture<Boolean> canCreateContract(UUID playerUuid, int maxContracts) {
        return getActiveContractCount(playerUuid)
            .thenApply(count -> count < maxContracts);
    }
}
