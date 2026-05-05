package com.kisquant.kis;

import com.kisquant.web.CallResult;

public interface KisClient {

	CallResult issueToken();

	CallResult balance();
}
