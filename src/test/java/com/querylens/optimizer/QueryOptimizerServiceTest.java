package com.querylens.optimizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryOptimizerServiceTest {

    private QueryOptimizerService service;

    @BeforeEach
    void setUp() {
        service = new QueryOptimizerService();
    }

    @Test
    void suggestOptimizations_detectsSelectStar() {
        String sql = "SELECT * FROM users WHERE age > 25";
        List<String> plan = List.of("Seq Scan on users");

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        assertThat(suggestions)
            .isNotEmpty()
            .anyMatch(s -> s.toLowerCase().contains("select *"));
    }

    @Test
    void suggestOptimizations_detectsNonSargablePredicate() {
        String sql = "SELECT * FROM users WHERE YEAR(created_at) = 2020";
        List<String> plan = List.of();

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        assertThat(suggestions)
            .isNotEmpty()
            .anyMatch(s -> s.toLowerCase().contains("sargable"));
    }

    @Test
    void suggestOptimizations_detectsMissingIndex() {
        String sql = "SELECT * FROM users WHERE age = 25";
        List<String> plan = List.of("Seq Scan on users  (cost=0.00..10.00 rows=100 width=32)");

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        assertThat(suggestions)
            .isNotEmpty()
            .anyMatch(s -> s.toLowerCase().contains("index"));
    }

    @Test
    void suggestOptimizations_detectsScalarSubquery() {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM orders WHERE user_id = u.id) FROM users u";
        List<String> plan = List.of();

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        assertThat(suggestions)
            .isNotEmpty()
            .anyMatch(s -> s.toLowerCase().matches(".*(cte|subquery).*"));
    }

    @Test
    void suggestOptimizations_detectsMultipleIssues() {
        String sql = "SELECT * FROM users WHERE YEAR(created_at) = 2020";
        List<String> plan = List.of("Seq Scan on users  (cost=0.00..50.00 rows=1000 width=32)");

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        // Should detect: SELECT *, non-sargable, missing index
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void suggestOptimizations_returnsEmptyForOptimalQuery() {
        String sql = "SELECT id, name FROM users WHERE id = 1";
        List<String> plan = List.of("Index Scan using idx_users_id on users");

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        assertThat(suggestions).isEmpty();
    }

    @Test
    void suggestOptimizations_handlesEmptyPlan() {
        String sql = "SELECT id FROM users";
        List<String> plan = List.of();

        List<String> suggestions = service.suggestOptimizations(sql, plan);

        // Should still work, just won't detect plan-based issues
        assertThat(suggestions).isNotNull();
    }
}

