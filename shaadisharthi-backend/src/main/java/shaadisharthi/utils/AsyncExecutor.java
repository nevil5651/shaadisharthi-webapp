package shaadisharthi.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AsyncExecutor - Simple asynchronous task execution utility
 * 
 * Provides a fixed thread pool for executing background tasks:
 * - Email sending
 * - Notification processing  
 * - Audit logging
 * - Other non-blocking operations
 * 
 * Uses fixed thread pool with 5 threads for balanced performance
 * and resource utilization.
 * 
 * @category Utilities & Infrastructure
 * @threading Fixed thread pool with 5 worker threads
 */
public class AsyncExecutor {
    // Fixed thread pool for background task execution
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * Execute task asynchronously in background thread
     * 
     * Submits Runnable task to thread pool for non-blocking execution.
     * Ideal for operations that don't require immediate response.
     * 
     * @param task Runnable task to execute asynchronously
     */
    public static void runAsync(Runnable task) {
        executor.submit(task);
    }

    /**
     * Gracefully shutdown the thread pool
     * 
     * Should be called during application shutdown to release
     * thread resources and allow graceful application termination.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}