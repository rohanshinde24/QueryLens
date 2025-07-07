package com.querylens.service;

import com.querylens.model.QueryMetrics;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

// import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class QueryAnalyzerServiceTest {

    @InjectMocks
    private QueryAnalyzerService service;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void analyzeQuery_parsesExecutionStatsAndFlags() {
        // Given
        String sql = "SELECT * FROM users WHERE active = true";
        List<String> plan = List.of(
            "Seq Scan on users  (cost=0.00..10.00 rows=100 width=32) (actual time=0.050..0.100 rows=100 loops=1)",
            "Execution Time: 0.120 ms"
        );
        when(jdbcTemplate.queryForList("EXPLAIN ANALYZE " + sql, String.class))
            .thenReturn(plan);

        // When
        QueryMetrics metrics = service.analyzeQuery(sql);

        // Then
        assertThat(metrics.getExecutionTime()).isEqualTo(0.120);
        assertThat(metrics.getRowsProcessed()).isEqualTo(100);
        assertThat(metrics.getCostEstimate()).isEqualTo(10.00);
        assertThat(metrics.getStatementType()).isEqualTo("SELECT");
        assertThat(metrics.getTablesUsed()).containsExactly("users");
        assertThat(metrics.isHasWhereClause()).isTrue();
        assertThat(metrics.isHasJoinClause()).isFalse();
        assertThat(metrics.isHasLimitClause()).isFalse();
        assertThat(metrics.getRawOutput()).contains("Seq Scan on users");
    }

    @Test
    void analyzeQuery_detectsJoinAndLimitFlags() {
        // Given
        String sql = "SELECT * FROM orders JOIN users ON orders.user_id = users.id LIMIT 5";
        List<String> plan = List.of(
            "Hash Join  (cost=5.00..20.00 rows=500 width=64) (actual time=1.00..2.00 rows=5 loops=1)",
            "Execution Time: 2.500 ms"
        );
        when(jdbcTemplate.queryForList("EXPLAIN ANALYZE " + sql, String.class))
            .thenReturn(plan);

        // When
        QueryMetrics metrics = service.analyzeQuery(sql);

        // Then
        assertThat(metrics.getExecutionTime()).isEqualTo(2.500);
        assertThat(metrics.getRowsProcessed()).isEqualTo(5);
        assertThat(metrics.getCostEstimate()).isEqualTo(20.00);
        assertThat(metrics.getStatementType()).isEqualTo("SELECT");
        assertThat(metrics.getTablesUsed()).contains("orders", "users");
        assertThat(metrics.isHasWhereClause()).isFalse();
        assertThat(metrics.isHasJoinClause()).isTrue();
        assertThat(metrics.isHasLimitClause()).isTrue();
    }
}
