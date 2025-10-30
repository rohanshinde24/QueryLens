package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects filters applied after expensive JOINs.
 * 
 * Common in BI queries:
 * - Joining 18M row fact table to dimension
 * - Then filtering on dimension attributes (business_unit, department)
 * - Should filter BEFORE joining to reduce row count early
 */
@Component
public class LateFilterDetector {
    
    // Pattern: JOIN followed by WHERE with filter on joined table
    private static final Pattern LATE_FILTER_PATTERN = Pattern.compile(
        "JOIN\\s+([\\w.]+)\\s+(\\w+)\\s+ON.*?WHERE.*?\\2\\.(\\w+)\\s*=",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Common dimension filters that should be early
    private static final List<String> FILTER_CANDIDATES = List.of(
        "business_unit", "department", "category", "status", 
        "type", "region", "division"
    );
    
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        // Check for filters on dimension tables after JOINs
        String[] lines = sql.split("\n");
        boolean inJoinSection = false;
        List<String> joinedTables = new ArrayList<>();
        List<Integer> joinLines = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            // Track JOIN statements
            if (line.toUpperCase().contains("JOIN ") && !line.toUpperCase().startsWith("--")) {
                inJoinSection = true;
                Pattern joinPattern = Pattern.compile("JOIN\\s+([\\w.]+)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher m = joinPattern.matcher(line);
                if (m.find()) {
                    joinedTables.add(m.group(2)); // table alias
                    joinLines.add(lineNumber);
                }
            }
            
            // Check WHERE clause for filters on joined tables
            if (line.toUpperCase().startsWith("WHERE") || (inJoinSection && line.contains("AND "))) {
                for (int j = 0; j < joinedTables.size(); j++) {
                    String alias = joinedTables.get(j);
                    
                    // Check if this WHERE filters on a dimension column
                    for (String filterCol : FILTER_CANDIDATES) {
                        Pattern filterPattern = Pattern.compile(
                            "\\b" + alias + "\\." + filterCol + "\\s*=",
                            Pattern.CASE_INSENSITIVE
                        );
                        
                        if (filterPattern.matcher(line).find()) {
                            bottlenecks.add(createLateFilterBottleneck(
                                alias,
                                filterCol,
                                lineNumber,
                                joinLines.get(j),
                                line.trim(),
                                executionPlan
                            ));
                        }
                    }
                }
            }
        }
        
        return bottlenecks;
    }
    
    private Bottleneck createLateFilterBottleneck(
            String tableAlias,
            String filterColumn,
            int filterLine,
            int joinLine,
            String fragment,
            List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.LATE_FILTER);
        bottleneck.setLineNumber(filterLine);
        bottleneck.setStartLine(joinLine);
        bottleneck.setEndLine(filterLine);
        bottleneck.setQueryFragment(fragment);
        
        // Estimate impact (usually moderate)
        bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        bottleneck.setCostPercentage(8.0);
        
        // Try to find associated JOIN in plan
        for (ExecutionPlanNode node : plan) {
            if (node.getOperatorType() != null && 
                node.getOperatorType().contains("Join") && 
                node.isExpensive()) {
                bottleneck.setCostPercentage(node.getCostPercentage() * 0.5); // Partial attribution
                bottleneck.setTimeImpactSeconds(node.getElapsedTimeMs() / 2000.0);
            }
        }
        
        bottleneck.setProblemDescription(
            String.format("Filter on %s.%s applied AFTER join", tableAlias, filterColumn)
        );
        
        bottleneck.setWhyItsASlow(
            String.format(
                "The join at line %d processes all rows from both tables, then filters on '%s'. " +
                "This wastes CPU and memory on rows that will be discarded. " +
                "Applying the filter earlier (in a CTE or subquery) reduces the dataset before the join.",
                joinLine, filterColumn
            )
        );
        
        // Generate CTE-based fix
        String optimized = String.format("""
            -- Push filter into CTE:
            
            WITH filtered_%s AS (
              SELECT *
              FROM table_name
              WHERE %s = @value
            )
            
            -- Then join to filtered CTE
            FROM fact_table
            JOIN filtered_%s %s 
              ON join_condition
            """, tableAlias, filterColumn, tableAlias, tableAlias);
        
        bottleneck.setOptimizedFragment(optimized);
        bottleneck.addFix(String.format("Move %s filter into a CTE", filterColumn));
        bottleneck.addFix("Filter dimension table BEFORE joining to fact table");
        bottleneck.addFix("This reduces row count early in execution");
        
        bottleneck.setExpectedImprovement(
            "20-40% reduction in rows processed, faster hash/merge join operations"
        );
        
        return bottleneck;
    }
    
    private ExecutionPlanNode findScanNode(List<ExecutionPlanNode> plan) {
        for (ExecutionPlanNode node : plan) {
            if (node.isScanOperation()) {
                return node;
            }
        }
        return null;
    }
}

