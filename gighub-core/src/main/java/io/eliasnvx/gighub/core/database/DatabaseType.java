package io.eliasnvx.gighub.core.database;

/**
 * Поддерживаемые типы баз данных
 */
public enum DatabaseType {
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:{file}"),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}?useSSL=false&autoReconnect=true"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}");
    
    private final String driverClass;
    private final String urlPattern;
    
    DatabaseType(String driverClass, String urlPattern) {
        this.driverClass = driverClass;
        this.urlPattern = urlPattern;
    }
    
    public String getDriverClass() {
        return driverClass;
    }
    
    public String getUrlPattern() {
        return urlPattern;
    }
}
