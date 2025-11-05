package io.eliasnvx.gighub.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Менеджер подключения к базе данных с HikariCP
 */
public class DatabaseManager {
    
    private final Logger logger;
    private final DatabaseConfig config;
    private HikariDataSource dataSource;
    
    public DatabaseManager(Logger logger, DatabaseConfig config) {
        this.logger = logger;
        this.config = config;
    }
    
    /**
     * Инициализация connection pool
     */
    public void initialize() throws SQLException {
        logger.info("Initializing database connection pool...");
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setDriverClassName(config.getType().getDriverClass());
        
        // Настройки для MySQL/PostgreSQL
        if (config.getType() != DatabaseType.SQLITE) {
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
        }
        
        // Pool settings
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setPoolName("GigHub-Pool");
        
        // Performance settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        
        try {
            dataSource = new HikariDataSource(hikariConfig);
            logger.info("Database connection pool initialized successfully!");
            
            // Создание таблиц
            createTables();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database!", e);
            throw new SQLException("Database initialization failed", e);
        }
    }
    
    /**
     * Создание таблиц БД
     */
    private void createTables() {
        logger.info("Creating database tables...");
        
        // Contracts table
        execute("""
            CREATE TABLE IF NOT EXISTS contracts (
                id VARCHAR(36) PRIMARY KEY,
                owner_uuid VARCHAR(36) NOT NULL,
                contractor_uuid VARCHAR(36),
                type VARCHAR(32) NOT NULL,
                title VARCHAR(100) NOT NULL,
                description TEXT,
                reward DECIMAL(15,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                created_at BIGINT NOT NULL,
                deadline BIGINT,
                completed_at BIGINT,
                auto_verify BOOLEAN DEFAULT false,
                featured BOOLEAN DEFAULT false,
                priority BOOLEAN DEFAULT false,
                location_x INT,
                location_y INT,
                location_z INT,
                world VARCHAR(50)
            )
        """);
        
        // User reputation table
        execute("""
            CREATE TABLE IF NOT EXISTS user_reputation (
                uuid VARCHAR(36) PRIMARY KEY,
                rating DECIMAL(3,2) DEFAULT 5.0,
                total_completed INT DEFAULT 0,
                total_failed INT DEFAULT 0,
                total_created INT DEFAULT 0,
                points INT DEFAULT 0,
                last_updated BIGINT
            )
        """);
        
        // Contract reviews table
        execute("""
            CREATE TABLE IF NOT EXISTS contract_reviews (
                id INTEGER PRIMARY KEY %s,
                contract_id VARCHAR(36) NOT NULL,
                reviewer_uuid VARCHAR(36) NOT NULL,
                reviewee_uuid VARCHAR(36) NOT NULL,
                rating INT NOT NULL,
                comment TEXT,
                created_at BIGINT NOT NULL
            )
        """.formatted(config.getType() == DatabaseType.SQLITE ? "AUTOINCREMENT" : "AUTO_INCREMENT"));
        
        // Transactions table
        execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY %s,
                contract_id VARCHAR(36) NOT NULL,
                from_uuid VARCHAR(36) NOT NULL,
                to_uuid VARCHAR(36),
                amount DECIMAL(15,2) NOT NULL,
                type VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                created_at BIGINT NOT NULL
            )
        """.formatted(config.getType() == DatabaseType.SQLITE ? "AUTOINCREMENT" : "AUTO_INCREMENT"));
        
        // Escrow transactions table
        execute("""
            CREATE TABLE IF NOT EXISTS escrow_transactions (
                id INTEGER PRIMARY KEY %s,
                contract_id VARCHAR(36) UNIQUE NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                commission DECIMAL(15,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                frozen_at BIGINT NOT NULL,
                released_at BIGINT,
                refunded_at BIGINT
            )
        """.formatted(config.getType() == DatabaseType.SQLITE ? "AUTOINCREMENT" : "AUTO_INCREMENT"));
        
        // Индексы для производительности
        execute("CREATE INDEX IF NOT EXISTS idx_contracts_owner ON contracts(owner_uuid)");
        execute("CREATE INDEX IF NOT EXISTS idx_contracts_contractor ON contracts(contractor_uuid)");
        execute("CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts(status)");
        execute("CREATE INDEX IF NOT EXISTS idx_contracts_type ON contracts(type)");
        execute("CREATE INDEX IF NOT EXISTS idx_reviews_reviewee ON contract_reviews(reviewee_uuid)");
        execute("CREATE INDEX IF NOT EXISTS idx_transactions_contract ON transactions(contract_id)");
        execute("CREATE INDEX IF NOT EXISTS idx_escrow_contract ON escrow_transactions(contract_id)");
        execute("CREATE INDEX IF NOT EXISTS idx_escrow_owner ON escrow_transactions(owner_uuid)");
        execute("CREATE INDEX IF NOT EXISTS idx_escrow_status ON escrow_transactions(status)");
        
        logger.info("Database tables created successfully!");
    }
    
    /**
     * Получение connection из pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or closed");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Выполнение SQL запроса (синхронно)
     */
    public void execute(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute SQL: " + sql, e);
        }
    }
    
    /**
     * Выполнение SQL запроса с параметрами (синхронно)
     */
    public void execute(String sql, Consumer<PreparedStatement> paramSetter) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            paramSetter.accept(stmt);
            stmt.execute();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute SQL: " + sql, e);
        }
    }
    
    /**
     * Выполнение SQL запроса асинхронно
     */
    public CompletableFuture<Void> executeAsync(String sql, Consumer<PreparedStatement> paramSetter) {
        return CompletableFuture.runAsync(() -> execute(sql, paramSetter));
    }
    
    /**
     * Выполнение SELECT запроса с обработкой результата
     */
    public <T> T query(String sql, Consumer<PreparedStatement> paramSetter, Function<ResultSet, T> resultHandler) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            paramSetter.accept(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                return resultHandler.apply(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute query: " + sql, e);
            return null;
        }
    }
    
    /**
     * Выполнение SELECT запроса асинхронно
     */
    public <T> CompletableFuture<T> queryAsync(String sql, Consumer<PreparedStatement> paramSetter, Function<ResultSet, T> resultHandler) {
        return CompletableFuture.supplyAsync(() -> query(sql, paramSetter, resultHandler));
    }
    
    /**
     * Закрытие connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool...");
            dataSource.close();
            logger.info("Database connection pool closed!");
        }
    }
    
    /**
     * Полный сброс и пересоздание схемы БД
     */
    public void recreateSchema() {
        logger.info("Recreating database schema...");
        // Удаляем таблицы, если существуют
        execute("DROP TABLE IF EXISTS transactions");
        execute("DROP TABLE IF EXISTS contract_reviews");
        execute("DROP TABLE IF EXISTS user_reputation");
        execute("DROP TABLE IF EXISTS contracts");
        // Создаем заново
        createTables();
        logger.info("Database schema recreated!");
    }
    
    /**
     * Проверка подключения к БД
     */
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
