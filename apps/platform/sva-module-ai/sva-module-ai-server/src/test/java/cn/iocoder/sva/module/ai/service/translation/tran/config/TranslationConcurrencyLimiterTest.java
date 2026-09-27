package cn.iocoder.sva.module.ai.service.translation.tran.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationConcurrencyLimiterTest {

    @Test
    void limitsConcurrentProtectedOperations() throws Exception {
        TranslationConcurrencyLimiter limiter = new TranslationConcurrencyLimiter(2);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch firstWaveEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 6; i++) {
                futures.add(executor.submit(() -> limiter.execute(() -> {
                    int current = active.incrementAndGet();
                    maximum.accumulateAndGet(current, Math::max);
                    firstWaveEntered.countDown();
                    try {
                        assertTrue(release.await(2, TimeUnit.SECONDS));
                        return current;
                    } finally {
                        active.decrementAndGet();
                    }
                })));
            }

            assertTrue(firstWaveEntered.await(2, TimeUnit.SECONDS));
            assertEquals(2, maximum.get());
            release.countDown();
            for (Future<Integer> future : futures) {
                future.get(2, TimeUnit.SECONDS);
            }
            assertEquals(2, maximum.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void releasesPermitWhenProtectedOperationFails() throws Exception {
        TranslationConcurrencyLimiter limiter = new TranslationConcurrencyLimiter(1);

        assertThrows(IllegalStateException.class,
                () -> limiter.execute(() -> { throw new IllegalStateException("boom"); }));
        assertEquals("next", limiter.execute(() -> "next"));
    }
}
