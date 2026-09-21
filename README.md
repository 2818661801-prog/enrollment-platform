# 特色班报名系统

> 高校特色班（拔尖班 / 成电联培 / ACCA 等）在线报名系统：学生扫描报名、管理员批量录取/驳回、低代码平台数据同步全链路。

## 项目现状（2026-09-21）

- **线上 v1 已退役**：原企业实习期间部署的 v1 版本自 2026-09-21 起不再维护、不再作为参考基准（历史代码保留在 `release/v1` 分支，仅作档案）。
- **`refactor/architecture-cleanup` 分支为唯一主线**（v2.0）：架构整改完成，本仓库目标是**完整、可本地运行、可放进个人作品集**。
- 当前主线已打 tag `v2.0-personal`。

## 核心链路（纯本地可完整演示）

```
管理员建班 → 学生扫码报名 → 查重拦截 → 批量录取/驳回（跳过无效记录） → 我的报名查询 → 低代码同步（curl 模拟）
```

- 学生端：报名、查重、我的报名、撤回
- 管理端（API + 低代码平台）：建班、类别、报名须知、批量录取/驳回、同步
- 业务规则：身份证/手机号全局唯一 + 同班防重；**被驳回后同班禁报、他班可报**（[ADR](./docs/ADR-驳回后禁止重报.md)）

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2.7 + Spring Data JPA + MySQL 8（端口 8081）|
| 前端 | Vue 3 + Element Plus + Vite + Pinia（端口 5173，proxy → 8081）|
| 数据库 | MySQL，库名 `enroll_db`，用户 `enroll` / `***REMOVED***` |
| 认证 | 管理员：账号密码 + JWT；学生：验证码 + JWT（测试模式固定 666666，见 CLAUDE.md §5）|

## 快速启动（本地）

前置：JDK 21、Maven、Node 18+、MySQL 8（本机服务或 Docker 均可）。

```bash
# 1. 建库（库不存在时）
mysql -uenroll -p'***REMOVED***' --default-character-set=utf8mb4 enroll_db < docs/sql/V20260701__特色班报名系统_INIT.sql

# 2. 后端
cd enroll-server && mvnw.cmd spring-boot:run -DskipTests    # → 8081

# 3. 前端
cd enroll-web && npm install && npm run dev                 # → 5173
```

验证：`curl http://localhost:8081/api/classes` 返回班级列表即成功。

## 测试与验证

```bash
# 后端单元测试（65 个，含 Mockito 单测 + MySQL 集成测试）
cd enroll-server && mvn test

# 前端单元测试（80 个）+ 构建
cd enroll-web && npm run test && npm run build
```

## 目录速览

```
enroll-server/  Spring Boot 后端（controller/service/repository/entity/dto.request）
enroll-web/     Vue 3 前端（views/components/stores/utils）
docs/
  sql/          数据库初始化与迁移脚本
  ADR-驳回后禁止重报.md      业务规则裁决记录
  00-项目学习手册.md         新手向维护手册（请求的一生 → 维护场景）
  冒烟记录-20260921.md       本地全链路实测记录
  superpowers/specs/        架构整改设计文档（13 个架构问题清单）
CLAUDE.md        项目级约定（路径/数据库/多轮/重启方法）
```

## ⚠️ 企业环境依赖说明（去公司化标注）

本项目源自企业实习期间的「智慧教务平台 + 低代码平台」内外网双系统架构，以下能力**依赖原企业环境，本地以文档 / mock 形式保留**，不影响核心链路演示：

| 能力 | 依赖 | 本地替代 |
|------|------|---------|
| 低代码平台同步 `POST /api/admin/sync/**` | 内网低代码平台按约定 REST 推送 | curl 模拟（见 `docs/冒烟记录-20260921.md` §2.9）|
| 管理员 UI | 内网低代码平台复刻 AdminDashboard | 本项目只保留 Admin API；学生端完整 |
| 学生短信验证码 | 第三方短信通道 | 后端测试开关：固定 `666666` |
| 内网表（SmsStudent / SysDepartment / 智慧教务账号） | 内网数据库，网络不可达 | 不接入；仅 `Sync*` 契约保留 |

设计文档：`CLAUDE.md` §11 内外网双系统架构。管理员账号（开发期）：`***REMOVED***` / `***REMOVED***`。

## 分支说明

- `main` — 当前主线（等于 v2.0 合并结果），tag `v2.0-personal`
- `refactor/architecture-cleanup` — v2.0 开发分支（架构整改 Task 1-30），已并入 main
- `release/v1` — 退役版历史档案（保留不删，仅作历史）