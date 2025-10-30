package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects OR conditions that prevent index usage.
 * 
 * Common in USC queries:
 * - WHERE (account = @id OR contact = @id)
 * - WHERE (designation_code IN (...) OR (business_unit = X AND dept = Y))
 * 
 * These often force table scans even when indexes exist.
 */
@Component
public class OrConditionDetector {
    
    // Pattern 1: Simple OR between two columns
    // Matches: (account = X OR contact = Y)
    private static final Pattern SIMPLE_OR_PATTERN = Pattern.compile(
        "\\(\\s*([\\w.]+)\\s*=\\s*[^)]+\\s+OR\\s+([\\w.]+)\\s*=\\s*[^)]+\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern 2: OR with complex conditions
    // Matches: (condition1 OR (condition2 AND condition3))
    private static final Pattern COMPLEX_OR_PATTERN = Pattern.compile(
        "\\([^()]+\\s+OR\\s+\\([^)]+\\)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern 3: COALESCE in WHERE (specific USC pattern)
    private static final Pattern COALESCE_EQUALS = Pattern.compile(
        "WHERE.*?COALESCE\\s*\\(\\s*([\\w.]+)\\s*,\\s*([\\w.]+)\\s*\\)\\s*=",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        String[] lines = sql.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Check for simple OR pattern
            Matcher simpleMatcher = SIMPLE_OR_PATTERN.matcher(line);
            if (simpleMatcher.find()) {
                bottlenecks.add(createSimpleOrBottleneck(
                    simpleMatcher.group(1),  // First column
                    simpleMatcher.group(2),  // Second column
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for complex OR
            Matcher complexMatcher = COMPLEX_OR_PATTERN.matcher(line);
            if (complexMatcher.find()) {
                bottlenecks.add(createComplexOrBottleneck(
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
        }
        
        // Also check for COALESCE in WHERE (very common USC pattern!)
        Matcher coalesceMatcher = COALESCE_EQUALS.matcher(sql);
        if (coalesceMatcher.find()) {
            int lineNum = findLineNumber(sql, coalesceMatcher.start());
            bottlenecks.add(createCoalesceOrBottleneck(
                coalesceMatcher.group(1),  // First column (account)
                coalesceMatcher.group(2),  // Second column (contact)
                lineNum,
                sql.substring(coalesceMatcher.start(), Math.min(coalesceMatcher.end() + 50, sql.length())),
                executionPlan
            ));
        }
        
        return bottlenecks;
    }
    
    /**
     * Handle simple OR: (col1 = X OR col2 = Y)
     */
    private Bottleneck createSimpleOrBottleneck(
            String col1, String col2, int lineNumber, 
            String fragment, List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.OR_CONDITION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        
        // Try to find scan in plan
        ExecutionPlanNode scanNode = findScanNode(plan);
        if (scanNode != null) {
            bottleneck.setCostPercentage(scanNode.getCostPercentage());
            bottleneck.setTimeImpactSeconds(scanNode.getElapsedTimeMs() / 1000.0);
            if (scanNode.getCostPercentage() >= 20) {
                bottleneck.setSeverity(Bottleneck.Severity.CRITICAL);
            }
        } else {
            bottleneck.setCostPercentage(15.0);
        }
        
        bottleneck.setProblemDescription(
            String.format("OR condition between '%s' and '%s' prevents index usage", col1, col2)
        );
        
        bottleneck.setWhyItsASlow(
            "SQL Server cannot use indexes on either column when they're connected with OR. " +
            "The query optimizer must scan the entire table to evaluate both conditions."
        );
        
        // Generate UNION ALL fix
        String optimized = String.format("""
            -- Split into two seekable branches:
            
            -- Branch 1: Seek on %s
            WHERE %s = @value
            
            UNION ALL
            
            -- Branch 2: Seek on %s (exclude rows already in branch 1)
            WHERE %s = @value
              AND %s IS NULL
            """, col1, col1, col2, col2, col1);
        
        bottleneck.setOptimizedFragment(optimized);
        bottleneck.addFix("Split OR condition into UNION ALL");
        bottleneck.addFix("Each branch can use its respective index");
        bottleneck.addFix("Deduplication happens at UNION ALL level");
        
        bottleneck.setExpectedImprovement(
            "Converts table scan to two index seeks, typically 10-20x faster"
        );
        
        return bottleneck;
    }
    
    /**
     * Handle COALESCE(col1, col2) = value pattern (USC specific!)
     */
    private Bottleneck createCoalesceOrBottleneck(
            String col1, String col2, int lineNumber,
            String fragment, List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.OR_CONDITION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.CRITICAL);
        
        ExecutionPlanNode scanNode = findScanNode(plan);
        if (scanNode != null) {
            bottleneck.setCostPercentage(scanNode.getCostPercentage());
            bottleneck.setTimeImpactSeconds(scanNode.getElapsedTimeMs() / 1000.0);
        } else {
            bottleneck.setCostPercentage(25.0); // High estimate for COALESCE
        }
        
        bottleneck.setProblemDescription(
            String.format("COALESCE(%s, %s) in WHERE clause prevents index seeks", col1, col2)
        );
        
        bottleneck.setWhyItsASlow(
            "COALESCE must be evaluated for every row, making indexes on '" + col1 + 
            "' or '" + col2 + "' unusable. This is equivalent to " +
            "(" + col1 + " = X OR " + col2 + " = X) which forces a full table scan."
        );
        
        // Generate the UNION ALL fix (your exact pattern!)
        String optimized = String.format("""
            -- Replace COALESCE with UNION ALL:
            
            WITH a AS (
              SELECT txn_id, posted_date, amount, %s AS donor_key
              FROM SFDC.dbo.GIVING_DETAIL
              WHERE %s = @donor_id 
                AND posted_date >= @start_dt 
                AND posted_date < @end_dt
            ),
            b AS (
              SELECT txn_id, posted_date, amount, %s AS donor_key
              FROM SFDC.dbo.GIVING_DETAIL
              WHERE %s = @donor_id 
                AND %s IS NULL
                AND posted_date >= @start_dt 
                AND posted_date < @end_dt
            )
            SELECT * FROM a
            UNION ALL
            SELECT * FROM b
            """, col1, col1, col2, col2, col1);
        
        bottleneck.setOptimizedFragment(optimized);
        bottleneck.addFix("Split COALESCE into two index-seekable branches");
        bottleneck.addFix("First branch: seek on '" + col1 + "'");
        bottleneck.addFix("Second branch: seek on '" + col2 + "' where " + col1 + " IS NULL");
        
        String indexSql = String.format("""
            -- Ensure indexes exist:
            CREATE INDEX IX_%s ON table_name (%s) INCLUDE (other_columns);
            CREATE INDEX IX_%s ON table_name (%s) INCLUDE (other_columns);
            """, 
            col1.replace(".", "_"), col1,
            col2.replace(".", "_"), col2
        );
        bottleneck.addFixQuery(indexSql);
        
        bottleneck.setExpectedImprovement(
            "Converts scan to two seeks. Your team saw 11s → 0.9s (92% faster) with this fix."
        );
        
        return bottleneck;
    }
    
    private Bottleneck createComplexOrBottleneck(
            int lineNumber, String fragment, List<ExecutionPlanNode> plan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.OR_CONDITION);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(fragment);
        bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        bottleneck.setCostPercentage(10.0);
        
        bottleneck.setProblemDescription("Complex OR condition may prevent index usage");
        bottleneck.setWhyItsASlow("Multiple OR conditions often force SQL Server to scan tables");
        bottleneck.addFix("Consider breaking into UNION ALL branches");
        bottleneck.addFix("Or restructure logic to use IN clauses where possible");
        
        return bottleneck;
    }
    
    private ExecutionPlanNode findScanNode(List<ExecutionPlanNode> plan) {
        for (ExecutionPlanNode node : plan) {
            if (node.isScanOperation() && node.isExpensive()) {
                return node;
            }
        }
        return null;
    }
    
    private int findLineNumber(String sql, int charPosition) {
        return (int) sql.substring(0, charPosition).chars().filter(c -> c == '\n').count() + 1;
    }
    
    private String formatNumber(long num) {
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }
}

