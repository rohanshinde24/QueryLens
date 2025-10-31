-- QueryLens Application Database Schema
-- Stores query analysis history, bottlenecks, and optimization tracking
-- Designed for production use by BI teams

-- ============================================================================
-- 1. QUERY SUBMISSIONS TABLE
-- Stores each query submitted for analysis
-- ============================================================================
CREATE TABLE query_submissions (
    id BIGSERIAL PRIMARY KEY,
    query_hash VARCHAR(64) NOT NULL,           -- SHA-256 of normalized query
    original_query TEXT NOT NULL,              -- The actual SQL submitted
    query_type VARCHAR(50),                    -- 'SELECT', 'INSERT', 'UPDATE', etc.
    database_context VARCHAR(100),             -- Which database/schema it's for
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_by VARCHAR(255),                 -- User email or ID
    estimated_baseline_ms DECIMAL(12, 2),      -- Estimated runtime in milliseconds
    actual_runtime_ms DECIMAL(12, 2),          -- Actual runtime if provided
    row_count_estimate BIGINT,                 -- Estimated rows processed
    
    -- Analysis metadata
    analysis_completed BOOLEAN DEFAULT FALSE,
    analysis_duration_ms INTEGER,              -- How long analysis took
    total_bottlenecks_found INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    info_count INTEGER DEFAULT 0,
    potential_improvement_percent DECIMAL(5, 2),
    
    -- Tracking
    is_archived BOOLEAN DEFAULT FALSE,
    last_analyzed_at TIMESTAMP,
    analysis_version VARCHAR(20) DEFAULT '2.0',  -- Track which version analyzed it
    
    -- Indexing for fast lookups
    CONSTRAINT unique_query_hash UNIQUE (query_hash, submitted_at)
);

-- Indexes for performance
CREATE INDEX idx_submissions_submitted_at ON query_submissions(submitted_at DESC);
CREATE INDEX idx_submissions_query_hash ON query_submissions(query_hash);
CREATE INDEX idx_submissions_user ON query_submissions(submitted_by, submitted_at DESC);
CREATE INDEX idx_submissions_unanalyzed ON query_submissions(analysis_completed, submitted_at) 
    WHERE analysis_completed = FALSE;

-- ============================================================================
-- 2. BOTTLENECKS TABLE
-- Stores each bottleneck detected in queries (one-to-many with submissions)
-- ============================================================================
CREATE TABLE bottlenecks (
    id BIGSERIAL PRIMARY KEY,
    query_submission_id BIGINT NOT NULL REFERENCES query_submissions(id) ON DELETE CASCADE,
    
    -- Classification
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO')),
    issue_type VARCHAR(50) NOT NULL,           -- 'NON_SARGABLE_PREDICATE', 'CORRELATED_SUBQUERY', etc.
    
    -- Location in query
    line_number INTEGER,
    start_line INTEGER,
    end_line INTEGER,
    query_fragment TEXT,                       -- The problematic SQL snippet
    
    -- Impact metrics
    cost_percentage DECIMAL(5, 2),             -- % of total query cost
    time_impact_seconds DECIMAL(10, 3),        -- Estimated seconds
    execution_count BIGINT,                    -- For subqueries
    operator_name VARCHAR(255),                -- 'Table Scan on GIVING_DETAIL'
    
    -- Problem description
    problem_description TEXT NOT NULL,
    why_its_slow TEXT,
    
    -- Resolution tracking
    is_fixed BOOLEAN DEFAULT FALSE,
    fix_applied_at TIMESTAMP,
    fix_validated BOOLEAN DEFAULT FALSE,
    actual_improvement_percent DECIMAL(5, 2), -- Actual improvement if fix was tested
    
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_bottlenecks_query_id ON bottlenecks(query_submission_id);
CREATE INDEX idx_bottlenecks_severity ON bottlenecks(severity, cost_percentage DESC);
CREATE INDEX idx_bottlenecks_issue_type ON bottlenecks(issue_type);
CREATE INDEX idx_bottlenecks_unfixed ON bottlenecks(is_fixed, detected_at) 
    WHERE is_fixed = FALSE;

-- ============================================================================
-- 3. OPTIMIZATION_FIXES TABLE
-- Stores recommended fixes for each bottleneck
-- ============================================================================
CREATE TABLE optimization_fixes (
    id BIGSERIAL PRIMARY KEY,
    bottleneck_id BIGINT NOT NULL REFERENCES bottlenecks(id) ON DELETE CASCADE,
    
    -- Fix details
    fix_type VARCHAR(50) NOT NULL,             -- 'REWRITE', 'INDEX', 'RESTRUCTURE'
    fix_description TEXT NOT NULL,
    optimized_sql TEXT,                        -- Rewritten SQL fragment
    fix_query TEXT,                            -- CREATE INDEX or other DDL
    expected_improvement TEXT,
    
    -- Application tracking
    was_applied BOOLEAN DEFAULT FALSE,
    applied_at TIMESTAMP,
    applied_by VARCHAR(255),
    
    -- Validation
    before_runtime_ms DECIMAL(12, 2),
    after_runtime_ms DECIMAL(12, 2),
    actual_improvement_percent DECIMAL(5, 2),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fixes_bottleneck ON optimization_fixes(bottleneck_id);
CREATE INDEX idx_fixes_applied ON optimization_fixes(was_applied, applied_at);

-- ============================================================================
-- 4. INDEX_RECOMMENDATIONS TABLE
-- Tracks index recommendations across queries (many queries may suggest same index)
-- ============================================================================
CREATE TABLE index_recommendations (
    id BIGSERIAL PRIMARY KEY,
    
    -- Index definition
    table_schema VARCHAR(100),
    table_name VARCHAR(255) NOT NULL,
    index_name VARCHAR(255) NOT NULL,
    key_columns TEXT NOT NULL,                 -- JSON array or comma-separated
    include_columns TEXT,                      -- For covering indexes
    index_ddl TEXT NOT NULL,                   -- Full CREATE INDEX statement
    
    -- Tracking
    times_recommended INTEGER DEFAULT 1,       -- How many queries suggested this
    first_recommended_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_recommended_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Implementation status
    is_implemented BOOLEAN DEFAULT FALSE,
    implemented_at TIMESTAMP,
    implemented_by VARCHAR(255),
    
    -- Impact tracking
    estimated_improvement_percent DECIMAL(5, 2),
    queries_benefited INTEGER DEFAULT 0,       -- Count of queries that would benefit
    
    CONSTRAINT unique_index_definition UNIQUE (table_name, index_name)
);

CREATE INDEX idx_index_rec_table ON index_recommendations(table_name);
CREATE INDEX idx_index_rec_not_impl ON index_recommendations(is_implemented, times_recommended DESC)
    WHERE is_implemented = FALSE;

-- ============================================================================
-- 5. QUERY_PATTERNS TABLE  
-- Aggregates common patterns across queries for trend analysis
-- ============================================================================
CREATE TABLE query_patterns (
    id BIGSERIAL PRIMARY KEY,
    pattern_type VARCHAR(50) NOT NULL,         -- 'NON_SARGABLE', 'CORRELATED_SUBQUERY', etc.
    pattern_signature VARCHAR(255) NOT NULL,   -- e.g., 'YEAR(date_column)'
    
    -- Frequency tracking
    occurrences INTEGER DEFAULT 1,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Impact statistics
    avg_cost_percentage DECIMAL(5, 2),
    max_cost_percentage DECIMAL(5, 2),
    avg_improvement_percent DECIMAL(5, 2),
    
    -- Examples
    sample_query_id BIGINT REFERENCES query_submissions(id),
    
    CONSTRAINT unique_pattern UNIQUE (pattern_type, pattern_signature)
);

CREATE INDEX idx_patterns_type ON query_patterns(pattern_type, occurrences DESC);
CREATE INDEX idx_patterns_impact ON query_patterns(avg_cost_percentage DESC);

-- ============================================================================
-- 6. ANALYSIS_SESSIONS TABLE
-- Groups related queries analyzed together (for team collaboration)
-- ============================================================================
CREATE TABLE analysis_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_name VARCHAR(255),
    description TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Statistics
    total_queries INTEGER DEFAULT 0,
    total_bottlenecks INTEGER DEFAULT 0,
    avg_improvement_percent DECIMAL(5, 2),
    
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_sessions_user ON analysis_sessions(created_by, created_at DESC);

-- Link queries to sessions
CREATE TABLE session_queries (
    session_id BIGINT REFERENCES analysis_sessions(id) ON DELETE CASCADE,
    query_id BIGINT REFERENCES query_submissions(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    PRIMARY KEY (session_id, query_id)
);

-- ============================================================================
-- 7. OPTIMIZATION_METRICS TABLE
-- Tracks before/after performance when optimizations are applied
-- ============================================================================
CREATE TABLE optimization_metrics (
    id BIGSERIAL PRIMARY KEY,
    query_submission_id BIGINT NOT NULL REFERENCES query_submissions(id),
    bottleneck_id BIGINT REFERENCES bottlenecks(id),
    
    -- Before metrics
    before_runtime_ms DECIMAL(12, 2) NOT NULL,
    before_logical_reads BIGINT,
    before_cpu_ms DECIMAL(12, 2),
    before_rows_processed BIGINT,
    
    -- After metrics
    after_runtime_ms DECIMAL(12, 2) NOT NULL,
    after_logical_reads BIGINT,
    after_cpu_ms DECIMAL(12, 2),
    after_rows_processed BIGINT,
    
    -- Improvement calculations
    runtime_improvement_percent DECIMAL(5, 2) GENERATED ALWAYS AS 
        (CASE WHEN before_runtime_ms > 0 
         THEN ((before_runtime_ms - after_runtime_ms) / before_runtime_ms * 100)
         ELSE 0 END) STORED,
    
    io_improvement_percent DECIMAL(5, 2) GENERATED ALWAYS AS
        (CASE WHEN before_logical_reads > 0
         THEN ((before_logical_reads - after_logical_reads)::DECIMAL / before_logical_reads * 100)
         ELSE 0 END) STORED,
    
    -- Metadata
    measured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    measured_by VARCHAR(255),
    environment VARCHAR(50) DEFAULT 'production',  -- 'dev', 'test', 'production'
    notes TEXT
);

CREATE INDEX idx_metrics_query ON optimization_metrics(query_submission_id);
CREATE INDEX idx_metrics_improvement ON optimization_metrics(runtime_improvement_percent DESC);

-- ============================================================================
-- 8. AUDIT_LOG TABLE
-- Track all changes and actions for compliance
-- ============================================================================
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,          -- 'QUERY', 'BOTTLENECK', 'FIX', etc.
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,               -- 'CREATED', 'UPDATED', 'DELETED', 'APPLIED'
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changes JSONB,                             -- Store old/new values
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_log(performed_at DESC);
CREATE INDEX idx_audit_user ON audit_log(performed_by, performed_at DESC);

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

-- Recent queries with bottleneck summary
CREATE VIEW vw_recent_queries AS
SELECT 
    qs.id,
    qs.query_hash,
    LEFT(qs.original_query, 100) as query_preview,
    qs.submitted_at,
    qs.submitted_by,
    qs.total_bottlenecks_found,
    qs.critical_count,
    qs.warning_count,
    qs.potential_improvement_percent,
    qs.actual_runtime_ms,
    COUNT(DISTINCT b.id) as bottleneck_details_count,
    COUNT(DISTINCT CASE WHEN b.is_fixed THEN b.id END) as fixed_count
FROM query_submissions qs
LEFT JOIN bottlenecks b ON b.query_submission_id = qs.id
GROUP BY qs.id
ORDER BY qs.submitted_at DESC;

-- Top problematic patterns
CREATE VIEW vw_top_patterns AS
SELECT 
    pattern_type,
    pattern_signature,
    occurrences,
    avg_cost_percentage,
    max_cost_percentage,
    avg_improvement_percent,
    last_seen_at
FROM query_patterns
ORDER BY occurrences DESC, avg_cost_percentage DESC;

-- Index recommendations by priority
CREATE VIEW vw_index_priority AS
SELECT 
    table_name,
    index_name,
    key_columns,
    include_columns,
    times_recommended,
    queries_benefited,
    estimated_improvement_percent,
    index_ddl,
    is_implemented
FROM index_recommendations
WHERE is_implemented = FALSE
ORDER BY times_recommended DESC, estimated_improvement_percent DESC;

-- Performance improvement tracking
CREATE VIEW vw_optimization_impact AS
SELECT 
    DATE(om.measured_at) as measurement_date,
    COUNT(*) as optimizations_measured,
    AVG(om.runtime_improvement_percent) as avg_runtime_improvement,
    AVG(om.io_improvement_percent) as avg_io_improvement,
    SUM(CASE WHEN om.runtime_improvement_percent >= 80 THEN 1 ELSE 0 END) as high_impact_count
FROM optimization_metrics om
WHERE om.measured_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(om.measured_at)
ORDER BY measurement_date DESC;

-- ============================================================================
-- FUNCTIONS FOR DATA MANAGEMENT
-- ============================================================================

-- Function to normalize and hash queries
CREATE OR REPLACE FUNCTION hash_query(query_text TEXT) 
RETURNS VARCHAR(64) AS $$
BEGIN
    -- Remove extra whitespace and normalize
    RETURN ENCODE(SHA256(
        REGEXP_REPLACE(
            REGEXP_REPLACE(LOWER(TRIM(query_text)), '\s+', ' ', 'g'),
            '--.*$', '', 'gm'  -- Remove comments
        )::BYTEA
    ), 'hex');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to update pattern statistics
CREATE OR REPLACE FUNCTION update_pattern_stats() 
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO query_patterns (
        pattern_type, 
        pattern_signature, 
        occurrences, 
        avg_cost_percentage,
        max_cost_percentage,
        sample_query_id,
        first_seen_at,
        last_seen_at
    ) VALUES (
        NEW.issue_type,
        COALESCE(NEW.problem_description, 'Unknown'),
        1,
        NEW.cost_percentage,
        NEW.cost_percentage,
        NEW.query_submission_id,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (pattern_type, pattern_signature) 
    DO UPDATE SET
        occurrences = query_patterns.occurrences + 1,
        avg_cost_percentage = (query_patterns.avg_cost_percentage * query_patterns.occurrences + NEW.cost_percentage) / (query_patterns.occurrences + 1),
        max_cost_percentage = GREATEST(query_patterns.max_cost_percentage, NEW.cost_percentage),
        last_seen_at = CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update patterns when bottleneck inserted
CREATE TRIGGER trg_update_patterns
    AFTER INSERT ON bottlenecks
    FOR EACH ROW
    EXECUTE FUNCTION update_pattern_stats();

-- Function to clean old archived data
CREATE OR REPLACE FUNCTION cleanup_old_data(days_to_keep INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM query_submissions
    WHERE is_archived = TRUE
      AND submitted_at < CURRENT_DATE - (days_to_keep || ' days')::INTERVAL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SEED DATA FOR TESTING
-- ============================================================================

-- Sample query submission
INSERT INTO query_submissions (
    query_hash,
    original_query,
    query_type,
    database_context,
    submitted_by,
    estimated_baseline_ms,
    actual_runtime_ms,
    analysis_completed,
    total_bottlenecks_found,
    critical_count,
    warning_count,
    potential_improvement_percent
) VALUES (
    hash_query('SELECT * FROM GIVING_DETAIL WHERE YEAR(posted_date) = 2023'),
    'SELECT * FROM GIVING_DETAIL WHERE YEAR(posted_date) = 2023',
    'SELECT',
    'SFDC.dbo',
    'rohan.shinde@usc.edu',
    68000.00,
    68000.00,
    TRUE,
    3,
    2,
    1,
    94.0
);

-- Sample bottleneck
INSERT INTO bottlenecks (
    query_submission_id,
    severity,
    issue_type,
    line_number,
    query_fragment,
    cost_percentage,
    time_impact_seconds,
    operator_name,
    problem_description,
    why_its_slow
) VALUES (
    1,
    'CRITICAL',
    'NON_SARGABLE_PREDICATE',
    1,
    'YEAR(posted_date) = 2023',
    72.0,
    49.0,
    'Table Scan on GIVING_DETAIL',
    'Function YEAR() on column prevents index seek',
    'SQL Server cannot use an index on posted_date because the YEAR() function must be applied to every row before comparison.'
);

-- Sample optimization fix
INSERT INTO optimization_fixes (
    bottleneck_id,
    fix_type,
    fix_description,
    optimized_sql,
    fix_query,
    expected_improvement
) VALUES (
    1,
    'REWRITE',
    'Replace YEAR() function with SARGABLE date range',
    'posted_date >= ''2023-01-01'' AND posted_date < ''2024-01-01''',
    'CREATE INDEX IX_posted_date ON GIVING_DETAIL (posted_date) INCLUDE (account, contact, amount);',
    '80-95% reduction in logical reads'
);

-- ============================================================================
-- SUMMARY
-- ============================================================================

-- Display schema summary
SELECT 'Schema created successfully!' as status;
SELECT 'Tables:' as info, COUNT(*) as count FROM information_schema.tables 
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
SELECT 'Views:' as info, COUNT(*) as count FROM information_schema.views 
    WHERE table_schema = 'public';
SELECT 'Indexes:' as info, COUNT(*) as count FROM pg_indexes 
    WHERE schemaname = 'public';

-- Show sample data
SELECT 'Sample Queries:' as section, COUNT(*) as count FROM query_submissions;
SELECT 'Sample Bottlenecks:' as section, COUNT(*) as count FROM bottlenecks;
SELECT 'Sample Fixes:' as section, COUNT(*) as count FROM optimization_fixes;

