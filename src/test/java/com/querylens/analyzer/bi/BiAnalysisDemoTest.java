package com.querylens.analyzer.bi;

import com.querylens.analyzer.ExecutionPlanNode;
import com.querylens.analyzer.bi.BiQueryAnalysisService.BiAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo test - just shows the output without strict assertions
 */
@SpringBootTest
class BiAnalysisDemoTest {
    
    @Autowired
    private BiQueryAnalysisService analysisService;
    
    @Autowired
    private ResultsFormatter formatter;
    
    @Test
    void demoAnalysis_YourRealQuery() {
        // YOUR REAL "Before" query that went from 68s → 4.1s
        String query = """
            SELECT
              d.descr AS donor_name,
              SUM(CASE WHEN gd.credit_type IN ('Hard','Soft') THEN gd.amount ELSE 0 END) AS total_giving,
              (
                SELECT MAX(gd2.posted_date)
                FROM SFDC.dbo.GIVING_DETAIL gd2
                WHERE COALESCE(gd2.account, gd2.contact) = COALESCE(gd.account, gd.contact)
                  AND gd2.isdeleted = 'false'
                  AND gd2.posted_date BETWEEN '2023-01-01' AND '2023-12-31'
              ) AS last_gift_date
            FROM SFDC.dbo.GIVING_DETAIL gd
            JOIN SFDC.dbo.DESIGNATION dd
              ON dd.id = gd.designation
            JOIN COGNOS_DW.dbo.DONOR_DIM d
              ON d.SF_ID = COALESCE(gd.account, gd.contact)
            WHERE gd.isdeleted = 'false'
              AND dd.business_unit = 'Dornsife'
              AND YEAR(gd.posted_date) = 2023
            GROUP BY d.descr
            ORDER BY total_giving DESC
            OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY;
            """;
        
        // Create execution plan (simulating your 68-second baseline)
        List<ExecutionPlanNode> plan = createExecutionPlan();
        
        // Run analysis
        BiAnalysisResult result = analysisService.analyzeQuery(query, plan);
        
        // Format and print
        String formatted = formatter.format(result, query);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("QueryLens BI Analysis - Your Real Donor Rollup Query");
        System.out.println("Baseline: 68 seconds → Optimized: 4.1 seconds (94% faster)");
        System.out.println("=".repeat(80) + "\n");
        System.out.println(formatted);
        
        // Print summary stats
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DETECTION SUMMARY:");
        System.out.println("=".repeat(80));
        System.out.println("Total Issues: " + result.getTotalBottlenecks());
        System.out.println("  🔴 Critical: " + result.getCriticalCount());
        System.out.println("  🟡 Warnings: " + result.getWarningCount());
        System.out.println("  🔵 Info: " + result.getInfoCount());
        System.out.println("\nIssues Found:");
        result.getBottlenecks().forEach(b -> {
            System.out.println(String.format("  %s [Line %d] %s - %.1f%% cost",
                b.getSeverityEmoji(),
                b.getLineNumber(),
                b.getIssueTypeDescription(),
                b.getCostPercentage()
            ));
        });
    }
    
    private List<ExecutionPlanNode> createExecutionPlan() {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(68000);
        root.setElapsedTimeMs(68000);
        
        // Table scan on GIVING_DETAIL (72% of cost)
        ExecutionPlanNode scan = new ExecutionPlanNode();
        scan.setOperatorType("Table Scan");
        scan.setObjectName("SFDC.dbo.GIVING_DETAIL");
        scan.setActualCost(49000);
        scan.setElapsedTimeMs(49000);
        scan.setActualRows(18_200_000);
        scan.setLogicalReads(2_500_000);
        scan.setQueryFragment("WHERE gd.isdeleted = 'false' AND YEAR(gd.posted_date) = 2023");
        scan.setStartLine(17);
        scan.calculateCostPercentage(68000);
        
        // Nested loops for correlated subquery (18% of cost)  
        ExecutionPlanNode nestedLoop = new ExecutionPlanNode();
        nestedLoop.setOperatorType("Nested Loops");
        nestedLoop.setActualCost(12240);
        nestedLoop.setElapsedTimeMs(12240);
        nestedLoop.setActualRows(45000);
        nestedLoop.setQueryFragment("Correlated subquery execution");
        nestedLoop.calculateCostPercentage(68000);
        
        // Hash aggregate (7% of cost)
        ExecutionPlanNode agg = new ExecutionPlanNode();
        agg.setOperatorType("Hash Match (Aggregate)");
        agg.setActualCost(4760);
        agg.setElapsedTimeMs(4760);
        agg.setActualRows(45000);
        agg.calculateCostPercentage(68000);
        
        nodes.add(root);
        nodes.add(scan);
        nodes.add(nestedLoop);
        nodes.add(agg);
        
        return nodes;
    }
}

