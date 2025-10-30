package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects correlated subqueries that execute once per row.
 * 
 * Common in BI queries:
 * - (SELECT MAX(date) FROM fact WHERE fact.id = outer.id)
 * - (SELECT COUNT(*) FROM fact WHERE fact.key = outer.key)
 * 
 * These are expensive because they run N times where N = outer query rows.
 */
@Component
public class CorrelatedSubqueryDetector {
    
    // Pattern: Subquery in SELECT list with correlation
    private static final Pattern CORRELATED_SUBQUERY = Pattern.compile(
        "\\(\\s*SELECT\\s+[^)]+?\\s+FROM\\s+[\\w.]+\\s+.*?WHERE\\s+[^)]*?\\b(\\w+)\\s*=\\s*\\2",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Simpler pattern: Any subquery in SELECT list
    private static final Pattern SELECT_CLAUSE_SUBQUERY = Pattern.compile(
        ",?\\s*\\(\\s*SELECT\\s+.*?\\)\\s+AS\\s+\\w+",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern for MAX/MIN/COUNT/SUM in subquery
    private static final Pattern AGGREGATE_SUBQUERY = Pattern.compile(
        "\\(\\s*SELECT\\s+(MAX|MIN|COUNT|SUM|AVG)\\s*\\([^)]+\\)\\s+FROM",
        Pattern.CASE_INSENSITIVE
    );
    
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        String[] lines = sql.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Look for SELECT clause subqueries (most common pattern)
            if (line.trim().contains("(SELECT") && !line.trim().startsWith("FROM") && !line.trim().startsWith("WHERE")) {
                
                // Try to match the full subquery across multiple lines
                StringBuilder subqueryBuilder = new StringBuilder(line);
                int parenCount = countChar(line, '(') - countChar(line, ')');
                int endLine = i;
                
                // Collect full subquery if it spans multiple lines
                while (parenCount > 0 && endLine < lines.length - 1) {
                    endLine++;
                    String nextLine = lines[endLine];
                    subqueryBuilder.append("\n").append(nextLine);
                    parenCount += countChar(nextLine, '(') - countChar(nextLine, ')');
                }
                
                String fullSubquery = subqueryBuilder.toString();
                
                // Check if it's an aggregate subquery
                Matcher aggMatcher = AGGREGATE_SUBQUERY.matcher(fullSubquery);
                if (aggMatcher.find()) {
                    bottlenecks.add(createCorrelatedSubqueryBottleneck(
                        fullSubquery.trim(),
                        aggMatcher.group(1), // Aggregate function name
                        lineNumber,
                        endLine + 1,
                        executionPlan
                    ));
                }
            }
        }
        
        return bottlenecks;
    }
    
    private Bottleneck createCorrelatedSubqueryBottleneck(
            String subquery,
            String aggregateFunction,
            int startLine,
            int endLine,
            List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.CORRELATED_SUBQUERY);
        bottleneck.setStartLine(startLine);
        bottleneck.setEndLine(endLine);
        bottleneck.setLineNumber(startLine);
        bottleneck.setQueryFragment(subquery.length() > 200 ? subquery.substring(0, 200) + "..." : subquery);
        
        // Estimate execution count (try to find from plan, otherwise estimate)
        long executionCount = estimateExecutionCount(plan);
        bottleneck.setExecutionCount(executionCount);
        
        // Set severity based on execution count
        if (executionCount > 10000) {
            bottleneck.setSeverity(Bottleneck.Severity.CRITICAL);
            bottleneck.setCostPercentage(20.0); // Estimate 20%+ cost
        } else if (executionCount > 1000) {
            bottleneck.setSeverity(Bottleneck.Severity.WARNING);
            bottleneck.setCostPercentage(10.0);
        } else {
            bottleneck.setSeverity(Bottleneck.Severity.INFO);
            bottleneck.setCostPercentage(5.0);
        }
        
        bottleneck.setProblemDescription(
            String.format("Correlated subquery with %s() executes once per row", aggregateFunction)
        );
        
        bottleneck.setWhyItsASlow(
            String.format(
                "This subquery runs %s times (once for each row in the outer query). " +
                "Each execution scans the table independently, preventing parallelism and " +
                "causing redundant I/O.",
                formatNumber(executionCount)
            )
        );
        
        // Generate fix based on aggregate function
        String fix = generateSubqueryFix(aggregateFunction);
        bottleneck.setOptimizedFragment(fix);
        
        bottleneck.addFix("Move the aggregate to the main query's GROUP BY");
        bottleneck.addFix("Or use a LEFT JOIN with pre-aggregated CTE");
        bottleneck.addFix("This allows single-pass processing with parallelism");
        
        bottleneck.setExpectedImprovement(
            String.format("Eliminates %s subquery executions, typically 50-90%% faster", 
                formatNumber(executionCount))
        );
        
        return bottleneck;
    }
    
    private String generateSubqueryFix(String aggregateFunction) {
        return String.format("""
            -- Instead of correlated subquery, use:
            
            -- Option 1: Move to main aggregation
            %s(column_name) AS result
            -- Add to main SELECT and GROUP BY
            
            -- Option 2: Pre-aggregate in CTE then JOIN
            WITH aggregated AS (
              SELECT 
                key_column,
                %s(value_column) AS agg_value
              FROM fact_table
              GROUP BY key_column
            )
            SELECT ... , agg.agg_value
            FROM main_table m
            LEFT JOIN aggregated agg ON agg.key_column = m.key_column
            """, aggregateFunction, aggregateFunction);
    }
    
    private long estimateExecutionCount(List<ExecutionPlanNode> plan) {
        // Try to find actual row count from outer query
        for (ExecutionPlanNode node : plan) {
            if (node.getActualRows() > 0 && !node.isScanOperation()) {
                return node.getActualRows();
            }
        }
        // Default estimate
        return 10000;
    }
    
    private int countChar(String str, char ch) {
        return (int) str.chars().filter(c -> c == ch).count();
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

