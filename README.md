# QueryLens

A Spring Boot-based SQL query analysis and optimization tool that provides intelligent insights and automated query rewriting for PostgreSQL databases.

## 🎯 Overview

QueryLens is a powerful SQL query analyzer that combines PostgreSQL's `EXPLAIN ANALYZE` functionality with intelligent pattern detection to provide comprehensive query optimization recommendations. It analyzes SQL queries, extracts performance metrics, identifies optimization opportunities, and can automatically rewrite queries for better performance.

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

- Java 21+
- Maven 3.6+
- PostgreSQL database

### Installation

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd QueryLens
   ```

2. **Configure database connection**
   Update `src/main/resources/application.properties` with your PostgreSQL connection details:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=org.postgresql.Driver
   ```

3. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

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

Run the test suite:

```bash
mvn test
```

The project includes comprehensive unit tests for:

- Query analysis functionality
- Pattern detection algorithms
- Query rewriting logic
- Controller endpoints

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

## 🔮 Future Enhancements

- Support for other database systems (MySQL, SQL Server)
- More sophisticated query pattern detection
- Machine learning-based optimization suggestions
- Query performance history tracking
- Web-based UI for query analysis
- Integration with database monitoring tools

## 📞 Support

For issues and questions, please open an issue on the project repository.
