package io.eliasnvx.gighub.core.database;

import lombok.Builder;
import lombok.Data;

/**
 * Конфигурация подключения к БД
 */
@Data
@Builder
public class DatabaseConfig {
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String file; // Для SQLite
    
    @Builder.Default
    private int poolSize = 10;
    
    @Builder.Default
    private long connectionTimeout = 30000;
    
    @Builder.Default
    private long maxLifetime = 1800000;
    
    /**
     * Получение JDBC URL
     */
    public String getJdbcUrl() {
        String url = type.getUrlPattern();
        
        switch (type) {
            case SQLITE:
                return url.replace("{file}", file);
            case MYSQL:
            case POSTGRESQL:
                return url
                    .replace("{host}", host)
                    .replace("{port}", String.valueOf(port))
                    .replace("{database}", database);
            default:
                throw new IllegalStateException("Unknown database type: " + type);
        }
    }
}
