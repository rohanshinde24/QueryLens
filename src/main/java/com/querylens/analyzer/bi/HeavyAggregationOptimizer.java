package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects heavy aggregation operations that can be optimized.
 * 
 * Common in USC BI queries:
 * - STRING_AGG(DISTINCT ...) on large result sets
 * - COUNT(DISTINCT) with high cardinality
 * - Multiple aggregations in same query
 * - GROUP BY with many columns
 */
@Component
public class HeavyAggregationOptimizer {
    
    // Pattern: STRING_AGG with DISTINCT
    private static final Pattern STRING_AGG_DISTINCT = Pattern.compile(
        "STRING_AGG\\s*\\(\\s*DISTINCT\\s+([^,]+),",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: STRING_AGG without DISTINCT
    private static final Pattern STRING_AGG_PATTERN = Pattern.compile(
        "STRING_AGG\\s*\\(\\s*([^,]+),",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: COUNT(DISTINCT ...)
    private static final Pattern COUNT_DISTINCT = Pattern.compile(
        "COUNT\\s*\\(\\s*DISTINCT\\s+([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: Multiple CASE expressions in aggregates
    private static final Pattern CASE_IN_AGGREGATE = Pattern.compile(
        "(COUNT|SUM)\\s*\\(\\s*CASE\\s+WHEN",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: GROUP BY with many columns
    private static final Pattern GROUP_BY = Pattern.compile(
        "GROUP\\s+BY\\s+([^\\n]+(?:,\\s*[^\\n]+)*)",
        Pattern.CASE_INSENSITIVE
    );
    
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        String[] lines = sql.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Check for STRING_AGG with DISTINCT
            Matcher stringAggDistinct = STRING_AGG_DISTINCT.matcher(line);
            if (stringAggDistinct.find()) {
                bottlenecks.add(createStringAggBottleneck(
                    stringAggDistinct.group(1),
                    true,  // has DISTINCT
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            } else {
                // Check for STRING_AGG without DISTINCT
                Matcher stringAgg = STRING_AGG_PATTERN.matcher(line);
                if (stringAgg.find()) {
                    bottlenecks.add(createStringAggBottleneck(
                        stringAgg.group(1),
                        false,  // no DISTINCT
                        lineNumber,
                        line.trim(),
                        executionPlan
                    ));
                }
            }
            
            // Check for COUNT(DISTINCT)
            Matcher countDistinct = COUNT_DISTINCT.matcher(line);
            if (countDistinct.find()) {
                bottlenecks.add(createCountDistinctBottleneck(
                    countDistinct.group(1),
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for multiple CASE in aggregates
            Matcher caseAggregate = CASE_IN_AGGREGATE.matcher(line);
            if (caseAggregate.find()) {
                // Count how many CASE expressions
                long caseCount = line.toUpperCase().split("CASE WHEN").length - 1;
                if (caseCount >= 3) {
                    bottlenecks.add(createMultipleCaseBottleneck(
                        (int) caseCount,
                        lineNumber,
                        line.trim(),
                        executionPlan
                    ));
                }
            }
        }
        
        // Check GROUP BY complexity
        Matcher groupBy = GROUP_BY.matcher(sql);
        if (groupBy.find()) {
            String groupByClause = groupBy.group(1);
            int columnCount = groupByClause.split(",").length;
            if (columnCount >= 5) {
                int lineNum = findLineNumber(sql, groupBy.start());
                bottlenecks.add(createComplexGroupByBottleneck(
                    columnCount,
                    lineNum,
                    groupByClause,
                    executionPlan
                ));
            }
        }
        
        return bottlenecks;
    }
    
    /**
     * STRING_AGG bottleneck
     */
    private Bottleneck createStringAggBottleneck(
            String column, boolean hasDistinct, int lineNumber,
            String fragment, List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.HEAVY_AGGREGATION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        bottleneck.setCostPercentage(5.0);
        
        // Find aggregate operator in plan
        for (ExecutionPlanNode node : plan) {
            if (node.getOperatorType() != null && 
                (node.getOperatorType().contains("Aggregate") || 
                 node.getOperatorType().contains("Hash Match"))) {
                bottleneck.setCostPercentage(node.getCostPercentage() * 0.6);
                bottleneck.setTimeImpactSeconds(node.getElapsedTimeMs() / 1000.0 * 0.6);
            }
        }
        
        bottleneck.setProblemDescription(
            hasDistinct ? "STRING_AGG with DISTINCT on large result set" : 
                         "STRING_AGG on large result set"
        );
        
        bottleneck.setWhyItsASlow(
            "STRING_AGG " + (hasDistinct ? "with DISTINCT " : "") + 
            "must process and concatenate all values in memory. " +
            (hasDistinct ? "DISTINCT adds sorting/hashing overhead. " : "") +
            "For large groups, this can be CPU and memory intensive."
        );
        
        if (hasDistinct) {
            bottleneck.addFix("Pre-aggregate DISTINCT values in a CTE");
            bottleneck.addFix("Then apply STRING_AGG to pre-deduplicated set");
            
            String optimized = String.format("""
                -- Pre-aggregate in CTE:
                WITH distinct_vals AS (
                  SELECT DISTINCT 
                    group_key,
                    %s AS value
                  FROM table_name
                  WHERE ...
                )
                SELECT 
                  group_key,
                  STRING_AGG(value, ',') AS aggregated
                FROM distinct_vals
                GROUP BY group_key
                """, column);
            bottleneck.setOptimizedFragment(optimized);
        } else {
            bottleneck.addFix("Consider if DISTINCT is needed in source data");
            bottleneck.addFix("Or pre-filter to reduce rows before aggregation");
        }
        
        bottleneck.setExpectedImprovement("20-40% faster for large aggregations");
        
        return bottleneck;
    }
    
    /**
     * COUNT(DISTINCT) bottleneck
     */
    private Bottleneck createCountDistinctBottleneck(
            String column, int lineNumber, String fragment,
            List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.HEAVY_AGGREGATION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.INFO);
        bottleneck.setCostPercentage(3.0);
        
        bottleneck.setProblemDescription("COUNT(DISTINCT) may be expensive on high-cardinality columns");
        bottleneck.setWhyItsASlow(
            "COUNT(DISTINCT) requires sorting or hashing to find unique values, " +
            "which can be expensive with millions of rows and high cardinality."
        );
        
        bottleneck.addFix("Consider approximate count if exact isn't needed");
        bottleneck.addFix("Or pre-aggregate in indexed view if query runs frequently");
        
        return bottleneck;
    }
    
    /**
     * Multiple CASE expressions in aggregates
     */
    private Bottleneck createMultipleCaseBottleneck(
            int caseCount, int lineNumber, String fragment,
            List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.HEAVY_AGGREGATION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.INFO);
        
        bottleneck.setProblemDescription(
            String.format("%d CASE expressions in aggregation", caseCount)
        );
        
        bottleneck.setWhyItsASlow(
            "Multiple CASE evaluations in aggregates add CPU overhead. " +
            "Each CASE is evaluated for every row in the group."
        );
        
        bottleneck.addFix("Consider using FILTER clause if supported");
        bottleneck.addFix("Or pivot the data first in a CTE");
        bottleneck.addFix("Or use conditional aggregation: SUM(column) instead of COUNT(CASE)");
        
        return bottleneck;
    }
    
    /**
     * Complex GROUP BY
     */
    private Bottleneck createComplexGroupByBottleneck(
            int columnCount, int lineNumber, String groupByClause,
            List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.HEAVY_AGGREGATION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment("GROUP BY " + groupByClause);
        bottleneck.setSeverity(Bottleneck.Severity.INFO);
        
        bottleneck.setProblemDescription(
            String.format("GROUP BY with %d columns may have high cardinality", columnCount)
        );
        
        bottleneck.setWhyItsASlow(
            "Large GROUP BY clauses create many groups, increasing memory usage " +
            "and hash/sort operations."
        );
        
        bottleneck.addFix("Review if all GROUP BY columns are necessary");
        bottleneck.addFix("Consider aggregating in stages with CTEs");
        
        return bottleneck;
    }
    
    private int findLineNumber(String sql, int charPosition) {
        return (int) sql.substring(0, charPosition).chars().filter(c -> c == '\n').count() + 1;
    }
}

