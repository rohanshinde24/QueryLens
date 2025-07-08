// src/test/java/com/querylens/optimizer/rewriter/SelectStarRewriterTest.java
package com.querylens.optimizer.rewriter;

import com.querylens.optimizer.QueryRewriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SelectStarRewriterTest {

    @Mock
    private JdbcTemplate jdbc;

    private QueryRewriter rewriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rewriter = new SelectStarRewriter(jdbc);
    }

    @Test
    void canRewrite_detectsSelectStar() {
        String sql = "SELECT * FROM my_table WHERE id = 1";
        assertThat(rewriter.canRewrite(sql, List.of())).isTrue();
    }

    @Test
    void rewrite_replacesStarWithColumnList() {
        String sql = "SELECT * FROM my_schema.my_table WHERE id = 1";

        // Stub JDBC to return a couple of fake columns
        when(jdbc.queryForList(
            anyString(), any(Object[].class), eq(String.class))
        ).thenReturn(List.of("id", "name", "created_at"));

        String rewritten = rewriter.rewrite(sql, List.of());

        // Should no longer contain "* FROM"
        assertThat(rewritten).doesNotContain("* FROM");

        // Should contain the explicit columns in place of the "*"
        assertThat(rewritten)
            .startsWith("SELECT id, name, created_at FROM my_schema.my_table")
            .contains("WHERE id = 1");
    }
}
