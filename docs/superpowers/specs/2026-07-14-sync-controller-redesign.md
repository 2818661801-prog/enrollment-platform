# 低代码平台同步接口重构设计

> 日期：2026-07-14
> 目标：简化 SyncController，用"先清再插"替代复杂判断逻辑

---

## 一、当前问题

| # | 问题 | 现状 |
|---|------|------|
| 1 | 日期格式不兼容 | `computePeriod` 和 `upsertClassRounds` 硬编码 `yyyy/MM/dd`，低代码平台传 ISO 格式会抛异常 |
| 2 | categories 同步逻辑混乱 | 删除时可能有外键约束报错，新建时可能有 unique 冲突 |
| 3 | classes 同步新建分支漏 category | 新建班级时 category 关联丢失 |
| 4 | syncConfig 的 updated 计数错误 | `updated++` 放错分支，永远少 1 |

---

## 二、新同步策略：先清再插

### 2.1 categories 同步（POST /api/admin/sync/categories）

**Body:**
```json
{
  "data": [
    { "name": "经管类" },
    { "name": "理工类" }
  ]
}
```

**逻辑：**
1. 清空 `ssc_categories` 表（TRUNCATE）
2. 清空 `ssc_class_category` 表（因为中间表引用 categories）
3. 把 `data` 里的 categories **全部插入**
4. 返回计数 `{ total, inserted }`

**说明：** categories 体量小（基础数据），清空重建风险可控。

---

### 2.2 classes 同步（POST /api/admin/sync/classes）

**Body:**
```json
{
  "data": [
    {
      "name": "成电班1",
      "quota": 50,
      "categoryNames": ["理工类", "竞赛类"],
      "classRounds": [
        { "roundNum": 1, "periodStart": "2026-09-01 00:00:00", "periodEnd": "2026-09-13 23:59:59" },
        { "roundNum": 2, "periodStart": "2026-09-15 08:00:00", "periodEnd": "2026-09-16 23:59:59" }
      ]
    }
  ]
}
```

**逻辑：**
1. 清空 `ssc_class_rounds` 表
2. 清空 `ssc_class_category` 表
3. 清空 `ssc_classes` 表
4. 遍历 `data`，每个班级：
   - 插入 `ssc_classes`（拿到自增 id）
   - 按 `categoryNames` 查 `categories` 表的 id，写入 `ssc_class_category`
   - 按 `classRounds` 写入 `ssc_class_rounds`
5. 返回计数 `{ total, inserted }`

**顺序：** categories 同步必须先跑，再跑 classes（因为 classes 依赖 categories 的 id）

---

### 2.3 轮次日期格式

统一用 **ISO 格式** `yyyy-MM-dd HH:mm:ss`，不再兼容斜杠格式。

低代码平台必须传 ISO 格式。如果传纯日期 `yyyy-MM-dd`，自动补 `00:00:00`（开始）或 `23:59:59`（结束）。

---

## 三、接口汇总

| 接口 | 方法 | 行为 |
|------|------|------|
| `/api/admin/sync/categories` | POST | 清空 + 全量插入 categories + class_category |
| `/api/admin/sync/classes` | POST | 清空 + 全量插入 classes + class_rounds + class_category |
| `/api/admin/sync/applications` | POST | 保持原样（报名记录同步逻辑没问题） |
| `/api/admin/sync/config` | POST | 保持原样（sys_config 同步已正常） |

---

## 四、错误处理

| 场景 | 处理方式 |
|------|----------|
| `categoryNames` 中的类别在外网不存在 | 跳过该类别（不报错，不插 class_category） |
| `classRounds` 为空 | 不插轮次，classes 表 period 字段设为 null |
| 数据格式错误（如日期格式不对） | 抛异常，整个事务回滚 |

---

## 五、验证方法

### categories 同步验证
1. 低代码平台新建类别 "测试类"
2. 调用 `POST /api/admin/sync/categories`
3. 查外网 DB：`SELECT * FROM ssc_categories;` — 有 "测试类"

### classes 同步验证
1. 低代码平台新建班级 "测试班"，关联类别 "测试类"，设两轮
2. 调用 `POST /api/admin/sync/classes`
3. 查外网 DB：
   - `SELECT * FROM ssc_classes WHERE name='测试班';` — 有记录
   - `SELECT * FROM ssc_class_rounds WHERE class_id=xx;` — 有 2 轮
   - `SELECT * FROM ssc_class_category WHERE class_id=xx;` — 有类别关联

---

## 六、现有代码保留/删除

| 代码 | 处理 |
|------|------|
| `computePeriod` 方法 | 删除（不再需要） |
| `upsertClassRounds` 方法 | 删除（改为直接 truncate + insert） |
| `upsertClassCategories` 方法 | 删除（改为直接 truncate + insert） |
| `parseDt` 方法 | 删除（日期解析在新逻辑里直接处理） |
| `syncClasses` 主逻辑 | 重写 |
| `syncCategories` 主逻辑 | 重写 |
| `syncConfig` | 保留原样 |

---

## 七、实现顺序

1. **先重写 `syncCategories`** — categories 简单，先验证
2. **再重写 `syncClasses`** — classes 依赖 categories，必须后跑
3. **验证** — 低代码平台实际跑一遍

---

## 八、风险点

| 风险 | 说明 | 后期补救 |
|------|------|----------|
| classes 清空会导致外网已有报名记录的班级消失 | 全量清 class_rounds + classes，报名记录还在（applications 表不动），但 class_id 外键还在，报名记录还保留，只是查不到班级信息 | 报名记录有 `classId`，低代码平台同步后重新拉取时，班级信息会恢复 |
| categories 清空会打断其他正在进行的报名 | 如果有学生正在报名，同时清 categories，可能短暂影响 | 实际影响极低（categories 只是基础数据，不影响报名核心流程） |
| 低代码平台传非 ISO 日期格式会抛异常 | 新逻辑只接受 ISO，不做斜杠兼容 | 低代码平台必须改传 ISO 格式 |
