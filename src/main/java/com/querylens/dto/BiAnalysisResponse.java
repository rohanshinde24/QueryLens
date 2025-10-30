package com.querylens.dto;

import com.querylens.analyzer.Bottleneck;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for BI query analysis
 */
@Data
public class BiAnalysisResponse {
    private List<Bottleneck> bottlenecks = new ArrayList<>();
    private int totalBottlenecks;
    private int criticalCount;
    private int warningCount;
    private int infoCount;
    private double estimatedBaselineSeconds;
    private double potentialImprovementPercent;
    private String formattedReport;         // Optional: Pre-formatted text report
}

