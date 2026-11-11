package org.mule.examples.springboot.cachescopewithfibonacci;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** {@code ee:object-store-caching-strategy} with the default in-memory store → Spring's simple (ConcurrentMap) cache. */
@Configuration
@EnableCaching
public class CacheConfig {
}
