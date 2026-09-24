# 高校特色班报名系统

面向高校特色班（拔尖班 / 联合培养 / 方向班）选拔场景的在线报名系统：学生扫码报名、多级防重校验、管理员批量录取 / 驳回、报名名额防超卖，并预留与低代码教务平台的跨网数据同步链路。曾在真实招生周期内服务 8 个特色班、承载 800+ 条报名记录。

## 核心能力

- **报名与防重**：三层防重校验（身份证全局唯一 / 手机号全局唯一 / 同班不重复占位），业务裁决"被驳回学生同班禁报、他班可报"（决策记录见 [ADR](./docs/ADR-驳回后禁止重报.md)）
- **名额与并发**：录取环节基于数据库事务 + 行级锁定实现名额扣减原子性，防并发超卖；外网入口 Redis 限流防刷
- **认证与会话**：管理员账密 + JWT；学生端短信验证码 + JWT（开发模式验证码走固定值开关，生产接第三方通道）
- **跨网同步契约**：与内网低代码平台以 REST 约定对接（40 个接口），`source` 字段溯源、`outer_id` 对账，保障内外网双库一致性
- **测试体系**：后端 JUnit 5 测试 65 个（Mockito 单测 + 真实 MySQL 集成测试），前端 Vitest 单测 80 个，另配 Python 冒烟 / E2E 脚本作为发布前检查

## 架构

```
 学生（外网）                                   管理员（内网）
     │                                              │
     ▼                                              ▼
 Vue 3 学生端  ◄────►  Spring Boot 3 后端  ◄────►  MySQL (enroll_db)
   (5173 → 8081)         │        ▲
                         │        └──── 同步 API（source 溯源 / outer_id 对账）
                         ▼               ◄──── 低代码平台（本地以 curl 模拟）
                     Redis（限流 / 验证码缓存）
```

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2.7 · Spring Data JPA · JWT · Redis · MySQL 8（:8081） |
| 前端 | Vue 3 · Element Plus · Vite · Pinia（:5173，dev proxy → 8081） |
| 测试 | JUnit 5 + Mockito · Vitest · Python（冒烟 / E2E） |

## 快速启动

前置：JDK 21、Maven、Node 18+、MySQL 8（本机或 Docker）。

```bash
# 1. 初始化数据库（含表结构与种子数据）
mysql -uroot -p --default-character-set=utf8mb4 enroll_db < docs/sql/V20260701__特色班报名系统_INIT.sql

# 2. 启动后端（application.yml 中配置本地数据库账号）
cd enroll-server && mvnw.cmd spring-boot:run -DskipTests    # → :8081

# 3. 启动前端
cd enroll-web && npm install && npm run dev                 # → :5173
```

验证：`curl http://localhost:8081/api/classes` 返回班级列表即成功。开发模式配置与初始管理员见 `docs/00-项目学习手册.md`。

## 测试

```bash
cd enroll-server && mvn test     # 后端 65 个测试（含 MySQL 集成测试）
cd enroll-web && npm run test    # 前端 Vitest 80 个单测
```

## 仓库导航

| 内容 | 位置 |
|------|------|
| 后端源码 | `enroll-server/`（controller / service / repository / entity / dto） |
| 前端源码 | `enroll-web/`（views / components / stores / utils） |
| 数据库脚本 | `docs/sql/`（初始化与增量迁移） |
| 用户使用手册 | `docs/user-manuals/`（学生端 / 教师端操作文档，随产交付，已脱敏） |
| 架构决策记录 | `docs/ADR-驳回后禁止重报.md` |
| 架构整改设计（13 项问题清单） | `docs/dev-docs/specs/` |
| 请求链路讲解（新手向） | `docs/00-项目学习手册.md` |
| 本地全链路冒烟记录 | `docs/冒烟记录-20260921.md` |

## 分支与版本

| 分支 / 标签 | 说明 |
|------|------|
| `main` | 当前主线（tag `v2.0-personal`）：架构整改后的完整可本地运行版本 |
| `refactor/architecture-cleanup` | v2.0 开发分支（架构整改 Task 1–30），已并入 main |
| `release/v1` | 初版历史档案，已退役归档，不再维护 |

## 说明

本项目为企业实习系统的个人重构与脱敏版本：内网低代码平台、第三方短信通道等外部依赖以接口契约与本地模拟保留，核心报名链路（报名 → 防重 → 录取 / 驳回 → 查询）纯本地完整可演示。
