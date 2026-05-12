package com.kisquant.shared.time;

import java.time.Instant;

@FunctionalInterface
public interface CurrentTimeProvider {

    Instant now();
}
