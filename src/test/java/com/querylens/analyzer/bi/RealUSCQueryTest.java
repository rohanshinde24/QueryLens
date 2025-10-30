package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.ExecutionPlanNode;
import com.querylens.analyzer.bi.BiQueryAnalysisService.BiAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test with REAL USC Advancement BI queries.
 * 
 * Query A: Recurring givers in a calendar year with exclusions and soft credits
 * Runtime: ~45 seconds baseline
 * Goal: Identify all bottlenecks and generate optimizations
 */
@SpringBootTest
class RealUSCQueryTest {
    
    @Autowired
    private BiQueryAnalysisService analysisService;
    
    @Autowired
    private ResultsFormatter formatter;
    
    @Test
    void analyzeQueryA_RecurringGivers() {
        // REAL Query A from USC BI team (simplified for test)
        String queryA = """
            -- Recurring givers in a calendar year with exclusions and soft credits
            -- Inputs: :cal_year, :business_unit ('Dornsife')
            
            WITH gifts AS (
              SELECT
                  gd.txn_id,
                  gd.posted_date,
                  gd.amount,
                  COALESCE(gd.account, gd.contact) AS donor_sf_id,
                  gd.credit_type,
                  dd.business_unit,
                  dd.department,
                  dd.designation_code
              FROM SFDC.dbo.GIVING_DETAIL gd
              JOIN SFDC.dbo.DESIGNATION dd
                ON dd.id = gd.designation
              WHERE gd.isdeleted = 'false'
                AND YEAR(gd.posted_date) = 2023
                AND dd.business_unit = 'Dornsife'
                AND NOT (
                  dd.designation_code IN ('9218010024','9233013015')
                  OR (dd.business_unit = 'Dornsife' AND dd.department = 'IACS')
                )
            ),
            first_gift AS (
              SELECT donor_sf_id, MIN(posted_date) AS first_dt
              FROM gifts
              WHERE credit_type IN ('Hard','Soft')
              GROUP BY donor_sf_id
            ),
            second_within_year AS (
              SELECT g.donor_sf_id
              FROM gifts g
              JOIN first_gift f ON f.donor_sf_id = g.donor_sf_id
              WHERE g.posted_date > f.first_dt
                AND DATEDIFF(day, f.first_dt, g.posted_date) <= 365
              GROUP BY g.donor_sf_id
            )
            SELECT
              d.descr AS donor_name,
              d.id_type AS donor_type,
              f.first_dt AS first_gift_in_year,
              COUNT(CASE WHEN g.credit_type IN ('Hard','Soft') THEN 1 END) AS num_gifts_in_year,
              STRING_AGG(DISTINCT g.designation_code, ',') AS designations_in_year
            FROM first_gift f
            JOIN second_within_year s ON s.donor_sf_id = f.donor_sf_id
            JOIN gifts g ON g.donor_sf_id = f.donor_sf_id
            JOIN COGNOS_DW.dbo.DONOR_DIM d
              ON d.SF_ID = f.donor_sf_id
            GROUP BY d.descr, d.id_type, f.first_dt
            ORDER BY d.descr;
            """;
        
        // Simulate execution plan
        List<ExecutionPlanNode> executionPlan = createQueryAExecutionPlan();
        
        // Analyze
        BiAnalysisResult result = analysisService.analyzeQuery(queryA, executionPlan);
        
        // Verify detections
        assertThat(result.getBottlenecks()).isNotEmpty();
        assertThat(result.getTotalBottlenecks()).isGreaterThan(2);
        
        // Should detect YEAR() function (line 15)
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getIssueType()).isEqualTo(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
            assertThat(b.getQueryFragment()).containsIgnoringCase("YEAR");
        });
        
        // Should detect COALESCE (line 8)
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getQueryFragment()).containsIgnoringCase("COALESCE");
        });
        
        // Should detect STRING_AGG DISTINCT (line 44)
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getIssueType()).isEqualTo(Bottleneck.IssueType.HEAVY_AGGREGATION);
        });
        
        // Should detect late business_unit filter (line 16)
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getIssueType()).isEqualTo(Bottleneck.IssueType.LATE_FILTER);
        });
        
        // Should have critical issues
        assertThat(result.getCriticalCount()).isGreaterThan(0);
        
        // Should suggest significant improvement
        assertThat(result.getPotentialImprovementPercent()).isGreaterThan(50.0);
        
        // Print formatted output for visual inspection
        String formattedOutput = formatter.format(result, queryA);
        System.out.println("\n" + "=".repeat(80));
        System.out.println("QUERY A ANALYSIS - Recurring Givers");
        System.out.println("=".repeat(80) + "\n");
        System.out.println(formattedOutput);
    }
    
    @Test
    void analyzeDonorRollupQuery_BeforeExample() {
        // The "Before" example from their documentation (68 seconds → 4.1 seconds)
        String beforeQuery = """
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
        
        List<ExecutionPlanNode> plan = createBeforeExamplePlan();
        
        BiAnalysisResult result = analysisService.analyzeQuery(beforeQuery, plan);
        
        // Should detect YEAR() - 72% of cost
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getIssueType()).isEqualTo(Bottleneck.IssueType.NON_SARGABLE_PREDICATE);
            assertThat(b.getQueryFragment()).contains("YEAR");
            assertThat(b.getCostPercentage()).isGreaterThan(50.0);
        });
        
        // Should detect correlated subquery - 18% of cost
        assertThat(result.getBottlenecks()).anySatisfy(b -> {
            assertThat(b.getIssueType()).isEqualTo(Bottleneck.IssueType.CORRELATED_SUBQUERY);
            assertThat(b.getQueryFragment()).contains("SELECT MAX");
        });
        
        // Should detect COALESCE in multiple places
        long coalesceIssues = result.getBottlenecks().stream()
            .filter(b -> b.getQueryFragment() != null && 
                        b.getQueryFragment().toUpperCase().contains("COALESCE"))
            .count();
        assertThat(coalesceIssues).isGreaterThan(0);
        
        // Should have very high potential improvement
        assertThat(result.getPotentialImprovementPercent()).isGreaterThan(80.0);
        
        // Print analysis
        String formattedOutput = formatter.format(result, beforeQuery);
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DONOR ROLLUP ANALYSIS - Before Example (68s → 4.1s)");
        System.out.println("=".repeat(80) + "\n");
        System.out.println(formattedOutput);
    }
    
    /**
     * Create realistic execution plan for Query A
     */
    private List<ExecutionPlanNode> createQueryAExecutionPlan() {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        // Root
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(45000); // 45 seconds
        root.setElapsedTimeMs(45000);
        
        // Expensive scan on GIVING_DETAIL due to YEAR() function
        ExecutionPlanNode gdScan = new ExecutionPlanNode();
        gdScan.setOperatorType("Table Scan");
        gdScan.setObjectName("SFDC.dbo.GIVING_DETAIL");
        gdScan.setActualCost(32400); // 72% of total
        gdScan.setElapsedTimeMs(32400);
        gdScan.setActualRows(18_200_000);
        gdScan.setLogicalReads(2_800_000);
        gdScan.setQueryFragment("WHERE gd.isdeleted = 'false' AND YEAR(gd.posted_date) = 2023");
        gdScan.setStartLine(14);
        gdScan.calculateCostPercentage(45000);
        
        // Hash aggregate
        ExecutionPlanNode hashAgg = new ExecutionPlanNode();
        hashAgg.setOperatorType("Hash Match (Aggregate)");
        hashAgg.setActualCost(3150); // 7%
        hashAgg.setElapsedTimeMs(3150);
        hashAgg.setActualRows(45000);
        hashAgg.calculateCostPercentage(45000);
        
        // STRING_AGG sort operation
        ExecutionPlanNode sort = new ExecutionPlanNode();
        sort.setOperatorType("Sort");
        sort.setActualCost(2250); // 5%
        sort.setElapsedTimeMs(2250);
        sort.setActualRows(45000);
        sort.calculateCostPercentage(45000);
        
        nodes.add(root);
        nodes.add(gdScan);
        nodes.add(hashAgg);
        nodes.add(sort);
        
        return nodes;
    }
    
    /**
     * Create execution plan for the "Before" example (68s baseline)
     */
    private List<ExecutionPlanNode> createBeforeExamplePlan() {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(68000);
        root.setElapsedTimeMs(68000);
        
        // Table scan due to YEAR() - 72% of cost
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
        
        // Correlated subquery execution - 18% of cost
        ExecutionPlanNode subquery = new ExecutionPlanNode();
        subquery.setOperatorType("Nested Loops");
        subquery.setActualCost(12240); // 18%
        subquery.setElapsedTimeMs(12240);
        subquery.setActualRows(45000);
        subquery.setQueryFragment("(SELECT MAX(gd2.posted_date)...");
        subquery.setStartLine(4);
        subquery.setEndLine(8);
        subquery.calculateCostPercentage(68000);
        
        // Hash aggregate - 7%
        ExecutionPlanNode agg = new ExecutionPlanNode();
        agg.setOperatorType("Hash Match (Aggregate)");
        agg.setActualCost(4760);
        agg.setElapsedTimeMs(4760);
        agg.calculateCostPercentage(68000);
        
        nodes.add(root);
        nodes.add(scan);
        nodes.add(subquery);
        nodes.add(agg);
        
        return nodes;
    }
}

