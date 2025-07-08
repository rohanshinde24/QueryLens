// // src/main/java/com/querylens/optimizer/rewriter/SelectStarRewriter.java
// package com.querylens.optimizer.rewriter;

// import com.querylens.optimizer.QueryRewriter;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Component;

// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;

// /**
//  * Rewrites "SELECT * FROM table" into "SELECT col1, col2, ... FROM table".
//  */
// @Component
// public class SelectStarRewriter implements QueryRewriter {

//     private static final Pattern SELECT_STAR = Pattern.compile(
//         "^\\s*SELECT\\s+\\*\\s+FROM\\s+([\\w.]+)", 
//         Pattern.CASE_INSENSITIVE
//     );

//     private final JdbcTemplate jdbc;

//     public SelectStarRewriter(JdbcTemplate jdbc) {
//         this.jdbc = jdbc;
//     }

//     @Override
//     public String name() {
//         return "SELECT_STAR";
//     }

//     @Override
//     public boolean canRewrite(String sql, List<String> plan) {
//         return SELECT_STAR.matcher(sql).find();
//     }

//     @Override
//     public String rewrite(String sql, List<String> plan) {
//         Matcher m = SELECT_STAR.matcher(sql);
//         if (!m.find()) {
//             return sql;
//         }
//         String table = m.group(1);
//         // Query information_schema for columns
//         List<String> cols = jdbc.queryForList(
//             "SELECT column_name FROM information_schema.columns " +
//             "WHERE table_name = ? ORDER BY ordinal_position",
//             new Object[]{ table.contains(".") ? table.split("\\.")[1] : table },
//             String.class
//         );
//         String colList = cols.stream().collect(Collectors.joining(", "));
//         // Replace the first occurrence of "SELECT *" with explicit list
//         return sql.replaceFirst("\\*\\s+FROM", colList + " FROM");
//     }

//     @Override
//     public String description() {
//         return "Rewrote SELECT * to explicit column list to reduce I/O.";
//     }
// }
// src/main/java/com/querylens/optimizer/rewriter/SelectStarRewriter.java
package com.querylens.optimizer.rewriter;

import com.querylens.optimizer.QueryRewriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Rewrites "SELECT * FROM table" (or "SELECT DISTINCT * FROM table")
 * into "SELECT col1, col2, ... FROM table".
 */
@Component
public class SelectStarRewriter implements QueryRewriter {

    private static final Pattern SELECT_STAR = Pattern.compile(
        // 1: SELECT keyword + optional DISTINCT/ALL/other modifiers
        "^\\s*(SELECT\\s+(?:DISTINCT\\s+|ALL\\s+)?)(\\*)\\s+FROM\\s+([\\w\\.]+)",
        Pattern.CASE_INSENSITIVE
    );

    private final JdbcTemplate jdbc;

    public SelectStarRewriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String name() {
        return "SELECT_STAR";
    }

    @Override
    public boolean canRewrite(String sql, List<String> plan) {
        return SELECT_STAR.matcher(sql).find();
    }

    @Override
    public String rewrite(String sql, List<String> plan) {
        Matcher m = SELECT_STAR.matcher(sql);
        if (!m.find()) {
            return sql;  // no match
        }

        // 1) Capture the SELECT prefix (with DISTINCT/ALL if present)
        String selectPrefix = m.group(1);  // e.g. "SELECT " or "SELECT DISTINCT "

        // 2) Capture the table name (may include schema: "schema.table")
        String fullTableName = m.group(3);
        String tableName = fullTableName.contains(".")
            ? fullTableName.substring(fullTableName.indexOf('.') + 1)
            : fullTableName;

        // 3) Query information_schema for its columns
        List<String> cols = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns " +
            "WHERE table_name = ? ORDER BY ordinal_position",
            new Object[]{ tableName },
            String.class
        );

        // 4) Build a comma‐separated list
        String colList = cols.stream()
            .collect(Collectors.joining(", "));

        // 5) Replace only the matched SELECT * phrase with SELECT col1, col2, ... FROM
        //    We use the literal match region rather than a regex string for safety
        int start = m.start(2);  // position of the '*'
        int end   = m.end(2);    // right after the '*'
        StringBuilder rewritten = new StringBuilder(sql);
        rewritten.replace(start, end, colList);

        return rewritten.toString();
    }

    @Override
    public String description() {
        return "Rewrote SELECT * to explicit column list to reduce I/O.";
    }
}
