package com.kisquant.order.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kisquant.marketdata.application.DailyCandle;
import com.kisquant.marketdata.application.MarketDataQueryService;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OperationApiControllerTest {

    @Test
    void symbolsReturnsConfiguredAllowedSymbols() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationApiController(
                null,
                null,
                null,
                null,
                null,
                List.of(Symbol.of("001510"))))
                .build();

        mvc.perform(get("/api/kis/symbols"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("001510"));
    }

    @Test
    void dailyCandlesReturnsLimitedDailyClosePrices() throws Exception {
        MarketDataQueryService marketData = mock(MarketDataQueryService.class);
        when(marketData.dailyCandles(Symbol.of("005930"), 2)).thenReturn(List.of(
                new DailyCandle(LocalDate.parse("2026-05-11"), Money.won(70_000L)),
                new DailyCandle(LocalDate.parse("2026-05-10"), Money.won(69_500L))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationApiController(
                null,
                marketData,
                null,
                null,
                null,
                List.of(Symbol.of("001510"))))
                .build();

        mvc.perform(get("/api/kis/daily-candles/005930").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-11"))
                .andExpect(jsonPath("$[0].closePrice").value(70000))
                .andExpect(jsonPath("$[1].date").value("2026-05-10"))
                .andExpect(jsonPath("$[1].closePrice").value(69500));
    }
}
