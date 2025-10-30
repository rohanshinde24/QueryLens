import React, { useState } from 'react';
import './App.css';
import QueryInput from './components/QueryInput';
import BiResults from './components/BiResults';
import Header from './components/Header';

function App() {
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleAnalyze = async (sql) => {
    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      // Use the new BI analysis endpoint
      const response = await fetch(`${apiUrl}/api/bi/analyze`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ sql }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setResults(data);
    } catch (err) {
      setError(err.message || 'Failed to analyze query. Please check your connection.');
      console.error('Error analyzing query:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="App">
      <Header />
      <main className="main-content">
        <QueryInput onAnalyze={handleAnalyze} loading={loading} />
        {error && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            {error}
          </div>
        )}
        {results && <BiResults data={results} />}
      </main>
    </div>
  );
}

export default App;

