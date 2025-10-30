package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service that orchestrates all BI-focused detectors.
 * 
 * This analyzes SQL queries specifically for USC Advancement BI team patterns:
 * - Complex donor analytics
 * - Multi-million row fact tables
 * - Heavy aggregations
 * - Performance-critical reports
 */
@Service
public class BiQueryAnalysisService {
    
    @Autowired
    private NonSargableDetector nonSargableDetector;
    
    @Autowired
    private CorrelatedSubqueryDetector correlatedSubqueryDetector;
    
    @Autowired
    private OrConditionDetector orConditionDetector;
    
    @Autowired
    private LateFilterDetector lateFilterDetector;
    
    @Autowired
    private MissingIndexAnalyzer missingIndexAnalyzer;
    
    @Autowired
    private HeavyAggregationOptimizer heavyAggregationOptimizer;
    
    /**
     * Analyze a SQL query and identify all bottlenecks
     */
    public BiAnalysisResult analyzeQuery(String sql, List<ExecutionPlanNode> executionPlan) {
        
        List<Bottleneck> allBottlenecks = new ArrayList<>();
        
        // Run all detectors
        allBottlenecks.addAll(nonSargableDetector.detect(sql, executionPlan));
        allBottlenecks.addAll(correlatedSubqueryDetector.detect(sql, executionPlan));
        allBottlenecks.addAll(orConditionDetector.detect(sql, executionPlan));
        allBottlenecks.addAll(lateFilterDetector.detect(sql, executionPlan));
        allBottlenecks.addAll(missingIndexAnalyzer.detect(sql, executionPlan));
        allBottlenecks.addAll(heavyAggregationOptimizer.detect(sql, executionPlan));
        
        // Sort by severity and cost
        List<Bottleneck> sortedBottlenecks = allBottlenecks.stream()
            .sorted(Comparator
                .comparing(Bottleneck::getSeverity)
                .thenComparing(Bottleneck::getCostPercentage, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        
        // Calculate total estimated runtime and improvement
        double totalCost = executionPlan.stream()
            .mapToDouble(ExecutionPlanNode::getActualCost)
            .sum();
        
        double totalImpactTime = sortedBottlenecks.stream()
            .mapToDouble(b -> b.getTimeImpactSeconds() != null ? b.getTimeImpactSeconds().doubleValue() : 0.0)
            .sum();
        
        // Build result
        BiAnalysisResult result = new BiAnalysisResult();
        result.setBottlenecks(sortedBottlenecks);
        result.setTotalBottlenecks(sortedBottlenecks.size());
        result.setCriticalCount((int) sortedBottlenecks.stream()
            .filter(b -> b.getSeverity() == Bottleneck.Severity.CRITICAL).count());
        result.setWarningCount((int) sortedBottlenecks.stream()
            .filter(b -> b.getSeverity() == Bottleneck.Severity.WARNING).count());
        result.setInfoCount((int) sortedBottlenecks.stream()
            .filter(b -> b.getSeverity() == Bottleneck.Severity.INFO).count());
        
        result.setTotalCostMs(totalCost);
        result.setTotalImpactSeconds(totalImpactTime);
        
        // Calculate potential improvement
        double potentialSavings = sortedBottlenecks.stream()
            .mapToDouble(Bottleneck::getCostPercentage)
            .sum();
        result.setPotentialImprovementPercent(Math.min(potentialSavings, 95.0)); // Cap at 95%
        
        return result;
    }
    
    /**
     * Result object containing all analysis
     */
    public static class BiAnalysisResult {
        private List<Bottleneck> bottlenecks = new ArrayList<>();
        private int totalBottlenecks;
        private int criticalCount;
        private int warningCount;
        private int infoCount;
        private double totalCostMs;
        private double totalImpactSeconds;
        private double potentialImprovementPercent;
        
        // Getters and setters
        public List<Bottleneck> getBottlenecks() { return bottlenecks; }
        public void setBottlenecks(List<Bottleneck> bottlenecks) { this.bottlenecks = bottlenecks; }
        
        public int getTotalBottlenecks() { return totalBottlenecks; }
        public void setTotalBottlenecks(int totalBottlenecks) { this.totalBottlenecks = totalBottlenecks; }
        
        public int getCriticalCount() { return criticalCount; }
        public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
        
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
        
        public int getInfoCount() { return infoCount; }
        public void setInfoCount(int infoCount) { this.infoCount = infoCount; }
        
        public double getTotalCostMs() { return totalCostMs; }
        public void setTotalCostMs(double totalCostMs) { this.totalCostMs = totalCostMs; }
        
        public double getTotalImpactSeconds() { return totalImpactSeconds; }
        public void setTotalImpactSeconds(double totalImpactSeconds) { this.totalImpactSeconds = totalImpactSeconds; }
        
        public double getPotentialImprovementPercent() { return potentialImprovementPercent; }
        public void setPotentialImprovementPercent(double potentialImprovementPercent) { 
            this.potentialImprovementPercent = potentialImprovementPercent; 
        }
    }
}

