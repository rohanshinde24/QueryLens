// src/main/java/com/querylens/dto/AnalysisResponse.java
package com.querylens.dto;

import com.querylens.model.QueryMetrics;
import java.util.List;

/**
 * Response for /analyze: metrics + optimization suggestions.
 */
public class AnalysisResponse {

    private QueryMetrics metrics;
    private List<String> suggestions;
    private String optimizedSql;
    
    public AnalysisResponse() {}

    public AnalysisResponse(QueryMetrics metrics, List<String> suggestions, String optimizedSql) {
        this.metrics        = metrics;
        this.suggestions    = suggestions;
        this.optimizedSql = optimizedSql;

    }

    public QueryMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(QueryMetrics metrics) {
        this.metrics = metrics;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }
}
