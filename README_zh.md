<h1 align="center">MHFL-VS</h1>

<p align="center"><i>模型异构联邦学习 · 全链路可视化与仿真平台</i></p>

<p align="center">
  联邦学习训练过程的可视化与仿真，支持实时查看训练进度与曲线。
</p>

<p align="center">
  中文 · <a href="README.md">English</a>
</p>

---

## 🛠 技术栈

| | 技术 |
|---|--------|
| 🖥️ **前端** | Vue, Vite, TypeScript, Element-Plus, ECharts, Pinia, UnoCSS |
| ⚙️ **Java 后端** | Spring Boot, Spring Security, MyBatis Plus, WebSocket |
| 🐍 **Python 后端** | FastAPI, PyTorch |
| 🐳 **基础设施** | MySQL (3306)、Redis (6379)、RabbitMQ (5672/15672) |

---

## 📁 项目结构

```
MHFL-VS/
├── mhfl-ui/          # Vue3 前端
├── mhfl-server/       # Spring Boot 后端（端口 8088）、compose.yaml
└── mhfl-algo/        # FastAPI + PyTorch 训练服务（端口 8000）
```

---

## ✅ 已实现

### 🔐 认证与用户
- [x] 注册、登录、密码重置（邮箱验证）
- [x] JWT 认证、图形验证码、请求限流
- [x] 退出登录：token 写入 Redis 黑名单
- [x] 个人资料与头像上传（本地存储）
- [x] 管理员：导入/删除用户、列表关键字搜索与分页

### ⚙️ 后端（Spring Boot）
- [x] 数据集与算法管理（管理员 CRUD）
- [x] 任务管理：创建、列表、详情、启动、停止、删除、设为推荐
- [x] Round/Client 接口：轮次列表、客户端最新、客户端详情
- [x] WebSocket `/ws/task/{taskId}` 实时训练监控
- [x] Redis 订阅：Round/Client/Status 消息 → 写 MySQL + 推 WebSocket
- [x] Dashboard：平台概览、任务统计、图表、系统健康检查
- [x] 资源代理：系统资源（CPU/内存/GPU）通过 FastAPI 获取

### 🖥️ 前端（Vue3）
- [x] 首页布局：Header、Sidebar、Footer、子路由
- [x] 仪表盘：统计卡片、图表（状态饼图、近 7 天趋势）、最近任务、系统健康
- [x] 任务管理：列表、创建、启动、停止、删除、详情
- [x] 任务详情：实验设置、指标卡片、Round 折线图、Client 100 格子
- [x] 进行中任务 WebSocket 实时推送
- [x] 中英文、黑白主题
- [x] 系统管理：人员、数据集、算法（完整 CRUD）
- [x] 推荐展示页：CIFAR-100 / Tiny-ImageNet 切换（当前为模拟数据）
- [x] 智能小助手页面：对话 UI、历史会话、流式回复（当前为模拟 AI）

### 🐍 Python（FastAPI）
- [x] 健康检查、资源监控接口（GPU、CPU、内存）
- [x] 训练接口：启动、停止（多进程）
- [x] Redis 发布：Round、Client、Status 回调
- [x] 训练前 GPU 检查（CIFAR-100: 3GB，Tiny-ImageNet: 5.5GB）
- [x] 算法：Standalone、FedProto、FedAvg、FedSSA、LG-FedAvg

---

## 📋 待实现

- [ ] **后端** — 推荐任务 API；数据分析接口；智能小助手 API（LLM 代理）
- [ ] **前端** — 推荐展示页对接真实 API；数据分析与导出；智能小助手对接后端 API

---

## 🚀 本地运行

1. 启动 MySQL、Redis、RabbitMQ（Spring Boot 通过 `spring-boot-docker-compose` 使用 `mhfl-server/compose.yaml`，也可手动启动）
2. 启动 Spring Boot（端口 8088）
3. 启动 FastAPI（端口 8000）
4. 启动 Vue 开发服务（`pnpm dev` / `npm run dev`）

```bash
# 1. 启动基础设施（进入 mhfl-server 目录）
cd mhfl-server && docker compose up -d

# 2. 启动 Java 后端
cd mhfl-server && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 启动 Python 后端
cd mhfl-algo && python -m uvicorn main:app --reload --port 8000

# 4. 启动前端
cd mhfl-ui && pnpm install && pnpm dev
```

---

## 📦 环境要求

- **Node**：^20.19.0 或 >=22.12.0
- **Java**：17
- **Python**：3.x（推荐 3.10+）
