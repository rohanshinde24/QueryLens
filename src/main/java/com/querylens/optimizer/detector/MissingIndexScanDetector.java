// src/main/java/com/querylens/optimizer/detector/MissingIndexScanDetector.java
package com.querylens.optimizer.detector;

import com.querylens.optimizer.QueryPatternDetector;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects full table scans, indicating a missing index.
 */
public class MissingIndexScanDetector implements QueryPatternDetector {
    private static final Pattern SEQ_SCAN = Pattern.compile(
        "\\bSeq Scan on ([\\w\\.]+)",  // captures the table name
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String name() {
        return "MISSING_INDEX_SCAN";
    }

    @Override
    public boolean matches(String sql, List<String> plan) {
        return plan.stream().anyMatch(line -> SEQ_SCAN.matcher(line).find());
    }

    @Override
    public String description() {
        return "Sequential scan detected; consider adding an index on the filtered/joined columns.";
    }
}
