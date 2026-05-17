package com.kisquant.account.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountQueryService {

    private final AccountGateway gateway;

    public AccountQueryService(AccountGateway gateway) {
        this.gateway = gateway;
    }

    public AccountBalance balance() {
        return gateway.balance();
    }

    public List<Holding> holdings() {
        return gateway.holdings();
    }

    public BuyableOrderAmount buyable(Symbol symbol, Money price) {
        return gateway.buyable(symbol, price);
    }
}
