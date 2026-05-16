package com.kisquant.marketdata.application;

import com.kisquant.shared.domain.Symbol;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataQueryService {

    private final MarketDataGateway gateway;

    public MarketDataQueryService(MarketDataGateway gateway) {
        this.gateway = gateway;
    }

    public Quote quote(Symbol symbol) {
        return gateway.quote(symbol);
    }

    public List<DailyCandle> dailyCandles(Symbol symbol) {
        return gateway.dailyCandles(symbol);
    }

    public List<DailyCandle> dailyCandles(Symbol symbol, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return gateway.dailyCandles(symbol, limit);
    }
}
