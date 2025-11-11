package shaadisarthi.customer;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import shaadisarthi.cache.RedisClient;
import shaadisharthi.DbConnection.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * DATA ACCESS OBJECT: ServiceDAO
 * 
 * PURPOSE: Comprehensive data access layer for service-related operations
 * Advanced service management with:
 * - Redis caching with 5-minute TTL for performance optimization
 * - Google Guava RateLimiter (100 req/sec) for API protection
 * - Multi-criteria service filtering and pagination
 * - Review management with cache invalidation
 * - Media and service detail retrieval
 * 
 * ARCHITECTURE: DAO pattern with caching layer and rate limiting
 * PERFORMANCE: Dual-layer caching (Redis + database fallback)
 * SECURITY: Rate limiting prevents API abuse
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
public class ServiceDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDAO.class);
    private static final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 requests/sec
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes
    private static final JedisPool jedisPool = RedisClient.getJedisPool();

    /**
     * Retrieves filtered services with advanced caching and rate limiting
     * Supports multiple filter criteria and pagination with Redis caching
     * 
     * @param category Service category filter
     * @param location Geographic location filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param rating Minimum rating filter
     * @param sortBy Sorting criteria (popular, rating, price_low, price_high)
     * @param page Pagination page number
     * @param limit Number of items per page
     * @return List of filtered Service objects
     * @throws RuntimeException if rate limit exceeded or database error
     */
    public List<Service> getFilteredServices(String category, String location, Double minPrice, Double maxPrice,
            Integer rating, String sortBy, int page, int limit) {
        // Rate limiting check - protect against API abuse
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for services fetch");
            throw new RuntimeException("Too many requests");
        }

        // Redis availability check with graceful fallback to database
        if (jedisPool == null) {
            LOGGER.warn("Redis unavailable, fetching from DB directly");
            return fetchServicesFromDb(category, location, minPrice, maxPrice, rating, sortBy, page, limit);
        }

        // Generate unique cache key based on all filter parameters
        String cacheKey = String.format("services:%s:%s:%s:%s:%s:%s:%d:%d",
                category != null ? category : "", location != null ? location : "",
                minPrice != null ? minPrice : "", maxPrice != null ? maxPrice : "",
                rating != null ? rating : "", sortBy != null ? sortBy : "popular", page, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(cacheKey);
            if (cached != null) {
                LOGGER.info("Cache hit for key: {}", cacheKey);
                return parseServicesFromJson(new JSONArray(cached));
            }

            // Cache miss - fetch from database and populate cache
            List<Service> services = fetchServicesFromDb(category, location, minPrice, maxPrice, rating, sortBy, page, limit);
            
            // Add cache key to each service's filter set for cache invalidation
            for (Service service : services) {
                String serviceFilterSetKey = "service_filters:" + service.getServiceId();
                jedis.sadd(serviceFilterSetKey, cacheKey);
            }
            
            jedis.setex(cacheKey, CACHE_TTL_SECONDS, new JSONArray(services).toString());
            LOGGER.info("Cached results for key: {}", cacheKey);
            return services;
        } catch (Exception e) {
            LOGGER.error("Redis error, falling back to DB: {}", e.getMessage(), e);
            return fetchServicesFromDb(category, location, minPrice, maxPrice, rating, sortBy, page, limit);
        }
    }

    /**
     * Retrieves service details by ID with caching and rate limiting
     * 
     * @param serviceId Unique service identifier
     * @return Service object with full details or null if not found
     * @throws RuntimeException if rate limit exceeded or database error
     */
    public Service getServiceById(int serviceId) {
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for service fetch");
            throw new RuntimeException("Too many requests");
        }

        String cacheKey = "service:" + serviceId;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String cached = jedis.get(cacheKey);
                if (cached != null) {
                    LOGGER.info("Cache hit for service: {}", cacheKey);
                    return parseServiceFromJson(new JSONObject(cached));
                }
            } catch (Exception e) {
                LOGGER.error("Redis error: {}", e.getMessage(), e);
            }
        }

        return fetchServiceFromDb(serviceId);
    }

    /**
     * Fetches service details directly from database (bypasses cache)
     * Includes provider information, ratings, and media URLs
     * 
     * @param serviceId Unique service identifier
     * @return Service object with complete details
     * @throws RuntimeException if database error occurs
     */
    private Service fetchServiceFromDb(int serviceId) {
        String sql = "SELECT s.service_id, s.service_name, s.category, CONCAT(sp.city, ', ', sp.state) as location, " +
                     "s.price, sp.business_name, s.description, sp.email, sp.phone_no, " +
                     "COALESCE(m.media_url, 'img/default-service.jpg') as image_url, " +
                     "COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.service_id = s.service_id), 0) as rating, " +
                     "COALESCE((SELECT COUNT(r.review_id) FROM reviews r WHERE r.service_id = s.service_id), 0) as reviewCount " +
                     "FROM services s " +
                     "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                     "LEFT JOIN (SELECT service_id, media_url FROM media WHERE media_type = 'Image' AND status = 'Active' LIMIT 1) m " +
                     "ON s.service_id = m.service_id " +
                     "WHERE s.service_id = ? AND s.status = 'Active'";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Service service = new Service();
                    service.setServiceId(rs.getInt("service_id"));
                    service.setServiceName(rs.getString("service_name"));
                    service.setCategory(rs.getString("category"));
                    service.setLocation(rs.getString("location"));
                    service.setPrice(rs.getDouble("price"));
                    service.setBusinessName(rs.getString("business_name"));
                    service.setDescription(rs.getString("description"));
                    service.setEmail(rs.getString("email"));
                    service.setPhone(rs.getString("phone_no"));
                    service.setImageUrl(rs.getString("image_url"));
                    service.setRating(rs.getDouble("rating"));
                    service.setReviewCount(rs.getInt("reviewCount"));
                    return service;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves media files associated with a service
     * 
     * @param serviceId Unique service identifier
     * @return List of Media objects (images, videos, etc.)
     * @throws RuntimeException if database error occurs
     */
    public List<Media> getMediaByServiceId(int serviceId) {
        List<Media> mediaList = new ArrayList<>();
        String sql = "SELECT media_id as id, media_url, media_type FROM media WHERE service_id = ? AND status = 'Active'";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Media media = new Media();
                    media.setMediaId(rs.getInt("id"));
                    media.setMediaUrl(rs.getString("media_url"));
                    media.setMediaType(rs.getString("media_type"));
                    mediaList.add(media);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        return mediaList;
    }

    /**
     * Retrieves paginated reviews for a service with caching
     * 
     * @param serviceId Unique service identifier
     * @param page Pagination page number
     * @param limit Number of reviews per page
     * @return List of Review objects for the specified page
     * @throws RuntimeException if rate limit exceeded or database error
     */
    public List<Review> getServiceReviews(int serviceId, int page, int limit) {
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for reviews fetch");
            throw new RuntimeException("Too many requests");
        }

        String cacheKey = String.format("reviews:%d:%d:%d", serviceId, page, limit);
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String cached = jedis.get(cacheKey);
                if (cached != null) {
                    LOGGER.info("Cache hit for reviews: {}", cacheKey);
                    return parseReviewsFromJson(new JSONArray(cached));
                }
            } catch (Exception e) {
                LOGGER.error("Redis error: {}", e.getMessage(), e);
            }
        }

        return fetchReviewsFromDb(serviceId, page, limit);
    }

    /**
     * Fetches reviews directly from database with pagination
     * 
     * @param serviceId Unique service identifier
     * @param page Pagination page number
     * @param limit Number of reviews per page
     * @return List of Review objects
     * @throws RuntimeException if database error occurs
     */
    private List<Review> fetchReviewsFromDb(int serviceId, int page, int limit) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.review_id, r.customer_id, c.name as customer_name, r.review_text, r.rating, r.created_at " +
                     "FROM reviews r " +
                     "JOIN customers c ON r.customer_id = c.customer_id " +
                     "WHERE r.service_id = ? " +
                     "ORDER BY r.created_at DESC " +
                     "LIMIT ? OFFSET ?";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, serviceId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setReviewId(rs.getInt("review_id"));
                    review.setCustomerId(rs.getInt("customer_id"));
                    review.setCustomerName(rs.getString("customer_name"));
                    review.setReviewText(rs.getString("review_text"));
                    review.setRating(rs.getInt("rating"));
                    review.setCreatedAt(rs.getTimestamp("created_at"));
                    reviews.add(review);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        return reviews;
    }

    /**
     * Retrieves total review count for a service with caching
     * 
     * @param serviceId Unique service identifier
     * @return Total number of reviews for the service
     * @throws RuntimeException if database error occurs
     */
    public long getServiceReviewsCount(int serviceId) {
        String cacheKey = "reviews_count:" + serviceId;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String cached = jedis.get(cacheKey);
                if (cached != null) {
                    LOGGER.info("Cache hit for reviews count: {}", cacheKey);
                    return Long.parseLong(cached);
                }
            } catch (Exception e) {
                LOGGER.error("Redis error: {}", e.getMessage(), e);
            }
        }

        String sql = "SELECT COUNT(*) as total FROM reviews WHERE service_id = ?";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong("total");
                    if (jedisPool != null) {
                        try (Jedis jedis = jedisPool.getResource()) {
                            jedis.setex(cacheKey, CACHE_TTL_SECONDS, String.valueOf(count));
                            LOGGER.info("Cached reviews count for key: {}", cacheKey);
                        }
                    }
                    return count;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Parses JSON array into List of Service objects
     * 
     * @param jsonArray JSON array containing service data
     * @return List of Service objects
     */
    private List<Service> parseServicesFromJson(JSONArray jsonArray) {
        List<Service> services = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            services.add(parseServiceFromJson(jsonArray.getJSONObject(i)));
        }
        return services;
    }

    /**
     * Parses JSON object into Service object
     * 
     * @param jsonObject JSON object containing service data
     * @return Service object
     */
    private Service parseServiceFromJson(JSONObject jsonObject) {
        Service service = new Service();
        service.setServiceId(jsonObject.getInt("serviceId"));
        service.setServiceName(jsonObject.getString("serviceName"));
        service.setCategory(jsonObject.getString("category"));
        service.setLocation(jsonObject.getString("location"));
        service.setRating(jsonObject.getDouble("rating"));
        service.setReviewCount(jsonObject.getInt("reviewCount"));
        service.setPrice(jsonObject.getDouble("price"));
        service.setBusinessName(jsonObject.getString("businessName"));
        service.setDescription(jsonObject.getString("description"));
        service.setEmail(jsonObject.getString("email"));
        service.setPhone(jsonObject.getString("phone"));
        service.setImageUrl(jsonObject.getString("imageUrl"));
        return service;
    }

    /**
     * Parses JSON array into List of Review objects
     * 
     * @param jsonArray JSON array containing review data
     * @return List of Review objects
     */
    private List<Review> parseReviewsFromJson(JSONArray jsonArray) {
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            reviews.add(parseReviewFromJson(jsonArray.getJSONObject(i)));
        }
        return reviews;
    }

    /**
     * Parses JSON object into Review object
     * 
     * @param jsonObject JSON object containing review data
     * @return Review object
     */
    private Review parseReviewFromJson(JSONObject jsonObject) {
        Review review = new Review();
        review.setReviewId(jsonObject.getInt("reviewId"));
        review.setServiceId(jsonObject.getInt("serviceId"));
        review.setCustomerId(jsonObject.getInt("customerId"));
        review.setCustomerName(jsonObject.getString("customerName"));
        review.setReviewText(jsonObject.getString("reviewText"));
        review.setRating(jsonObject.getInt("rating"));
        review.setCreatedAt(jsonObject.has("createdAt") ? new java.sql.Timestamp(jsonObject.getLong("createdAt")) : null);
        return review;
    }

    /**
     * Retrieves count of filtered services for pagination with caching
     * 
     * @param category Service category filter
     * @param location Geographic location filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param rating Minimum rating filter
     * @return Total count of services matching filters
     * @throws RuntimeException if database error occurs
     */
    public long getFilteredServicesCount(String category, String location, Double minPrice, Double maxPrice, Integer rating) {
        if (jedisPool == null) {
            LOGGER.warn("Redis unavailable, counting from DB directly");
            return fetchServicesCountFromDb(category, location, minPrice, maxPrice, rating);
        }

        String cacheKey = String.format("services_count:%s:%s:%s:%s:%s",
                category != null ? category : "", location != null ? location : "",
                minPrice != null ? minPrice : "", maxPrice != null ? maxPrice : "",
                rating != null ? rating : "");
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(cacheKey);
            if (cached != null) {
                LOGGER.info("Cache hit for count key: {}", cacheKey);
                return Long.parseLong(cached);
            }

            long count = fetchServicesCountFromDb(category, location, minPrice, maxPrice, rating);
            jedis.setex(cacheKey, CACHE_TTL_SECONDS, String.valueOf(count));
            LOGGER.info("Cached count for key: {}", cacheKey);
            return count;
        } catch (Exception e) {
            LOGGER.error("Redis error, falling back to DB count: {}", e.getMessage(), e);
            return fetchServicesCountFromDb(category, location, minPrice, maxPrice, rating);
        }
    }

    /**
     * Fetches count of filtered services directly from database
     * Uses different query strategies based on rating filter presence
     * 
     * @param category Service category filter
     * @param location Geographic location filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param rating Minimum rating filter
     * @return Total count of matching services
     * @throws RuntimeException if database error occurs
     */
    private long fetchServicesCountFromDb(String category, String location, Double minPrice, Double maxPrice, Integer rating) {
        String query;
        List<Object> params = new ArrayList<>();

        if (rating != null && rating > 0) {
            // Use subquery to count services meeting rating filter with GROUP BY and HAVING
            query = "SELECT COUNT(*) as total FROM (" +
                    "SELECT s.service_id " +
                    "FROM services s " +
                    "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                    "LEFT JOIN reviews r ON s.service_id = r.service_id " +
                    "WHERE s.status = 'Active' ";
            if (category != null && !category.trim().isEmpty()) {
                query += "AND s.category = ? ";
                params.add(category.trim());
            }
            if (location != null && !location.trim().isEmpty()) {
                query += "AND (sp.city = ? OR sp.state = ?) ";
                params.add(location.trim());
                params.add(location.trim());
            }
            if (minPrice != null) {
                query += "AND s.price >= ? ";
                params.add(minPrice);
            }
            if (maxPrice != null) {
                query += "AND s.price <= ? ";
                params.add(maxPrice);
            }
            query += "GROUP BY s.service_id HAVING COALESCE(AVG(r.rating), 0) >= ? " +
                     ") AS subquery";
            params.add(rating);
        } else {
            // Simple count without reviews join for rating=0 or null (better performance)
            query = "SELECT COUNT(DISTINCT s.service_id) as total " +
                    "FROM services s " +
                    "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                    "WHERE s.status = 'Active' ";
            if (category != null && !category.trim().isEmpty()) {
                query += "AND s.category = ? ";
                params.add(category.trim());
            }
            if (location != null && !location.trim().isEmpty()) {
                query += "AND (sp.city = ? OR sp.state = ?) ";
                params.add(location.trim());
                params.add(location.trim());
            }
            if (minPrice != null) {
                query += "AND s.price >= ? ";
                params.add(minPrice);
            }
            if (maxPrice != null) {
                query += "AND s.price <= ? ";
                params.add(maxPrice);
            }
        }

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            LOGGER.info("Executing query: {}, Params: {}", query, params);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                long total = rs.next() ? rs.getLong("total") : 0;
                LOGGER.info("Total services counted: {}", total);
                return total;
            }
        } catch (Exception e) {
            LOGGER.error("Database error in fetchServicesCountFromDb: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches filtered services directly from database with complex query
     * Includes joins, grouping, sorting, and pagination
     * 
     * @param category Service category filter
     * @param location Geographic location filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param rating Minimum rating filter
     * @param sortBy Sorting criteria
     * @param page Pagination page number
     * @param limit Number of items per page
     * @return List of filtered Service objects
     * @throws RuntimeException if database error occurs
     */
    private List<Service> fetchServicesFromDb(String category, String location, Double minPrice, Double maxPrice,
                                             Integer rating, String sortBy, int page, int limit) {
        List<Service> services = new ArrayList<>();
        String query = "SELECT s.service_id, s.service_name, s.category, s.price, " +
                      "CONCAT(COALESCE(sp.city, 'Unknown'), ', ', COALESCE(sp.state, 'Unknown')) as location, " +
                      "sp.business_name, s.description, sp.email, sp.phone_no, " +
                      "COALESCE((SELECT m.media_url FROM media m WHERE m.service_id = s.service_id " +
                      "AND m.media_type = 'Image' AND m.status = 'Active' ORDER BY m.upload_time LIMIT 1), " +
                      "'img/default-service.jpg') as image_url, " +
                      "COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.review_id) as review_count " +
                      "FROM services s " +
                      "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                      "LEFT JOIN reviews r ON s.service_id = r.service_id " +
                      "WHERE s.status = 'Active' ";
        List<Object> params = new ArrayList<>();

        // Build WHERE clause dynamically based on provided filters
        if (category != null && !category.trim().isEmpty()) {
            query += "AND s.category = ? ";
            params.add(category.trim());
        }
        if (location != null && !location.trim().isEmpty()) {
            query += "AND (sp.city = ? OR sp.state = ?) ";
            params.add(location.trim());
            params.add(location.trim());
        }
        if (minPrice != null) {
            query += "AND s.price >= ? ";
            params.add(minPrice);
        }
        if (maxPrice != null) {
            query += "AND s.price <= ? ";
            params.add(maxPrice);
        }
        
        // Group by all selected columns for proper aggregation
        query += "GROUP BY s.service_id, s.service_name, s.category, s.price, sp.city, sp.state, sp.business_name, " +
                 "s.description, sp.email, sp.phone_no, image_url ";
        
        // Apply rating filter in HAVING clause (after aggregation)
        if (rating != null && rating > 0) {
            query += "HAVING avg_rating >= ? ";
            params.add(rating);
        }
        
        // Apply sorting based on sortBy parameter
        switch (sortBy != null ? sortBy.trim() : "popular") {
            case "rating": query += "ORDER BY avg_rating DESC "; break;
            case "price_low": query += "ORDER BY s.price ASC "; break;
            case "price_high": query += "ORDER BY s.price DESC "; break;
            default: query += "ORDER BY review_count DESC "; break; // Default: popularity
        }
        
        // Apply pagination
        query += "LIMIT ? OFFSET ?";
        params.add(limit);
        params.add((page - 1) * limit);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Service service = new Service();
                    service.setServiceId(rs.getInt("service_id"));
                    service.setServiceName(rs.getString("service_name"));
                    service.setCategory(rs.getString("category"));
                    service.setLocation(rs.getString("location"));
                    service.setRating(rs.getDouble("avg_rating"));
                    service.setReviewCount(rs.getInt("review_count"));
                    service.setPrice(rs.getDouble("price"));
                    service.setBusinessName(rs.getString("business_name"));
                    service.setDescription(rs.getString("description"));
                    service.setEmail(rs.getString("email"));
                    service.setPhone(rs.getString("phone_no"));
                    service.setImageUrl(rs.getString("image_url"));
                    services.add(service);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Database error in fetchServicesFromDb: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return services;
    }

    /**
     * Fetches service by ID directly from database (alternative implementation)
     * 
     * @param serviceId Unique service identifier
     * @return Service object or null if not found
     * @throws RuntimeException if database error occurs
     */
    private Service fetchServiceByIdFromDb(int serviceId) {
        String query = "SELECT s.service_id, s.service_name, s.category, s.price, sp.city || ', ' || sp.state as location, " +
                "sp.business_name, s.description, sp.email, sp.phone_no, COALESCE((SELECT m.media_url " +
                "FROM media m WHERE m.service_id = s.service_id AND m.media_type = 'Image' AND m.status = 'Active' " +
                "ORDER BY m.created_at LIMIT 1), 'img/default-service.jpg') as image_url " +
                "FROM services s JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                "WHERE s.service_id = ? AND s.status = 'Active'";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Service service = new Service();
                    service.setServiceId(rs.getInt("service_id"));
                    service.setServiceName(rs.getString("service_name"));
                    service.setCategory(rs.getString("category"));
                    service.setLocation(rs.getString("location"));
                    service.setPrice(rs.getDouble("price"));
                    service.setBusinessName(rs.getString("business_name"));
                    service.setDescription(rs.getString("description"));
                    service.setEmail(rs.getString("email"));
                    service.setPhone(rs.getString("phone_no"));
                    service.setImageUrl(rs.getString("image_url"));
                    return service;
                }
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a new review for a service with cache invalidation
     * Invalidates related caches to ensure data consistency
     * 
     * @param review Review object containing review data
     * @return true if review was added successfully
     * @throws RuntimeException if rate limit exceeded or database error
     */
    public boolean addReview(Review review) {
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for adding review");
            throw new RuntimeException("Too many requests");
        }

        try {
            boolean success = addReviewToDb(review);
            if (success && jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    // Invalidate service and reviews caches to maintain data consistency
                    String serviceCacheKey = "service:" + review.getServiceId();
                    String reviewsCachePattern = "reviews:" + review.getServiceId() + ":*";
                    jedis.del(serviceCacheKey);
                    for (String key : jedis.keys(reviewsCachePattern)) {
                        jedis.del(key);
                    }
                    // Invalidate services cache entries containing this service
                    String serviceFilterSetKey = "service_filters:" + review.getServiceId();
                    for (String filterKey : jedis.smembers(serviceFilterSetKey)) {
                        jedis.del(filterKey);
                    }
                    LOGGER.info("Invalidated caches for service ID: {}", review.getServiceId());
                } catch (Exception e) {
                    LOGGER.warn("Failed to invalidate cache, proceeding: {}", e.getMessage());
                }
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error adding review: {}", e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    /**
     * Fetches review count directly from database
     * 
     * @param serviceId Unique service identifier
     * @return Total number of reviews
     * @throws RuntimeException if database error occurs
     */
    private long fetchReviewsCountFromDb(int serviceId) {
        String query = "SELECT COUNT(*) as total FROM reviews WHERE service_id = ?";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("total") : 0;
            }
        } catch (Exception e) {
            LOGGER.error("Database error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts review into database
     * 
     * @param review Review object to insert
     * @return true if insertion was successful
     * @throws RuntimeException if database error occurs
     */
    private boolean addReviewToDb(Review review) {
        String insertQuery = "INSERT INTO reviews (service_id, customer_id, review_text, rating, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            stmt.setInt(1, review.getServiceId());
            stmt.setInt(2, review.getCustomerId());
            stmt.setString(3, review.getReviewText());
            stmt.setInt(4, review.getRating());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            LOGGER.error("Database error in addReviewToDb: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Advanced review addition with immediate cache refresh
     * Atomic operation: add review + refresh paginated results + update cache
     * 
     * @param review Review object to add
     * @param page Page number for which to refresh results
     * @param limit Number of items per page
     * @return Fresh list of reviews for the specified page
     * @throws RuntimeException if operation fails
     */
    public List<Review> addReviewAndGetPage(Review review, int page, int limit) {
        int serviceId = review.getServiceId();

        // 1) Pre-invalidate caches (best-effort) to ensure fresh data
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String serviceCacheKey = "service:" + serviceId;
                jedis.del(serviceCacheKey);

                // Delete reviews:serviceId:* safely
                String reviewsPattern = "reviews:" + serviceId + ":*";
                // Note: deleteKeysByPattern method is commented out in original code
                // deleteKeysByPattern(jedis, reviewsPattern);

                // Delete service_filters set entries if present
                String serviceFilterSetKey = "service_filters:" + serviceId;
                if (jedis.exists(serviceFilterSetKey)) {
                    for (String filterKey : jedis.smembers(serviceFilterSetKey)) {
                        jedis.del(filterKey);
                    }
                }

                LOGGER.info("Pre-invalidated caches for serviceId: {}", serviceId);
            } catch (Exception e) {
                LOGGER.warn("Failed to pre-invalidate redis cache: {}", e.getMessage());
            }
        }

        // 2) Insert review in its own transaction for data consistency
        try (Connection conn = DbConnection.getCon()) {
            conn.setAutoCommit(false);
            try {
                String insertQuery = "INSERT INTO reviews (service_id, customer_id, review_text, rating, created_at) VALUES (?, ?, ?, ?, NOW())";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setInt(1, review.getServiceId());
                    pstmt.setInt(2, review.getCustomerId());
                    pstmt.setString(3, review.getReviewText());
                    pstmt.setInt(4, review.getRating());
                    pstmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                LOGGER.error("DB insert rollback: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to insert review: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        // 3) Fetch fresh page from DB (bypass cache for immediate consistency)
        List<Review> fresh = fetchReviewsFromDb(serviceId, page, limit);

        // 4) Cache fresh page back into Redis (best-effort cache warm-up)
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String cacheKey = String.format("reviews:%d:%d:%d", serviceId, page, limit);
                JSONArray arr = new JSONArray();
                for (Review r : fresh) {
                    JSONObject o = new JSONObject();
                    o.put("reviewId", r.getReviewId());
                    o.put("serviceId", r.getServiceId());
                    o.put("customerId", r.getCustomerId());
                    o.put("customerName", r.getCustomerName());
                    o.put("reviewText", r.getReviewText());
                    o.put("rating", r.getRating());
                    o.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().getTime() : JSONObject.NULL);
                    arr.put(o);
                }
                jedis.setex(cacheKey, CACHE_TTL_SECONDS, arr.toString());
                LOGGER.info("Cached updated reviews for key: {}", cacheKey);
            } catch (Exception e) {
                LOGGER.warn("Failed to cache updated reviews: {}", e.getMessage());
            }
        }

        return fresh;
    }
}