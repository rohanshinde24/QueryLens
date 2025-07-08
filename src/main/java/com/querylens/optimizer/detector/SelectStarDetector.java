// src/main/java/com/querylens/optimizer/detector/SelectStarDetector.java
package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects use of SELECT * in queries.
 */
public class SelectStarDetector implements QueryPatternDetector {
    private static final Pattern SELECT_STAR = Pattern.compile(
        "^\\s*SELECT\\s+\\*\\b", Pattern.CASE_INSENSITIVE
    );

    @Override
    public String name() {
        return "SELECT_STAR";
    }

    @Override
    public boolean matches(String sql, List<String> plan) {
        // Only need to examine the SQL text itself
        return SELECT_STAR.matcher(sql).find();
    }

    @Override
    public String description() {
        return "Avoid using SELECT *; list only the columns you need to reduce I/O.";
    }
}
