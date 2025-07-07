
package com.querylens.model;

import java.util.List;
import java.util.regex.Matcher;

public class QueryMetrics {

    private String rawOutput;
    private double executionTime;
    private int rowsProcessed;
    private double costEstimate;

    private String statementType;
    private List<String> tablesUsed;

    private boolean hasWhereClause;
    private boolean hasJoinClause;
    private boolean hasLimitClause;

    // ✅ No-arg constructor (needed for Spring or manual setting)
    public QueryMetrics() {}

    // ✅ All-args constructor for convenience (if needed elsewhere)
    public QueryMetrics(String rawOutput, double executionTime, int rowsProcessed, double costEstimate) {
        this.rawOutput = rawOutput;
        this.executionTime = executionTime;
        this.rowsProcessed = rowsProcessed;
        this.costEstimate = costEstimate;
    }

    // ✅ Getters & setters (fully encapsulated)
    public String getRawOutput() {
        return rawOutput;
    }

    public void setRawOutput(String rawOutput) {
        this.rawOutput = rawOutput;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(int rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public double getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(double costEstimate) {
        this.costEstimate = costEstimate;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public List<String> getTablesUsed() {
        return tablesUsed;
    }

    public void setTablesUsed(List<String> tablesUsed) {
        this.tablesUsed = tablesUsed;
    }

    public boolean isHasWhereClause() {
        return hasWhereClause;
    }

    public void setHasWhereClause(boolean hasWhereClause) {
        this.hasWhereClause = hasWhereClause;
    }

    public boolean isHasJoinClause() {
        return hasJoinClause;
    }

    public void setHasJoinClause(boolean hasJoinClause) {
        this.hasJoinClause = hasJoinClause;
    }

    public boolean isHasLimitClause() {
        return hasLimitClause;
    }

    public void setHasLimitClause(boolean hasLimitClause) {
        this.hasLimitClause = hasLimitClause;
    }
}
