import React, { useState } from 'react';
import './QueryInput.css';

function QueryInput({ onAnalyze, loading }) {
  const [sql, setSql] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (sql.trim()) {
      onAnalyze(sql.trim());
    }
  };

  const handleExample = (exampleSql) => {
    setSql(exampleSql);
  };

  const examples = [
    {
      name: 'YEAR() Function (Non-SARGABLE)',
      sql: `-- Donor giving with non-SARGABLE date filter
SELECT 
  d.descr AS donor_name,
  SUM(gd.amount) AS total_giving
FROM SFDC.dbo.GIVING_DETAIL gd
JOIN COGNOS_DW.dbo.DONOR_DIM d
  ON d.SF_ID = COALESCE(gd.account, gd.contact)
WHERE gd.isdeleted = 'false'
  AND YEAR(gd.posted_date) = 2023
GROUP BY d.descr;`
    },
    {
      name: 'COALESCE() Blocking Index',
      sql: `-- Account/Contact pattern
SELECT * 
FROM GIVING_DETAIL gd
WHERE COALESCE(gd.account, gd.contact) = @donor_id
  AND gd.posted_date BETWEEN '2023-01-01' AND '2023-12-31';`
    },
    {
      name: 'Correlated Subquery',
      sql: `-- Per-row subquery execution
SELECT 
  d.descr,
  (SELECT MAX(gd.posted_date) 
   FROM GIVING_DETAIL gd 
   WHERE COALESCE(gd.account, gd.contact) = d.SF_ID) AS last_gift
FROM DONOR_DIM d;`
    },
    {
      name: 'Late Filter on Dimension',
      sql: `-- Business unit filtered after JOIN
SELECT *
FROM GIVING_DETAIL gd
JOIN DESIGNATION dd ON dd.id = gd.designation
WHERE dd.business_unit = 'Dornsife';`
    }
  ];

  return (
    <div className="query-input-container">
      <div className="card">
        <h2>Enter Your SQL Query</h2>
        <form onSubmit={handleSubmit}>
          <textarea
            className="sql-input"
            value={sql}
            onChange={(e) => setSql(e.target.value)}
            placeholder="Enter your PostgreSQL query here...&#10;Example: SELECT * FROM users WHERE age > 25;"
            rows="8"
            disabled={loading}
          />
          <div className="button-group">
            <button 
              type="submit" 
              className="analyze-button"
              disabled={loading || !sql.trim()}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Analyzing...
                </>
              ) : (
                <>
                  <span>🔍</span>
                  Analyze Query
                </>
              )}
            </button>
            <button 
              type="button" 
              className="clear-button"
              onClick={() => setSql('')}
              disabled={loading || !sql}
            >
              Clear
            </button>
          </div>
        </form>
        
        <div className="examples">
          <h3>Try Examples:</h3>
          <div className="example-buttons">
            {examples.map((example, index) => (
              <button
                key={index}
                className="example-button"
                onClick={() => handleExample(example.sql)}
                disabled={loading}
              >
                {example.name}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default QueryInput;

