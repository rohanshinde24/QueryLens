import React from 'react';
import './Results.css';
import MetricsCard from './MetricsCard';
import SuggestionsCard from './SuggestionsCard';
import OptimizedQueryCard from './OptimizedQueryCard';

function Results({ data }) {
  if (!data) return null;

  return (
    <div className="results-container">
      <h2 className="results-title">Analysis Results</h2>
      
      <div className="results-grid">
        <MetricsCard metrics={data.metrics} />
        <SuggestionsCard suggestions={data.suggestions} />
      </div>

      {data.optimizedSql && (
        <OptimizedQueryCard 
          originalSql={data.metrics?.rawOutput} 
          optimizedSql={data.optimizedSql} 
        />
      )}
    </div>
  );
}

export default Results;

