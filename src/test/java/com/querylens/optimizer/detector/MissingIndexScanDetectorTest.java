package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingIndexScanDetectorTest {

    private QueryPatternDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MissingIndexScanDetector();
    }

    @Test
    void name_returnsCorrectName() {
        assertThat(detector.name()).isEqualTo("MISSING_INDEX");
    }

    @Test
    void matches_detectsSeqScanInPlan() {
        List<String> plan = List.of(
            "Seq Scan on users  (cost=0.00..10.00 rows=100 width=32)",
            "Filter: (age > 25)"
        );
        assertThat(detector.matches("SELECT * FROM users WHERE age > 25", plan)).isTrue();
    }

    @Test
    void matches_detectsSequentialScanVariant() {
        List<String> plan = List.of(
            "Sequential Scan on orders  (cost=0.00..50.00 rows=1000 width=64)"
        );
        assertThat(detector.matches("SELECT * FROM orders", plan)).isTrue();
    }

    @Test
    void matches_caseInsensitive() {
        List<String> plan = List.of("seq scan on products");
        assertThat(detector.matches("SELECT * FROM products", plan)).isTrue();
    }

    @Test
    void matches_doesNotMatchIndexScan() {
        List<String> plan = List.of(
            "Index Scan using idx_users_age on users  (cost=0.00..8.00 rows=10 width=32)"
        );
        assertThat(detector.matches("SELECT * FROM users WHERE age = 25", plan)).isFalse();
    }

    @Test
    void matches_doesNotMatchBitmapScan() {
        List<String> plan = List.of(
            "Bitmap Heap Scan on users  (cost=5.00..15.00 rows=100 width=32)"
        );
        assertThat(detector.matches("SELECT * FROM users", plan)).isFalse();
    }

    @Test
    void matches_doesNotMatchEmptyPlan() {
        assertThat(detector.matches("SELECT * FROM users", List.of())).isFalse();
    }

    @Test
    void description_mentionsIndexAndSequentialScan() {
        String description = detector.description();
        assertThat(description)
            .isNotEmpty()
            .containsIgnoringCase("sequential")
            .containsIgnoringCase("index");
    }
}

