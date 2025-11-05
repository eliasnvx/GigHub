package io.eliasnvx.gighub.core.api;

import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractReview;
import io.eliasnvx.gighub.core.model.ContractType;
import io.eliasnvx.gighub.core.model.UserReputation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Главный API интерфейс GigHub
 */
public interface GigHubAPI {
    
    // ==================== Contract Management ====================
    
    /**
     * Создание нового контракта
     * 
     * @param ownerUuid UUID заказчика
     * @param type Тип контракта
     * @param title Название
     * @param description Описание
     * @param reward Награда
     * @param deadline Дедлайн (nullable)
     * @return CompletableFuture с созданным контрактом
     */
    CompletableFuture<Contract> createContract(
        UUID ownerUuid,
        ContractType type,
        String title,
        String description,
        double reward,
        Long deadline
    );
    
    /**
     * Получение контракта по ID
     * 
     * @param contractId UUID контракта
     * @return CompletableFuture с Optional контракта
     */
    CompletableFuture<Optional<Contract>> getContract(UUID contractId);
    
    /**
     * Получение всех активных контрактов
     * 
     * @return CompletableFuture со списком контрактов
     */
    CompletableFuture<List<Contract>> getActiveContracts();
    
    /**
     * Получение контрактов игрока
     * 
     * @param playerUuid UUID игрока
     * @param asOwner true - как заказчик, false - как исполнитель
     * @return CompletableFuture со списком контрактов
     */
    CompletableFuture<List<Contract>> getPlayerContracts(UUID playerUuid, boolean asOwner);
    
    /**
     * Принятие контракта
     * 
     * @param contractId UUID контракта
     * @param contractorUuid UUID исполнителя
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> acceptContract(UUID contractId, UUID contractorUuid);
    
    /**
     * Завершение контракта
     * 
     * @param contractId UUID контракта
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> completeContract(UUID contractId);
    
    /**
     * Верификация контракта
     * 
     * @param contractId UUID контракта
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> verifyContract(UUID contractId);
    
    /**
     * Отмена контракта
     * 
     * @param contractId UUID контракта
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> cancelContract(UUID contractId);
    
    /**
     * Получение контрактов по типу
     * 
     * @param type Тип контракта
     * @return CompletableFuture со списком контрактов
     */
    CompletableFuture<List<Contract>> getContractsByType(ContractType type);
    
    // ==================== Reputation Management ====================
    
    /**
     * Получение репутации игрока
     * 
     * @param playerUuid UUID игрока
     * @return CompletableFuture с репутацией
     */
    CompletableFuture<UserReputation> getReputation(UUID playerUuid);
    
    /**
     * Обновление репутации
     * 
     * @param reputation Объект репутации
     * @return CompletableFuture с результатом
     */
    CompletableFuture<Boolean> updateReputation(UserReputation reputation);
    
    /**
     * Добавление отзыва
     * 
     * @param contractId UUID контракта
     * @param reviewerUuid UUID того, кто оставляет отзыв
     * @param revieweeUuid UUID того, кому оставляют отзыв
     * @param rating Рейтинг (1-5)
     * @param comment Комментарий
     * @return CompletableFuture с созданным отзывом
     */
    CompletableFuture<ContractReview> addReview(
        UUID contractId,
        UUID reviewerUuid,
        UUID revieweeUuid,
        int rating,
        String comment
    );
    
    /**
     * Получение отзывов о игроке
     * 
     * @param playerUuid UUID игрока
     * @return CompletableFuture со списком отзывов
     */
    CompletableFuture<List<ContractReview>> getPlayerReviews(UUID playerUuid);
    
    /**
     * Получение отзывов по контракту
     * 
     * @param contractId UUID контракта
     * @return CompletableFuture со списком отзывов
     */
    CompletableFuture<List<ContractReview>> getContractReviews(UUID contractId);
    
    // ==================== Statistics ====================
    
    /**
     * Получение общего количества контрактов
     * 
     * @return CompletableFuture с количеством
     */
    CompletableFuture<Integer> getTotalContracts();
    
    /**
     * Получение количества активных контрактов игрока
     * 
     * @param playerUuid UUID игрока
     * @return CompletableFuture с количеством
     */
    CompletableFuture<Integer> getActiveContractCount(UUID playerUuid);
    
    /**
     * Очистка истекших контрактов
     * 
     * @return CompletableFuture с количеством удаленных
     */
    CompletableFuture<Integer> cleanupExpiredContracts();
}
