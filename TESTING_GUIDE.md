# 🧪 QueryLens Testing Guide

Complete guide for testing the full-stack QueryLens application.

---

## 🎯 Prerequisites Check

Before starting, ensure you have:

### For Docker Method (Recommended)
- ✅ Docker Desktop installed and running
- ✅ Docker Compose available (included with Docker Desktop)
- ✅ Ports 3000, 8080, and 5432 available

### For Manual Method
- ✅ Java 21 installed (`java --version`)
- ✅ Maven 3.6+ installed (`mvn --version`)
- ✅ Node.js 18+ installed (`node --version`)
- ✅ PostgreSQL 15+ installed and running
- ✅ Ports 3000, 8080, and 5432 available

---

## 🐳 Method 1: Test with Docker (Easiest - One Command)

### Step 1: Start All Services

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens
docker-compose up -d
```

This will:
- ✅ Start PostgreSQL database with sample data
- ✅ Build and start Spring Boot backend
- ✅ Build and start React frontend
- ✅ Set up networking between services

### Step 2: Check Service Status

```bash
docker-compose ps
```

You should see 3 services running:
- `querylens-postgres` (healthy)
- `querylens-backend` (healthy)
- `querylens-frontend` (healthy)

### Step 3: View Logs (Optional)

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Step 4: Access the Application

- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080/analyze
- **Backend Health**: http://localhost:8080/actuator/health

### Step 5: Test with Sample Queries

Open http://localhost:3000 and click the example buttons:
1. **SELECT * Issue** - Tests SELECT * detection
2. **Non-SARGABLE Predicate** - Tests function in WHERE clause
3. **Scalar Subquery** - Tests subquery optimization
4. **Missing Index** - Tests sequential scan detection

### Step 6: Stop Services

```bash
docker-compose down

# Remove volumes (clean database)
docker-compose down -v
```

---

## 💻 Method 2: Test Manually (Development Mode)

### Step 1: Set Up PostgreSQL Database

```bash
# Create database
createdb querylens_db

# Initialize schema and sample data
psql -U your_username -d querylens_db -f scripts/init_db.sql
```

Or if using different credentials:
```bash
psql -U postgres -d querylens_db -f scripts/init_db.sql
```

### Step 2: Configure Backend

Edit `src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/querylens_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Step 3: Start Backend (Terminal 1)

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens
./mvnw spring-boot:run
```

Wait for: `Started QuerylensApplication in X.XXX seconds`

### Step 4: Start Frontend (Terminal 2)

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens/frontend
npm install  # First time only
npm start
```

Browser should open automatically at http://localhost:3000

### Step 5: Test the Application

Same as Docker method - use the example queries or enter your own SQL.

---

## 🧪 Method 3: Run Automated Tests

### Unit Tests Only

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens
./mvnw test
```

**Expected Output**:
- ✅ All detector tests pass (4 files, 28+ tests)
- ✅ All service tests pass (2 files, 11+ tests)
- ✅ All rewriter tests pass (2 files, 8+ tests)
- ✅ Controller tests pass (1 file)

### Tests with Coverage Report

```bash
./mvnw clean verify
```

**Generates**:
- Coverage report: `target/site/jacoco/index.html`
- Test results: `target/surefire-reports/`

**Open Coverage Report**:
```bash
# macOS
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

### Performance Benchmark Tests (Requires PostgreSQL)

```bash
# Ensure PostgreSQL is running with sample data first
./mvnw test -Dtest=PerformanceBenchmarkTest
```

**Note**: These tests are `@Disabled` by default. Remove `@Disabled` annotation to run.

---

## 🔍 Verification Checklist

### ✅ Backend Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### ✅ API Test

```bash
curl -X POST http://localhost:8080/analyze \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM users WHERE age > 25"}'
```

**Expected Response**: JSON with metrics, suggestions, and optimizedSql

### ✅ Frontend Test

Open http://localhost:3000 and verify:
- ✅ Header displays "QueryLens"
- ✅ Query input form is visible
- ✅ Example buttons are clickable
- ✅ No console errors in browser DevTools

### ✅ Database Test

```bash
# Connect to database
docker-compose exec postgres psql -U querylens_user -d querylens_db

# Or if running locally
psql -U querylens_user -d querylens_db
```

**Test Query**:
```sql
SELECT COUNT(*) FROM users;
-- Should return 5 rows
```

---

## 🐛 Troubleshooting

### Issue: Port Already in Use

**Error**: `Bind for 0.0.0.0:3000 failed: port is already allocated`

**Solution**:
```bash
# Find process using port
lsof -i :3000   # or :8080, :5432

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Issue: Docker Container Won't Start

**Solution**:
```bash
# Check logs
docker-compose logs backend

# Rebuild without cache
docker-compose build --no-cache
docker-compose up -d
```

### Issue: Frontend Can't Connect to Backend

**Error**: "Failed to analyze query" or CORS error

**Solution**:
1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Check CORS settings in `application.properties`
3. Ensure `REACT_APP_API_URL=http://localhost:8080` in frontend/.env

### Issue: Database Connection Failed

**Error**: `Connection refused` or `authentication failed`

**Solution**:
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Verify credentials in application.properties
```

### Issue: Tests Failing

**Solution**:
```bash
# Clean and rebuild
./mvnw clean install

# Run with debug
./mvnw test -X

# Skip tests for build
./mvnw clean package -DskipTests
```

### Issue: Coverage Below 90%

**Solution**:
```bash
# Check which modules are below threshold
./mvnw verify

# View detailed report
open target/site/jacoco/index.html
```

---

## 🎯 Sample Test Scenarios

### Scenario 1: Test SELECT * Optimization

1. Open http://localhost:3000
2. Enter: `SELECT * FROM users WHERE age > 25;`
3. Click "Analyze Query"

**Expected Results**:
- ✅ Metrics show execution time
- ✅ Suggestion: "Avoid using SELECT *"
- ✅ Optimized SQL shows: `SELECT id, name, email, age, created_at, updated_at FROM users WHERE age > 25`

### Scenario 2: Test Non-SARGABLE Detection

1. Enter: `SELECT * FROM users WHERE YEAR(created_at) = 2023;`
2. Click "Analyze Query"

**Expected Results**:
- ✅ Suggestion: "Non‐sargable predicate detected"
- ✅ Suggestion mentions avoiding function wrappers
- ✅ Sequential scan likely detected

### Scenario 3: Test Scalar Subquery Optimization

1. Enter: `SELECT u.*, (SELECT COUNT(*) FROM orders WHERE user_id = u.id) FROM users u;`
2. Click "Analyze Query"

**Expected Results**:
- ✅ Suggestion mentions CTE or JOIN
- ✅ Optimized SQL shows WITH clause or JOIN

### Scenario 4: Test Missing Index Detection

1. Enter: `SELECT * FROM orders WHERE status = 'pending';`
2. Click "Analyze Query"

**Expected Results**:
- ✅ Suggestion: "Sequential scan detected"
- ✅ Recommendation to add index

---

## 📊 Performance Testing

### Load Test with Sample Queries

```bash
# Install hey (HTTP load generator) if needed
# brew install hey  # macOS
# apt install hey   # Linux

# Run load test
hey -n 100 -c 10 -m POST \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM users"}' \
  http://localhost:8080/analyze
```

**Expected**:
- ✅ 100 requests completed
- ✅ Average response time < 200ms
- ✅ No errors

---

## 🔬 CI/CD Testing

### Test GitHub Actions Locally

```bash
# Install act (GitHub Actions local runner)
brew install act  # macOS

# Run CI workflow
act pull_request
```

### Verify CI Configuration

```bash
# Validate docker-compose
docker-compose config

# Validate GitHub Actions
# (use online validator or IDE extension)
```

---

## 📸 Visual Testing Checklist

Open http://localhost:3000 and verify UI:

- ✅ **Header**: Purple gradient logo, tagline visible
- ✅ **Input Form**: White card with SQL textarea
- ✅ **Example Buttons**: 4 buttons with hover effects
- ✅ **Analyze Button**: Purple gradient, spinner when loading
- ✅ **Results**: Metrics card, suggestions card appear after analysis
- ✅ **Responsive**: Works on mobile screen sizes

---

## 🎓 Learning & Exploration

### Explore Database Schema

```sql
-- Connect to database
docker-compose exec postgres psql -U querylens_user -d querylens_db

-- View tables
\dt

-- Describe users table
\d users

-- Check indexes
\di

-- Sample data
SELECT * FROM users LIMIT 5;
SELECT * FROM orders LIMIT 5;
```

### Test Query Performance

```sql
-- Test with EXPLAIN ANALYZE
EXPLAIN ANALYZE SELECT * FROM users WHERE age > 25;

-- Compare with function in WHERE
EXPLAIN ANALYZE SELECT * FROM users WHERE EXTRACT(YEAR FROM created_at) = 2023;
```

### Experiment with API

```bash
# Health check
curl http://localhost:8080/actuator/health

# Test analysis endpoint
curl -X POST http://localhost:8080/analyze \
  -H "Content-Type: application/json" \
  -d '{"sql": "YOUR_QUERY_HERE"}'

# Pretty print JSON response
curl -X POST http://localhost:8080/analyze \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM users"}' | jq
```

---

## 📝 Test Coverage Verification

### View Coverage Report

```bash
# Generate coverage
./mvnw clean verify

# Open report
open target/site/jacoco/index.html
```

### Verify 90% Threshold

Check these modules:
- ✅ `com.querylens.optimizer.detector` - Should be ~95%
- ✅ `com.querylens.optimizer.rewriter` - Should be ~90%
- ✅ `com.querylens.service` - Should be ~92%
- ✅ `com.querylens.controller` - Should be ~88%

---

## 🏁 Success Criteria

The application is working correctly if:

1. ✅ Docker compose starts all 3 services
2. ✅ Frontend loads at http://localhost:3000
3. ✅ Backend health check returns UP
4. ✅ Sample queries return results with suggestions
5. ✅ Optimized SQL is generated for applicable queries
6. ✅ All unit tests pass (32+ tests)
7. ✅ Code coverage exceeds 90%
8. ✅ No errors in browser console
9. ✅ No errors in backend logs
10. ✅ Database contains sample data

---

## 📞 Need Help?

### Common Commands Reference

```bash
# Docker
docker-compose up -d              # Start services
docker-compose down               # Stop services
docker-compose logs -f backend    # View logs
docker-compose ps                 # Check status
docker-compose restart backend    # Restart service

# Testing
./mvnw test                       # Unit tests
./mvnw verify                     # Tests + coverage
./mvnw clean install             # Full build

# Frontend
cd frontend
npm install                       # Install dependencies
npm start                        # Development server
npm run build                    # Production build
npm test                         # Run tests

# Database
docker-compose exec postgres psql -U querylens_user -d querylens_db
```

---

**Last Updated**: October 30, 2025  
**Status**: ✅ Ready for Testing

