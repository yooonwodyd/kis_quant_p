package com.kisquant.web;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.kisquant.kis.KisClient;
import com.kisquant.order.OrderGuard;

@WebMvcTest(Controller.class)
class ControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KisClient kisClient;

	@MockitoBean
	private OrderGuard orderGuard;

	@Test
	void rejectsDangerousOrderBeforeCallingKis() throws Exception {
		doThrow(new IllegalArgumentException("Only symbol 001510 is allowed"))
				.when(this.orderGuard)
				.validateSymbol("005930");

		this.mockMvc.perform(get("/api/kis/quotes/005930"))
				.andExpect(status().isBadRequest());
	}
}
