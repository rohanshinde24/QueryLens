# Deploy QueryLens to Render

## Quick Deployment Guide

### Step 1: Deploy Backend to Render

1. **Go to Render Dashboard**
   - Visit: https://dashboard.render.com
   - Sign in with GitHub

2. **Create New Web Service**
   - Click "New +" button
   - Select "Web Service"
   - Connect your GitHub account if not already
   - Select repository: **rohanshinde24/QueryLens**

3. **Configure Service**
   ```
   Name: querylens-api
   Region: Choose closest to you (e.g., Oregon)
   Branch: main
   Root Directory: (leave empty)
   Runtime: Java
   Build Command: ./mvnw clean package -DskipTests
   Start Command: java -Dserver.port=$PORT -jar target/querylens-0.0.1-SNAPSHOT.jar
   Instance Type: Free
   ```

4. **Add PostgreSQL Database**
   - In the same project, click "New +"
   - Select "PostgreSQL"
   - Name: querylens-db
   - Plan: Free
   - Click "Create Database"

5. **Link Database to Web Service**
   - Go back to your web service
   - Click "Environment" tab
   - Add these variables:
   ```
   SPRING_DATASOURCE_URL = (copy Internal Database URL from postgres)
   SPRING_DATASOURCE_USERNAME = (copy from postgres)
   SPRING_DATASOURCE_PASSWORD = (copy from postgres)
   SERVER_PORT = (Render provides this automatically as $PORT)
   ```

6. **Initialize Database**
   - Go to your PostgreSQL instance
   - Click "Connect" → "External Connection"
   - Use the PSQL command or connection details
   - Run: `psql <connection-string> -f scripts/init_db.sql`
   
   Or use the web shell in Render dashboard.

7. **Deploy**
   - Click "Create Web Service"
   - Wait ~3-5 minutes for build
   - Your backend will be live at: `https://querylens-api.onrender.com`

---

### Step 2: Update Frontend for Production

1. **Update API URL**
   ```bash
   cd /Users/rohanshinde/Documents/projects/QueryLens
   
   # Update production env
   echo "REACT_APP_API_URL=https://querylens-api.onrender.com" > frontend/.env.production
   
   # Commit
   git add frontend/.env.production
   git commit -m "Configure production API URL for Render backend"
   git push origin main
   ```

2. **Deploy Frontend to Vercel**
   - Go to: https://vercel.com/new
   - Import: rohanshinde24/QueryLens
   - Root Directory: `frontend`
   - Framework: Create React App
   - Click Deploy
   
   Your frontend will be live at: `https://querylens.vercel.app`

---

### Step 3: Test Production Deployment

1. Visit your Vercel URL
2. Click example query
3. Click "Analyze Query"
4. Should see results from Render backend!

---

## Troubleshooting

### Backend won't start on Render

**Check logs:**
- Go to your web service on Render
- Click "Logs" tab
- Look for errors

**Common issues:**
- Database connection: Make sure DATABASE_URL is set correctly
- Port: Render uses dynamic $PORT, make sure start command has `-Dserver.port=$PORT`
- Build: Check if Maven build completed successfully

### Frontend can't reach backend

**Check:**
1. Backend is deployed and healthy: `https://querylens-api.onrender.com/actuator/health`
2. CORS is configured (already done in WebConfig.java)
3. Frontend has correct REACT_APP_API_URL

### Database initialization

**To run init script:**
```bash
# Get connection string from Render PostgreSQL dashboard
psql postgresql://user:pass@host/db -f scripts/init_db.sql
```

---

## URLs After Deployment

| Service | URL | 
|---------|-----|
| Frontend | https://querylens.vercel.app |
| Backend API | https://querylens-api.onrender.com |
| Health Check | https://querylens-api.onrender.com/actuator/health |
| Analysis API | https://querylens-api.onrender.com/api/bi/analyze |

---

## Cost

**Both services are FREE:**
- Render Free Tier: Backend + PostgreSQL
- Vercel Free Tier: Frontend

**Limitations:**
- Render free tier sleeps after 15 min inactivity (first request takes ~30s to wake)
- 750 hours/month limit on Render
- Perfect for portfolio/demo purposes!

---

## Alternative: Deploy Both to Render

If you want everything on Render:

1. Deploy backend as above
2. For frontend:
   - New Static Site on Render
   - Build: `cd frontend && npm run build`
   - Publish: `frontend/build`

Both services will be on Render (simpler management).

