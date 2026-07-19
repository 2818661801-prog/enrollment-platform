# 特色班报名系统 · 接口文档

> 版本：v2.4
> 更新：2026-07-07
> 主题：班级-类别中间表（替代 category_names JSON）

---

## 一、查询接口（b 查 a）

> 同步按钮第①步：低代码平台了解 enroll_db 现状
> 所有接口均通过 admin JWT 认证（低代码平台调用时需携带 `Authorization: Bearer token`）

### 同步按钮完整流程

```
① b查a  GET /api/admin/classes         → 低代码平台了解 enroll_db 现状
② 在b里做增删改                         → 低代码平台操作自己的内网 DB
③ b查b                                 → 低代码平台拿到 b 的完整数据
④ 增删a  POST /api/admin/sync/{table}  → 全量同步到 enroll_db
```

---

### 1. 查询全部班级（含软删）

```
GET /api/admin/classes
```

### 2. 查询全部类别

```
GET /api/admin/categories
```

### 3. 查询全部报名记录

```
GET /api/admin/applications?page=0&size=20&classId=1&status=1&idCard=xxx&name=xxx
```

### 4. 查询报名须知

```
GET /api/admin/notice
```

---

## 二、同步接口（b → a）

> 同步按钮第④步：全量同步到 enroll_db

### 同步逻辑（4张表统一）

| 场景 | 动作 |
|------|------|
| b有、a无 | 新增 |
| b有、a有 | 更新 |
| b有、isDeleted=1 | 软删（更新 is_deleted=1）|
| a有、b无 | 真正删除 |

---

### 1. 同步班级 `POST /api/admin/sync/classes`

**主键**：`name`

**请求：**
```json
{
  "data": [
    {
      "name": "成电班",
      "period": "2026/09/01 08:00 - 2026/09/13 23:59",
      "classRounds": [
        {
          "roundNum": 1,
          "periodStart": "2026/09/01 08:00",
          "periodEnd": "2026/09/13 23:59"
        },
        {
          "roundNum": 2,
          "periodStart": "2026/09/15 08:00",
          "periodEnd": "2026/09/16 23:59"
        }
      ],
      "quota": 50,
      "description": "电子科技大学特色班",
      "categoryNames": ["成电班"],
      "isDeleted": 0,
      "innerId": 9
    }
  ]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "total": 1,
    "inserted": 0,
    "updated": 1,
    "deleted": 0,
    "softDeleted": 0
  }
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 班级名称（主键）|
| period | string | 否 | 当前有效时间段（可由 classRounds 第一轮自动计算）|
| classRounds | array | ✅ | 轮次列表 |
| classRounds[].roundNum | int | ✅ | 轮次编号（1/2/3...）|
| classRounds[].periodStart | string | ✅ | 报名开始时间（格式：yyyy/MM/dd 或 yyyy/MM/dd HH:mm）|
| classRounds[].periodEnd | string | ✅ | 报名结束时间（格式：yyyy/MM/dd 或 yyyy/MM/dd HH:mm）|
| quota | int | 否 | 名额（-1=不限，>=0=具体名额）|
| description | string | 否 | 班级描述 |
| categoryNames | array | 否 | 班级类别标签（同步时按名称写入中间表 ssc_class_category）|
| isDeleted | int | 否 | 0=正常，1=软删除 |
| innerId | int | 否 | 内网班级ID（外网收到后存到 outer_id 字段，用于跨系统 id 映射）|

**⚠️ periods 字段已废弃**（v2.2 起由 classRounds 数组替代）

---

### 2. 同步类别 `POST /api/admin/sync/categories`

**主键**：`name`

**请求：**
```json
{
  "data": [
    { "name": "理工类" },
    { "name": "经管类" }
  ]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "total": 2,
    "inserted": 1,
    "updated": 1,
    "deleted": 0
  }
}
```

---

### 3. 同步报名记录 `POST /api/admin/sync/applications`

**主键**：`idCard` + `classId`

**请求：**
```json
{
  "data": [
    {
      "idCard": "110101***REMOVED***4",
      "classId": 1,
      "name": "张三",
      "gender": "男",
      "phone": "***REMOVED***",
      "hasPhysics": "是",
      "hasEnglish": "否",
      "appliedCategory": "理工类",
      "status": 1,
      "noticeAgreed": 1,
      "round": 1,
      "enrollmentYear": 26
    }
  ]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "total": 1,
    "inserted": 1,
    "updated": 0,
    "deleted": 0
  }
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| idCard | string | ✅ | 身份证号（主键 part 1）|
| classId | int | ✅ | 班级ID（主键 part 2）|
| name | string | 否 | 学生姓名 |
| gender | string | 否 | 性别 |
| phone | string | 否 | 手机号 |
| hasPhysics | string | 否 | 是否选考物理 |
| hasEnglish | string | 否 | 是否选考英语 |
| appliedCategory | string | 否 | 报考类别 |
| status | int | 否 | 状态（0未报/1已报/2撤回/3录取/4未录取）|
| noticeAgreed | int | 否 | 是否同意须知（0否/1是）|
| round | int | 否 | 报名轮次（1/2）|
| enrollmentYear | int | 否 | 报名年级（26=2026年，27=2027年），后端自动写入，也可由同步接口传入 |

---

### 4. 同步系统配置 `POST /api/admin/sync/config`

**说明**：sys_config 只有一条记录（id=1），直接 upsert

**请求：**
```json
{
  "data": [
    {
      "title": "2026年特色班报名须知",
      "conditions": "报名者须为2026级新生\n每人限报1个特色班\n报名信息填写须真实有效",
      "notices": "部分特色班设有两轮报名...\n报名时间截止后不可修改\n录取结果另行通知\n如有疑问请联系教务处",
      "updatedBy": "admin"
    }
  ]
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 报名须知标题 |
| conditions | string | 否 | 报名条件（换行分隔） |
| notices | string | 否 | 报名须知（换行分隔） |
| updatedBy | string | 否 | 最后修改人 |

**响应：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "total": 1,
    "inserted": 1,
    "updated": 0
  }
}
```

---

## 三、通用响应格式

**成功：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

**失败：**
```json
{
  "code": <错误码>,
  "message": "<错误信息>",
  "data": null
}
```

**常见错误码：**
| code | 说明 |
|------|------|
| 200 | 成功 |
| 4001 | 缺少登录凭证 |
| 4002 | 登录失败 |
| 5000 | 服务器内部错误 |

---

## 四、数据字典

### 班级（classes）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键 |
| name | string | 班级名称 |
| period | string | 当前有效时间段（由 class_rounds 第一轮自动计算）|
| classRounds | array | 报名轮次数组 `[{roundNum, periodStart, periodEnd}]`，**ISO 格式** |
| quota | int | 名额（-1=不限）|
| enrolled | int | 已报名人数 |
| description | string | 描述 |
| isDeleted | int | 0正常/1已删除 |
| categories | array | 关联的类别名称数组，如 `["经管类","理工类"]`，**由中间表查询返回** |
| source | string | 数据来源（admin/sync/student）|

> **category_names 字段已移除**，改为通过中间表 `ssc_class_category` 关联查询
> **periods JSON 字段已移除**，改为 `classRounds` 数组

### 班级-类别中间表（class_category）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键 |
| class_id | int | 班级ID，关联 classes.id |
| category_id | int | 类别ID，关联 categories.id |
| created_at | datetime | 创建时间 |

> 一个班级可属多个类别（N:N），通过中间表实现

### 班级轮次（class_rounds）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键 |
| class_id | int | 班级ID，关联 classes.id |
| round_num | int | 轮次编号（1/2/3...）|
| period_start | datetime | 报名开始时间 |
| period_end | datetime | 报名结束时间 |
| created_at | datetime | 创建时间 |

### 报名记录（applications）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键 |
| name | string | 学生姓名 |
| id_card | string | 身份证号 |
| id_card_masked | string | 脱敏身份证（中间8位***）|
| gender | string | 性别 |
| phone | string | 手机号 |
| has_physics | string | 是否选考物理 |
| has_english | string | 是否选考英语 |
| class_id | int | 班级ID |
| applied_category | string | 报考类别 |
| status | int | 状态（0未报/1已报/2撤回/3录取/4未录取）|
| notice_agreed | int | 是否同意须知（0否/1是）|
| apply_time | datetime | 报名时间 |
| round | int | 轮次 |
| source | string | 数据来源 |
| enrollment_year | int | 报名年级（26=2026年，27=2027年），后端自动写入，前端/同步接口不需传参 |

### 系统配置（sys_config）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键（固定为1）|
| title | string | 报名须知标题 |
| conditions | string | 报名条件（换行分隔） |
| notices | string | 报名须知（换行分隔） |
| updated_at | datetime | 最后修改时间 |
| updated_by | string | 最后修改人 |
