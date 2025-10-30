package com.querylens.controller;

import com.querylens.analyzer.bi.BiQueryAnalysisService;
import com.querylens.analyzer.bi.BiQueryAnalysisService.BiAnalysisResult;
import com.querylens.analyzer.bi.ResultsFormatter;
import com.querylens.analyzer.ExecutionPlanNode;
import com.querylens.dto.BiAnalysisRequest;
import com.querylens.dto.BiAnalysisResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST API for BI-focused query analysis.
 * 
 * Endpoints:
 * - POST /api/bi/analyze - Analyze a query and get bottleneck report
 * - POST /api/bi/analyze/formatted - Get formatted text report
 */
@RestController
@RequestMapping("/api/bi")
@CrossOrigin(origins = {"http://localhost:3000", "http://frontend:3000"})
public class BiAnalysisController {
    
    @Autowired
    private BiQueryAnalysisService analysisService;
    
    @Autowired
    private ResultsFormatter formatter;
    
    /**
     * Analyze a query and return structured bottleneck data
     */
    @PostMapping("/analyze")
    public BiAnalysisResponse analyze(@RequestBody BiAnalysisRequest request) {
        
        // For now, create mock execution plan
        // TODO: Parse actual execution plan from request or run EXPLAIN
        List<ExecutionPlanNode> executionPlan = createMockExecutionPlan(request.getSql());
        
        // Run analysis
        BiAnalysisResult analysisResult = analysisService.analyzeQuery(
            request.getSql(), 
            executionPlan
        );
        
        // Build response
        BiAnalysisResponse response = new BiAnalysisResponse();
        response.setBottlenecks(analysisResult.getBottlenecks());
        response.setTotalBottlenecks(analysisResult.getTotalBottlenecks());
        response.setCriticalCount(analysisResult.getCriticalCount());
        response.setWarningCount(analysisResult.getWarningCount());
        response.setInfoCount(analysisResult.getInfoCount());
        response.setPotentialImprovementPercent(analysisResult.getPotentialImprovementPercent());
        response.setEstimatedBaselineSeconds(analysisResult.getTotalCostMs() / 1000.0);
        
        return response;
    }
    
    /**
     * Get formatted text report (for CLI/console display)
     */
    @PostMapping("/analyze/formatted")
    public String analyzeFormatted(@RequestBody BiAnalysisRequest request) {
        
        List<ExecutionPlanNode> executionPlan = createMockExecutionPlan(request.getSql());
        BiAnalysisResult analysisResult = analysisService.analyzeQuery(request.getSql(), executionPlan);
        
        return formatter.format(analysisResult, request.getSql());
    }
    
    /**
     * Create mock execution plan based on query patterns
     * TODO: Replace with actual execution plan parser
     */
    private List<ExecutionPlanNode> createMockExecutionPlan(String sql) {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        // Estimate baseline runtime based on query complexity
        double estimatedTime = estimateQueryTime(sql);
        
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(estimatedTime);
        root.setElapsedTimeMs(estimatedTime);
        
        // If query has YEAR(), create expensive scan
        if (sql.toUpperCase().contains("YEAR(")) {
            ExecutionPlanNode scan = new ExecutionPlanNode();
            scan.setOperatorType("Table Scan");
            scan.setObjectName("fact_table");
            scan.setActualCost(estimatedTime * 0.7); // 70% of cost
            scan.setElapsedTimeMs(estimatedTime * 0.7);
            scan.setActualRows(10_000_000);
            scan.setLogicalReads(1_500_000);
            scan.calculateCostPercentage(estimatedTime);
            nodes.add(scan);
        }
        
        // If query has correlated subquery
        if (sql.matches("(?is).*\\(\\s*SELECT.*FROM.*WHERE.*\\).*AS\\s+\\w+.*")) {
            ExecutionPlanNode subq = new ExecutionPlanNode();
            subq.setOperatorType("Nested Loops");
            subq.setActualCost(estimatedTime * 0.18);
            subq.setElapsedTimeMs(estimatedTime * 0.18);
            subq.setActualRows(50000);
            subq.calculateCostPercentage(estimatedTime);
            nodes.add(subq);
        }
        
        // Add aggregate if GROUP BY present
        if (sql.toUpperCase().contains("GROUP BY")) {
            ExecutionPlanNode agg = new ExecutionPlanNode();
            agg.setOperatorType("Hash Match (Aggregate)");
            agg.setActualCost(estimatedTime * 0.07);
            agg.setElapsedTimeMs(estimatedTime * 0.07);
            agg.calculateCostPercentage(estimatedTime);
            nodes.add(agg);
        }
        
        nodes.add(0, root);
        return nodes;
    }
    
    private double estimateQueryTime(String sql) {
        // Simple heuristics
        double baseTime = 5000; // 5 seconds baseline
        
        if (sql.toUpperCase().contains("YEAR(")) baseTime *= 3;
        if (sql.toUpperCase().contains("COALESCE")) baseTime *= 1.5;
        if (sql.contains("SELECT") && sql.substring(sql.indexOf("SELECT")).contains("(SELECT")) {
            baseTime *= 2; // Correlated subquery
        }
        if (sql.toUpperCase().contains("STRING_AGG")) baseTime *= 1.2;
        
        return baseTime;
    }
}

