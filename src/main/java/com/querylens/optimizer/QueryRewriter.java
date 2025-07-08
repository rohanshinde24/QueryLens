// src/main/java/com/querylens/optimizer/QueryRewriter.java
package com.querylens.optimizer;

import java.util.List;

/**
 * Interface for SQL rewrite rules.
 */
public interface QueryRewriter {

    /** A unique key for this rewriter, e.g. "SELECT_STAR" */
    String name();

    /**
     * Returns true if this rewriter can rewrite the given SQL+plan.
     */
    boolean canRewrite(String sql, List<String> plan);

    /**
     * Returns the rewritten SQL. Only call when canRewrite(...) is true.
     */
    String rewrite(String sql, List<String> plan);

    /**
     * A human-readable description of what this rewrite does.
     */
    String description();
}
