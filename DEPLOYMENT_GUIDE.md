# 🚀 QueryLens - Complete Deployment Guide

**Your app is stateless (no database needed for core functionality)**  
This makes deployment very simple and FREE!

---

## 📋 **Prerequisites**

Before starting, make sure you have:
- ✅ GitHub account
- ✅ Code pushed to GitHub (rohanshinde24/QueryLens)
- ✅ Vercel account (free) - Sign up at https://vercel.com
- ✅ Render account (free) - Sign up at https://render.com

**Time needed:** 15-20 minutes total

---

## Part 1: Deploy Backend to Render (10 minutes)

### **Step 1: Sign Up / Log In to Render**

1. Go to: **https://dashboard.render.com**
2. Click **"Get Started"** or **"Sign In"**
3. Choose **"Sign in with GitHub"**
4. Authorize Render to access your GitHub repos

---

### **Step 2: Create New Web Service**

1. In Render Dashboard, click the **"New +"** button (top right)
2. Select **"Web Service"**
3. You'll see a list of your GitHub repos
4. Find and click **"Connect"** next to **QueryLens**

*(If you don't see it, click "Configure account" and grant access to the repo)*

---

### **Step 3: Configure the Service**

Fill in these fields **exactly**:

| Field | Value |
|-------|-------|
| **Name** | `querylens-api` |
| **Region** | Oregon (US West) - or closest to you |
| **Branch** | `main` |
| **Root Directory** | *(leave empty)* |
| **Environment** | `Java` |
| **Build Command** | `./mvnw clean package -DskipTests` |
| **Start Command** | `java -Dserver.port=$PORT -jar target/querylens-0.0.1-SNAPSHOT.jar` |

---

### **Step 4: Configure Environment Variables**

Scroll down to **"Environment Variables"** section.

Click **"Add Environment Variable"** and add these:

| Key | Value |
|-----|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:testdb` |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.h2.Driver` |
| `SPRING_DATASOURCE_USERNAME` | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | *(leave empty)* |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `none` |

*(We're using H2 in-memory database for now since the app is stateless)*

---

### **Step 5: Select Plan**

1. Scroll down to **"Instance Type"**
2. Select **"Free"**
3. Click **"Create Web Service"**

---

### **Step 6: Wait for Build**

You'll see a log stream showing:
```
==> Downloading dependencies
==> Building
==> Deploying
```

**Wait 5-10 minutes** for the first build.

When you see:
```
✅ Live
Your service is live at https://querylens-api.onrender.com
```

**Backend is deployed!** ✅

---

### **Step 7: Test Backend**

1. Click the URL shown: `https://querylens-api.onrender.com` (or similar)
2. Add `/actuator/health` to the URL
3. Should see: `{"status":"UP"}`

**If you see that, backend is working!** ✅

Copy your backend URL (e.g., `https://querylens-api-xxxx.onrender.com`)

---

## Part 2: Deploy Frontend to Vercel (5 minutes)

### **Step 1: Update API URL for Production**

Before deploying frontend, we need to tell it where the backend is.

**In your terminal:**

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens

# Replace with YOUR actual Render URL
echo "REACT_APP_API_URL=https://querylens-api-xxxx.onrender.com" > frontend/.env.production

# Commit this change
git add frontend/.env.production
git commit -m "Configure production API URL"
git push origin main
```

*(Replace `querylens-api-xxxx.onrender.com` with your actual Render URL from Step 7)*

---

### **Step 2: Sign Up / Log In to Vercel**

1. Go to: **https://vercel.com**
2. Click **"Sign Up"** or **"Log In"**
3. Choose **"Continue with GitHub"**
4. Authorize Vercel to access your GitHub repos

---

### **Step 3: Import Your Project**

1. From Vercel Dashboard, click **"Add New..."** → **"Project"**
2. You'll see a list of your GitHub repos
3. Find **QueryLens** and click **"Import"**

---

### **Step 4: Configure Project Settings**

| Field | Value |
|-------|-------|
| **Framework Preset** | `Create React App` (should auto-detect) |
| **Root Directory** | `frontend` ← **IMPORTANT!** Click "Edit" and type this |
| **Build Command** | `npm run build` (should auto-fill) |
| **Output Directory** | `build` (should auto-fill) |
| **Install Command** | `npm install` (should auto-fill) |

---

### **Step 5: Add Environment Variable (Optional)**

If you didn't commit the .env.production file:

1. Expand **"Environment Variables"**
2. Add:
   - **Name:** `REACT_APP_API_URL`
   - **Value:** `https://querylens-api-xxxx.onrender.com` (your Render URL)

---

### **Step 6: Deploy**

1. Click **"Deploy"** button
2. Wait 2-3 minutes

You'll see:
```
Building...
✓ Deployed
```

When done, Vercel shows:
```
🎉 Your project is live!
Visit: https://querylens-xxx.vercel.app
```

**Frontend is deployed!** ✅

---

## Part 3: Test the Live Application

### **Step 1: Open Your Vercel URL**

Click the URL Vercel gave you (e.g., `https://querylens-xxx.vercel.app`)

You should see your QueryLens UI!

---

### **Step 2: Test with Example Query**

1. Click **"YEAR() Function (Non-SARGABLE)"** example button
2. Click **"Analyze Query"**
3. Wait 2-3 seconds...

**Important:** First request to Render takes ~30 seconds (cold start on free tier).  
After that, it's fast!

---

### **Step 3: Verify Results**

You should see:
- ✅ Bottleneck cards appear
- ✅ Shows severity (🔴 Critical)
- ✅ Shows line numbers
- ✅ Shows cost percentages
- ✅ Shows fixes

**If you see results, deployment is successful!** 🎉

---

## 🎯 **Your Live URLs:**

After deployment, you'll have:

| Service | URL | Example |
|---------|-----|---------|
| **Frontend** | Your Vercel URL | `https://querylens-xxx.vercel.app` |
| **Backend** | Your Render URL | `https://querylens-api-xxx.onrender.com` |
| **Health Check** | Backend + `/actuator/health` | Test this first! |

---

## 🐛 **Troubleshooting**

### **Issue: Frontend shows "Failed to analyze query"**

**Check:**
1. Is backend URL correct in frontend/.env.production?
2. Is backend health endpoint working? Visit: `https://your-render-url.onrender.com/actuator/health`
3. Open browser DevTools (F12) → Console tab → Look for CORS or network errors

**Fix:**
- Make sure `REACT_APP_API_URL` doesn't have trailing slash
- Make sure backend CORS is configured (already done in WebConfig.java)

---

### **Issue: Backend shows "Application failed to start"**

**Check Render logs:**
1. Go to Render dashboard
2. Click on your querylens-api service
3. Click "Logs" tab
4. Look for red error messages

**Common issues:**
- Port configuration: Make sure start command has `-Dserver.port=$PORT`
- H2 database: Make sure environment variables are set correctly

---

### **Issue: Render backend takes 30+ seconds to respond**

**This is normal!** Free tier sleeps after 15 minutes of inactivity.

**First request:** ~30 seconds (waking up)  
**Subsequent requests:** <2 seconds

**To fix:** Upgrade to paid tier ($7/month) for always-on service.

---

## 💰 **Cost Breakdown**

### **Current Setup:**

| Service | Plan | Cost |
|---------|------|------|
| Vercel (Frontend) | Free | $0/month |
| Render (Backend) | Free | $0/month |
| **Total** | | **$0/month** ✅ |

### **Free Tier Limitations:**

**Vercel:**
- 100 GB bandwidth/month
- Unlimited sites
- Perfect for portfolio!

**Render:**
- 750 hours/month (plenty!)
- Sleeps after 15 min inactivity
- First request takes ~30s to wake

---

## 🎯 **Summary of Deployment Steps:**

### **Backend (Render):**
1. Go to dashboard.render.com
2. New Web Service → Connect GitHub → QueryLens
3. Configure: Java, build command, start command
4. Add environment variables (H2 in-memory DB)
5. Deploy → Wait 5-10 min
6. Test: `/actuator/health`

### **Frontend (Vercel):**
1. Update REACT_APP_API_URL in frontend/.env.production
2. Commit and push to GitHub
3. Go to vercel.com/new
4. Import QueryLens from GitHub
5. Root Directory: `frontend`
6. Deploy → Wait 2-3 min
7. Test: Click example query

---

## ✅ **Checklist:**

Before deploying:
- [ ] Code pushed to GitHub
- [ ] Have Render account
- [ ] Have Vercel account

Backend (Render):
- [ ] Web service created
- [ ] Environment variables set
- [ ] Build successful
- [ ] Health endpoint returns {"status":"UP"}
- [ ] Copy backend URL

Frontend (Vercel):
- [ ] Update .env.production with backend URL
- [ ] Push to GitHub
- [ ] Import project on Vercel
- [ ] Set root directory to "frontend"
- [ ] Deployment successful
- [ ] Can access UI
- [ ] Example query works

---

## 🎊 **After Deployment:**

You'll have:
- ✅ Live demo at your Vercel URL
- ✅ Can share with anyone
- ✅ Perfect for resume/portfolio
- ✅ Works on mobile
- ✅ Always available
- ✅ Costs $0

Add to your resume:
> **Live Demo:** https://your-vercel-url.vercel.app

---

## 🔮 **Future: Add Database (Phase 2)**

When you want to add history/tracking features:

1. **On Render:** Add PostgreSQL database
2. **Update application.properties** with real database URL
3. **Create JPA entities** for the schema tables
4. **Implement save/retrieve** in BiAnalysisService
5. **Add "History" UI** in React
6. **Redeploy**

But for now, **you don't need this!** The app works perfectly without it.

---

**Ready to deploy?** Follow the steps above! 🚀

