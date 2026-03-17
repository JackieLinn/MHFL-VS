<h1 align="center">MHFL-VS</h1>

<p align="center"><i>Model Heterogeneous Federated Learning — End-to-End Visualization and Simulation</i></p>

<p align="center">
  Real-time visualization and simulation platform for federated learning training.
</p>

<p align="center">
  <a href="README_zh.md">中文</a> · English
</p>

---

## 🛠 Tech Stack

| | Stack                                                       |
|---|-------------------------------------------------------------|
| 🖥️ **Frontend** | Vue, Vite, TypeScript, Element-Plus, ECharts, Pinia, UnoCSS |
| ⚙️ **Java Backend** | Spring Boot, Spring Security, MyBatis Plus, WebSocket |
| 🐍 **Python Backend** | FastAPI, PyTorch |
| 🐳 **Infrastructure** | MySQL (3306), Redis (6379), RabbitMQ (5672/15672) |

---

## 📁 Project Structure

```
MHFL-VS/
├── mhfl-ui/          # Vue3 frontend
├── mhfl-server/       # Spring Boot backend (port 8088), compose.yaml
└── mhfl-algo/        # FastAPI + PyTorch training (port 8000)
```

---

## ✅ Implemented

### 🔐 Auth & Users
- [x] Register, login, password reset (email verification)
- [x] JWT auth, captcha, rate limiting
- [x] Logout: token blacklist in Redis
- [x] Profile & avatar upload (local storage)
- [x] Admin: create/delete users, list with keyword search & pagination

### ⚙️ Backend (Spring Boot)
- [x] Dataset & Algorithm CRUD (admin only)
- [x] Task CRUD: create, list, detail, start, stop, delete, set recommend
- [x] Round/Client APIs: list rounds, clients latest, client detail
- [x] WebSocket `/ws/task/{taskId}` for real-time training monitoring
- [x] Redis subscription: Round/Client/Status messages → MySQL + WebSocket
- [x] Dashboard: platform stats, task stats, charts, system health
- [x] Resource proxy: system resources (CPU/Memory/GPU) via FastAPI

### 🖥️ Frontend (Vue3)
- [x] Home layout: Header, Sidebar, Footer, children routes
- [x] Dashboard: stat cards, charts (status pie, 7-day trend), recent tasks, system health
- [x] Task management: list, create, start, stop, delete, detail
- [x] Task detail: experiment settings, metrics cards, Round curves, Client grid (100)
- [x] WebSocket real-time updates for in-progress tasks
- [x] i18n (EN/ZH), light/dark theme
- [x] Admin: members, datasets, algorithms (full CRUD)
- [x] Recommended page: CIFAR-100 / Tiny-ImageNet switch (mock data)
- [x] Smart Assistant page: chat UI, conversation history, streaming replies (mock AI)

### 🐍 Python (FastAPI)
- [x] Health check, resource APIs (GPU, CPU, memory)
- [x] Training API: start, stop (with multiprocessing)
- [x] Redis publish: Round, Client, Status callbacks
- [x] GPU check before training (CIFAR-100: 3GB, Tiny-ImageNet: 5.5GB)
- [x] Algorithms: Standalone, FedProto, FedAvg, FedSSA, LG-FedAvg

---

## 📋 To Do

- [ ] **Backend** — Recommended task API; analytics endpoints; Smart Assistant API (LLM proxy)
- [ ] **Frontend** — Recommended page: real API; analytics & export; Smart Assistant: connect to backend API

---

## 🚀 Run Locally

1. Start MySQL, Redis, RabbitMQ (Spring Boot uses `mhfl-server/compose.yaml` via `spring-boot-docker-compose`, or run manually)
2. Start Spring Boot (port 8088)
3. Start FastAPI (port 8000)
4. Start Vue dev server (`pnpm dev` / `npm run dev`)

```bash
# 1. Start infra (from mhfl-server directory)
cd mhfl-server && docker compose up -d

# 2. Start backend
cd mhfl-server && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Start Python
cd mhfl-algo && python -m uvicorn main:app --reload --port 8000

# 4. Start frontend
cd mhfl-ui && pnpm install && pnpm dev
```

---

## 📦 Environment

- **Node**: ^20.19.0 or >=22.12.0
- **Java**: 17
- **Python**: 3.x (recommended 3.10+)
