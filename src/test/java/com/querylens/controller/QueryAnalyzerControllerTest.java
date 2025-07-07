package com.querylens.controller;

import com.querylens.model.QueryMetrics;
import com.querylens.service.QueryAnalyzerService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryAnalyzerController.class)
class QueryAnalyzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryAnalyzerService analyzerService;

    @Test
    void analyzeEndpoint_returnsExpectedJson() throws Exception {
        String sql = "SELECT 1";
        QueryMetrics stubMetrics = new QueryMetrics();
        stubMetrics.setRawOutput("StubOutput");
        stubMetrics.setExecutionTime(0.5);
        stubMetrics.setRowsProcessed(1);
        stubMetrics.setCostEstimate(0.0);
        stubMetrics.setStatementType("SELECT");
        stubMetrics.setTablesUsed(List.of()); 
        stubMetrics.setHasWhereClause(false);
        stubMetrics.setHasJoinClause(false);
        stubMetrics.setHasLimitClause(false);

        when(analyzerService.analyzeQuery(sql))
            .thenReturn(stubMetrics);

        mockMvc.perform(post("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT 1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rawOutput").value("StubOutput"))
            .andExpect(jsonPath("$.executionTime").value(0.5))
            .andExpect(jsonPath("$.rowsProcessed").value(1))
            .andExpect(jsonPath("$.costEstimate").value(0.0))
            .andExpect(jsonPath("$.statementType").value("SELECT"))
            .andExpect(jsonPath("$.tablesUsed").isArray())
            .andExpect(jsonPath("$.hasWhereClause").value(false))
            .andExpect(jsonPath("$.hasJoinClause").value(false))
            .andExpect(jsonPath("$.hasLimitClause").value(false));
    }
}
