package shaadisharthi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigUtil - Centralized configuration management with environment variable support
 * 
 * Hierarchical configuration lookup:
 * 1. Environment variables (highest priority)
 * 2. System properties (middle priority) 
 * 3. config.properties file (lowest priority)
 * 
 * Provides flexible deployment configuration for different environments
 * (development, staging, production) without code changes.
 * 
 * @category Utilities & Infrastructure
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    // Properties loaded from config.properties file
    private static final Properties props = new Properties();

    // Static initializer - loads configuration during class initialization
    static {
        try (InputStream in = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                logger.info("Loaded config.properties");
            } else {
                logger.warn("config.properties not found in classpath");
            }
        } catch (IOException e) {
            logger.error("Failed to load config.properties: {}", e.getMessage(), e);
        }
    }

    /**
     * Get configuration value with hierarchical lookup
     * 
     * Lookup order:
     * 1. Environment variable (envKey)
     * 2. System property (envKey) 
     * 3. config.properties (propertyKey)
     * 
     * @param propertyKey Configuration key in config.properties
     * @param envKey Environment variable or system property name
     * @return Configuration value, or null if not found
     */
    public static String get(String propertyKey, String envKey) {
        // First priority: environment variables
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) {
            logger.debug("Using environment variable {}: {}", envKey, env);
            return env;
        }
        logger.debug("Environment variable {} not set or empty", envKey);
        
        // Second priority: system properties
        String sys = System.getProperty(envKey);
        if (sys != null && !sys.isEmpty()) {
            logger.debug("Using system property {}: {}", envKey, sys);
            return sys;
        }
        logger.debug("System property {} not set or empty", envKey);
        
        // Third priority: config.properties file
        String prop = props.getProperty(propertyKey);
        if (prop != null) {
            logger.debug("Using config.properties {}: {}", propertyKey, prop);
        } else {
            logger.warn("Property {} not found in config.properties", propertyKey);
        }
        return prop;
    }

    /**
     * Get configuration value with fallback default
     * 
     * @param propertyKey Configuration key in config.properties
     * @param envKey Environment variable or system property name
     * @param def Default value if configuration not found
     * @return Configuration value or default if not found
     */
    public static String getOrDefault(String propertyKey, String envKey, String def) {
        String v = get(propertyKey, envKey);
        if (v == null) {
            logger.debug("Using default value for {}: {}", envKey, def);
            return def;
        }
        return v;
    }
}