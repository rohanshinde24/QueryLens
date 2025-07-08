

// package com.querylens.controller;

// import com.querylens.model.QueryMetrics;
// import com.querylens.optimizer.QueryOptimizerService;
// import com.querylens.optimizer.QueryRewriteService;
// import com.querylens.service.QueryAnalyzerService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// import java.util.List;
// import java.util.Optional;

// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @ExtendWith(MockitoExtension.class)
// class QueryAnalyzerControllerTest {

//     @InjectMocks
//     private QueryAnalyzerController controller;

//     @Mock
//     private QueryAnalyzerService analyzerService;

//     @Mock
//     private QueryOptimizerService optimizerService;
    
//     @Mock
//     private QueryRewriteService rewriteService;

//     private MockMvc mockMvc;

//     @BeforeEach
//     void setUp() {
//         mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
//     }

//     @Test
//     void analyzeEndpoint_includesAllDetectorDescriptions() throws Exception {
//         // 1) Use a SQL that has SELECT * and a function in WHERE
//         String sql = "SELECT * FROM users WHERE YEAR(created_at)=2020";

//         // 2) Stub the metrics returned by the analyzer
//         QueryMetrics stubMetrics = new QueryMetrics();
//         stubMetrics.setRawOutput("Seq Scan on users  (cost=0.00..10.00 rows=100 width=32)");
//         stubMetrics.setExecutionTime(0.120);
//         stubMetrics.setRowsProcessed(100);
//         stubMetrics.setCostEstimate(10.0);
//         stubMetrics.setStatementType("SELECT");
//         stubMetrics.setTablesUsed(List.of("users"));
//         stubMetrics.setHasWhereClause(true);
//         stubMetrics.setHasJoinClause(false);
//         stubMetrics.setHasLimitClause(false);

//         // 3) Make the plan lines trigger the MissingIndexScanDetector
//         List<String> planLines = List.of(
//             "Seq Scan on users  (cost=0.00..10.00 rows=100 width=32) (actual time=0.050..0.100 rows=100 loops=1)"
//         );

//         // 4) Define the expected detector descriptions
//         List<String> expectedSuggestions = List.of(
//             "Avoid using SELECT *; list only the columns you need to reduce I/O.",
//             "Non‐sargable predicate detected; avoid wrapping indexed columns in functions to allow index usage.",
//             "Sequential scan detected; consider adding an index on the filtered/joined columns."
//         );

//         // 5) Stub service methods
//         when(analyzerService.analyzeQuery(sql)).thenReturn(stubMetrics);
//         when(analyzerService.getRawPlanLines(sql)).thenReturn(planLines);
//         when(optimizerService.suggestOptimizations(sql, planLines))
//             .thenReturn(expectedSuggestions);
//         // stub rewriteService so it never NPEs
//         when(rewriteService.rewrite(sql, planLines))
//             .thenReturn(Optional.of("SELECT id, name FROM users"));
//         // 6) Perform the POST and verify suggestions
//         mockMvc.perform(post("/analyze")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content("{\"sql\":\"" + sql + "\"}"))
//             .andExpect(status().isOk())
//             // Verify suggestions array contents in order
//             .andExpect(jsonPath("$.suggestions[0]")
//                 .value(expectedSuggestions.get(0)))
//             .andExpect(jsonPath("$.suggestions[1]")
//                 .value(expectedSuggestions.get(1)))
//             .andExpect(jsonPath("$.suggestions[2]")
//                 .value(expectedSuggestions.get(2)))
//             .andExpect(jsonPath("$.optimizedSql")
//                 .value("SELECT id, name FROM users"));
//     }
// }
package com.querylens.controller;

import com.querylens.model.QueryMetrics;
import com.querylens.optimizer.QueryOptimizerService;
import com.querylens.optimizer.QueryRewriteService;
import com.querylens.service.QueryAnalyzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueryAnalyzerControllerTest {

    @InjectMocks
    private QueryAnalyzerController controller;

    @Mock
    private QueryAnalyzerService analyzerService;

    @Mock
    private QueryOptimizerService optimizerService;

    @Mock
    private QueryRewriteService rewriteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void analyzeEndpoint_returnsMetricsSuggestionsAndOptimizedSql() throws Exception {
        // 1) SQL under test
        String sql = "SELECT * FROM users WHERE YEAR(created_at)=2020";

        // 2) Stubbed metrics
        QueryMetrics metrics = new QueryMetrics();
        metrics.setRawOutput("Seq Scan on users  (cost=0.00..10.00 rows=100 width=32)");
        metrics.setExecutionTime(0.120);
        metrics.setRowsProcessed(100);
        metrics.setCostEstimate(10.0);
        metrics.setStatementType("SELECT");
        metrics.setTablesUsed(List.of("users"));
        metrics.setHasWhereClause(true);
        metrics.setHasJoinClause(false);
        metrics.setHasLimitClause(false);

        // 3) Stubbed plan lines
        List<String> planLines = List.of(
            "Seq Scan on users  (cost=0.00..10.00 rows=100 width=32) (actual time=0.050..0.100 rows=100 loops=1)"
        );

        // 4) Expected suggestions
        List<String> suggestions = List.of(
            "Avoid using SELECT *; list only the columns you need to reduce I/O.",
            "Non‐sargable predicate detected; avoid wrapping indexed columns in functions to allow index usage.",
            "Sequential scan detected; consider adding an index on the filtered/joined columns."
        );

        // 5) Stub all service calls
        when(analyzerService.analyzeQuery(sql)).thenReturn(metrics);
        when(analyzerService.getRawPlanLines(sql)).thenReturn(planLines);
        when(optimizerService.suggestOptimizations(sql, planLines)).thenReturn(suggestions);
        when(rewriteService.rewrite(sql, planLines))
            .thenReturn(Optional.of("SELECT id, name FROM users"));

        // 6) Execute & verify full JSON payload
        mockMvc.perform(post("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"" + sql + "\"}"))
            .andExpect(status().isOk())

            // --- metrics ---
            .andExpect(jsonPath("$.metrics.rawOutput").value(metrics.getRawOutput()))
            .andExpect(jsonPath("$.metrics.executionTime").value(metrics.getExecutionTime()))
            .andExpect(jsonPath("$.metrics.rowsProcessed").value(metrics.getRowsProcessed()))
            .andExpect(jsonPath("$.metrics.costEstimate").value(metrics.getCostEstimate()))
            .andExpect(jsonPath("$.metrics.statementType").value(metrics.getStatementType()))
            .andExpect(jsonPath("$.metrics.tablesUsed[0]").value("users"))
            .andExpect(jsonPath("$.metrics.hasWhereClause").value(true))
            .andExpect(jsonPath("$.metrics.hasJoinClause").value(false))
            .andExpect(jsonPath("$.metrics.hasLimitClause").value(false))

            // --- suggestions ---
            .andExpect(jsonPath("$.suggestions[0]").value(suggestions.get(0)))
            .andExpect(jsonPath("$.suggestions[1]").value(suggestions.get(1)))
            .andExpect(jsonPath("$.suggestions[2]").value(suggestions.get(2)))

            // --- optimized SQL ---
            .andExpect(jsonPath("$.optimizedSql")
                .value("SELECT id, name FROM users"));
    }
}
