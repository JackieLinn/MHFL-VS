<p align="center" style="margin-bottom: 4px;">
  <img src="mhfl-ui/public/logo.svg" width="74" alt="MHFL-VS Logo" />
</p>

<h1 align="center" style="margin: 0;">MHFL-VS</h1>

<p align="center" style="margin-top: 6px;"><i>模型异构联邦学习全链路可视化与仿真平台</i></p>

<p align="center">
  中文 · <a href="README.md">English</a>
</p>

---

## 🚀 项目简介

MHFL-VS 是一个联邦学习训练可视化平台，覆盖任务创建、训练调度、实时监控、数据持久化与系统观测全流程。
平台采用前后端分离架构，由 Spring Boot 统一网关，FastAPI 负责训练与推理能力。

## ✨ 系统功能

- 🔐 认证与权限：注册登录、邮箱找回、JWT、验证码、限流、角色权限与 Redis 黑名单。
- 🧭 任务全生命周期：任务创建、查询、详情、启动、停止、删除、推荐状态管理。
- 🔄 训练实时链路：Python 训练过程通过 Redis 发布 Round/Client/Status，Spring Boot 写库并推送 WebSocket。
- 📈 可视化监控：任务指标卡片、Round 曲线、客户端网格、客户端详情历史。
- 🛠️ 平台管理：用户、数据集、算法管理，支持关键字搜索、分页与管理员操作。
- 🖥️ 仪表盘：平台概览、任务状态分布、7 天趋势、最近任务、系统健康、CPU/内存/GPU 实时资源。
- 🎨 界面能力：深浅色主题适配与中英文多语言支持。
- 🤖 智能助手：基于 RAG 的工作流智能体，提供会话管理与非流式/流式问答能力。

## 🧩 技术栈

| 分层 | 技术 |
|---|---|
| 前端 | ![Vue](https://img.shields.io/badge/Vue-4FC08D?logo=vuedotjs&logoColor=white) ![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white) ![Element Plus](https://img.shields.io/badge/Element_Plus-409EFF?logo=element&logoColor=white) ![ECharts](https://img.shields.io/badge/ECharts-AA344D?logo=apacheecharts&logoColor=white) ![Pinia](https://img.shields.io/badge/Pinia-FFD859?logo=pinia&logoColor=black) ![UnoCSS](https://img.shields.io/badge/UnoCSS-333333?logo=unocss&logoColor=white) ![Vue I18n](https://img.shields.io/badge/Vue_I18n-41B883?logo=vuedotjs&logoColor=white) |
| Java 后端 | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white) ![Spring WebSocket](https://img.shields.io/badge/Spring_WebSocket-6DB33F?logo=spring&logoColor=white) ![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-2E8B57?logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white) |
| Python 后端 | ![FastAPI](https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white) ![Uvicorn](https://img.shields.io/badge/Uvicorn-499848?logo=uvicorn&logoColor=white) ![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?logo=pytorch&logoColor=white) ![LangChain](https://img.shields.io/badge/LangChain-1C3C3C?logo=chainlink&logoColor=white) ![Pydantic](https://img.shields.io/badge/Pydantic-E92063?logo=pydantic&logoColor=white) ![OpenAI](https://img.shields.io/badge/OpenAI-412991?logo=openai&logoColor=white) ![Pytest](https://img.shields.io/badge/Pytest-0A9EDC?logo=pytest&logoColor=white) |
| 基础设施 | ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white) |

## 🗂️ 目录结构

```text
MHFL-VS/
├── mhfl-algo/                # FastAPI + 训练服务（8000）
│   ├── assistant/            # RAG、检索、重排、知识库脚本
│   ├── fl_core/              # 联邦学习算法与 trainer
│   ├── routers/              # health/resource/train/assistant 接口
│   ├── services/             # 训练与助手编排
│   ├── utils/                # redis 发布、schema、资源检查
│   └── requirements.txt
├── mhfl-server/              # Spring Boot 后端（8088）
│   ├── src/main/java/        # controller、service、mapper、security
│   ├── src/main/resources/   # application-*.yaml、mapper xml
│   ├── compose.yaml
│   └── pom.xml
├── mhfl-ui/                  # Vue3 前端
│   ├── public/               # 静态资源（含 logo.svg）
│   ├── src/                  # 页面、组件、状态、接口封装
│   └── package.json
├── README.md
└── README_zh.md
```

## 📦 环境要求

- Node.js：`22.20.0`
- Java：`25`
- Python：`3.11`
- Docker + Docker Compose（用于 MySQL/Redis/RabbitMQ）

## ⚡ 启动方式

### 0. 获取项目

```bash
git clone https://github.com/JackieLinn/MHFL-VS.git
cd MHFL-VS
```

### 1. 启动基础设施

```bash
cd mhfl-server
docker compose up -d
```

### 2. 启动 FastAPI（8000）

```bash
cd mhfl-algo
pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```

### 3. 启动 Spring Boot（8088）

```bash
cd mhfl-server
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. 启动前端（Vite）

```bash
cd mhfl-ui
pnpm install
pnpm dev
```

## 🧪 测试命令

### 前端

```bash
cd mhfl-ui
pnpm test:coverage
```

### Java 后端

```bash
cd mhfl-server
mvn clean test
```

### Python 后端

```bash
cd mhfl-algo
python -m pytest -vv -s
```

## 🌐 访问入口

- 前端：`http://localhost:5173`
- Spring Boot 接口文档（Swagger UI）：`http://localhost:8088/doc/swagger-ui/index.html`
- FastAPI 接口文档：`http://localhost:8000/api/docs`
- 任务监控 WebSocket：`ws://localhost:8088/ws/task/{taskId}`
