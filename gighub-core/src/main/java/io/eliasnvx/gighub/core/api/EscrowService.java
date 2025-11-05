package io.eliasnvx.gighub.core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис управления escrow (заморозка/разморозка средств)
 */
public interface EscrowService {
    
    /**
     * Заморозка средств при создании контракта
     * 
     * @param playerUuid UUID игрока
     * @param contractId UUID контракта
     * @param amount Сумма
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> freezeFunds(UUID playerUuid, UUID contractId, double amount);
    
    /**
     * Выплата средств исполнителю
     * 
     * @param contractId UUID контракта
     * @param contractorUuid UUID исполнителя
     * @param amount Сумма
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> releaseFunds(UUID contractId, UUID contractorUuid, double amount);
    
    /**
     * Возврат средств заказчику
     * 
     * @param contractId UUID контракта
     * @param ownerUuid UUID заказчика
     * @param amount Сумма
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> refundFunds(UUID contractId, UUID ownerUuid, double amount);
    
    /**
     * Проверка наличия средств у игрока
     * 
     * @param playerUuid UUID игрока
     * @param amount Сумма
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> hasBalance(UUID playerUuid, double amount);
    
    /**
     * Получение баланса игрока
     * 
     * @param playerUuid UUID игрока
     * @return CompletableFuture с балансом
     */
    CompletableFuture<Double> getBalance(UUID playerUuid);
    
    /**
     * Расчет комиссии платформы
     * 
     * @param amount Сумма
     * @return Сумма комиссии
     */
    double calculateCommission(double amount);
}
