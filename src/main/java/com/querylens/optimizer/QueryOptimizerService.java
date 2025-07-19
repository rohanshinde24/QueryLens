package com.querylens.optimizer;

import com.querylens.optimizer.detector.SelectStarDetector;
import com.querylens.optimizer.detector.MissingIndexScanDetector;
import com.querylens.optimizer.detector.NonSargablePredicateDetector;
import com.querylens.optimizer.detector.ScalarSubqueryDetector;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs all registered QueryPatternDetectors against a SQL + plan
 * and returns the list of human‐readable suggestions.
 */
@Service
public class QueryOptimizerService {

    private final List<QueryPatternDetector> detectors = List.of(
        new SelectStarDetector(),
        new ScalarSubqueryDetector(),
        new NonSargablePredicateDetector(),    // newly added
        new MissingIndexScanDetector() 
        // → add more detectors here as you implement them
    );

    /**
     * Returns a list of optimization suggestions based on the SQL text
     * and the EXPLAIN ANALYZE plan.
     *
     * @param sql  the cleaned SQL string
     * @param plan the list of EXPLAIN ANALYZE output lines
     */
    public List<String> suggestOptimizations(String sql, List<String> plan) {
        return detectors.stream()
            .filter(det -> det.matches(sql, plan))
            .map(QueryPatternDetector::description)
            .toList();
    }
}
