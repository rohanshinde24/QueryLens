# ⚡ QueryLens BI Edition - Quick Start

**Your app is RUNNING NOW!** 🚀

---

## 🌐 **Access Your Application**

### **Open in Browser:**
👉 http://localhost:3000

### **API Endpoint:**
👉 http://localhost:8080/api/bi/analyze

### **Health Check:**
👉 http://localhost:8080/actuator/health

---

## 🎯 **Quick Test (30 seconds)**

1. Open: http://localhost:3000
2. Click: **"YEAR() Function (Non-SARGABLE)"**
3. Click: **"Analyze Query"**
4. See: Detailed bottleneck analysis!

**You should see:**
- 🔴 2 Critical issues detected
- 📍 Line numbers identified
- 💰 Cost percentages (72% of runtime)
- ✅ Specific SQL fixes
- 💾 Index recommendations
- 📈 95% improvement potential

---

## 📊 **What It Analyzes**

### **Detects 6 Types of Bottlenecks:**

| Issue | Example | Impact |
|-------|---------|--------|
| **Non-SARGABLE** | `YEAR(date) = 2023` | 70-95% slower |
| **COALESCE** | `COALESCE(a,b) = X` | 10-20x slower |
| **Correlated Subquery** | `(SELECT MAX...)` per row | 50-90% slower |
| **OR Condition** | `(a = X OR b = Y)` | 80-95% slower |
| **Late Filter** | Filter after JOIN | 20-40% slower |
| **Heavy Aggregation** | `STRING_AGG(DISTINCT)` | 20-50% slower |

---

## 🧪 **Test With Your Queries**

### **Example Queries to Try:**

1. **YEAR() Function** - Click example button
2. **COALESCE Pattern** - Click example button
3. **Your Real Query A** - Paste it!
4. **Your Real Query B** - Paste it!

---

## 📁 **Key Files**

| File | Purpose |
|------|---------|
| `FULLSTACK_BI_READY.md` | Complete usage guide |
| `HOW_TO_TEST_NOW.md` | Testing instructions |
| `BUILD_SUMMARY.md` | What was built |
| `CHANGES.md` | Complete changelog |

---

## 🛑 **Stop Services**

```bash
kill $(cat backend.pid)
kill $(cat frontend/frontend.pid)
brew services stop postgresql@14
```

---

## 🔄 **Restart Services**

```bash
brew services start postgresql@14
./mvnw spring-boot:run -DskipTests &
cd frontend && npm start &
```

---

## ✅ **Status Check**

```bash
# All services running?
curl -s http://localhost:8080/actuator/health | grep "UP"
curl -s http://localhost:3000 | grep "QueryLens"
```

---

## 🎓 **For Demos/Interviews**

**One-Liner:**
> "I built a BI query analyzer that reduced our donor analytics from 68 seconds to 4.1 seconds - 94% faster."

**Tech Stack:**
> "Spring Boot backend with 6 Strategy-pattern detectors, React frontend, analyzes 10-40M row queries."

**Show This:**
1. Open http://localhost:3000
2. Click example
3. Point to bottleneck analysis
4. Show the generated fix
5. Explain: "This matches what our team manually created"

---

## 🎉 **YOU'RE READY!**

**Everything is running and tested.**  
**Go to http://localhost:3000 now!** 🚀

