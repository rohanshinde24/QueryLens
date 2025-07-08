// src/main/java/com/querylens/optimizer/QueryRewriteService.java
package com.querylens.optimizer;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Applies the first matching QueryRewriter to produce an optimized SQL.
 */
@Service
public class QueryRewriteService {

    private final List<QueryRewriter> rewriters;

    public QueryRewriteService(List<QueryRewriter> rewriters) {
        this.rewriters = rewriters;
    }

    /**
     * Returns an optimized SQL if any rewriter applies, or empty() otherwise.
     */
    public Optional<String> rewrite(String sql, List<String> plan) {
        return rewriters.stream()
            .filter(r -> r.canRewrite(sql, plan))
            .findFirst()
            .map(r -> r.rewrite(sql, plan));
    }
}
