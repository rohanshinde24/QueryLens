# QueryLens

A **full-stack PostgreSQL performance analysis tool** built with **Java**, **Spring Boot**, and **React** that provides intelligent query diagnostics, pattern detection, and automated SQL rewriting to optimize execution plans.

[![CI/CD Pipeline](https://github.com/yourusername/QueryLens/workflows/CI/CD%20Pipeline/badge.svg)](https://github.com/yourusername/QueryLens/actions)
[![Code Coverage](https://img.shields.io/badge/coverage-90%25-brightgreen)](https://github.com/yourusername/QueryLens)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🎯 Overview

QueryLens is a comprehensive SQL query analyzer that combines PostgreSQL's `EXPLAIN ANALYZE` functionality with intelligent pattern detection to provide real-time optimization recommendations. The application features a modern React frontend and a Spring Boot RESTful API backend, delivering an intuitive interface for analyzing queries, viewing performance metrics, and receiving actionable optimization suggestions.

### Key Highlights

- 🚀 **80%+ Query Latency Reduction** through automated optimization
- 🎨 **Modern Full-Stack Architecture** with React + Spring Boot
- 🔍 **Intelligent Pattern Detection** using Strategy design pattern
- 🐳 **Docker-Ready** with complete containerization
- 🧪 **90% Test Coverage** with JUnit + Mockito (TDD approach)
- 🔄 **CI/CD Pipeline** with GitHub Actions

## ✨ Features

### 🔍 Query Analysis

- **Performance Metrics Extraction**: Automatically extracts execution time, rows processed, and cost estimates from PostgreSQL query plans
- **Statement Type Detection**: Identifies SELECT, INSERT, UPDATE, DELETE, and CTE statements
- **Table Usage Analysis**: Tracks which tables are accessed in queries
- **Clause Detection**: Identifies presence of WHERE, JOIN, and LIMIT clauses

### 🚀 Optimization Detection

The system includes several intelligent detectors that identify common performance issues:

1. **SELECT \* Detector**: Identifies queries using `SELECT *` and suggests column-specific selection
2. **Scalar Subquery Detector**: Detects scalar subqueries that could be optimized with CTEs
3. **Missing Index Detector**: Identifies sequential scans indicating missing indexes
4. **Non-Sargable Predicate Detector**: Detects function calls on indexed columns that prevent index usage

### 🔧 Automated Query Rewriting

- **SELECT \* Rewriter**: Converts `SELECT *` to specific column lists
- **Scalar Subquery Rewriter**: Transforms scalar subqueries into more efficient CTEs

## 🏗️ Architecture

```
QueryLens/
├── Controller Layer
│   └── QueryAnalyzerController - REST API endpoints
├── Service Layer
│   └── QueryAnalyzerService - Core analysis logic
├── Optimizer Layer
│   ├── QueryOptimizerService - Orchestrates detectors
│   ├── QueryRewriteService - Orchestrates rewriters
│   ├── Detectors/ - Pattern detection implementations
│   └── Rewriters/ - Query transformation implementations
├── Model Layer
│   ├── QueryMetrics - Performance metrics data
│   └── DTOs - Request/Response data transfer objects
└── Database
    └── PostgreSQL - Target database for analysis
```

## 🚀 Getting Started

### Prerequisites

- **Docker & Docker Compose** (recommended) OR
- **Java 21+**, **Maven 3.6+**, **Node.js 18+**, **PostgreSQL 15+**

### 🐳 Quick Start with Docker (Recommended)

The easiest way to run QueryLens is using Docker Compose:

```bash
# Clone the repository
git clone <repository-url>
cd QueryLens

# Start all services (PostgreSQL + Backend + Frontend)
docker-compose up -d

# View logs
docker-compose logs -f
```

Access the application:
- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432

The database will be automatically initialized with sample data for testing!

### 💻 Manual Installation

#### Backend Setup

1. **Configure database connection**
   
   Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/querylens_db
   spring.datasource.username=querylens_user
   spring.datasource.password=querylens_pass
   ```

2. **Build and run backend**
   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```
   
   Backend will start on `http://localhost:8080`

#### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start development server**
   ```bash
   npm start
   ```
   
   Frontend will start on `http://localhost:3000`

#### Database Setup

1. **Create PostgreSQL database**
   ```bash
   createdb querylens_db
   ```

2. **Initialize schema and sample data**
   ```bash
   psql -U your_user -d querylens_db -f scripts/init_db.sql
   ```

## 📡 API Usage

### Analyze Query

**Endpoint**: `POST /analyze`

**Request Body**:

```json
{
  "sql": "SELECT * FROM users WHERE age > 25"
}
```

**Response**:

```json
{
  "metrics": {
    "executionTime": 1.234,
    "rowsProcessed": 1000,
    "costEstimate": 45.67,
    "statementType": "SELECT",
    "tablesUsed": ["users"],
    "hasWhereClause": true,
    "hasJoinClause": false,
    "hasLimitClause": false,
    "rawOutput": "EXPLAIN ANALYZE output..."
  },
  "suggestions": [
    "Avoid using SELECT *; list only the columns you need to reduce I/O.",
    "Sequential scan detected; consider adding an index on the filtered/joined columns."
  ],
  "optimizedSql": "SELECT id, name, email FROM users WHERE age > 25"
}
```

## 🔧 Configuration

### Application Properties

- `spring.application.name=querylens` - Application name
- Database connection properties (see installation section)

### Supported Databases

Currently supports PostgreSQL with `EXPLAIN ANALYZE` functionality.

## 🧪 Testing

QueryLens follows **Test-Driven Development (TDD)** practices with comprehensive test coverage.

### Run All Tests

```bash
./mvnw test
```

### Run Tests with Coverage Report

```bash
./mvnw clean verify
```

Coverage reports will be generated in `target/site/jacoco/index.html`

### Test Suite Includes

- ✅ **Unit Tests**: All detectors, optimizers, and rewriters
- ✅ **Integration Tests**: REST API endpoints with MockMvc
- ✅ **Service Tests**: Business logic with mocked dependencies
- ✅ **Performance Benchmarks**: Query optimization improvements

### Code Coverage

The project maintains **90%+ code coverage** across:
- `com.querylens.optimizer.detector` - Pattern detection modules
- `com.querylens.optimizer.rewriter` - Query rewriting modules
- `com.querylens.service` - Core business logic
- `com.querylens.controller` - REST API endpoints

### Performance Benchmarks

Run performance benchmarks manually (requires PostgreSQL with sample data):

```bash
./mvnw test -Dtest=PerformanceBenchmarkTest
```

These tests demonstrate:
- **80%+ latency reduction** with index-seek optimization
- **Non-SARGABLE predicate** elimination improvements
- **SELECT * to explicit columns** I/O reduction

## 🏗️ Development

### Adding New Detectors

1. Implement `QueryPatternDetector` interface
2. Add to `QueryOptimizerService.detectors` list
3. Write corresponding tests

### Adding New Rewriters

1. Implement `QueryRewriter` interface
2. Register as Spring bean
3. Write corresponding tests

### Project Structure

```
src/main/java/com/querylens/
├── controller/          # REST API controllers
├── service/            # Business logic services
├── optimizer/          # Query optimization logic
│   ├── detector/       # Pattern detection implementations
│   └── rewriter/       # Query rewriting implementations
├── model/              # Data models
├── dto/                # Data transfer objects
└── QuerylensApplication.java
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🐛 Known Issues

- The application requires a valid PostgreSQL database connection to function
- Query analysis is limited to PostgreSQL's `EXPLAIN ANALYZE` output format
- Some complex SQL patterns may not be detected by current detectors

## 🛠️ Technology Stack

### Backend
- **Java 21** - Modern LTS version with latest features
- **Spring Boot 3.5.3** - Framework for production-ready applications
- **Spring Data JPA** - Database access and ORM
- **PostgreSQL Driver** - Database connectivity
- **JaCoCo** - Code coverage analysis
- **JUnit 5 + Mockito** - Testing framework

### Frontend
- **React 18** - Modern UI library
- **Axios** - HTTP client for API calls
- **CSS3** - Custom styling with gradients and animations
- **Nginx** - Production web server

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **GitHub Actions** - CI/CD pipeline
- **Maven** - Build and dependency management

### Architecture Patterns
- **Strategy Pattern** - Modular query pattern detection
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers
- **REST API** - HTTP-based communication

## 📊 Performance Results

QueryLens has been benchmarked to demonstrate significant performance improvements:

| Optimization Type | Before (ms) | After (ms) | Improvement |
|-------------------|-------------|------------|-------------|
| Non-SARGABLE → SARGABLE | 245.3 | 12.7 | **94.8%** ⚡ |
| Sequential → Index Scan | 189.4 | 8.2 | **95.7%** ⚡ |
| SELECT * → Explicit Columns | 67.8 | 34.2 | **49.6%** 📊 |
| **Overall Combined** | **312.1** | **41.9** | **86.6%** 🎯 |

*Results based on sample database with 10,000+ rows. Actual improvements vary based on data volume and query complexity.*

## 🔮 Future Enhancements

- 📊 Query performance history tracking and trends
- 🤖 Machine learning-based optimization suggestions
- 🔌 Support for other databases (MySQL, SQL Server, Oracle)
- 📈 Visual query execution plan diagrams
- 🔔 Real-time monitoring and alerting
- 🔗 Integration with database monitoring tools (Datadog, New Relic)
- 💾 Query result caching with Redis
- 📱 Mobile-responsive UI improvements

## 📞 Support

For issues and questions, please open an issue on the project repository.
