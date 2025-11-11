package shaadisharthi.DbConnection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import shaadisharthi.utils.ConfigUtil;

/**
 * DbConnection - Database connection pool management using HikariCP
 * 
 * Provides efficient database connection pooling with:
 * - Environment variable and configuration file support
 * - Connection pooling with optimal settings
 * - Prepared statement caching
 * - Comprehensive connection lifecycle management
 * 
 * Uses ConfigUtil for flexible configuration (environment variables override config files)
 * 
 * @category Database Infrastructure
 * @threading Thread-safe connection pool
 */
public class DbConnection {
    private static final Logger logger = LoggerFactory.getLogger(DbConnection.class);
    // HikariCP data source instance - initialized once at class loading
    private static final HikariDataSource dataSource;

    // Static initializer - configures connection pool during class loading
    static {
        try {
            // ConfigUtil handles loading config.properties and prefers environment variables
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(ConfigUtil.get("db.url", "DB_URL"));
            hikariConfig.setUsername(ConfigUtil.get("db.username", "DB_USERNAME"));
            hikariConfig.setPassword(ConfigUtil.get("db.password", "DB_PASSWORD"));
            hikariConfig.setDriverClassName(ConfigUtil.get("db.driver", "DB_DRIVER"));

            // Validate required database configuration properties
            if (hikariConfig.getJdbcUrl() == null || hikariConfig.getUsername() == null ||
                hikariConfig.getPassword() == null || hikariConfig.getDriverClassName() == null) {
                logger.error("Missing database configuration in environment variables or config.properties");
                throw new RuntimeException("Missing database configuration in environment variables or config.properties");
            }

            // Configure HikariCP connection pool settings for optimal performance
            hikariConfig.setMaximumPoolSize(10); // Maximum number of connections in the pool
            hikariConfig.setMinimumIdle(5);      // Minimum number of idle connections
            hikariConfig.setIdleTimeout(300000); // 5 minutes (connection max idle time)
            hikariConfig.setMaxLifetime(1800000); // 30 minutes (connection max lifetime)
            hikariConfig.setConnectionTimeout(30000); // 30 seconds (connection acquisition timeout)
            
            // Performance optimization settings
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // Initialize the data source
            dataSource = new HikariDataSource(hikariConfig);
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    /**
     * Get database connection from the pool
     * 
     * @return Connection object from HikariCP pool
     * @throws SQLException If connection acquisition fails
     */
    public static Connection getCon() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Close the data source and release all connections
     * 
     * Should be called during application shutdown for clean resource cleanup
     */
    public static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}