package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes execution plans to identify missing indexes.
 * 
 * Looks for:
 * - Table Scan / Clustered Index Scan operations
 * - High logical reads
 * - Expensive scan operators
 * 
 * Generates covering index recommendations based on:
 * - WHERE clause predicates
 * - JOIN keys
 * - SELECT columns (for INCLUDE)
 */
@Component
public class MissingIndexAnalyzer {
    
    // Extract WHERE clause predicates
    private static final Pattern WHERE_PREDICATE = Pattern.compile(
        "WHERE\\s+([\\w.]+)\\s*([=<>]+|IN|LIKE|BETWEEN)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Extract JOIN conditions
    private static final Pattern JOIN_CONDITION = Pattern.compile(
        "ON\\s+([\\w.]+)\\s*=\\s*([\\w.]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        // Find all scan operations in the plan
        for (ExecutionPlanNode node : executionPlan) {
            if (node.isScanOperation() && node.getCostPercentage() >= 5.0) {
                
                // Extract table name from node
                String tableName = node.getObjectName();
                if (tableName == null || tableName.isEmpty()) {
                    continue;
                }
                
                // Find predicates and join keys for this table
                Set<String> keyColumns = extractKeyColumns(sql, tableName);
                Set<String> includeColumns = extractIncludeColumns(sql, tableName);
                
                bottlenecks.add(createMissingIndexBottleneck(
                    node,
                    tableName,
                    keyColumns,
                    includeColumns,
                    sql
                ));
            }
        }
        
        return bottlenecks;
    }
    
    private Bottleneck createMissingIndexBottleneck(
            ExecutionPlanNode scanNode,
            String tableName,
            Set<String> keyColumns,
            Set<String> includeColumns,
            String sql) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.MISSING_INDEX);
        bottleneck.setOperatorName(scanNode.getDescription());
        bottleneck.setCostPercentage(scanNode.getCostPercentage());
        bottleneck.setTimeImpactSeconds(scanNode.getElapsedTimeMs() / 1000.0);
        bottleneck.setStartLine(scanNode.getStartLine());
        bottleneck.setEndLine(scanNode.getEndLine());
        bottleneck.setQueryFragment(scanNode.getQueryFragment());
        
        // Set severity based on cost
        if (scanNode.getCostPercentage() >= 20) {
            bottleneck.setSeverity(Bottleneck.Severity.CRITICAL);
        } else if (scanNode.getCostPercentage() >= 10) {
            bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        } else {
            bottleneck.setSeverity(Bottleneck.Severity.INFO);
        }
        
        bottleneck.setProblemDescription(
            String.format("Table/Index Scan on %s", tableName)
        );
        
        bottleneck.setWhyItsASlow(
            String.format(
                "SQL Server is scanning %s rows from %s instead of using an index seek. " +
                "This reads %s logical pages from disk/memory.",
                formatNumber(scanNode.getActualRows()),
                tableName,
                formatNumber(scanNode.getLogicalReads())
            )
        );
        
        // Generate index recommendation
        if (!keyColumns.isEmpty()) {
            String indexName = generateIndexName(tableName, keyColumns);
            String keyColumnList = String.join(", ", keyColumns);
            String includeColumnList = includeColumns.isEmpty() ? "" : 
                "\nINCLUDE (" + String.join(", ", includeColumns) + ")";
            
            String indexSql = String.format(
                "CREATE INDEX %s\nON %s (%s)%s;",
                indexName,
                tableName,
                keyColumnList,
                includeColumnList
            );
            
            bottleneck.setOptimizedFragment(indexSql);
            bottleneck.addFixQuery(indexSql);
            
            bottleneck.addFix("Create a covering index on key columns");
            bottleneck.addFix("Include frequently selected columns to avoid key lookups");
            if (keyColumns.size() > 1) {
                bottleneck.addFix("Column order matters: most selective first, then equality, then range");
            }
        } else {
            bottleneck.addFix("Add index on filtered/joined columns");
            bottleneck.addFix("Run: sp_executesql with SET STATISTICS IO ON to see missing index hints");
        }
        
        // Estimate improvement
        long rowsScanned = scanNode.getActualRows();
        double estimatedReduction = calculateExpectedReduction(rowsScanned, keyColumns.size());
        
        bottleneck.setExpectedImprovement(
            String.format(
                "~%.0f%% reduction in logical reads, seek instead of scan (estimated %s → %s rows)",
                estimatedReduction,
                formatNumber(rowsScanned),
                formatNumber((long)(rowsScanned * (1 - estimatedReduction / 100)))
            )
        );
        
        return bottleneck;
    }
    
    /**
     * Extract key columns from WHERE and JOIN clauses
     */
    private Set<String> extractKeyColumns(String sql, String tableName) {
        Set<String> columns = new LinkedHashSet<>();
        
        // Extract table alias
        Pattern aliasPattern = Pattern.compile(
            tableName.replaceAll(".*\\.", "") + "\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher aliasMatcher = aliasPattern.matcher(sql);
        String alias = aliasMatcher.find() ? aliasMatcher.group(1) : "";
        
        if (alias.isEmpty()) return columns;
        
        // Find WHERE predicates
        Pattern wherePattern = Pattern.compile(
            "\\b" + alias + "\\.(\\w+)\\s*(?:=|>|<|>=|<=|IN|BETWEEN)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher whereMatcher = wherePattern.matcher(sql);
        while (whereMatcher.find()) {
            columns.add(whereMatcher.group(1));
        }
        
        // Find JOIN keys
        Pattern joinPattern = Pattern.compile(
            "ON\\s+" + alias + "\\.(\\w+)\\s*=",
            Pattern.CASE_INSENSITIVE
        );
        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            columns.add(joinMatcher.group(1));
        }
        
        return columns;
    }
    
    /**
     * Extract columns to include in covering index
     */
    private Set<String> extractIncludeColumns(String sql, String tableName) {
        Set<String> columns = new LinkedHashSet<>();
        
        // Extract alias
        Pattern aliasPattern = Pattern.compile(
            tableName.replaceAll(".*\\.", "") + "\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher aliasMatcher = aliasPattern.matcher(sql);
        String alias = aliasMatcher.find() ? aliasMatcher.group(1) : "";
        
        if (alias.isEmpty()) return columns;
        
        // Find SELECT columns
        Pattern selectPattern = Pattern.compile(
            "\\b" + alias + "\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher selectMatcher = selectPattern.matcher(sql);
        while (selectMatcher.find()) {
            String col = selectMatcher.group(1);
            // Skip if it's already a key column
            columns.add(col);
        }
        
        // Limit to 5-6 most common columns to keep index reasonable
        return columns.stream().limit(6).collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
    
    private String generateIndexName(String tableName, Set<String> keyColumns) {
        String shortTable = tableName.replaceAll(".*\\.", "").replaceAll("\\W+", "");
        String shortCols = String.join("_", keyColumns).substring(0, Math.min(30, String.join("_", keyColumns).length()));
        return "IX_" + shortTable + "_" + shortCols;
    }
    
    private double calculateExpectedReduction(long rowsScanned, int numKeyColumns) {
        // Estimate based on selectivity
        if (numKeyColumns == 0) return 50.0;
        if (numKeyColumns == 1) return 80.0;
        if (numKeyColumns == 2) return 95.0;
        return 98.0;
    }
    
    private String formatNumber(long num) {
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }
}

