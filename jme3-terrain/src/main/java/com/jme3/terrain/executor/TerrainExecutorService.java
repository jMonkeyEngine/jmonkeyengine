package com.jme3.terrain.executor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class to provide single executor service to run background tasks of terrain stuff.
 *
 * @author JavaSaBr
 */
public class TerrainExecutorService {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    /**
     * The constructor of the terrain executor service.
     */
    private static volatile Callable<ExecutorService> constructor = new Callable<ExecutorService>() {

        @Override
        public ExecutorService call() throws Exception {
            return Executors.newFixedThreadPool(RUNTIME.availableProcessors(), new ThreadFactory() {

                private final AtomicInteger counter = new AtomicInteger(-1);

                @Override
                public Thread newThread(final Runnable task) {
                    final Thread thread = new Thread(task);
                    thread.setName("jME3 Terrain Thread [" + counter.incrementAndGet() + "]");
                    thread.setDaemon(true);
                    return thread;
                }
            });
        }
    };

    /**
     * Set a new constructor of executor service to provide other implementation.
     *
     * @param constructor the constructor.
     */
    public static void setConstructor(final Callable<ExecutorService> constructor) {
        TerrainExecutorService.constructor = constructor;
    }

    /**
     * https://stackoverflow.com/questions/29883403/double-checked-locking-without-volatile
     * <p>
     * This suggestion is of Aleksey Shipilev
     */
    private static class LazyInitializer {
        public final TerrainExecutorService instance;
        public LazyInitializer(final TerrainExecutorService instance) {
            this.instance = instance;
        }
    }

    /**
     * The lazy singleton.
     */
    private static LazyInitializer initializer;

    public static TerrainExecutorService getInstance() {

        LazyInitializer lazy = initializer;

        if (lazy == null) { // check 1
            synchronized (TerrainExecutorService.class) {
                lazy = initializer;
                if (lazy == null) { // check2
                    lazy = new LazyInitializer(new TerrainExecutorService());
                    initializer = lazy;
                }
            }
        }

        return lazy.instance;
    }

    /**
     * The implementation of executor service.
     */
    private final ExecutorService executorService;

    private TerrainExecutorService() {
        try {
            this.executorService = constructor.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Future<T> submit(final Callable<T> task) {
        return executorService.submit(task);
    }

    public <T> Future<T> submit(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    public Future<?> submit(final Runnable task) {
        return executorService.submit(task);
    }

    public void execute(final Runnable command) {
        executorService.execute(command);
    }
}
