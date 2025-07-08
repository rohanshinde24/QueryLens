package com.querylens.optimizer;

import java.util.List;

/**
 * A pluggable detector of SQL anti-patterns.
 */
public interface QueryPatternDetector {

    /** A unique key for this detector (e.g. "SELECT_STAR") */
    String name();

    /**
     * Returns true if the given SQL or its plan triggers this pattern.
     *
     * @param sql  the cleaned SQL string
     * @param plan the list of EXPLAIN ANALYZE output lines
     */
    boolean matches(String sql, List<String> plan);

    /** A human-readable description of the issue */
    String description();
}