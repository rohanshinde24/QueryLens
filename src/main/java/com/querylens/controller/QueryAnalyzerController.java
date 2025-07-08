package com.querylens.controller;

import com.querylens.optimizer.QueryOptimizerService;
import com.querylens.optimizer.QueryRewriteService;
import com.querylens.dto.QueryRequest;
import com.querylens.model.QueryMetrics;
import com.querylens.service.QueryAnalyzerService;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.querylens.dto.AnalysisResponse;

// @RestController
// @RequestMapping("/analyze")
// public class QueryAnalyzerController {

//     @Autowired
//     private QueryAnalyzerService analyzerService;

//     @Autowired
//     private QueryOptimizerService optimizerService;

//     @PostMapping
//     public AnalysisResponse analyze(@RequestBody QueryRequest request) {
//         String sql = request.getSql();
//         List<String> plan = analyzerService.getRawPlanLines(sql);      // you might expose plan lines
//         QueryMetrics metrics = analyzerService.analyzeQuery(sql);
//         List<String> suggestions = optimizerService.suggestOptimizations(sql, plan);

//         return new AnalysisResponse(metrics, suggestions);
//     }
// }
@RestController
@RequestMapping("/analyze")
public class QueryAnalyzerController {

    @Autowired
    private QueryAnalyzerService analyzerService;

    @Autowired
    private QueryOptimizerService optimizerService;

    @Autowired
    private QueryRewriteService rewriteService;         // <-- new

    @PostMapping
    public AnalysisResponse analyze(@RequestBody QueryRequest request) {
        String sql = request.getSql();
        List<String> planLines = analyzerService.getRawPlanLines(sql);
        QueryMetrics metrics = analyzerService.analyzeQuery(sql);
        List<String> suggestions = optimizerService.suggestOptimizations(sql, planLines);

        Optional<String> optimized = rewriteService.rewrite(sql, planLines);

        return new AnalysisResponse(
            metrics,
            suggestions,
            optimized.orElse(null)
        );
    }
}