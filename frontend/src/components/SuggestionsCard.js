import React from 'react';
import './SuggestionsCard.css';

function SuggestionsCard({ suggestions }) {
  if (!suggestions || suggestions.length === 0) {
    return (
      <div className="card suggestions-card">
        <h3>
          <span className="card-icon">✅</span>
          Optimization Suggestions
        </h3>
        <div className="no-suggestions">
          <span className="success-icon">🎉</span>
          <p>Great! No optimization issues detected.</p>
          <p className="subtitle">Your query looks well-optimized.</p>
        </div>
      </div>
    );
  }

  const getSeverityIcon = (suggestion) => {
    const lower = suggestion.toLowerCase();
    if (lower.includes('select *')) return '⚠️';
    if (lower.includes('sargable') || lower.includes('function')) return '🔴';
    if (lower.includes('index') || lower.includes('sequential')) return '🟡';
    if (lower.includes('subquery') || lower.includes('cte')) return '🔵';
    return '💡';
  };

  return (
    <div className="card suggestions-card">
      <h3>
        <span className="card-icon">💡</span>
        Optimization Suggestions
      </h3>
      <div className="suggestions-list">
        {suggestions.map((suggestion, index) => (
          <div key={index} className="suggestion-item">
            <span className="suggestion-icon">{getSeverityIcon(suggestion)}</span>
            <p className="suggestion-text">{suggestion}</p>
          </div>
        ))}
      </div>
      <div className="suggestions-footer">
        <span className="issue-count">
          {suggestions.length} issue{suggestions.length !== 1 ? 's' : ''} detected
        </span>
      </div>
    </div>
  );
}

export default SuggestionsCard;

