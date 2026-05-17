package com.kisquant.account.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import java.util.List;

public interface AccountGateway {

    AccountBalance balance();

    List<Holding> holdings();

    BuyableOrderAmount buyable(Symbol symbol, Money price);
}
