package com.kisquant.marketdata.application;

import com.kisquant.shared.domain.Money;
import java.time.LocalDate;

public record DailyCandle(LocalDate date, Money closePrice) {
}
