package io.eliasnvx.gighub.bukkit.manager;

import io.eliasnvx.gighub.core.api.EscrowService;
import io.eliasnvx.gighub.core.database.dao.EscrowDAO;
import io.eliasnvx.gighub.core.model.EscrowTransaction;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Менеджер escrow системы через Vault
 */
public class EscrowManager implements EscrowService {
    
    private final Logger logger;
    private final double commissionPercentage;
    private final EscrowDAO escrowDAO;
    private Economy economy;
    private boolean vaultEnabled = false;
    
    // Кэш замороженных средств: contractId -> amount (write-through cache)
    private final Map<UUID, Double> frozenFunds = new ConcurrentHashMap<>();
    
    public EscrowManager(Logger logger, double commissionPercentage, EscrowDAO escrowDAO) {
        this.logger = logger;
        this.commissionPercentage = commissionPercentage;
        this.escrowDAO = escrowDAO;
        setupEconomy();
        loadExistingEscrows();
    }
    
    /**
     * Настройка Vault Economy
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Vault not found! Economy features disabled.");
            vaultEnabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = 
            Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (rsp == null) {
            logger.warning("No economy provider found! Economy features disabled.");
            vaultEnabled = false;
            return;
        }
        
        economy = rsp.getProvider();
        vaultEnabled = true;
        logger.info("Vault economy integration enabled: " + economy.getName());
    }
    
    /**
     * Загрузка существующих эскроу из БД в кэш
     */
    private void loadExistingEscrows() {
        escrowDAO.findActive().thenAccept(escrows -> {
            int loaded = 0;
            for (EscrowTransaction escrow : escrows) {
                frozenFunds.put(escrow.getContractId(), escrow.getAmount());
                loaded++;
            }
            logger.info("Loaded " + loaded + " active escrow transactions from database");
        }).exceptionally(e -> {
            logger.severe("Failed to load existing escrows: " + e.getMessage());
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> freezeFunds(UUID playerUuid, UUID contractId, double amount) {
        if (!vaultEnabled) {
            logger.warning("Vault not enabled, cannot freeze funds");
            return CompletableFuture.completedFuture(false);
        }
        
        // Проверяем, не заморожены ли уже средства
        if (frozenFunds.containsKey(contractId)) {
            logger.warning("Funds already frozen for contract: " + contractId);
            return CompletableFuture.completedFuture(false);
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
        
        // Фаза 1: Проверяем баланс
        if (!economy.has(player, amount)) {
            logger.warning("Player " + playerUuid + " has insufficient funds: " + amount);
            return CompletableFuture.completedFuture(false);
        }
        
        // Фаза 2: Снимаем деньги из Vault
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        
        if (!response.transactionSuccess()) {
            logger.severe("Failed to withdraw funds: " + response.errorMessage);
            return CompletableFuture.completedFuture(false);
        }
        
        // Фаза 3: Создаем запись в БД
        double commission = calculateCommission(amount);
        EscrowTransaction escrow = EscrowTransaction.builder()
            .contractId(contractId)
            .ownerUuid(playerUuid)
            .amount(amount)
            .commission(commission)
            .status(EscrowTransaction.EscrowStatus.FROZEN)
            .frozenAt(System.currentTimeMillis())
            .build();
        
        return escrowDAO.create(escrow)
            .thenCompose(created -> {
                // Фаза 4: Обновляем кэш (write-through)
                frozenFunds.put(contractId, amount);
                
                logger.info("Successfully frozen " + amount + " for contract " + contractId + 
                    " (commission: " + commission + ")");
                return CompletableFuture.completedFuture(true);
            })
            .exceptionally(e -> {
                // Компенсирующая транзакция: возвращаем деньги в Vault
                logger.severe("Failed to create escrow record, refunding: " + e.getMessage());
                economy.depositPlayer(player, amount);
                return false;
            });
    }
    
    @Override
    public CompletableFuture<Boolean> releaseFunds(UUID contractId, UUID contractorUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultEnabled) {
                logger.warning("Vault not enabled, cannot release funds");
                return false;
            }
            
            // Проверяем наличие в кэше
            Double frozenAmount = frozenFunds.get(contractId);
            if (frozenAmount == null) {
                logger.warning("No frozen funds for contract: " + contractId);
                return false;
            }
            
            // Фаза 1: Обновляем статус в БД
            try {
                boolean updated = escrowDAO.updateStatus(contractId, 
                    EscrowTransaction.EscrowStatus.RELEASED, System.currentTimeMillis()).get();
                
                if (!updated) {
                    logger.warning("Failed to update escrow status for contract: " + contractId);
                    return false;
                }
                
            } catch (Exception e) {
                logger.severe("Failed to update escrow in DB: " + e.getMessage());
                return false;
            }
            
            // Фаза 2: Выплачиваем исполнителю (комиссия уже в БД)
            EscrowTransaction escrow;
            try {
                escrow = escrowDAO.findByContractId(contractId).get();
                if (escrow == null) {
                    logger.severe("Escrow not found for contract: " + contractId);
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Failed to fetch escrow: " + e.getMessage());
                return false;
            }
            
            double payoutAmount = escrow.getPayoutAmount();
            OfflinePlayer contractor = Bukkit.getOfflinePlayer(contractorUuid);
            
            EconomyResponse response = economy.depositPlayer(contractor, payoutAmount);
            
            if (!response.transactionSuccess()) {
                logger.severe("Failed to deposit funds: " + response.errorMessage);
                // Rollback: возвращаем статус в FROZEN
                escrowDAO.updateStatus(contractId, EscrowTransaction.EscrowStatus.FROZEN, 
                    escrow.getFrozenAt());
                return false;
            }
            
            // Фаза 3: Обновляем кэш
            frozenFunds.remove(contractId);
            
            logger.info("Released " + payoutAmount + " to " + contractorUuid + 
                " (commission: " + escrow.getCommission() + ")");
            
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> refundFunds(UUID contractId, UUID ownerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultEnabled) {
                logger.warning("Vault not enabled, cannot refund funds");
                return false;
            }
            
            // Проверяем наличие в кэше
            Double frozenAmount = frozenFunds.get(contractId);
            if (frozenAmount == null) {
                logger.warning("No frozen funds for contract: " + contractId);
                return false;
            }
            
            // Фаза 1: Обновляем статус в БД
            try {
                boolean updated = escrowDAO.updateStatus(contractId, 
                    EscrowTransaction.EscrowStatus.REFUNDED, System.currentTimeMillis()).get();
                
                if (!updated) {
                    logger.warning("Failed to update escrow status for contract: " + contractId);
                    return false;
                }
                
            } catch (Exception e) {
                logger.severe("Failed to update escrow in DB: " + e.getMessage());
                return false;
            }
            
            // Фаза 2: Возвращаем деньги заказчику
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            
            EconomyResponse response = economy.depositPlayer(owner, frozenAmount);
            
            if (!response.transactionSuccess()) {
                logger.severe("Failed to refund funds: " + response.errorMessage);
                // Rollback: возвращаем статус в FROZEN
                escrowDAO.updateStatus(contractId, EscrowTransaction.EscrowStatus.FROZEN, 
                    System.currentTimeMillis());
                return false;
            }
            
            // Фаза 3: Обновляем кэш
            frozenFunds.remove(contractId);
            
            logger.info("Refunded " + frozenAmount + " to " + ownerUuid);
            
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> hasBalance(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultEnabled) {
                return false;
            }
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.has(player, amount);
        });
    }
    
    @Override
    public CompletableFuture<Double> getBalance(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultEnabled) {
                return 0.0;
            }
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.getBalance(player);
        });
    }
    
    @Override
    public double calculateCommission(double amount) {
        return amount * (commissionPercentage / 100.0);
    }
    
    /**
     * Получение процента комиссии
     */
    public double getCommissionPercentage() {
        return commissionPercentage;
    }
    
    /**
     * Получение замороженных средств по контракту
     */
    public double getFrozenAmount(UUID contractId) {
        return frozenFunds.getOrDefault(contractId, 0.0);
    }
    
    /**
     * Проверка наличия Vault
     */
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    /**
     * Форматирование суммы
     */
    public String format(double amount) {
        if (vaultEnabled && economy != null) {
            return economy.format(amount);
        }
        return String.format("$%.2f", amount);
    }
    
    /**
     * Получение имени валюты
     */
    public String getCurrencyName(boolean plural) {
        if (vaultEnabled && economy != null) {
            return plural ? economy.currencyNamePlural() : economy.currencyNameSingular();
        }
        return plural ? "dollars" : "dollar";
    }
    
    /**
     * Размораживает средства (удаляет запись эскроу)
     */
    public CompletableFuture<Boolean> unfreezeFunds(UUID playerUuid, UUID contractId) {
        // Удаляем из памяти (синхронная операция)
        frozenFunds.remove(contractId);
        
        // Удаляем из базы данных (асинхронная операция)
        return escrowDAO.delete(contractId)
            .exceptionally(e -> {
                logger.severe("Failed to unfreeze funds: " + e.getMessage());
                return false;
            });
    }
    
    /**
     * Обновляет ID контракта в записи эскроу
     */
    public CompletableFuture<Boolean> updateEscrowContractId(UUID tempContractId, UUID realContractId) {
        // Обновляем в памяти (синхронная операция)
        Double amount = frozenFunds.remove(tempContractId);
        if (amount != null) {
            frozenFunds.put(realContractId, amount);
        }
        
        // Обновляем в базе данных (асинхронная операция)
        return escrowDAO.updateContractId(tempContractId, realContractId)
            .exceptionally(e -> {
                logger.severe("Failed to update escrow contract ID: " + e.getMessage());
                logger.severe("Temp contract ID: " + tempContractId + ", Real contract ID: " + realContractId);
                e.printStackTrace();
                return false;
            });
    }
    
    /**
     * Взимает комиссию за создание контракта
     */
    public CompletableFuture<Boolean> chargeCreationFee(UUID playerUuid, double fee) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultEnabled || fee <= 0) {
                return true; // Если Vault отключен или комиссия 0, пропускаем
            }
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            
            // Проверяем баланс
            if (!economy.has(player, fee)) {
                logger.warning("Player " + playerUuid + " doesn't have enough balance for creation fee: " + fee);
                return false;
            }
            
            // Снимаем комиссию
            EconomyResponse response = economy.withdrawPlayer(player, fee);
            if (response.transactionSuccess()) {
                logger.info("Charged creation fee of " + fee + " from player " + playerUuid);
                return true;
            } else {
                logger.warning("Failed to charge creation fee: " + response.errorMessage);
                return false;
            }
        });
    }
    
    /**
     * Взимает плату за featured контракт
     */
    public CompletableFuture<Boolean> chargeFeaturedCost(UUID playerUuid, double cost) {
        return chargeCreationFee(playerUuid, cost); // Используем ту же логику
    }
    
    /**
     * Взимает плату за priority контракт
     */
    public CompletableFuture<Boolean> chargePriorityCost(UUID playerUuid, double cost) {
        return chargeCreationFee(playerUuid, cost); // Используем ту же логику
    }
    
    /**
     * Получает экземпляр Economy
     */
    public Economy getEconomy() {
        return economy;
    }
}
