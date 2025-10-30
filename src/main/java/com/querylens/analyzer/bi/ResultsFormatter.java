package com.querylens.analyzer.bi;

import com.querylens.analyzer.Bottleneck;
import com.querylens.analyzer.bi.BiQueryAnalysisService.BiAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Formats analysis results into human-readable output.
 * 
 * Creates the comprehensive "one-pager" with:
 * - High-level 80/20 cost split
 * - Line-by-line breakdown
 * - Specific fix recommendations
 * - Before/After comparison
 */
@Component
public class ResultsFormatter {
    
    private static final String SEPARATOR = "━".repeat(80);
    private static final String THIN_SEPARATOR = "─".repeat(80);
    
    /**
     * Format complete analysis into readable text
     */
    public String format(BiAnalysisResult analysis, String originalSql) {
        StringBuilder output = new StringBuilder();
        
        // Header
        output.append(formatHeader(analysis));
        output.append("\n\n");
        
        // Executive summary
        output.append(formatSummary(analysis));
        output.append("\n\n");
        
        // Cost breakdown table
        output.append(formatCostBreakdown(analysis));
        output.append("\n\n");
        
        // Detailed bottleneck analysis
        output.append(formatBottlenecks(analysis));
        output.append("\n\n");
        
        // Index recommendations summary
        output.append(formatIndexRecommendations(analysis));
        output.append("\n\n");
        
        // Performance projection
        output.append(formatPerformanceProjection(analysis));
        
        return output.toString();
    }
    
    private String formatHeader(BiAnalysisResult analysis) {
        return String.format("""
            ╔════════════════════════════════════════════════════════════════════════════╗
            ║  QueryLens BI Analysis                                                     ║
            ╚════════════════════════════════════════════════════════════════════════════╝
            
            📊 Issues Detected: %d (%d Critical, %d Warnings, %d Info)
            ⏱️  Estimated Runtime Impact: %.1f seconds
            📈 Potential Improvement: %.0f%% faster
            """,
            analysis.getTotalBottlenecks(),
            analysis.getCriticalCount(),
            analysis.getWarningCount(),
            analysis.getInfoCount(),
            analysis.getTotalImpactSeconds(),
            analysis.getPotentialImprovementPercent()
        );
    }
    
    private String formatSummary(BiAnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 EXECUTIVE SUMMARY\n");
        sb.append(THIN_SEPARATOR).append("\n\n");
        
        // Group by issue type
        Map<Bottleneck.IssueType, Long> issueTypeCounts = analysis.getBottlenecks().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Bottleneck::getIssueType,
                java.util.stream.Collectors.counting()
            ));
        
        if (issueTypeCounts.containsKey(Bottleneck.IssueType.NON_SARGABLE_PREDICATE)) {
            sb.append("⚠️  Non-SARGABLE predicates detected - blocking index seeks\n");
        }
        if (issueTypeCounts.containsKey(Bottleneck.IssueType.CORRELATED_SUBQUERY)) {
            sb.append("⚠️  Correlated subqueries detected - executing per row\n");
        }
        if (issueTypeCounts.containsKey(Bottleneck.IssueType.OR_CONDITION)) {
            sb.append("⚠️  OR conditions detected - preventing index usage\n");
        }
        if (issueTypeCounts.containsKey(Bottleneck.IssueType.MISSING_INDEX)) {
            sb.append("⚠️  Missing indexes detected - causing full table scans\n");
        }
        
        return sb.toString();
    }
    
    private String formatCostBreakdown(BiAnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 COST BREAKDOWN\n");
        sb.append(SEPARATOR).append("\n");
        sb.append(String.format("%-40s │ %6s │ %10s │ %12s\n", 
            "Operation", "Cost %", "Time", "Category"));
        sb.append(THIN_SEPARATOR).append("\n");
        
        for (Bottleneck b : analysis.getBottlenecks()) {
            if (b.getCostPercentage() >= 5.0) {  // Only show significant costs
                String emoji = b.getSeverityEmoji();
                String operation = truncate(b.getOperatorName() != null ? 
                    b.getOperatorName() : b.getIssueTypeDescription(), 38);
                String cost = String.format("%.1f%%", b.getCostPercentage());
                String time = b.getTimeImpactSeconds() != null ? 
                    String.format("%.1fs", b.getTimeImpactSeconds()) : "N/A";
                String category = b.getIssueTypeDescription();
                
                sb.append(String.format("%s %-37s │ %6s │ %10s │ %12s\n",
                    emoji, operation, cost, time, truncate(category, 12)));
            }
        }
        
        return sb.toString();
    }
    
    private String formatBottlenecks(BiAnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 DETAILED ANALYSIS\n");
        sb.append(SEPARATOR).append("\n\n");
        
        int issueNumber = 1;
        for (Bottleneck b : analysis.getBottlenecks()) {
            sb.append(formatSingleBottleneck(b, issueNumber++));
            sb.append("\n\n");
        }
        
        return sb.toString();
    }
    
    private String formatSingleBottleneck(Bottleneck bottleneck, int number) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append(SEPARATOR).append("\n");
        sb.append(String.format("%s %s #%d: %s (%.1f%% of runtime)\n",
            bottleneck.getSeverityEmoji(),
            bottleneck.getSeverity(),
            number,
            bottleneck.getIssueTypeDescription(),
            bottleneck.getCostPercentage()
        ));
        sb.append(SEPARATOR).append("\n\n");
        
        // Location
        sb.append(String.format("📍 Location: Line %d\n", bottleneck.getLineNumber()));
        if (bottleneck.getQueryFragment() != null) {
            sb.append("   ").append(bottleneck.getQueryFragment()).append("\n\n");
        }
        
        // Problem description
        sb.append("⚠️  Problem:\n");
        sb.append("   ").append(bottleneck.getProblemDescription()).append("\n\n");
        
        if (bottleneck.getWhyItsASlow() != null) {
            sb.append("🐌 Why It's Slow:\n");
            sb.append(wrapText(bottleneck.getWhyItsASlow(), 3)).append("\n\n");
        }
        
        // Impact
        if (bottleneck.getTimeImpactSeconds() != null && bottleneck.getTimeImpactSeconds().doubleValue() > 0) {
            sb.append(String.format("💰 Impact: %.1f seconds (%.1f%% of total)\n\n",
                bottleneck.getTimeImpactSeconds(),
                bottleneck.getCostPercentage()
            ));
        }
        
        if (bottleneck.getExecutionCount() > 0) {
            sb.append(String.format("🔄 Executes: %s times\n\n",
                formatNumber(bottleneck.getExecutionCount())
            ));
        }
        
        // Fixes
        if (!bottleneck.getFixes().isEmpty()) {
            sb.append("✅ Recommended Fixes:\n");
            for (int i = 0; i < bottleneck.getFixes().size(); i++) {
                sb.append(String.format("   %d. %s\n", i + 1, bottleneck.getFixes().get(i)));
            }
            sb.append("\n");
        }
        
        // Optimized SQL
        if (bottleneck.getOptimizedFragment() != null) {
            sb.append("✨ Optimized Code:\n");
            sb.append(indentCode(bottleneck.getOptimizedFragment(), 3)).append("\n\n");
        }
        
        // Index SQL
        if (!bottleneck.getFixQueries().isEmpty()) {
            sb.append("💾 Index Recommendations:\n");
            for (String indexSql : bottleneck.getFixQueries()) {
                sb.append(indentCode(indexSql, 3)).append("\n");
            }
            sb.append("\n");
        }
        
        // Expected improvement
        if (bottleneck.getExpectedImprovement() != null) {
            sb.append("📈 Expected Improvement:\n");
            sb.append("   ").append(bottleneck.getExpectedImprovement()).append("\n");
        }
        
        return sb.toString();
    }
    
    private String formatIndexRecommendations(BiAnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("💾 INDEX RECOMMENDATIONS SUMMARY\n");
        sb.append(SEPARATOR).append("\n\n");
        
        List<String> allIndexes = analysis.getBottlenecks().stream()
            .flatMap(b -> b.getFixQueries().stream())
            .filter(sql -> sql.contains("CREATE INDEX"))
            .distinct()
            .collect(Collectors.toList());
        
        if (allIndexes.isEmpty()) {
            sb.append("   No index recommendations - query may already be well-indexed.\n");
        } else {
            for (int i = 0; i < allIndexes.size(); i++) {
                sb.append(String.format("Index #%d:\n", i + 1));
                sb.append(indentCode(allIndexes.get(i), 3)).append("\n\n");
            }
        }
        
        return sb.toString();
    }
    
    private String formatPerformanceProjection(BiAnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append("📊 PERFORMANCE PROJECTION\n");
        sb.append(SEPARATOR).append("\n\n");
        
        double baselineTime = analysis.getTotalCostMs() / 1000.0;
        double impactTime = analysis.getTotalImpactSeconds();
        double optimizedTime = baselineTime - (impactTime * analysis.getPotentialImprovementPercent() / 100.0);
        
        if (baselineTime > 0) {
            sb.append(String.format("Baseline:   %.1f seconds\n", baselineTime));
            sb.append(String.format("Optimized:  %.1f seconds (estimated)\n", optimizedTime));
            sb.append(String.format("\nImprovement: %.0f%% faster ⚡\n", analysis.getPotentialImprovementPercent()));
        } else {
            sb.append("Run query with execution plan to see performance projection.\n");
        }
        
        return sb.toString();
    }
    
    // Utility methods
    
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }
    
    private String wrapText(String text, int indent) {
        String indentStr = " ".repeat(indent);
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        StringBuilder line = new StringBuilder(indentStr);
        
        for (String word : words) {
            if (line.length() + word.length() > 77) {
                sb.append(line).append("\n");
                line = new StringBuilder(indentStr);
            }
            line.append(word).append(" ");
        }
        sb.append(line);
        
        return sb.toString();
    }
    
    private String indentCode(String code, int spaces) {
        String indent = " ".repeat(spaces);
        return indent + code.replace("\n", "\n" + indent);
    }
    
    private String formatNumber(long num) {
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }
}

