# RoadSaathi — Making Indian Highways Safer

> **One-tap hazard reporting. Real-time alerts. AI-powered triage.**

India sees **4.6 lakh road accidents** and over **5 deaths from potholes every day**. RoadSaathi is an end-to-end platform for citizens, field engineers, and NHAI administrators to report, triage, and resolve highway hazards in real time.

---

## Problem Statement

- **4.6L+ accidents/year** on Indian national highways
- **5 deaths/day** attributed to pothole-related crashes
- No unified system for citizens to report hazards offline
- Manual triage delays cost lives
- Existing apps require internet, good cameras, and multiple steps

## Solution

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Mobile (Citizen) | **Android/Kotlin + CameraX + TFLite** | One-tap offline reporting with ML classification |
| Backend | **Spring Boot 3 + PostGIS + Claude AI** | Triage, clustering, geofencing, alerts |
| Admin Panel | **React + TypeScript + Leaflet + Recharts** | Real-time dashboard for NHAI |
| Infrastructure | **Docker Compose + GitHub Actions** | CI/CD, self-hosted or cloud |

---

## Tech Stack

| Component | Stack |
|-----------|-------|
| **Mobile App** | Kotlin, Jetpack Compose, CameraX, TensorFlow Lite, Room, Hilt, WorkManager |
| **Backend API** | Spring Boot 3, Spring Security, PostGIS, Hibernate Spatial, Claude AI |
| **Admin Dashboard** | React 18, TypeScript, Vite, Tailwind CSS, Leaflet, Recharts, TanStack Query |
| **Database** | PostgreSQL 15 + PostGIS 3.4 |
| **Storage** | AWS S3 (photo uploads) |
| **CI/CD** | GitHub Actions |
| **Deployment** | Docker Compose, Docker, Nginx |

---

## Quick Start

### Prerequisites

- JDK 21+
- Android Studio Hedgehog (2023.1.1+)
- Docker Desktop
- Node.js 22+
- npm 10+

### 1. Clone & Start Infrastructure

```bash
git clone https://github.com/your-org/roadsaathi.git
cd roadsaathi
cp .env.example .env   # edit secrets
docker compose up -d
```

### 2. Run Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Run Admin Dashboard

```bash
cd admin
npm install
npm run dev
```

Open http://localhost:5173

### 4. Build Android App

```bash
cd android
./gradlew assembleDebug
```

APK at `android/app/build/outputs/apk/debug/app-debug.apk`

---

## Project Structure

```
roadsaathi/
├── android/                  # Android app (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/in/roadsaathi/
│   │   │   │   ├── camera/       # CameraX + ML Kit
│   │   │   │   ├── ml/           # TFLite classifier
│   │   │   │   ├── data/         # Room DB + repository
│   │   │   │   ├── sync/         # WorkManager sync
│   │   │   │   ├── location/     # Geocoding + NH detection
│   │   │   │   └── ui/           # Jetpack Compose screens
│   │   │   └── res/
│   │   └── build.gradle.kts
│   └── gradle/
├── backend/                  # Spring Boot API
│   ├── src/main/java/in/roadsaathi/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   └── config/
│   └── pom.xml
├── admin/                    # React admin dashboard
│   ├── src/
│   │   ├── components/       # UI components + Layout
│   │   ├── pages/            # Dashboard, Map, Triage, etc.
│   │   ├── hooks/            # Auth context
│   │   └── lib/              # API client + types
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
├── .env.example
└── .github/workflows/        # CI/CD pipelines
```

---

## Key Features

### Offline-First Reporting
Citizens capture hazards with zero network — photos, GPS, ML classification all work offline. Reports sync via WorkManager when connectivity is restored.

### ML Classification (TFLite)
On-device CNN classifies hazard types (pothole, road collapse, waterlogging, etc.) with 87% accuracy. No cloud dependency for the core ML pipeline.

### Real-Time Alerts
Geo-fenced alerts notify NHAI engineers when a high-severity hazard is reported within their jurisdiction.

### Heatmap & Clustering
PostGIS `ST_ClusterDBSCAN` groups nearby reports into clusters. The admin panel visualizes these as an interactive heatmap.

### Auto-Expiry
Reports auto-expire after configurable TTL (default: 7 days). Resolved reports are archived.

### Full Admin Panel
- **Live Map** with Leaflet + heatmap overlay
- **Triage Queue** — sortable, filterable table with severity bars
- **Status Board** — Kanban-style workflow management
- **Analytics** — time-series charts, hazard distribution, blackspot ranking

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Admin login |
| POST | `/api/auth/register` | Admin registration |
| GET | `/api/hazards` | List reports (paginated, filterable) |
| GET | `/api/hazards/:id` | Single report detail |
| POST | `/api/hazards` | Submit new report |
| PATCH | `/api/hazards/:id/status` | Update report status |
| PATCH | `/api/hazards/:id/assign` | Assign engineer |
| GET | `/api/hazards/heatmap` | Heatmap cluster data |
| GET | `/api/triage` | Triage queue |
| GET | `/api/analytics` | Analytics data |
| GET | `/api/dashboard/summary` | Dashboard summary cards |

---

## PostGIS Spatial Queries

RoadSaathi leverages PostGIS for all spatial operations:

```sql
-- Find reports within 50km of a point
SELECT * FROM hazard_reports
WHERE ST_DWithin(
  location,
  ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
  0.5  -- ~50km at equator
);

-- Cluster nearby reports (DBSCAN)
SELECT ST_ClusterDBSCAN(location, 0.01, 3) OVER() AS cluster_id,
       COUNT(*) AS report_count,
       ST_Centroid(ST_Collect(location)) AS centroid
FROM hazard_reports
WHERE status != 'resolved'
GROUP BY cluster_id;

-- Find the NH corridor nearest to a point
SELECT nh_name FROM nh_corridors
ORDER BY location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
LIMIT 1;
```

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Citizen (Android)                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │ CameraX  │  │  TFLite  │  │   Room DB (Offline)│  │
│  │  Capture │→ │ Classify │→ │   + WorkManager   │  │
│  └──────────┘  └──────────┘  └────────┬──────────┘  │
│                                       │              │
│                              HTTP (when online)      │
└──────────────────────────────┬──────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────┐
│                   Backend (Spring Boot)              │
│  ┌────────────┐  ┌───────────┐  ┌───────────────┐  │
│  │ Controller │→ │  Service  │→ │  Repository   │  │
│  └────────────┘  └─────┬─────┘  └───────┬───────┘  │
│                        │                │           │
│                 ┌──────▼──────┐  ┌──────▼───────┐  │
│                 │ Claude AI   │  │  PostGIS     │  │
│                 │ (triage)    │  │  (spatial)   │  │
│                 └─────────────┘  └──────────────┘  │
└──────────────────────────────┬──────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────┐
│          Admin Dashboard (React + Leaflet)           │
│  ┌─────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐  │
│  │Live Map │ │Triage  │ │Kanban    │ │Analytics │  │
│  │+ Heatmap│ │  Queue │ │  Board   │ │  Charts  │  │
│  └─────────┘ └────────┘ └──────────┘ └──────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## Offline-First Architecture

The Android app follows an offline-first pattern:

1. **Capture**: User taps "Report" — app captures photo via CameraX + GPS
2. **Classify**: TFLite model runs inference locally → hazard type + confidence
3. **Store**: Report saved to Room DB with `isSynced = false`
4. **Sync**: WorkManager periodic task uploads pending reports when online:
   - Checks network availability
   - Uploads photos to S3 presigned URL
   - Sends JSON payload to POST `/api/hazards`
   - Marks as synced on success
5. **Retry**: Exponential backoff on failure (up to 3 retries)

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes | — | 256-bit key for JWT signing |
| `CLAUDE_API_KEY` | No | — | Anthropic Claude API key for AI triage |
| `AWS_ACCESS_KEY_ID` | No | — | AWS credentials for S3 uploads |
| `AWS_SECRET_ACCESS_KEY` | No | — | AWS credentials for S3 uploads |
| `AWS_REGION` | No | `ap-south-1` | AWS region |
| `S3_BUCKET` | No | `roadsaathi-uploads` | S3 bucket name |

---

## Deployment Guide

### Production Deployment

1. Set up an EC2 instance (t3.medium or larger)
2. Install Docker + Docker Compose
3. Clone the repo and configure `.env`
4. Run `docker compose up -d`

### Domain Setup

- `admin.roadsaathi.in` → admin dashboard (port 80)
- `api.roadsaathi.in` → backend API (port 8080)

Configure CNAME records and Nginx reverse proxy as needed.

### CI/CD

GitHub Actions workflows:
- **android-ci.yml** — Builds and lints Android app on PR/push to main
- **backend-ci.yml** — Runs Spring Boot tests with PostGIS service container
- **deploy.yml** — Triggered on `v*` tags; builds, pushes Docker images, and deploys via SSH to EC2

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

*Built with ❤️ for safer Indian highways.*
