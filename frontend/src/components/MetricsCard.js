import React from 'react';
import './MetricsCard.css';

function MetricsCard({ metrics }) {
  if (!metrics) return null;

  const formatTime = (time) => {
    if (time < 1) return `${(time * 1000).toFixed(2)} μs`;
    return `${time.toFixed(2)} ms`;
  };

  const getPerformanceClass = (time) => {
    if (time < 1) return 'excellent';
    if (time < 10) return 'good';
    if (time < 100) return 'moderate';
    return 'slow';
  };

  return (
    <div className="card metrics-card">
      <h3>
        <span className="card-icon">📊</span>
        Performance Metrics
      </h3>
      
      <div className="metrics-grid">
        <div className="metric">
          <span className="metric-label">Execution Time</span>
          <span className={`metric-value ${getPerformanceClass(metrics.executionTime)}`}>
            {formatTime(metrics.executionTime)}
          </span>
        </div>

        <div className="metric">
          <span className="metric-label">Rows Processed</span>
          <span className="metric-value">{metrics.rowsProcessed.toLocaleString()}</span>
        </div>

        <div className="metric">
          <span className="metric-label">Cost Estimate</span>
          <span className="metric-value">{metrics.costEstimate.toFixed(2)}</span>
        </div>

        <div className="metric">
          <span className="metric-label">Statement Type</span>
          <span className="metric-value statement-type">{metrics.statementType}</span>
        </div>
      </div>

      <div className="clause-indicators">
        <span className={`clause-badge ${metrics.hasWhereClause ? 'active' : ''}`}>
          WHERE
        </span>
        <span className={`clause-badge ${metrics.hasJoinClause ? 'active' : ''}`}>
          JOIN
        </span>
        <span className={`clause-badge ${metrics.hasLimitClause ? 'active' : ''}`}>
          LIMIT
        </span>
      </div>

      {metrics.tablesUsed && metrics.tablesUsed.length > 0 && (
        <div className="tables-used">
          <span className="tables-label">Tables:</span>
          <div className="table-tags">
            {metrics.tablesUsed.map((table, index) => (
              <span key={index} className="table-tag">{table}</span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default MetricsCard;

