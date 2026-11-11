package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test clock the parity cases set from {@code given.now}. */
class MutableClock extends Clock {

    private volatile Instant now = Instant.now();

    void set(Instant instant) {
        this.now = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
