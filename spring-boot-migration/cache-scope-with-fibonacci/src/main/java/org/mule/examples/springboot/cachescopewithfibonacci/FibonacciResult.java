package org.mule.examples.springboot.cachescopewithfibonacci;

/** Payload and {@code cost} outbound property of one {@code calculateFibonacci} invocation. */
public record FibonacciResult(String value, long cost) {

    /** What {@code FibonacciResponseGenerator} returns on a cache hit: the cached payload with cost 0. */
    FibonacciResult asCacheHit() {
        return new FibonacciResult(value, 0);
    }
}
