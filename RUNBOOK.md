# RUNBOOK — mileage tracker
Backend: Kotlin + Ktor + Exposed + SQLite

## Prerequisites
- [ ] JDK 21+: `java -version`
- [ ] Node.js 18+: `node --version`
- [ ] gh CLI authenticated: `gh auth status`
- [ ] All output files present: design.md, frontend_output.md, backend_output.md, REVIEW.md

## Before Running — Merge the Review PR
The review agent opened a `fix/review-agent` Pull Request on your GitHub repo.
Read the PR comments, review the diff, and merge it before running the app locally.

## Step 1 — Pull the latest code (after merging the review PR)
git pull origin main

## Step 2 — Start the Backend (Kotlin/Ktor)
cd backend
./gradlew run          (Mac/Linux)
gradlew.bat run        (Windows)
→ API running at http://localhost:3001
  First run downloads Gradle dependencies (~1-2 min)

## Step 3 — Start the Frontend (React/Vite)
Open a second terminal tab:
cd frontend
npm install
npm run dev
→ App running at http://localhost:5173

## Step 4 — Open in Browser
http://localhost:5173

## Resetting the Database
rm backend/data/app.db    (Mac/Linux)
del backend\data\app.db   (Windows)
Restart the backend — tables and seed data are recreated automatically.
