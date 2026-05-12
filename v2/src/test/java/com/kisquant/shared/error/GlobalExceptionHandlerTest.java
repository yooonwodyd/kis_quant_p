package com.kisquant.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class GlobalExceptionHandlerTest {

    @Test
    void illegalArgumentExceptionReturnsCommonBadRequestResponse() throws Exception {
        MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(get("/boom"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청"))
                .andExpect(jsonPath("$.path").value("/boom"));
    }

    @Controller
    static class FailingController {

        @GetMapping("/boom")
        void boom() {
            throw new IllegalArgumentException("잘못된 요청");
        }
    }
}
