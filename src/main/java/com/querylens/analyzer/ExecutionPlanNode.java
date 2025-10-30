package com.querylens.analyzer;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the SQL Server execution plan tree.
 * Each node corresponds to an operator (Scan, Seek, Join, Aggregate, etc.)
 */
@Data
public class ExecutionPlanNode {
    private String operatorType;          // e.g., "Table Scan", "Index Seek", "Hash Match"
    private String objectName;            // Table or index name
    private double estimatedCost;         // Cost from plan
    private double actualCost;            // Calculated from actual metrics
    private long estimatedRows;
    private long actualRows;
    private double elapsedTimeMs;         // Actual elapsed time in milliseconds
    private double cpuTimeMs;
    private long logicalReads;
    private long physicalReads;
    
    // Query text association
    private Integer startLine;            // Line number in original query
    private Integer endLine;
    private String queryFragment;         // The SQL fragment this operation relates to
    
    // Tree structure
    private List<ExecutionPlanNode> children = new ArrayList<>();
    private ExecutionPlanNode parent;
    
    // Cost attribution
    private double costPercentage;        // % of total query cost
    private String costCategory;          // "CRITICAL", "WARNING", "OK"
    
    // Issue classification
    private boolean isScanOperation;
    private boolean isExpensive;          // Cost > threshold
    private String issueType;             // "NON_SARGABLE", "MISSING_INDEX", "LATE_FILTER", etc.
    
    /**
     * Calculate cost percentage relative to root
     */
    public void calculateCostPercentage(double totalCost) {
        if (totalCost > 0) {
            this.costPercentage = (this.actualCost / totalCost) * 100;
            
            // Categorize
            if (costPercentage >= 20) {
                this.costCategory = "CRITICAL";
                this.isExpensive = true;
            } else if (costPercentage >= 10) {
                this.costCategory = "WARNING";
                this.isExpensive = true;
            } else {
                this.costCategory = "OK";
            }
        }
    }
    
    /**
     * Add child node and set parent reference
     */
    public void addChild(ExecutionPlanNode child) {
        this.children.add(child);
        child.setParent(this);
    }
    
    /**
     * Get all descendant nodes (DFS)
     */
    public List<ExecutionPlanNode> getAllDescendants() {
        List<ExecutionPlanNode> descendants = new ArrayList<>();
        for (ExecutionPlanNode child : children) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants());
        }
        return descendants;
    }
    
    /**
     * Check if this is a scan operation (expensive)
     */
    public boolean isScanOperation() {
        return operatorType != null && (
            operatorType.contains("Table Scan") ||
            operatorType.contains("Clustered Index Scan") ||
            operatorType.contains("Index Scan")
        );
    }
    
    /**
     * Check if this is a seek operation (good)
     */
    public boolean isSeekOperation() {
        return operatorType != null && (
            operatorType.contains("Index Seek") ||
            operatorType.contains("Clustered Index Seek")
        );
    }
    
    /**
     * Human-readable description
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(operatorType);
        if (objectName != null && !objectName.isEmpty()) {
            sb.append(" on ").append(objectName);
        }
        if (actualRows > 0) {
            sb.append(" (").append(formatNumber(actualRows)).append(" rows)");
        }
        return sb.toString();
    }
    
    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        } else {
            return String.valueOf(num);
        }
    }
}

