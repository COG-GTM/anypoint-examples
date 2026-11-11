package org.mule.examples.springboot.cachescopewithfibonacci;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * The {@code calculateFibonacci} flow wrapped in {@code ee:cache} (object-store caching strategy keyed by the
 * {@code n} query parameter, {@code filterExpression="nocache == null"}, {@code FibonacciResponseGenerator}).
 * <p>
 * The Spring Cache is used through the {@link Cache} API rather than {@code @Cacheable} because a cache hit must be
 * distinguishable from a computation: Mule's response generator rewrites {@code cost} to 0 on a hit.
 */
@Service
public class FibonacciService {

    public static final String CACHE = "fibonacci";

    private static final Logger log = LoggerFactory.getLogger(FibonacciService.class);

    private final Cache cache;

    public FibonacciService(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE);
    }

    /**
     * @param n               the raw {@code n} query parameter of the message entering the cache scope (the cache key)
     * @param bypassCache     the message carries a {@code nocache} property, so the cache scope is skipped entirely
     * @param childrenNoCache the message carries a {@code nocache} query parameter, so {@code fibonacciRequest(n-1/n-2)}
     *                        is sent with {@code nocache=true}
     */
    public FibonacciResult calculate(String n, boolean bypassCache, boolean childrenNoCache) {
        if (!bypassCache) {
            FibonacciResult cached = cache.get(n, FibonacciResult.class);
            if (cached != null) {
                return cached.asCacheHit();
            }
        }
        FibonacciResult result = compute(n, childrenNoCache);
        if (!bypassCache) {
            cache.put(n, result);
        }
        return result;
    }

    private FibonacciResult compute(String n, boolean childrenNoCache) {
        log.info("aaa: Fibonacci: {}", n);
        long value = Long.parseLong(n.trim());
        if (value < 2) {
            return new FibonacciResult(n, 1);
        }
        // fibonacciRequest(n, cached) sends a fresh message whose only query parameter is n, so grandchildren are cached.
        FibonacciResult fib1 = calculate(Long.toString(value - 1), childrenNoCache, false);
        FibonacciResult fib2 = calculate(Long.toString(value - 2), childrenNoCache, false);
        return new FibonacciResult(Long.toString(Long.parseLong(fib1.value()) + Long.parseLong(fib2.value())),
                fib1.cost() + fib2.cost() + 1);
    }
}
