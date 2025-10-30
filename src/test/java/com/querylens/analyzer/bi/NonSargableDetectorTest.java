package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the Non-SARGABLE detector with REAL USC BI queries
 */
class NonSargableDetectorTest {

    private NonSargableDetector detector;

    @BeforeEach
    void setUp() {
        detector = new NonSargableDetector();
    }

    @Test
    void detectYearFunction_RealUSCQuery() {
        // REAL query from USC BI team (simplified)
        String sql = """
            SELECT
              d.descr AS donor_name,
              SUM(CASE WHEN gd.credit_type IN ('Hard','Soft') THEN gd.amount ELSE 0 END) AS total_giving,
              MAX(gd.posted_date) AS last_gift_date
            FROM SFDC.dbo.GIVING_DETAIL gd
            JOIN COGNOS_DW.dbo.DONOR_DIM d
              ON d.SF_ID = COALESCE(gd.account, gd.contact)
            WHERE gd.isdeleted = 'false'
              AND YEAR(gd.posted_date) = 2023
            GROUP BY d.descr
            """;

        // Simulate execution plan with expensive scan
        List<ExecutionPlanNode> executionPlan = createMockExecutionPlan();

        // Detect issues
        List<Bottleneck> bottlenecks = detector.detect(sql, executionPlan);

        // Should detect the YEAR() function
        assertThat(bottlenecks).isNotEmpty();
        
        Bottleneck yearIssue = bottlenecks.stream()
            .filter(b -> b.getIssueType() == Bottleneck.IssueType.NON_SARGABLE_PREDICATE)
            .filter(b -> b.getQueryFragment().contains("YEAR"))
            .findFirst()
            .orElse(null);

        assertThat(yearIssue).isNotNull();
        assertThat(yearIssue.getQueryFragment()).contains("YEAR(gd.posted_date) = 2023");
        assertThat(yearIssue.getProblemDescription()).containsIgnoringCase("index seek");
        assertThat(yearIssue.getOptimizedFragment()).contains("2023-01-01");
        assertThat(yearIssue.getOptimizedFragment()).contains("2024-01-01");
        
        // Should provide actionable fix
        assertThat(yearIssue.getFixes()).isNotEmpty();
        assertThat(yearIssue.getFixes().get(0)).containsIgnoringCase("SARGABLE");
    }

    @Test
    void detectCoalesce_RealUSCPattern() {
        // REAL pattern from USC: COALESCE(account, contact)
        String sql = """
            WHERE COALESCE(gd.account, gd.contact) = @donor_id
            """;

        List<ExecutionPlanNode> plan = new ArrayList<>();
        List<Bottleneck> bottlenecks = detector.detect(sql, plan);

        assertThat(bottlenecks).isNotEmpty();
        
        Bottleneck coalesceIssue = bottlenecks.get(0);
        assertThat(coalesceIssue.getIssueType()).isEqualTo(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
        assertThat(coalesceIssue.getProblemDescription()).containsIgnoringCase("COALESCE");
        
        // Should suggest UNION ALL fix
        assertThat(coalesceIssue.getFixes()).anySatisfy(fix -> 
            fix.toLowerCase().contains("union")
        );
        assertThat(coalesceIssue.getOptimizedFragment()).contains("UNION ALL");
    }

    @Test
    void detectMultipleIssues_ComplexQuery() {
        // Query with BOTH YEAR() and COALESCE issues
        String sql = """
            SELECT *
            FROM GIVING_DETAIL gd
            WHERE YEAR(gd.posted_date) = 2023
              AND COALESCE(gd.account, gd.contact) = @id
            """;

        List<ExecutionPlanNode> plan = new ArrayList<>();
        List<Bottleneck> bottlenecks = detector.detect(sql, plan);

        // Should detect both issues
        assertThat(bottlenecks).hasSizeGreaterThanOrEqualTo(2);
        
        assertThat(bottlenecks).anySatisfy(b -> 
            b.getQueryFragment().contains("YEAR")
        );
        
        assertThat(bottlenecks).anySatisfy(b -> 
            b.getQueryFragment().contains("COALESCE")
        );
    }

    /**
     * Create mock execution plan showing expensive table scan
     */
    private List<ExecutionPlanNode> createMockExecutionPlan() {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        // Root node
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(68000); // 68 seconds in ms
        root.setElapsedTimeMs(68000);
        
        // Expensive table scan (the problem!)
        ExecutionPlanNode scan = new ExecutionPlanNode();
        scan.setOperatorType("Table Scan");
        scan.setObjectName("SFDC.dbo.GIVING_DETAIL");
        scan.setActualCost(49000); // 49 seconds
        scan.setElapsedTimeMs(49000);
        scan.setActualRows(18_200_000); // 18.2M rows
        scan.setLogicalReads(2_500_000);
        scan.setQueryFragment("WHERE gd.isdeleted = 'false' AND YEAR(gd.posted_date) = 2023");
        scan.setStartLine(7);
        scan.calculateCostPercentage(68000); // 72% of total!
        
        root.addChild(scan);
        
        // Hash aggregate
        ExecutionPlanNode agg = new ExecutionPlanNode();
        agg.setOperatorType("Hash Match (Aggregate)");
        agg.setActualCost(4800);
        agg.setElapsedTimeMs(4800);
        agg.calculateCostPercentage(68000);
        
        root.addChild(agg);
        
        nodes.add(root);
        nodes.add(scan);
        nodes.add(agg);
        
        return nodes;
    }
}

