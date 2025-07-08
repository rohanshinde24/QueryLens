// src/main/java/com/querylens/optimizer/detector/ScalarSubqueryDetector.java
package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects simple scalar subqueries that can be extracted to a CTE.
 */
public class ScalarSubqueryDetector implements QueryPatternDetector {
    private static final Pattern SCALAR_SUBQUERY = Pattern.compile(
        "\\w+\\s*=\\s*\\(SELECT\\s+[^)]+\\)", Pattern.CASE_INSENSITIVE
    );

    @Override
    public String name() {
        return "SCALAR_SUBQUERY";
    }

    @Override
    public boolean matches(String sql, List<String> plan) {
        // Only need to examine the SQL text itself
        return SCALAR_SUBQUERY.matcher(sql).find();
    }

    @Override
    public String description() {
        return "Scalar subquery detected; consider moving it into a CTE to avoid per-row execution.";
    }
}
