# 📝 Deployment Checklist - Follow These Exact Steps

---

## 🎯 **Before You Start:**

✅ Make sure code is pushed to GitHub:
```bash
cd /Users/rohanshinde/Documents/projects/QueryLens
git status  # Should say "nothing to commit, working tree clean"
```

If not clean, run:
```bash
git add .
git commit -m "Prepare for deployment"
git push origin main
```

---

## Part A: Deploy Backend (Render)

### **Step 1: Go to Render**
- Open browser: https://dashboard.render.com
- Click "Get Started" or "Sign In"
- Choose "Sign in with GitHub"

### **Step 2: Create Web Service**
- Click "New +" button (top right corner)
- Select "Web Service"
- You'll see your GitHub repos
- Find "QueryLens" and click "Connect"

### **Step 3: Fill in the Form**

**Copy these values EXACTLY:**

```
Name: querylens-api

Region: Oregon (US West)

Branch: main

Root Directory: (leave empty - just delete any text)

Environment: Java

Build Command: 
./mvnw clean package -DskipTests

Start Command:
java -Dserver.port=$PORT -jar target/querylens-0.0.1-SNAPSHOT.jar
```

### **Step 4: Add Environment Variables**

Scroll down to "Environment Variables" section.

Click "+ Add Environment Variable" for each:

```
Key: SPRING_DATASOURCE_URL
Value: jdbc:h2:mem:testdb

Key: SPRING_DATASOURCE_DRIVER_CLASS_NAME  
Value: org.h2.Driver

Key: SPRING_DATASOURCE_USERNAME
Value: sa

Key: SPRING_DATASOURCE_PASSWORD
Value: (leave empty)

Key: SPRING_JPA_HIBERNATE_DDL_AUTO
Value: none
```

### **Step 5: Select Free Plan**
- Scroll to "Instance Type"
- Select "Free"

### **Step 6: Deploy**
- Click "Create Web Service" button at bottom
- Wait 5-10 minutes (grab coffee!)

### **Step 7: Verify Backend Works**

When build completes, you'll see a URL like:
`https://querylens-api-xxxx.onrender.com`

**Test it:**
1. Click the URL
2. Add `/actuator/health` to end
3. Should see: `{"status":"UP"}`

**If you see that ✅ → Backend is LIVE!**

**⚠️ IMPORTANT:** Copy your full backend URL (you need it for frontend!)

Example: `https://querylens-api-abcd.onrender.com`

---

## Part B: Deploy Frontend (Vercel)

### **Step 1: Update Frontend with Backend URL**

**In your terminal:**

```bash
cd /Users/rohanshinde/Documents/projects/QueryLens

# Replace XXX with your actual Render URL!
echo "REACT_APP_API_URL=https://querylens-api-XXX.onrender.com" > frontend/.env.production

# Commit
git add frontend/.env.production
git commit -m "Add production API URL"
git push origin main
```

### **Step 2: Go to Vercel**
- Open browser: https://vercel.com/new
- Sign in with GitHub if prompted

### **Step 3: Import Project**
- You'll see "Import Git Repository"
- Find "QueryLens" in the list
- Click "Import"

### **Step 4: Configure Project**

**IMPORTANT - Click "Edit" next to Root Directory!**

```
Framework Preset: Create React App (auto-detected)

Root Directory: frontend  ← TYPE THIS!

Build Command: npm run build (auto-filled)

Output Directory: build (auto-filled)

Install Command: npm install (auto-filled)
```

### **Step 5: Deploy**
- Click "Deploy" button
- Wait 2-3 minutes

### **Step 6: Success!**

When done, Vercel shows:
```
🎉 Congratulations!
Visit: https://querylens-xxx.vercel.app
```

**Frontend is LIVE!** ✅

---

## Part C: Test Everything

### **Test 1: Open Your Frontend**
- Go to your Vercel URL
- Should see QueryLens UI with purple gradient header

### **Test 2: Run Example Query**
1. Click "YEAR() Function (Non-SARGABLE)" button
2. Click "Analyze Query"
3. **Wait ~30 seconds first time** (Render waking up)
4. Should see bottleneck analysis appear!

### **Test 3: Verify All Features**
- [ ] Example buttons work
- [ ] Can paste custom SQL
- [ ] Analysis returns results
- [ ] Bottleneck cards show up
- [ ] Fixes are displayed
- [ ] No errors in browser console (F12)

**If all working ✅ → DEPLOYMENT SUCCESSFUL!**

---

## 🎊 **You're Done!**

**Your live URLs:**
- Frontend: `https://querylens-xxx.vercel.app`
- Backend: `https://querylens-api-xxx.onrender.com`

**Add to your resume:**
```
QueryLens (Live Demo) | Java, Spring Boot, React, PostgreSQL, Docker, TDD, CI/CD

• Full-stack SQL query analyzer: https://querylens-xxx.vercel.app
• Detects performance bottlenecks in complex BI queries
• Automated optimization recommendations with 70-95% improvements
```

---

## ⚠️ **Known Limitations (Free Tier):**

1. **Render Free Tier:**
   - Sleeps after 15 min inactivity
   - First request takes ~30 seconds to wake up
   - After that, responds in <2 seconds
   
2. **Solution:** 
   - For portfolio: Free tier is fine (shows it works!)
   - For production use: Upgrade to $7/month (always-on)

---

## 📞 **If Something Goes Wrong:**

### **Backend not starting:**
- Check Render logs (Dashboard → querylens-api → Logs)
- Verify environment variables are set
- Verify build command completed successfully

### **Frontend can't reach backend:**
- Check browser console (F12 → Console tab)
- Verify REACT_APP_API_URL is correct
- Test backend directly: `curl https://your-backend-url.onrender.com/actuator/health`

### **Build fails:**
- Check GitHub repo has all files
- Verify pom.xml and package.json are present
- Check build logs for specific errors

---

## 🚀 **Next Steps After Deployment:**

1. Test with your real USC queries
2. Share URL with team for feedback
3. Add to LinkedIn/portfolio
4. Screenshot the UI for presentations
5. Update resume with live link

---

**Ready to deploy?** Start with Part A (Backend on Render)! 🚀

