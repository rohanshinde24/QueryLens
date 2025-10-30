import React, { useState } from 'react';
import './OptimizedQueryCard.css';

function OptimizedQueryCard({ optimizedSql }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(optimizedSql);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!optimizedSql) return null;

  return (
    <div className="card optimized-query-card">
      <div className="card-header">
        <h3>
          <span className="card-icon">✨</span>
          Optimized Query
        </h3>
        <button className="copy-button" onClick={handleCopy}>
          {copied ? (
            <>
              <span>✓</span> Copied!
            </>
          ) : (
            <>
              <span>📋</span> Copy
            </>
          )}
        </button>
      </div>
      
      <div className="query-comparison">
        <div className="query-display">
          <pre className="sql-code">
            <code>{optimizedSql}</code>
          </pre>
        </div>
      </div>

      <div className="optimization-note">
        <span className="note-icon">💡</span>
        <p>This optimized query should provide better performance by addressing detected issues.</p>
      </div>
    </div>
  );
}

export default OptimizedQueryCard;

