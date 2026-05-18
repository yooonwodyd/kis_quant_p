package com.kisquant.operationlog.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kisquant.operationlog.application.OperationLogBuffer;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OperationLogApiControllerTest {

    @Test
    void returnsRecentOperationLogsWithOptionalCategoryFilter() throws Exception {
        OperationLogBuffer buffer = new OperationLogBuffer(() -> Instant.parse("2026-05-08T01:00:00Z"), 100);
        buffer.append(OperationLogLevel.INFO, OperationLogCategory.POLLING, "polling 완료", "{\"synced\":0}");
        buffer.append(OperationLogLevel.ERROR, OperationLogCategory.POLLING_ERROR, "polling 실패", "{\"message\":\"timeout\"}");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationLogApiController(buffer)).build();

        mvc.perform(get("/api/admin/operation-logs")
                        .param("category", "POLLING_ERROR")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("ERROR"))
                .andExpect(jsonPath("$[0].category").value("POLLING_ERROR"))
                .andExpect(jsonPath("$[0].message").value("polling 실패"))
                .andExpect(jsonPath("$[0].payloadJson").value("{\"message\":\"timeout\"}"));
    }
}
