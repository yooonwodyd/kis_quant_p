package com.kisquant.shared.time;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider() {
        this(Clock.systemDefaultZone());
    }

    SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
