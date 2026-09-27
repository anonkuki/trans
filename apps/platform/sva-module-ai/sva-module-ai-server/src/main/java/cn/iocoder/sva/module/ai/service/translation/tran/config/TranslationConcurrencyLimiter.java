package cn.iocoder.sva.module.ai.service.translation.tran.config;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * Process-local fair limiter for remote translation model calls.
 */
public final class TranslationConcurrencyLimiter {

    private final Semaphore permits;

    public TranslationConcurrencyLimiter(int maximumConcurrency) {
        this.permits = new Semaphore(Math.max(1, maximumConcurrency), true);
    }

    public <T> T execute(Callable<T> operation) throws Exception {
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            return operation.call();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            if (acquired) {
                permits.release();
            }
        }
    }
}
