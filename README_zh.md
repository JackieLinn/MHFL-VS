<h1 align="center">MHFL-VS</h1>
<p align="center"><i>模型异构联邦学习 · 全链路可视化与仿真平台</i></p>

<p align="center">
  联邦学习训练过程的可视化与仿真，支持实时查看训练进度与曲线。
</p>

---

### 🛠 技术栈

| | 技术 |
|---|--------|
| 🖥 **前端** | Vue 3、TypeScript、Element Plus、ECharts、Pinia、UnoCSS、Vite |
| ☕ **Java** | Spring Boot 3.5、Spring Security、JWT、WebSocket、MyBatis Plus、MySQL、Redis |
| 🐍 **Python** | FastAPI、PyTorch、Redis |
| 🐳 **基础设施** | MySQL、Redis、RabbitMQ |

---

### 已实现

- [x] **认证与用户** — 注册、登录、密码重置（邮箱）、JWT、验证码；个人资料与头像；管理员：导入/删除用户、列表搜索与分页
- [x] **前端** — 首页布局、CPU/内存/GPU 实时监控、中英文与黑白主题、用户信息抽屉、系统管理（人员管理；数据集/算法占位）
- [x] **Python** — 健康检查、资源监控接口、统一响应格式

### 待实现

- [ ] **后端** — WebSocket 订阅、Redis 消息、任务 CRUD 与启停、监控与数据分析接口
- [ ] **前端** — 任务管理、实时监控（WebSocket + ECharts）、分析与导出
- [ ] **Python** — 训练任务接口、Redis 回调、GPU 资源管理

---

### 🚀 本地运行

1. 启动 MySQL、Redis、RabbitMQ（如 Docker Compose）
2. 启动 Spring Boot（端口 8088）
3. 启动 FastAPI（端口 8000）
4. 启动 Vue 开发服务（Vite）
