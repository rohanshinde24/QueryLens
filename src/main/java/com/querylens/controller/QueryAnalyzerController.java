package com.querylens.controller;

import com.querylens.model.QueryRequest;
import com.querylens.model.QueryMetrics;
import com.querylens.service.QueryAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyze")
public class QueryAnalyzerController {

    @Autowired
    private QueryAnalyzerService analyzerService;

    @PostMapping
    public QueryMetrics analyze(@RequestBody QueryRequest request) {
        return analyzerService.analyzeQuery(request.getSql());
    }
}