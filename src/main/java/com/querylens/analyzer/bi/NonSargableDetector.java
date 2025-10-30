package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects non-SARGABLE predicates in T-SQL queries.
 * 
 * SARGABLE = Search ARGument ABLE - predicates that can use indexes efficiently.
 * 
 * This detector specifically looks for BI team's common issues:
 * - YEAR(date_column) = value
 * - MONTH(date_column) = value  
 * - DATEPART(...) on indexed columns
 * - ISNULL(column, default) = value
 * - COALESCE(col1, col2) = value
 * - String functions: SUBSTRING, LEFT, RIGHT, UPPER, LOWER
 */
@Component
public class NonSargableDetector {
    
    // Pattern 1: Date functions on columns
    private static final Pattern YEAR_PATTERN = Pattern.compile(
        "YEAR\\s*\\(\\s*([\\w.]+)\\s*\\)\\s*=\\s*([\\d:]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MONTH_PATTERN = Pattern.compile(
        "MONTH\\s*\\(\\s*([\\w.]+)\\s*\\)\\s*=\\s*([\\d:]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATEPART_PATTERN = Pattern.compile(
        "DATEPART\\s*\\(\\s*\\w+\\s*,\\s*([\\w.]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern 2: ISNULL and COALESCE on indexed columns
    private static final Pattern ISNULL_PATTERN = Pattern.compile(
        "ISNULL\\s*\\(\\s*([\\w.]+)\\s*,\\s*[^)]+\\)\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COALESCE_PATTERN = Pattern.compile(
        "COALESCE\\s*\\(\\s*([\\w.]+)(?:\\s*,\\s*[\\w.]+)*\\s*\\)\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern 3: String functions
    private static final Pattern STRING_FUNCTION_PATTERN = Pattern.compile(
        "(SUBSTRING|LEFT|RIGHT|UPPER|LOWER|LTRIM|RTRIM)\\s*\\(\\s*([\\w.]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Detect all non-SARGABLE predicates in a query
     */
    public List<Bottleneck> detect(String sql, List<ExecutionPlanNode> executionPlan) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        // Split query into lines for precise location tracking
        String[] lines = sql.split("\n");
        
        // Check each line for non-SARGABLE patterns
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Check for YEAR() function
            Matcher yearMatcher = YEAR_PATTERN.matcher(line);
            if (yearMatcher.find()) {
                bottlenecks.add(createYearFunctionBottleneck(
                    yearMatcher.group(1),   // column name
                    yearMatcher.group(2),   // year value
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for MONTH() function
            Matcher monthMatcher = MONTH_PATTERN.matcher(line);
            if (monthMatcher.find()) {
                bottlenecks.add(createMonthFunctionBottleneck(
                    monthMatcher.group(1),
                    monthMatcher.group(2),
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for DATEPART
            Matcher datepartMatcher = DATEPART_PATTERN.matcher(line);
            if (datepartMatcher.find()) {
                bottlenecks.add(createDatePartBottleneck(
                    datepartMatcher.group(1),
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for COALESCE (common in their queries!)
            Matcher coalesceMatcher = COALESCE_PATTERN.matcher(line);
            if (coalesceMatcher.find()) {
                bottlenecks.add(createCoalesceBottleneck(
                    coalesceMatcher.group(1),
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for ISNULL
            Matcher isnullMatcher = ISNULL_PATTERN.matcher(line);
            if (isnullMatcher.find()) {
                bottlenecks.add(createIsnullBottleneck(
                    isnullMatcher.group(1),
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
            
            // Check for string functions
            Matcher stringMatcher = STRING_FUNCTION_PATTERN.matcher(line);
            if (stringMatcher.find()) {
                bottlenecks.add(createStringFunctionBottleneck(
                    stringMatcher.group(1),  // function name
                    stringMatcher.group(2),  // column name
                    lineNumber,
                    line.trim(),
                    executionPlan
                ));
            }
        }
        
        return bottlenecks;
    }
    
    /**
     * Create bottleneck for YEAR() function - THE MOST COMMON ISSUE
     */
    private Bottleneck createYearFunctionBottleneck(
            String columnName, 
            String yearValue, 
            int lineNumber, 
            String queryFragment,
            List<ExecutionPlanNode> executionPlan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(queryFragment);
        
        // Try to find associated scan operation in execution plan
        ExecutionPlanNode scanNode = findRelatedScanNode(columnName, executionPlan);
        if (scanNode != null) {
            bottleneck.setCostPercentage(scanNode.getCostPercentage());
            bottleneck.setTimeImpactSeconds(scanNode.getElapsedTimeMs() / 1000.0);
            bottleneck.setOperatorName(scanNode.getDescription());
            bottleneck.setSeverity(
                scanNode.getCostPercentage() >= 20 ? Bottleneck.Severity.CRITICAL : Bottleneck.Severity.WARNING
            );
        } else {
            // Estimate if we don't have plan
            bottleneck.setSeverity(Bottleneck.Severity.WARNING);
            bottleneck.setCostPercentage(50.0); // Typical for date function scans
        }
        
        // Problem description
        bottleneck.setProblemDescription(
            String.format("Function YEAR() on column '%s' prevents index seek", columnName)
        );
        bottleneck.setWhyItsASlow(
            "SQL Server cannot use an index on '" + columnName + "' because the YEAR() " +
            "function must be applied to every row before comparison. This forces a full table scan."
        );
        
        // Generate fix
        String startDate = yearValue + "-01-01";
        String endDate = (Integer.parseInt(yearValue) + 1) + "-01-01";
        
        String optimizedFragment = String.format(
            "%s >= '%s' AND %s < '%s'",
            columnName, startDate, columnName, endDate
        );
        
        bottleneck.setOptimizedFragment(optimizedFragment);
        bottleneck.addFix("Replace YEAR() function with SARGABLE date range");
        bottleneck.addFix("This allows SQL Server to use an index seek instead of scan");
        
        // Index recommendation
        String indexSql = String.format(
            "-- Consider creating an index if not exists:\n" +
            "CREATE INDEX IX_%s ON table_name (%s) INCLUDE (other_columns);",
            columnName.replace(".", "_"),
            columnName
        );
        bottleneck.addFixQuery(indexSql);
        
        bottleneck.setExpectedImprovement(
            "~70-90% reduction in logical reads, ~80-95% faster execution for selective date ranges"
        );
        
        return bottleneck;
    }
    
    /**
     * Create bottleneck for COALESCE - very common in USC queries!
     */
    private Bottleneck createCoalesceBottleneck(
            String columnName,
            int lineNumber,
            String queryFragment,
            List<ExecutionPlanNode> executionPlan) {
        
        Bottleneck bottleneck = new Bottleneck();
        bottleneck.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        bottleneck.setLineNumber(lineNumber);
        bottleneck.setQueryFragment(queryFragment);
        bottleneck.setSeverity(Bottleneck.Severity.WARNING);
        
        bottleneck.setProblemDescription(
            "COALESCE() on indexed column prevents index usage"
        );
        bottleneck.setWhyItsASlow(
            "When you filter on COALESCE(account, contact), SQL Server must evaluate " +
            "the function for every row, making any index on 'account' or 'contact' unusable."
        );
        
        // Specific fix for account/contact pattern (very common!)
        if (queryFragment.contains("account") && queryFragment.contains("contact")) {
            bottleneck.addFix("Split the condition into UNION ALL branches");
            bottleneck.addFix("First seek on 'account', then seek on 'contact' where account IS NULL");
            
            String optimizedFragment = 
                "-- Branch 1: Seek on account\n" +
                "WHERE account = @value\n" +
                "UNION ALL\n" +
                "-- Branch 2: Seek on contact where no account\n" +
                "WHERE contact = @value AND account IS NULL";
            
            bottleneck.setOptimizedFragment(optimizedFragment);
            bottleneck.setExpectedImprovement("Converts scan to two index seeks, typically 10-20x faster");
        } else {
            bottleneck.addFix("Consider separate filtered queries with UNION");
            bottleneck.addFix("Or create a computed column with an index");
        }
        
        return bottleneck;
    }
    
    /**
     * Find related scan node in execution plan
     */
    private ExecutionPlanNode findRelatedScanNode(String columnHint, List<ExecutionPlanNode> nodes) {
        // Look for scan operations that might be related to this column
        for (ExecutionPlanNode node : nodes) {
            if (node.isScanOperation() && node.isExpensive()) {
                // Match by table name from column hint (e.g., "gd.posted_date" -> look for "GIVING_DETAIL")
                if (columnHint.contains(".")) {
                    String tableAlias = columnHint.substring(0, columnHint.indexOf("."));
                    if (node.getQueryFragment() != null && 
                        node.getQueryFragment().toLowerCase().contains(tableAlias.toLowerCase())) {
                        return node;
                    }
                }
                // Return first expensive scan if no specific match
                return node;
            }
        }
        return null;
    }
    
    // Placeholder methods for other detectors
    private Bottleneck createMonthFunctionBottleneck(String col, String val, int line, String frag, List<ExecutionPlanNode> plan) {
        // Similar to YEAR
        Bottleneck b = new Bottleneck();
        b.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        b.setLineNumber(line);
        b.setQueryFragment(frag);
        b.setProblemDescription("MONTH() function prevents index usage");
        return b;
    }
    
    private Bottleneck createDatePartBottleneck(String col, int line, String frag, List<ExecutionPlanNode> plan) {
        Bottleneck b = new Bottleneck();
        b.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        b.setLineNumber(line);
        b.setQueryFragment(frag);
        b.setProblemDescription("DATEPART() function prevents index usage");
        return b;
    }
    
    private Bottleneck createIsnullBottleneck(String col, int line, String frag, List<ExecutionPlanNode> plan) {
        Bottleneck b = new Bottleneck();
        b.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        b.setLineNumber(line);
        b.setQueryFragment(frag);
        b.setProblemDescription("ISNULL() function prevents index usage");
        return b;
    }
    
    private Bottleneck createStringFunctionBottleneck(String func, String col, int line, String frag, List<ExecutionPlanNode> plan) {
        Bottleneck b = new Bottleneck();
        b.setIssueType(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        b.setLineNumber(line);
        b.setQueryFragment(frag);
        b.setProblemDescription(func + "() function prevents index usage on " + col);
        return b;
    }
}

