import React from 'react';
import './Header.css';

function Header() {
  return (
    <header className="header">
      <div className="header-content">
        <div className="logo">
          <span className="logo-icon">🔍</span>
          <h1>QueryLens</h1>
        </div>
        <p className="tagline">BI Query Performance Analyzer - SQL Server Optimization for Donor Analytics</p>
      </div>
    </header>
  );
}

export default Header;

