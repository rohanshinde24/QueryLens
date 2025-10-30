package com.querylens.dto;

import lombok.Data;

/**
 * Request DTO for BI query analysis
 */
@Data
public class BiAnalysisRequest {
    private String sql;                    // The SQL query to analyze
    private String executionPlanXml;       // Optional: SQL Server execution plan XML
    private Double actualRuntimeSeconds;   // Optional: Actual observed runtime
    private String databaseContext;        // Optional: Which database/schema
}

