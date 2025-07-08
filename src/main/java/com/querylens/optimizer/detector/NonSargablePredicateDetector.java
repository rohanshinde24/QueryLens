package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects non‐sargable predicates, e.g. function calls on columns in WHERE.
 */
public class NonSargablePredicateDetector implements QueryPatternDetector {
    private static final Pattern NON_SARGABLE = Pattern.compile(
        "\\bWHERE\\b.*\\b\\w+\\([^\\)]+\\)\\s*=\\s*[^=]+",  // e.g. WHERE YEAR(col) = 2020
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String name() {
        return "NON_SARGABLE_PREDICATE";
    }

    @Override
    public boolean matches(String sql, List<String> plan) {
        return NON_SARGABLE.matcher(sql).find();
    }

    @Override
    public String description() {
        return "Non‐sargable predicate detected; avoid wrapping indexed columns in functions to allow index usage.";
    }
}