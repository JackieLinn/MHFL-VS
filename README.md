<h1 align="center">MHFL-VS</h1>
<p align="center"><i>Model Heterogeneous Federated Learning End-to-End Visualization and Simulation</i></p>

<p align="center">
  Real-time visualization and simulation platform for federated learning training.
</p>

<p align="center">
  <a href="README_zh.md">中文</a> · English
</p>

---

### 🛠 Tech Stack

| | Stack |
|---|--------|
| 🖥 **Frontend** | Vue 3, TypeScript, Element Plus, ECharts, Pinia, UnoCSS, Vite |
| ☕ **Java** | Spring Boot 3.5, Spring Security, JWT, WebSocket, MyBatis Plus, MySQL, Redis |
| 🐍 **Python** | FastAPI, PyTorch, Redis |
| 🐳 **Infra** | MySQL, Redis, RabbitMQ |

---

### Implemented

- [x] **Auth & users** — Register, login, password reset (email), JWT, captcha; profile & avatar; admin: create/delete users, list with search & pagination
- [x] **Frontend** — Home layout, real-time CPU/Memory/GPU monitor, i18n (EN/ZH), light/dark theme, user profile drawer, Admin (members only; dataset/algorithm placeholders)
- [x] **Python** — Health check, resource APIs, unified response format

### To Do

- [ ] **Backend** — WebSocket subscription, Redis messaging, task CRUD & start/stop, monitor & analytics APIs
- [ ] **Frontend** — Task management, real-time monitor (WebSocket + ECharts), analytics & export
- [ ] **Python** — Training task API, Redis callbacks, GPU resource management

---

### 🚀 Run locally

1. Start MySQL, Redis, RabbitMQ (e.g. Docker Compose)
2. Start Spring Boot (port 8088)
3. Start FastAPI (port 8000)
4. Start Vue dev server (Vite)
