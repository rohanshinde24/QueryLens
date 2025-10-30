package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NonSargablePredicateDetectorTest {

    private QueryPatternDetector detector;

    @BeforeEach
    void setUp() {
        detector = new NonSargablePredicateDetector();
    }

    @Test
    void name_returnsCorrectName() {
        assertThat(detector.name()).isEqualTo("NON_SARGABLE_PREDICATE");
    }

    @Test
    void matches_detectsFunctionInWhere() {
        String sql = "SELECT * FROM users WHERE YEAR(created_at) = 2020";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsUpperFunction() {
        String sql = "SELECT * FROM users WHERE UPPER(email) = 'TEST@EXAMPLE.COM'";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsSubstringFunction() {
        String sql = "SELECT * FROM users WHERE SUBSTRING(name, 1, 3) = 'John'";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_caseInsensitive() {
        String sql = "select * from orders where year(order_date) = 2023";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_doesNotMatchSargableQuery() {
        String sql = "SELECT * FROM users WHERE age = 25";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void matches_doesNotMatchNoWhere() {
        String sql = "SELECT YEAR(created_at) FROM users";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void matches_doesNotMatchFunctionNotOnColumn() {
        String sql = "SELECT * FROM users WHERE age = YEAR(CURRENT_DATE)";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void description_mentionsNonSargableAndIndexUsage() {
        String description = detector.description();
        assertThat(description)
            .isNotEmpty()
            .containsIgnoringCase("non")
            .containsIgnoringCase("sargable")
            .containsIgnoringCase("index");
    }
}

