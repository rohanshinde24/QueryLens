# QueryLens Deployment Guide

## Overview

QueryLens is a full-stack application requiring separate deployment for frontend and backend.

---

## Frontend Deployment (Vercel)

### Prerequisites
- Vercel account (free tier works)
- GitHub repository pushed

### Option 1: Vercel CLI

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy from project root
cd /Users/rohanshinde/Documents/projects/QueryLens
vercel

# Follow prompts:
# - Link to existing project or create new
# - Set root directory: ./
# - Framework: Create React App
# - Build command: cd frontend && npm install && npm run build
# - Output directory: frontend/build
```

### Option 2: Vercel Dashboard (Recommended)

1. Go to https://vercel.com
2. Click "Add New Project"
3. Import your GitHub repository (rohanshinde24/QueryLens)
4. Configure:
   - **Framework Preset:** Create React App
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `build`
   - **Install Command:** `npm install`

5. Environment Variables:
   - Add: `REACT_APP_API_URL` = your backend URL (add after backend deployed)

6. Click "Deploy"

---

## Backend Deployment Options

Since Vercel doesn't support Java/Spring Boot, choose one of these:

### Option A: Railway (Recommended - Easy)

1. Go to https://railway.app
2. Sign in with GitHub
3. Click "New Project" → "Deploy from GitHub repo"
4. Select QueryLens repository
5. Railway auto-detects Spring Boot
6. Add environment variables:
   ```
   SPRING_DATASOURCE_URL=<your-postgres-url>
   SPRING_DATASOURCE_USERNAME=<username>
   SPRING_DATASOURCE_PASSWORD=<password>
   ```
7. Railway provides PostgreSQL addon (click "New" → "Database" → "PostgreSQL")
8. Deploy!

**Cost:** Free tier available

### Option B: Render

1. Go to https://render.com
2. New → Web Service
3. Connect GitHub repo
4. Configure:
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/*.jar`
   - **Environment:** Java
5. Add PostgreSQL database (Render provides this)
6. Deploy

**Cost:** Free tier available

### Option C: Heroku

```bash
# Install Heroku CLI
brew tap heroku/brew && brew install heroku

# Login
heroku login

# Create app
cd /Users/rohanshinde/Documents/projects/QueryLens
heroku create querylens-api

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini

# Deploy
git push heroku main

# Open
heroku open
```

**Cost:** Paid ($7/month minimum)

---

## Full Deployment Steps (Complete Setup)

### Step 1: Deploy Backend

Choose Railway (easiest):

```bash
1. Go to https://railway.app
2. New Project → Deploy from GitHub
3. Select QueryLens
4. Add PostgreSQL database
5. Copy the public URL (e.g., querylens-api.railway.app)
```

### Step 2: Update Frontend Environment

```bash
# Update frontend/.env.production
REACT_APP_API_URL=https://querylens-api.railway.app
```

### Step 3: Deploy Frontend to Vercel

```bash
# Commit the env change
git add frontend/.env.production
git commit -m "Update API URL for production"
git push origin main

# Deploy to Vercel
vercel --prod
```

Or use Vercel dashboard and set environment variable there.

### Step 4: Test

Visit your Vercel URL and test with example queries!

---

## Environment Variables Summary

### Frontend (Vercel)
```
REACT_APP_API_URL=https://your-backend-url.railway.app
```

### Backend (Railway/Render/Heroku)
```
SPRING_DATASOURCE_URL=jdbc:postgresql://hostname:5432/dbname
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
SERVER_PORT=8080
```

---

## Quick Deploy (If you want both deployed now)

I can help you:
1. Set up Railway for backend (free tier)
2. Set up Vercel for frontend (free tier)
3. Connect them together
4. Test the live deployment

Just let me know if you want to proceed with full deployment!

---

## Cost Summary

**Free Tier Option:**
- Vercel (Frontend): Free
- Railway (Backend + DB): Free tier (500 hrs/month)
- Total: $0/month

**Note:** For production use with your team, you may want paid tiers for better performance and uptime.

