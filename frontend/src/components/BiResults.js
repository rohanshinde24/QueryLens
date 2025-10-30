import React from 'react';
import './BiResults.css';

/**
 * Display BI-focused analysis results with bottlenecks
 */
function BiResults({ data }) {
  if (!data || !data.bottlenecks) return null;

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL': return '#ef4444';
      case 'WARNING': return '#f59e0b';
      case 'INFO': return '#3b82f6';
      default: return '#6b7280';
    }
  };

  const getSeverityEmoji = (severity) => {
    switch (severity) {
      case 'CRITICAL': return '🔴';
      case 'WARNING': return '🟡';
      case 'INFO': return '🔵';
      default: return '⚪';
    }
  };

  const getIssueIcon = (issueType) => {
    switch (issueType) {
      case 'NON_SARGABLE_PREDICATE': return '⚡';
      case 'CORRELATED_SUBQUERY': return '🔄';
      case 'OR_CONDITION': return '🔀';
      case 'LATE_FILTER': return '🕒';
      case 'MISSING_INDEX': return '📇';
      case 'HEAVY_AGGREGATION': return '📊';
      default: return '💡';
    }
  };

  return (
    <div className="bi-results-container">
      <div className="results-header">
        <h2>Analysis Results</h2>
        <div className="summary-badges">
          {data.criticalCount > 0 && (
            <span className="badge critical">
              🔴 {data.criticalCount} Critical
            </span>
          )}
          {data.warningCount > 0 && (
            <span className="badge warning">
              🟡 {data.warningCount} Warnings
            </span>
          )}
          {data.infoCount > 0 && (
            <span className="badge info">
              🔵 {data.infoCount} Info
            </span>
          )}
        </div>
      </div>

      <div className="performance-card card">
        <h3>📊 Performance Impact</h3>
        <div className="perf-metrics">
          <div className="perf-metric">
            <span className="perf-label">Total Issues</span>
            <span className="perf-value">{data.totalBottlenecks}</span>
          </div>
          <div className="perf-metric">
            <span className="perf-label">Potential Improvement</span>
            <span className="perf-value highlight">
              {data.potentialImprovementPercent?.toFixed(0)}%
            </span>
          </div>
          {data.estimatedBaselineSeconds > 0 && (
            <div className="perf-metric">
              <span className="perf-label">Estimated Baseline</span>
              <span className="perf-value">{data.estimatedBaselineSeconds.toFixed(1)}s</span>
            </div>
          )}
        </div>
      </div>

      <div className="bottlenecks-section">
        <h3>🔍 Bottlenecks Detected</h3>
        {data.bottlenecks.map((bottleneck, index) => (
          <div 
            key={index} 
            className={`bottleneck-card card ${bottleneck.severity.toLowerCase()}`}
            style={{ borderLeftColor: getSeverityColor(bottleneck.severity) }}
          >
            <div className="bottleneck-header">
              <div className="bottleneck-title">
                <span className="severity-emoji">{getSeverityEmoji(bottleneck.severity)}</span>
                <span className="issue-icon">{getIssueIcon(bottleneck.issueType)}</span>
                <h4>
                  {bottleneck.severity} #{index + 1}: {' '}
                  {bottleneck.issueType?.replace(/_/g, ' ')}
                </h4>
              </div>
              <span className="cost-badge">
                {bottleneck.costPercentage?.toFixed(1)}% of runtime
              </span>
            </div>

            {bottleneck.lineNumber && (
              <div className="location">
                📍 Line {bottleneck.lineNumber}
              </div>
            )}

            {bottleneck.queryFragment && (
              <pre className="query-fragment">
                <code>{bottleneck.queryFragment}</code>
              </pre>
            )}

            <div className="problem-section">
              <strong>⚠️ Problem:</strong>
              <p>{bottleneck.problemDescription}</p>
            </div>

            {bottleneck.whyItsASlow && (
              <div className="why-slow-section">
                <strong>🐌 Why It's Slow:</strong>
                <p>{bottleneck.whyItsASlow}</p>
              </div>
            )}

            {bottleneck.timeImpactSeconds > 0 && (
              <div className="impact-section">
                <strong>💰 Impact:</strong>
                <span> {bottleneck.timeImpactSeconds.toFixed(1)} seconds</span>
              </div>
            )}

            {bottleneck.executionCount > 0 && (
              <div className="execution-count">
                <strong>🔄 Executes:</strong>
                <span> {bottleneck.executionCount.toLocaleString()} times</span>
              </div>
            )}

            {bottleneck.fixes && bottleneck.fixes.length > 0 && (
              <div className="fixes-section">
                <strong>✅ Recommended Fixes:</strong>
                <ol>
                  {bottleneck.fixes.map((fix, i) => (
                    <li key={i}>{fix}</li>
                  ))}
                </ol>
              </div>
            )}

            {bottleneck.optimizedFragment && (
              <div className="optimized-section">
                <strong>✨ Optimized Code:</strong>
                <pre className="optimized-code">
                  <code>{bottleneck.optimizedFragment}</code>
                </pre>
              </div>
            )}

            {bottleneck.fixQueries && bottleneck.fixQueries.length > 0 && (
              <div className="index-section">
                <strong>💾 Index Recommendations:</strong>
                {bottleneck.fixQueries.map((indexSql, i) => (
                  <pre key={i} className="index-sql">
                    <code>{indexSql}</code>
                  </pre>
                ))}
              </div>
            )}

            {bottleneck.expectedImprovement && (
              <div className="improvement-section">
                <strong>📈 Expected Improvement:</strong>
                <p>{bottleneck.expectedImprovement}</p>
              </div>
            )}
          </div>
        ))}
      </div>

      {data.bottlenecks.length === 0 && (
        <div className="no-issues card">
          <span className="success-icon">🎉</span>
          <h3>No Performance Issues Detected!</h3>
          <p>Your query looks well-optimized.</p>
        </div>
      )}
    </div>
  );
}

export default BiResults;

