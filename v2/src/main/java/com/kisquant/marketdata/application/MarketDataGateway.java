package com.kisquant.marketdata.application;

import com.kisquant.shared.domain.Symbol;
import java.util.List;

public interface MarketDataGateway {

    Quote quote(Symbol symbol);

    List<DailyCandle> dailyCandles(Symbol symbol);

    default List<DailyCandle> dailyCandles(Symbol symbol, int limit) {
        return dailyCandles(symbol).stream()
                .limit(limit)
                .toList();
    }
}
