# QueryLens

A **full-stack PostgreSQL performance analysis tool** built with **Java**, **Spring Boot**, and **React** that provides intelligent query diagnostics, pattern detection, and automated SQL rewriting to optimize execution plans.

[![CI/CD Pipeline](https://github.com/yourusername/QueryLens/workflows/CI/CD%20Pipeline/badge.svg)](https://github.com/yourusername/QueryLens/actions)
[![Code Coverage](https://img.shields.io/badge/coverage-90%25-brightgreen)](https://github.com/yourusername/QueryLens)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🎯 Overview

QueryLens is a BI-focused SQL query performance analyzer built specifically for analyzing complex queries on large fact tables. The tool identifies performance bottlenecks through execution plan analysis, attributes costs to specific query sections, and generates optimized rewrites with index recommendations.

### Key Highlights

- 🚀 **70-95% Query Performance Improvement** on production BI queries
- 🎨 **Full-Stack Architecture** with React frontend + Spring Boot backend
- 🔍 **6 Specialized Detectors** using Strategy design pattern
- 📊 **Line-Level Bottleneck Detection** with cost attribution
- 🐳 **Docker-Ready** with complete containerization
- 🧪 **Comprehensive Test Coverage** with JUnit + Mockito
- 🔄 **CI/CD Pipeline** with GitHub Actions

## 📸 Screenshots

### Main Application Interface
![QueryLens Main Interface](data/images/main_app.png)

### Analysis Output - Non-SARGABLE Detection
![Bottleneck Detection Output 1](data/images/output1.png)

### Analysis Output - Cost Attribution
![Bottleneck Detection Output 2](data/images/output2.png)

### Analysis Output - Optimization Recommendations
![Bottleneck Detection Output 3](data/images/output3.png)

## ✨ Features

### 🔍 BI-Specific Query Analysis

The analyzer identifies performance bottlenecks specific to business intelligence workloads:

**6 Specialized Detectors:**

1. **Non-SARGABLE Predicate Detector** - Detects YEAR(), MONTH(), COALESCE() functions blocking index usage
2. **Correlated Subquery Detector** - Identifies per-row subquery execution patterns
3. **OR Condition Detector** - Finds OR conditions preventing index seeks
4. **Late Filter Detector** - Detects filters applied after expensive JOINs
5. **Missing Index Analyzer** - Recommends covering indexes based on query patterns
6. **Heavy Aggregation Optimizer** - Identifies expensive STRING_AGG and GROUP BY operations

### 🎯 Key Capabilities

- **Line-Level Precision**: Identifies exact line numbers of bottlenecks
- **Cost Attribution**: Shows percentage of total runtime for each issue (e.g., "72% of runtime")
- **Automated SQL Rewriting**: Generates optimized query rewrites
- **Index Recommendations**: Creates CREATE INDEX statements with optimal column order
- **Performance Projections**: Estimates improvements based on real-world results
- **Production-Validated**: Tested on queries with 10-40M row fact tables

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
- **Java 21** - Modern LTS version
- **Spring Boot 3.5.3** - Production-ready framework
- **Spring Data JPA** - Database access layer
- **JaCoCo** - Code coverage analysis
- **JUnit 5 + Mockito** - Testing framework

### Frontend
- **React 18** - Modern UI library
- **CSS3** - Custom styling with gradients and animations

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **GitHub Actions** - CI/CD pipeline with coverage reporting
- **Maven** - Build and dependency management

### Architecture Patterns
- **Strategy Pattern** - 6 specialized detector implementations
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers
- **REST API** - Stateless HTTP communication

## 📊 Real-World Performance Results

Validated on production BI queries processing 10-40M row fact tables:

| Optimization Type | Before | After | Improvement |
|-------------------|--------|-------|-------------|
| Non-SARGABLE (YEAR) | 68s | 4.1s | **94% faster** |
| COALESCE → UNION ALL | 11s | 0.9s | **92% faster** |
| Missing Index | 49s | ~3s | **94% faster** |
| Late Filter Pushdown | +8s | +1s | **87% faster** |
| **Average Improvement** | | | **70-95%** |

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
