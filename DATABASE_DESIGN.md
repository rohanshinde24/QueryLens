# QueryLens Database Schema Design

## Overview

The application database stores **query analysis metadata**, not the data being queried. This tracks:
- Queries submitted for analysis
- Bottlenecks detected
- Optimization recommendations
- Performance improvements over time

---

## Schema Design (8 Tables)

### 1. **query_submissions** (Main table)
**Purpose:** Every query analyzed by the tool

**Key Fields:**
- `query_hash` - SHA-256 of normalized query (dedupe identical queries)
- `original_query` - Full SQL text
- `submitted_by` - Who ran the analysis
- `actual_runtime_ms` - How slow it actually is
- `total_bottlenecks_found` - Count of issues
- `potential_improvement_percent` - How much faster it could be

**Why:** Central record of every analysis. Can track which queries are analyzed most often.

---

### 2. **bottlenecks** (One-to-many with submissions)
**Purpose:** Each performance issue detected

**Key Fields:**
- `query_submission_id` - Links to parent query
- `severity` - CRITICAL / WARNING / INFO
- `issue_type` - NON_SARGABLE_PREDICATE, CORRELATED_SUBQUERY, etc.
- `line_number` - Where in the query
- `cost_percentage` - % of total runtime
- `is_fixed` - Has the user applied the fix?
- `actual_improvement_percent` - Did the fix work?

**Why:** Track each specific issue. Can measure which bottleneck types are most common.

---

### 3. **optimization_fixes** (One-to-many with bottlenecks)
**Purpose:** Recommended fixes for each bottleneck

**Key Fields:**
- `bottleneck_id` - Which issue this fixes
- `optimized_sql` - The rewritten SQL
- `fix_query` - CREATE INDEX or other DDL
- `was_applied` - Did user implement this?
- `before_runtime_ms` / `after_runtime_ms` - Validation metrics

**Why:** Track whether recommendations are useful. Measure actual improvement when applied.

---

### 4. **index_recommendations** (Aggregated across queries)
**Purpose:** Track which indexes are suggested most often

**Key Fields:**
- `table_name` - Which table needs the index
- `index_ddl` - CREATE INDEX statement
- `times_recommended` - How many queries suggested this
- `is_implemented` - Has DBA created it?
- `queries_benefited` - How many queries improved

**Why:** If 10 queries all suggest the same index, DBA knows it's high priority!

---

### 5. **query_patterns** (Trend analysis)
**Purpose:** Aggregate common anti-patterns

**Key Fields:**
- `pattern_type` - NON_SARGABLE, OR_CONDITION, etc.
- `pattern_signature` - e.g., "YEAR(date_column)"
- `occurrences` - How many times seen
- `avg_cost_percentage` - Average impact
- `avg_improvement_percent` - Average gain from fixing

**Why:** Show "YEAR() on dates is our #1 issue (seen 47 times, avg 72% cost)"

---

### 6. **analysis_sessions** (Team collaboration)
**Purpose:** Group related queries (e.g., "Q4 Report Optimization Sprint")

**Key Fields:**
- `session_name` - "Donor Analytics Optimization"
- `created_by` - Team lead
- `total_queries` - Queries in this session
- `avg_improvement_percent` - Overall team impact

**Why:** Teams can work together on optimizing a set of reports.

---

### 7. **optimization_metrics** (Before/After validation)
**Purpose:** Store actual performance measurements

**Key Fields:**
- `before_runtime_ms` - Original query time
- `after_runtime_ms` - After optimization
- `runtime_improvement_percent` - Calculated automatically
- `before_logical_reads` / `after_logical_reads` - I/O metrics

**Why:** Prove the optimizations work! "Applied 15 fixes, avg 87% improvement"

---

### 8. **audit_log** (Compliance)
**Purpose:** Track all actions for security/compliance

**Key Fields:**
- `entity_type` - What was changed
- `action` - CREATED, UPDATED, DELETED
- `performed_by` - Who did it
- `changes` - JSONB of old/new values

**Why:** Enterprise compliance, debugging, security audits.

---

## Key Design Decisions (DBA Best Practices)

### 1. **Normalization**
- Properly normalized (3NF)
- Separate tables for entities
- No redundant data

### 2. **Indexing Strategy**
- Primary keys on all tables
- Foreign keys indexed
- Query patterns indexed (submitted_at DESC for recent queries)
- Partial indexes for common filters (WHERE is_fixed = FALSE)

### 3. **Generated Columns**
```sql
runtime_improvement_percent GENERATED ALWAYS AS 
    ((before - after) / before * 100) STORED
```
- Auto-calculates improvement
- Always accurate
- No application logic needed

### 4. **Triggers for Automation**
```sql
CREATE TRIGGER trg_update_patterns
    AFTER INSERT ON bottlenecks
    FOR EACH ROW EXECUTE FUNCTION update_pattern_stats();
```
- Auto-aggregates patterns when bottleneck inserted
- No manual aggregation needed
- Always up-to-date statistics

### 5. **JSONB for Flexibility**
- `audit_log.changes` - Flexible change tracking
- Can query with JSON operators
- Future-proof for schema evolution

### 6. **Soft Deletes**
- `is_archived` instead of DELETE
- Keeps history
- Can restore if needed

### 7. **Constraints**
- CHECK constraints for data integrity
- UNIQUE constraints to prevent duplicates
- Foreign keys with CASCADE for referential integrity

---

## Query Examples

### Get recent queries with bottlenecks
```sql
SELECT * FROM vw_recent_queries LIMIT 10;
```

### Find most common optimization opportunities
```sql
SELECT * FROM vw_top_patterns;
```

### High-priority index recommendations
```sql
SELECT * FROM vw_index_priority WHERE times_recommended >= 5;
```

### Team performance metrics
```sql
SELECT 
    submitted_by,
    COUNT(*) as queries_analyzed,
    AVG(potential_improvement_percent) as avg_improvement,
    COUNT(CASE WHEN critical_count > 0 THEN 1 END) as queries_with_critical_issues
FROM query_submissions
WHERE submitted_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY submitted_by;
```

### Track optimization adoption
```sql
SELECT 
    issue_type,
    COUNT(*) as detected,
    SUM(CASE WHEN is_fixed THEN 1 ELSE 0 END) as fixed,
    AVG(actual_improvement_percent) as avg_improvement
FROM bottlenecks
WHERE detected_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY issue_type
ORDER BY detected DESC;
```

---

## Why This Design?

### For Your BI Team:
- ✅ Track which queries are slowest
- ✅ See which optimizations work best
- ✅ Identify recurring patterns
- ✅ Measure team impact
- ✅ Prioritize index creation
- ✅ Compliance/audit trail

### For DBAs:
- ✅ See which indexes are needed most
- ✅ Track implementation status
- ✅ Validate index effectiveness
- ✅ Historical trend analysis

### For Developers:
- ✅ Easy to query
- ✅ Well-indexed for performance
- ✅ Automated statistics
- ✅ Clear relationships

---

## Size Estimates

For 1 year of heavy use:
- **query_submissions:** ~50K rows (~50MB)
- **bottlenecks:** ~150K rows (~100MB)
- **optimization_fixes:** ~200K rows (~150MB)
- **index_recommendations:** ~500 rows (~1MB)
- **query_patterns:** ~200 rows (~0.5MB)
- **optimization_metrics:** ~10K rows (~10MB)
- **audit_log:** ~500K rows (~250MB)

**Total:** ~550MB/year (very manageable)

---

## Maintenance

### Archive old data (keep last 90 days active):
```sql
-- Mark as archived
UPDATE query_submissions 
SET is_archived = TRUE
WHERE submitted_at < CURRENT_DATE - INTERVAL '90 days';

-- Clean up after 1 year
SELECT cleanup_old_data(365);
```

### Vacuum and analyze:
```sql
VACUUM ANALYZE query_submissions;
VACUUM ANALYZE bottlenecks;
```

---

This is a **production-ready schema** designed by DBA best practices!

