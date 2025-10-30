package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScalarSubqueryDetectorTest {

    private QueryPatternDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ScalarSubqueryDetector();
    }

    @Test
    void name_returnsCorrectName() {
        assertThat(detector.name()).isEqualTo("SCALAR_SUBQUERY");
    }

    @Test
    void matches_detectsScalarSubqueryInSelect() {
        String sql = "SELECT u.*, (SELECT COUNT(*) FROM orders WHERE user_id = u.id) FROM users u";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsScalarSubqueryWithAggregation() {
        String sql = "SELECT name, (SELECT MAX(amount) FROM transactions WHERE user_id = u.id) FROM users u";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsScalarSubqueryWithAvg() {
        String sql = "SELECT id, (SELECT AVG(score) FROM reviews WHERE product_id = p.id) FROM products p";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_caseInsensitive() {
        String sql = "select *, (select count(*) from orders) from users";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_doesNotMatchRegularSubquery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void matches_doesNotMatchNoSubquery() {
        String sql = "SELECT * FROM users";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void matches_doesNotMatchSubqueryInFrom() {
        String sql = "SELECT * FROM (SELECT * FROM users WHERE active = true) AS active_users";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void description_mentionsCteOrJoin() {
        String description = detector.description();
        assertThat(description)
            .isNotEmpty()
            .matches("(?i).*(cte|join|lateral).*");
    }
}

