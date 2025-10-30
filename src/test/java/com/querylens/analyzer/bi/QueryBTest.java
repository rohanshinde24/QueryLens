package com.querylens.analyzer.bi;

import com.querylens.analyzer.ExecutionPlanNode;
import com.querylens.analyzer.bi.BiQueryAnalysisService.BiAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Query B - Pledge Payments with designation and manager
 */
@SpringBootTest
class QueryBTest {
    
    @Autowired
    private BiQueryAnalysisService analysisService;
    
    @Autowired
    private ResultsFormatter formatter;
    
    @Test
    void analyzeQueryB_PledgePayments() {
        // YOUR REAL Query B
        String queryB = """
            -- Recent pledge payments with designation and manager
            -- Inputs: :start_dt, :end_dt, :business_unit
            
            WITH pledge_payments AS (
              SELECT
                o.id                AS opportunity_id,
                o.pledge_id,
                o.pledge_date,
                o.pledge_amount,
                o.pledge_type,
                o.last_pledge_payment_date,
                o.last_payment_amount
              FROM SFDC.dbo.OPPORTUNITY o
              WHERE o.isdeleted = 'false'
                AND o.pledge_date BETWEEN :start_dt AND :end_dt
            ),
            opp_designation AS (
              SELECT
                dd.opportunity,
                d.id AS designation_id,
                d.name AS designation_name,
                d.sf_acknowledgement_description,
                d.business_unit
              FROM SFDC.dbo.DESIGNATION_DETAIL dd
              JOIN SFDC.dbo.DESIGNATION d
                ON d.id = dd.designation
              WHERE dd.isdeleted = 'false'
                AND d.isdeleted = 'false'
            ),
            steward AS (
              SELECT
                sr.opportunity,
                u.name AS manager_name,
                sr.assignment_type AS role
              FROM SFDC.dbo.STEWARDSHIP_ROLE sr
              LEFT JOIN SFDC.dbo.[USER] u
                ON u.id = sr.user_id
              WHERE sr.isdeleted = 'false'
            )
            SELECT DISTINCT
              dnr.donor_id,
              dnr.descr AS donor_name,
              pp.pledge_id,
              pp.pledge_date,
              pp.pledge_amount,
              pp.pledge_type,
              pp.last_pledge_payment_date,
              pp.last_payment_amount,
              od.designation_name,
              od.sf_acknowledgement_description,
              st.manager_name,
              st.role
            FROM pledge_payments pp
            JOIN SFDC.dbo.OPPORTUNITY o
              ON o.pledge_id = pp.pledge_id
            JOIN opp_designation od
              ON od.opportunity = o.id AND od.business_unit = :business_unit
            LEFT JOIN steward st
              ON st.opportunity = o.id
            JOIN COGNOS_DW.dbo.DONOR_DIM dnr
              ON dnr.SF_ID = COALESCE(o.account, o.contact);
            """;
        
        // Create execution plan
        List<ExecutionPlanNode> plan = createQueryBExecutionPlan();
        
        // Analyze
        BiAnalysisResult result = analysisService.analyzeQuery(queryB, plan);
        
        // Format and print
        String formatted = formatter.format(result, queryB);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("QUERY B ANALYSIS - Pledge Payments");
        System.out.println("Baseline: ~30 seconds → Optimized: ~4 seconds");
        System.out.println("=".repeat(80) + "\n");
        System.out.println(formatted);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ISSUES DETECTED:");
        System.out.println("=".repeat(80));
        result.getBottlenecks().forEach(b -> {
            System.out.println(String.format("%s Line %d: %s - %.1f%% cost",
                b.getSeverityEmoji(),
                b.getLineNumber(),
                b.getIssueTypeDescription(),
                b.getCostPercentage()
            ));
            if (!b.getFixes().isEmpty()) {
                System.out.println("   Fix: " + b.getFixes().get(0));
            }
        });
    }
    
    private List<ExecutionPlanNode> createQueryBExecutionPlan() {
        List<ExecutionPlanNode> nodes = new ArrayList<>();
        
        ExecutionPlanNode root = new ExecutionPlanNode();
        root.setOperatorType("SELECT");
        root.setActualCost(30000); // 30 seconds baseline
        root.setElapsedTimeMs(30000);
        
        // COALESCE in final JOIN causes scan
        ExecutionPlanNode scan = new ExecutionPlanNode();
        scan.setOperatorType("Table Scan");
        scan.setObjectName("SFDC.dbo.OPPORTUNITY");
        scan.setActualCost(18000); // 60% of cost
        scan.setElapsedTimeMs(18000);
        scan.setActualRows(5_200_000);
        scan.setLogicalReads(850_000);
        scan.setQueryFragment("WHERE dnr.SF_ID = COALESCE(o.account, o.contact)");
        scan.setStartLine(62);
        scan.calculateCostPercentage(30000);
        
        // Late business_unit filter
        ExecutionPlanNode lateFilter = new ExecutionPlanNode();
        lateFilter.setOperatorType("Hash Match (Inner Join)");
        lateFilter.setActualCost(6000); // 20%
        lateFilter.setElapsedTimeMs(6000);
        lateFilter.setActualRows(125_000);
        lateFilter.setQueryFragment("AND od.business_unit = :business_unit");
        lateFilter.setStartLine(59);
        lateFilter.calculateCostPercentage(30000);
        
        // DISTINCT overhead
        ExecutionPlanNode distinct = new ExecutionPlanNode();
        distinct.setOperatorType("Hash Match (Aggregate)");
        distinct.setActualCost(3000); // 10%
        distinct.setElapsedTimeMs(3000);
        distinct.setQueryFragment("SELECT DISTINCT");
        distinct.setStartLine(45);
        distinct.calculateCostPercentage(30000);
        
        nodes.add(root);
        nodes.add(scan);
        nodes.add(lateFilter);
        nodes.add(distinct);
        
        return nodes;
    }
}

