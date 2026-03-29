<p align="center" style="margin-bottom: 4px;">
  <img src="mhfl-ui/public/logo.svg" width="74" alt="MHFL-VS Logo" />
</p>

<h1 align="center" style="margin: 0;">MHFL-VS</h1>

<p align="center" style="margin-top: 6px;"><i>Model Heterogeneous Federated Learning Visualization & Simulation Platform</i></p>

<p align="center">
  <a href="README_zh.md">中文</a> · English
</p>

---

## 🚀 Overview

MHFL-VS is an end-to-end federated learning platform for visual training simulation.
It provides unified task orchestration, real-time training monitoring, system resource dashboards, and assistant capabilities.

## ✨ Core Features

- 🔐 Authentication and authorization: register/login/reset password, JWT, captcha, rate limiting, token blacklist, role-based access.
- 🧭 Task lifecycle: create/list/detail/start/stop/delete tasks, recommended-task controls, and permission isolation.
- 🔄 Real-time training pipeline: Python training publishes Round/Client/Status to Redis; Spring Boot persists to MySQL and pushes WebSocket updates.
- 📈 Training visualization: task metrics cards, round curves, client grid, per-client detail history.
- 🛠️ Management console: user/dataset/algorithm management with search, pagination, and audit-friendly operations.
- 🖥️ Dashboard: platform overview, task status/trend, recent tasks, health checks, and real-time CPU/Memory/GPU monitoring.
- 🎨 UI adaptation: light/dark theme support and bilingual localization (EN/ZH).
- 🤖 Assistant: a RAG-based workflow agent with conversation/session APIs and streaming chat endpoints.

## 🧩 Tech Stack

| Layer | Stack |
|---|---|
| Frontend | ![Vue](https://img.shields.io/badge/Vue-4FC08D?logo=vuedotjs&logoColor=white) ![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white) ![Element Plus](https://img.shields.io/badge/Element_Plus-409EFF?logo=element&logoColor=white) ![ECharts](https://img.shields.io/badge/ECharts-AA344D?logo=apacheecharts&logoColor=white) ![Pinia](https://img.shields.io/badge/Pinia-FFD859?logo=pinia&logoColor=black) ![UnoCSS](https://img.shields.io/badge/UnoCSS-333333?logo=unocss&logoColor=white) ![Vue I18n](https://img.shields.io/badge/Vue_I18n-41B883?logo=vuedotjs&logoColor=white) |
| Java Backend | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white) ![Spring WebSocket](https://img.shields.io/badge/Spring_WebSocket-6DB33F?logo=spring&logoColor=white) ![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-2E8B57?logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white) |
| Python Backend | ![FastAPI](https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white) ![Uvicorn](https://img.shields.io/badge/Uvicorn-499848?logo=uvicorn&logoColor=white) ![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?logo=pytorch&logoColor=white) ![LangChain](https://img.shields.io/badge/LangChain-1C3C3C?logo=chainlink&logoColor=white) ![Pydantic](https://img.shields.io/badge/Pydantic-E92063?logo=pydantic&logoColor=white) ![OpenAI](https://img.shields.io/badge/OpenAI-412991?logo=openai&logoColor=white) ![Pytest](https://img.shields.io/badge/Pytest-0A9EDC?logo=pytest&logoColor=white) |
| Infrastructure | ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white) |

## 🗂️ Project Structure

```text
MHFL-VS/
├── mhfl-algo/                # FastAPI + training service (8000)
│   ├── assistant/            # RAG, retrieval, rerank, KB scripts
│   ├── fl_core/              # federated learning algorithms and trainer
│   ├── routers/              # health/resource/train/assistant APIs
│   ├── services/             # training and assistant orchestration
│   ├── utils/                # redis publisher, schemas, resource checker
│   └── requirements.txt
├── mhfl-server/              # Spring Boot backend (8088)
│   ├── src/main/java/        # controller, service, mapper, security
│   ├── src/main/resources/   # application-*.yaml, mapper xml
│   ├── compose.yaml
│   └── pom.xml
├── mhfl-ui/                  # Vue3 frontend
│   ├── public/               # static assets (including logo.svg)
│   ├── src/                  # pages, components, stores, api
│   └── package.json
├── README.md
└── README_zh.md
```

## 📦 Requirements

- Node.js: `22.20.0`
- Java: `25`
- Python: `3.11`
- Docker + Docker Compose (for MySQL/Redis/RabbitMQ)

## ⚡ Quick Start

### 0. Clone project

```bash
git clone https://github.com/JackieLinn/MHFL-VS.git
cd MHFL-VS
```

### 1. Start infrastructure

```bash
cd mhfl-server
docker compose up -d
```

### 2. Start Python backend (8000)

```bash
cd mhfl-algo
pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```

### 3. Start Spring Boot backend (8088)

```bash
cd mhfl-server
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Start frontend (Vite)

```bash
cd mhfl-ui
pnpm install
pnpm dev
```

## 🧪 Test Commands

### Frontend

```bash
cd mhfl-ui
pnpm test:coverage
```

### Java Backend

```bash
cd mhfl-server
mvn clean test
```

### Python Backend

```bash
cd mhfl-algo
python -m pytest -vv -s
```

## 🌐 Service Endpoints

- Frontend: `http://localhost:5173`
- Spring Boot API Docs (Swagger UI): `http://localhost:8088/doc/swagger-ui/index.html`
- FastAPI API Docs: `http://localhost:8000/api/docs`
- Task WebSocket: `ws://localhost:8088/ws/task/{taskId}`
