package com.querylens.service;

import com.querylens.model.QueryMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class QueryAnalyzerService {

    // private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);
    // private static final Pattern TABLE_PATTERN = Pattern.compile("\\bFROM\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
    // private static final Pattern WHERE_PATTERN = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    // private static final Pattern JOIN_PATTERN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    // private static final Pattern LIMIT_PATTERN = Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE);
    // private static final Pattern PLAN_TABLE_PATTERN = Pattern.compile("on\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern SELECT_PATTERN   = Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN   = Pattern.compile("^\\s*INSERT", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN   = Pattern.compile("^\\s*UPDATE", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN   = Pattern.compile("^\\s*DELETE", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_PATTERN    = Pattern.compile("\\bFROM\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN    = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN     = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN    = Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_TABLE_PATTERN = Pattern.compile("on\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_TABLE_PATTERN = Pattern.compile("\\bJOIN\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean isValidTable(String table) {
        String lower = table.toLowerCase();
        return !(
            lower.equals("scan") ||
            lower.equals("hash") ||
            lower.equals("time") ||
            lower.equals("loop") ||
            lower.startsWith("pg_stat_get")
        );
    }
    public List<String> getRawPlanLines(String sql) {
        return jdbcTemplate.queryForList("EXPLAIN ANALYZE " + sql, String.class);
    }
    
    public QueryMetrics analyzeQuery(String sql) {
        String explainQuery = "EXPLAIN ANALYZE " + sql;
        List<String> output = jdbcTemplate.queryForList(explainQuery, String.class);

        double executionTime = 0.0;
        int rowsProcessed = 0;
        double costEstimate = 0.0;

        // Pattern to capture actual rows from the "actual time=... rows=X" clause
        Pattern actualRowsPattern = Pattern.compile("actual time=[^ ]+ rows=(\\d+)", Pattern.CASE_INSENSITIVE);

        for (String line : output) {
            // 1) Execution time
            if (line.contains("Execution Time:")) {
                try {
                    executionTime = Double.parseDouble(
                        line.split("Execution Time: ")[1].split(" ")[0]
                    );
                } catch (Exception ignored) {}
            }

            // 2) Cost estimate
            if (line.contains("cost=")) {
                try {
                    String costSubstr = line.substring(line.indexOf("cost=") + 5);
                    String[] costParts = costSubstr.split("\\.\\.");
                    if (costParts.length > 1) {
                        costEstimate = Double.parseDouble(
                            costParts[1].split(" ")[0].replaceAll("[^\\d.]", "")
                        );
                    }
                } catch (Exception ignored) {}
            }

            // 3) Estimated rows (from cost=.. rows=..)
            if (line.contains("rows=") && !line.toLowerCase().contains("actual time")) {
                try {
                    String[] parts = line.split("rows=");
                    if (parts.length > 1) {
                        int est = Integer.parseInt(parts[1].split(" ")[0]);
                        if (rowsProcessed == 0) {
                            rowsProcessed = est;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 4) Actual rows override
            Matcher m = actualRowsPattern.matcher(line);
            if (m.find()) {
                try {
                    rowsProcessed = Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }
        String cleanedSql = sql.replaceAll("[\\n\\r]+", " ").trim();
        QueryMetrics metrics = new QueryMetrics();
        metrics.setRawOutput(String.join("\n", output));
        metrics.setExecutionTime(executionTime);
        metrics.setRowsProcessed(rowsProcessed);
        metrics.setCostEstimate(costEstimate);

        // Statement type detection (with CTE support)
        String upperSql = cleanedSql.toUpperCase();
        if (upperSql.startsWith("WITH")) {
            int selectIndex = upperSql.indexOf("SELECT");
            int insertIndex = upperSql.indexOf("INSERT");
            int updateIndex = upperSql.indexOf("UPDATE");
            int deleteIndex = upperSql.indexOf("DELETE");

            int minIndex = Integer.MAX_VALUE;
            String type = "UNKNOWN";
            if (selectIndex  > -1 && selectIndex  < minIndex) { minIndex = selectIndex;  type = "SELECT"; }
            if (insertIndex  > -1 && insertIndex  < minIndex) { minIndex = insertIndex;  type = "INSERT"; }
            if (updateIndex  > -1 && updateIndex  < minIndex) { minIndex = updateIndex;  type = "UPDATE"; }
            if (deleteIndex  > -1 && deleteIndex  < minIndex) { minIndex = deleteIndex;  type = "DELETE"; }
            metrics.setStatementType(type);
        } else if (SELECT_PATTERN.matcher(cleanedSql).find()) {
            metrics.setStatementType("SELECT");
        } else if (INSERT_PATTERN.matcher(cleanedSql).find()) {
            metrics.setStatementType("INSERT");
        } else if (UPDATE_PATTERN.matcher(cleanedSql).find()) {
            metrics.setStatementType("UPDATE");
        } else if (DELETE_PATTERN.matcher(cleanedSql).find()) {
            metrics.setStatementType("DELETE");
        } else {
            metrics.setStatementType("UNKNOWN");
        }

        // Table extraction from both SQL and query plan
        Set<String> tables = new LinkedHashSet<>();

        Matcher tableMatcher = TABLE_PATTERN.matcher(cleanedSql);
        while (tableMatcher.find()) {
            tables.add(tableMatcher.group(1));
        }

        // JOIN
        Matcher joinTableMatcher = JOIN_TABLE_PATTERN.matcher(cleanedSql);
        while (joinTableMatcher.find()) {
            tables.add(joinTableMatcher.group(1));
}
        for (String line : output) {
            Matcher planMatcher = PLAN_TABLE_PATTERN.matcher(line);
            while (planMatcher.find()) {
                tables.add(planMatcher.group(1));
            }
        }

        tables.removeIf(table -> !isValidTable(table));
        metrics.setTablesUsed(new ArrayList<>(tables));

        // Clause presence flags
        boolean joinInPlan = output.stream().anyMatch(line -> line.toLowerCase().contains("join"));
        metrics.setHasWhereClause(WHERE_PATTERN.matcher(cleanedSql).find());
        metrics.setHasJoinClause(JOIN_PATTERN.matcher(cleanedSql).find() || joinInPlan);
        metrics.setHasLimitClause(LIMIT_PATTERN.matcher(cleanedSql).find());

        return metrics;
    }
}

