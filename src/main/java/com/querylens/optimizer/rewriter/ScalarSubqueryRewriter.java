// // src/main/java/com/querylens/optimizer/rewriter/ScalarSubqueryRewriter.java
// package com.querylens.optimizer.rewriter;

// import com.querylens.optimizer.QueryRewriter;
// import org.springframework.stereotype.Component;

// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// /**
//  * Finds single‐column scalar subqueries in the SELECT list and
//  * moves them into a WITH‐CTE to potentially improve planning.
//  *
//  * e.g. transforms:
//  *   SELECT u.*, (SELECT COUNT(*) FROM orders o WHERE o.user_id=u.id) AS order_count
//  *   FROM users u;
//  *
//  * into:
//  *   WITH order_count_cte AS (
//  *     SELECT user_id, COUNT(*) AS cnt
//  *     FROM orders
//  *     GROUP BY user_id
//  *   )
//  *   SELECT u.*, cte.cnt AS order_count
//  *   FROM users u
//  *   LEFT JOIN order_count_cte cte ON cte.user_id = u.id;
//  */
// @Component
// public class ScalarSubqueryRewriter implements QueryRewriter {

//     // Matches a basic scalar subquery WITH alias
//     private static final Pattern SCALAR_SUBQUERY = Pattern.compile(
//       "\\(\\s*SELECT\\s+([^,]+?)\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(.+?)\\)\\s+AS\\s+(\\w+)",
//       Pattern.CASE_INSENSITIVE | Pattern.DOTALL
//     );

//     @Override
//     public String name() {
//         return "SCALAR_SUBQUERY_TO_CTE";
//     }

//     @Override
//     public boolean canRewrite(String sql, List<String> plan) {
//         return SCALAR_SUBQUERY.matcher(sql).find();
//     }

//     @Override
//     public String rewrite(String sql, List<String> plan) {
//         Matcher m = SCALAR_SUBQUERY.matcher(sql);
//         if (!m.find()) {
//             return sql;
//         }

//         // Capture bits
//         String selectExpr = m.group(1).trim();   // e.g. "COUNT(*)"
//         String fromTbl    = m.group(2).trim();   // e.g. "orders"
//         String whereCond  = m.group(3).trim();   // e.g. "o.user_id = u.id"
//         String alias      = m.group(4).trim();   // e.g. "order_count"

//         // Build a CTE name
//         String cteName = alias + "_cte";

//         // Construct the CTE
//         String cte = String.format(
//           "WITH %s AS (\n" +
//           "  SELECT %s AS val, %s AS key_col\n" +
//           "  FROM %s\n" +
//           "  WHERE %s\n" +
//           "  GROUP BY %s\n" +
//           ")\n",
//           cteName, selectExpr, whereCond.split("=")[0].trim(), fromTbl, whereCond, whereCond.split("=")[0].trim()
//         );

//         // Replace the scalar subquery with a join to CTE.val
//         String rewrittenMain = m.replaceFirst(
//           String.format("cte.val AS %s", alias)
//         );

//         // Inject the JOIN clause
//         // assume original has "FROM users u"
//         rewrittenMain = rewrittenMain.replaceFirst(
//           "(FROM\\s+\\w+\\s+\\w+)",
//           "$1\nLEFT JOIN " + cteName + " cte ON cte.key_col = u.id"
//         );

//         return cte + rewrittenMain;
//     }

//     @Override
//     public String description() {
//         return "Moved scalar subquery into a CTE with join to improve plan reuse.";
//     }
// }
// src/main/java/com/querylens/optimizer/rewriter/ScalarSubqueryRewriter.java
package com.querylens.optimizer.rewriter;

import com.querylens.optimizer.QueryRewriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScalarSubqueryRewriter implements QueryRewriter {

    /**
     * Now matches:
     *  ( SELECT <expr> FROM <schema?>.<table> [alias]? WHERE <cond> ) AS <alias>
     */
    private static final Pattern SCALAR_SUBQUERY = Pattern.compile(
        "\\(\\s*SELECT\\s+([^,]+?)\\s+" +             // group(1): the select expression
        "FROM\\s+([\\w\\.]+)(?:\\s+\\w+)?\\s+" +       // group(2): table (with optional schema), skip alias
        "WHERE\\s+(.+?)\\s*\\)\\s+AS\\s+(\\w+)",        // group(3): condition, group(4): alias
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public String name() {
        return "SCALAR_SUBQUERY_TO_CTE";
    }

    @Override
    public boolean canRewrite(String sql, List<String> plan) {
        return SCALAR_SUBQUERY.matcher(sql).find();
    }

    @Override
    public String rewrite(String sql, List<String> plan) {
        Matcher m = SCALAR_SUBQUERY.matcher(sql);
        if (!m.find()) {
            return sql;
        }

        // 1) Capture the pieces
        String selectExpr = m.group(1).trim();   // e.g. "COUNT(*)"
        String fullTable  = m.group(2).trim();   // e.g. "orders" or "schema.orders"
        String condition  = m.group(3).trim();   // e.g. "o.user_id = u.id"
        String alias      = m.group(4).trim();   // e.g. "order_count"

        // 2) Deduce key column from the left side of the condition
        String keyCol = condition.split("=")[0].trim(); // "o.user_id"
        // but we want the column after the dot if alias used:
        if (keyCol.contains(".")) {
            keyCol = keyCol.substring(keyCol.indexOf('.') + 1);
        }

        // 3) Build CTE name and definition
        String cteName = alias + "_cte";
        String cte = String.format(
          "WITH %s AS (\n" +
          "  SELECT %s AS val, %s AS key_col\n" +
          "  FROM %s\n" +
          "  WHERE %s\n" +
          "  GROUP BY %s\n" +
          ")\n",
          cteName,         // name
          selectExpr,      // e.g. COUNT(*)
          keyCol,          // e.g. user_id
          fullTable,       // e.g. orders
          condition,       // e.g. o.user_id = u.id
          keyCol           // GROUP BY user_id
        );

        // 4) Replace the subquery in the SELECT list with cte.val
        String rewritten = m.replaceFirst("cte.val AS " + alias);

        // 5) Inject the JOIN right after the main FROM clause
        //    Assumes the main FROM is like "FROM users u"
        rewritten = rewritten.replaceFirst(
          "(FROM\\s+\\w+\\s+\\w+)", 
          "$1\nLEFT JOIN " + cteName + " cte ON cte.key_col = u.id"
        );

        // 6) Prepend the CTE
        return cte + rewritten;
    }

    @Override
    public String description() {
        return "Moved scalar subquery into a CTE with join to improve plan reuse.";
    }
}
