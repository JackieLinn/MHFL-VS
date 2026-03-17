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

| | Stack |
|---|--------|
| <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M615.6%20123.6h165.5L512%20589.7%20242.9%20123.6H63.5L512%20900.4l448.5-776.9z%22%20fill%3D%22%2341B883%22%2F%3E%3Cpath%20d%3D%22M781.1%20123.6H615.6L512%20303%20408.4%20123.6H242.9L512%20589.7z%22%20fill%3D%22%2334495E%22%2F%3E%3C%2Fsvg%3E" width="20" height="20"/><strong>Frontend</strong></span> | Vue 3, TypeScript, Element Plus, ECharts, Pinia, UnoCSS, Vite |
| <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M862.08%20699.392c-105.344%20140.330667-330.282667%2092.970667-474.538667%2099.754667%200%200-25.557333%201.450667-51.242666%205.674666%200%200%209.728-4.138667%2022.144-8.448%20101.290667-35.029333%20149.162667-42.069333%20210.730666-73.685333%20115.626667-59.221333%20230.741333-188.288%20254.165334-322.346667-44.032%20128.938667-177.92%20239.914667-299.818667%20284.970667-83.413333%2030.805333-234.325333%2060.757333-234.368%2060.757333a225.28%20225.28%200%200%201-6.101333-3.242666c-102.613333-49.92-105.6-272.213333%2080.810666-343.850667%2081.749333-31.402667%20159.872-14.165333%20248.234667-35.2%2094.208-22.4%20203.349333-93.013333%20247.68-185.344%2049.706667%20147.541333%20109.44%20378.282667%202.304%20520.96z%20m1.792-566.613333a393.045333%20393.045333%200%200%201-45.44%2080.64%20425.898667%20425.898667%200%200%200-305.792-129.322667C276.992%2084.096%2085.333333%20275.754667%2085.333333%20511.36a425.941333%20425.941333%200%200%200%20136.746667%20312.917333l9.386667%208.277334a36.522667%2036.522667%200%201%201%200.042666%200.042666l6.357334%205.632A424.96%20424.96%200%200%200%20512.64%20938.666667c225.194667%200%20410.154667-175.274667%20425.984-396.458667%2011.690667-108.330667-20.309333-245.888-74.752-409.429333%22%20fill%3D%22%232aa515%22%2F%3E%3C%2Fsvg%3E" width="20" height="20"/><strong>Java Backend</strong></span> | Spring Boot 3.5.10, Spring Security, JWT, WebSocket, MyBatis Plus, MySQL, Redis |
| <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M512%20512m-512%200a512%20512%200%201%200%201024%200%20512%20512%200%201%200-1024%200Z%22%20fill%3D%22%2305998B%22%2F%3E%3Cpath%20d%3D%22M541.090909%20124.123429l-257.662338%20465.454545H515.324675l-25.766233%20310.298597%20257.662337-465.454545H515.324675z%22%20fill%3D%22%23FFFFFF%22%2F%3E%3C%2Fsvg%3E" width="20" height="20"/><strong>Python Backend</strong></span> | FastAPI, PyTorch, Redis |
| <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201423%201024%22%3E%3Cpath%20d%3D%22M285.924091%20824.783652c-40.580953%200-77.378948-33.238872-77.378948-73.54594s33.238872-73.730847%2077.423437-73.730847c44.362522%200%2077.734861%2033.238872%2077.734861%2073.684968s-36.886973%2073.54594-77.77935%2073.641869z%20m949.952168-401.222992c-8.009417-58.868728-44.495989-106.791763-92.55249-143.589758l-18.688176-14.81763-15.084565%2018.376753c-29.322446%2033.238872-40.936866%2092.152089-37.376352%20136.158698%203.559123%2033.32785%2014.239272%2066.433255%2032.88296%2092.196579-15.084564%207.697994-33.683762%2014.81763-48.056502%2022.383547-33.817229%2011.079161-66.700189%2014.81763-99.672127%2014.81763H5.686255l-3.560513%2021.934485c-7.119636%2070.125845%203.560513%20143.588368%2033.325069%20210.023013l14.458937%2025.807813v3.559123c88.991977%20147.326837%20247.399365%20213.582136%20419.911169%20213.582136%20331.89795%200%20604.077709-143.589758%20733.120942-452.838618%2084.543074%203.69259%20169.931439-18.376753%20210.023013-99.449681l10.68293-18.376753-17.795615-11.079161c-48.056502-29.322446-113.910009-33.238872-169.086147-18.376753z%20m-475.087306-58.868728h-144.033258v143.589758h144.172286V364.602954z%20m0-180.521221h-144.033258v143.589759h144.172286V184.348668z%20m0-184.170711h-144.033258v143.589758h144.172286V0.004171z%20m176.204393%20364.691932H793.7595v143.589758h143.589758V364.602954z%20m-533.868447%200h-143.143478v143.589758h143.678736V364.602954z%20m179.763516%200H440.504032v143.589758h143.232455V364.602954z%20m-357.750252%200H82.976225v143.589758h144.033259V364.602954zM582.891195%20184.170711H440.504032v143.589759h143.232455V184.348668z%20m-180.654687%200H260.203866v143.589759H403.348734V184.348668z%22%20fill%3D%22%231488C6%22%2F%3E%3C%2Fsvg%3E" width="24" height="24"/><strong>Infrastructure</strong></span> | MySQL (3306), Redis (6379), RabbitMQ (5672/15672) |

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

### <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M862.08%20699.392c-105.344%20140.330667-330.282667%2092.970667-474.538667%2099.754667%200%200-25.557333%201.450667-51.242666%205.674666%200%200%209.728-4.138667%2022.144-8.448%20101.290667-35.029333%20149.162667-42.069333%20210.730666-73.685333%20115.626667-59.221333%20230.741333-188.288%20254.165334-322.346667-44.032%20128.938667-177.92%20239.914667-299.818667%20284.970667-83.413333%2030.805333-234.325333%2060.757333-234.368%2060.757333a225.28%20225.28%200%200%201-6.101333-3.242666c-102.613333-49.92-105.6-272.213333%2080.810666-343.850667%2081.749333-31.402667%20159.872-14.165333%20248.234667-35.2%2094.208-22.4%20203.349333-93.013333%20247.68-185.344%2049.706667%20147.541333%20109.44%20378.282667%202.304%20520.96z%20m1.792-566.613333a393.045333%20393.045333%200%200%201-45.44%2080.64%20425.898667%20425.898667%200%200%200-305.792-129.322667C276.992%2084.096%2085.333333%20275.754667%2085.333333%20511.36a425.941333%20425.941333%200%200%200%20136.746667%20312.917333l9.386667%208.277334a36.522667%2036.522667%200%201%201%200.042666%200.042666l6.357334%205.632A424.96%20424.96%200%200%200%20512.64%20938.666667c225.194667%200%20410.154667-175.274667%20425.984-396.458667%2011.690667-108.330667-20.309333-245.888-74.752-409.429333%22%20fill%3D%22%232aa515%22%2F%3E%3C%2Fsvg%3E" width="18" height="18"/> Backend (Spring Boot)</span>
- [x] Dataset & Algorithm CRUD (admin only)
- [x] Task CRUD: create, list, detail, start, stop, delete, set recommend
- [x] Round/Client APIs: list rounds, clients latest, client detail
- [x] WebSocket `/ws/task/{taskId}` for real-time training monitoring
- [x] Redis subscription: Round/Client/Status messages → MySQL + WebSocket
- [x] Dashboard: platform stats, task stats, charts, system health
- [x] Resource proxy: system resources (CPU/Memory/GPU) via FastAPI

### <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M615.6%20123.6h165.5L512%20589.7%20242.9%20123.6H63.5L512%20900.4l448.5-776.9z%22%20fill%3D%22%2341B883%22%2F%3E%3Cpath%20d%3D%22M781.1%20123.6H615.6L512%20303%20408.4%20123.6H242.9L512%20589.7z%22%20fill%3D%22%2334495E%22%2F%3E%3C%2Fsvg%3E" width="18" height="18"/> Frontend (Vue3)</span>
- [x] Home layout: Header, Sidebar, Footer, children routes
- [x] Dashboard: stat cards, charts (status pie, 7-day trend), recent tasks, system health
- [x] Task management: list, create, start, stop, delete, detail
- [x] Task detail: experiment settings, metrics cards, Round curves, Client grid (100)
- [x] WebSocket real-time updates for in-progress tasks
- [x] i18n (EN/ZH), light/dark theme
- [x] Admin: members, datasets, algorithms (full CRUD)
- [x] Recommended page: CIFAR-100 / Tiny-ImageNet switch (mock data)
- [x] Smart Assistant page: chat UI, conversation history, streaming replies (mock AI)

### <span style="display:inline-flex;align-items:center;gap:6px"><img src="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%201024%201024%22%3E%3Cpath%20d%3D%22M512%20512m-512%200a512%20512%200%201%200%201024%200%20512%20512%200%201%200-1024%200Z%22%20fill%3D%22%2305998B%22%2F%3E%3Cpath%20d%3D%22M541.090909%20124.123429l-257.662338%20465.454545H515.324675l-25.766233%20310.298597%20257.662337-465.454545H515.324675z%22%20fill%3D%22%23FFFFFF%22%2F%3E%3C%2Fsvg%3E" width="18" height="18"/> Python (FastAPI)</span>
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
