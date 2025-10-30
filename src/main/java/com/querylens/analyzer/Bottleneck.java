package com.querylens.analyzer;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a performance bottleneck identified in a query.
 * Links an issue to specific query lines and provides actionable fixes.
 */
@Data
public class Bottleneck {
    
    public enum Severity {
        CRITICAL,   // > 20% of total cost
        WARNING,    // 10-20% of cost
        INFO        // < 10% but still notable
    }
    
    public enum IssueType {
        NON_SARGABLE_PREDICATE,
        CORRELATED_SUBQUERY,
        OR_CONDITION,
        LATE_FILTER,
        MISSING_INDEX,
        HEAVY_AGGREGATION,
        EXPENSIVE_JOIN,
        CARTESIAN_PRODUCT
    }
    
    private Severity severity;
    private IssueType issueType;
    
    // Location in query
    private Integer lineNumber;
    private Integer startLine;
    private Integer endLine;
    private String queryFragment;          // The problematic SQL text
    
    // Cost impact
    private double costPercentage;         // % of total runtime
    private Double timeImpactSeconds;      // Estimated seconds (nullable)
    private String operatorName;           // "Table Scan on GIVING_DETAIL"
    
    // Problem description
    private String problemDescription;     // What's wrong
    private String whyItsASlow;            // Why it's slow
    private long executionCount;           // How many times it executed (for subqueries)
    
    // Fix recommendations
    private List<String> fixes = new ArrayList<>();          // Text descriptions
    private List<String> fixQueries = new ArrayList<>();     // SQL to fix it
    private String optimizedFragment;                        // Rewritten SQL
    private String expectedImprovement;                      // "~87% reduction in logical reads"
    
    // Related execution plan node
    private ExecutionPlanNode relatedNode;
    
    /**
     * Create a bottleneck from an execution plan node
     */
    public static Bottleneck fromNode(ExecutionPlanNode node) {
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setOperatorName(node.getDescription());
        bottleneck.setCostPercentage(node.getCostPercentage());
        bottleneck.setTimeImpactSeconds(node.getElapsedTimeMs() / 1000.0);
        bottleneck.setStartLine(node.getStartLine());
        bottleneck.setEndLine(node.getEndLine());
        bottleneck.setQueryFragment(node.getQueryFragment());
        bottleneck.setRelatedNode(node);
        
        // Set severity based on cost
        if (node.getCostPercentage() >= 20) {
            bottleneck.setSeverity(Severity.CRITICAL);
        } else if (node.getCostPercentage() >= 10) {
            bottleneck.setSeverity(Severity.WARNING);
        } else {
            bottleneck.setSeverity(Severity.INFO);
        }
        
        return bottleneck;
    }
    
    /**
     * Add a fix recommendation
     */
    public void addFix(String description) {
        this.fixes.add(description);
    }
    
    /**
     * Add SQL to implement a fix
     */
    public void addFixQuery(String sql) {
        this.fixQueries.add(sql);
    }
    
    /**
     * Get emoji for severity
     */
    public String getSeverityEmoji() {
        return switch (severity) {
            case CRITICAL -> "🔴";
            case WARNING -> "🟡";
            case INFO -> "🔵";
        };
    }
    
    /**
     * Get formatted cost impact
     */
    public String getFormattedCostImpact() {
        return String.format("%.1f%% of runtime (%.1fs)", costPercentage, timeImpactSeconds);
    }
    
    /**
     * Get issue type description
     */
    public String getIssueTypeDescription() {
        return switch (issueType) {
            case NON_SARGABLE_PREDICATE -> "Non-SARGABLE Predicate";
            case CORRELATED_SUBQUERY -> "Correlated Subquery";
            case OR_CONDITION -> "OR Condition Blocking Index";
            case LATE_FILTER -> "Late Filter Application";
            case MISSING_INDEX -> "Missing Index";
            case HEAVY_AGGREGATION -> "Heavy Aggregation";
            case EXPENSIVE_JOIN -> "Expensive JOIN Operation";
            case CARTESIAN_PRODUCT -> "Cartesian Product";
        };
    }
}

