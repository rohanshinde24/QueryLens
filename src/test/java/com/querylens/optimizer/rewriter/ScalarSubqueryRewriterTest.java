// src/test/java/com/querylens/optimizer/rewriter/ScalarSubqueryRewriterTest.java
package com.querylens.optimizer.rewriter;

import com.querylens.optimizer.QueryRewriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScalarSubqueryRewriterTest {

    private QueryRewriter rewriter;

    @BeforeEach
    void setUp() {
        rewriter = new ScalarSubqueryRewriter();
    }

    @Test
    void canRewrite_detectsScalarSubquery() {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM orders o WHERE o.user_id=u.id) AS order_count FROM users u";
        assertThat(rewriter.canRewrite(sql, List.of())).isTrue();
    }

    @Test
    void rewrite_liftsSubqueryIntoCte() {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM orders o WHERE o.user_id=u.id) AS order_count FROM users u";
        String out = rewriter.rewrite(sql, List.of());

        // Should start with a WITH-clause
        assertThat(out).startsWith("WITH order_count_cte AS");
        // Should contain a LEFT JOIN on order_count_cte
        assertThat(out).contains("LEFT JOIN order_count_cte cte ON cte.key_col = u.id");
        // Should end selecting cte.val AS order_count
        assertThat(out).contains("cte.val AS order_count FROM users u");
    }
}
