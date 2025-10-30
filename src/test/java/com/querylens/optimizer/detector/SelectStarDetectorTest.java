package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SelectStarDetectorTest {

    private QueryPatternDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SelectStarDetector();
    }

    @Test
    void name_returnsCorrectName() {
        assertThat(detector.name()).isEqualTo("SELECT_STAR");
    }

    @Test
    void matches_detectsSelectStar() {
        String sql = "SELECT * FROM users WHERE id = 1";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsSelectStarWithDistinct() {
        String sql = "SELECT DISTINCT * FROM orders";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_detectsSelectStarCaseInsensitive() {
        String sql = "select * from products";
        assertThat(detector.matches(sql, List.of())).isTrue();
    }

    @Test
    void matches_doesNotMatchSpecificColumns() {
        String sql = "SELECT id, name, email FROM users";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void matches_doesNotMatchPartialMatch() {
        String sql = "SELECT user_id * 2 FROM calculations";
        assertThat(detector.matches(sql, List.of())).isFalse();
    }

    @Test
    void description_providesHelpfulMessage() {
        String description = detector.description();
        assertThat(description)
            .isNotEmpty()
            .containsIgnoringCase("SELECT *")
            .containsIgnoringCase("columns");
    }
}

