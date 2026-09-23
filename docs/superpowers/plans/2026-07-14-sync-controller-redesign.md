# 低代码平台同步接口重构实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 重构 SyncController，用"先清再插"策略替代复杂判断逻辑

**架构：** categories 同步和 classes 同步都采用 TRUNCATE + 全量 INSERT 模式，不做增量判断。categories 先跑，classes 后跑（因为 classes 依赖 categories 的 id）。日期格式统一用 ISO。

**技术栈：** Java Spring Boot + JPA + MySQL

---

## 文件变更清单

| 文件 | 职责 |
|------|------|
| `enroll-server/src/main/java/com/enroll/server/controller/SyncController.java` | 全部重写 syncCategories 和 syncClasses 方法；删除 computePeriod、upsertClassRounds、upsertClassCategories、parseDt 这4个辅助方法 |

---

## 任务 1：重写 syncCategories 方法

**文件：** `enroll-server/src/main/java/com/enroll/server/controller/SyncController.java`

- [ ] **步骤 1：在 SyncController.java 中找到 syncCategories 方法（当前 L251-296），替换为新实现**

```java
/**
 * 全量同步类别（先清再插）
 * Body: { "data": [{ "name": "经管类" }] }
 */
@PostMapping("/categories")
@Transactional
public Map<String, Object> syncCategories(@RequestBody Map<String, Object> body) {
    List<Map> dataList = extractList(body, "data");

    // ① 清空 categories 和 class_category（外键约束先清中间表）
    classCatRepo.deleteAll();
    categoryRepo.deleteAll();

    // ② 全量插入
    int inserted = 0;
    for (Map<String, Object> item : dataList) {
        String name = (String) item.get("name");
        if (name == null || name.isBlank()) continue;
        name = name.trim();
        Category cat = new Category();
        cat.setName(name);
        categoryRepo.save(cat);
        inserted++;
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", dataList.size());
    result.put("inserted", inserted);
    return R.ok("同步成功", result);
}
```

- [ ] **步骤 2：验证代码语法正确（目测）**

确认方法签名、注解、变量名无误。

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/SyncController.java
git commit -m "refactor: syncCategories 改为先清再插策略"
```

---

## 任务 2：重写 syncClasses 方法

**文件：** `enroll-server/src/main/java/com/enroll/server/controller/SyncController.java`

- [ ] **步骤 1：在 SyncController.java 中找到 syncClasses 方法（当前 L72-139），替换为新实现**

```java
/**
 * 全量同步班级（先清再插）
 * Body: {
 *   "data": [{
 *     "name": "成电班1",
 *     "quota": 50,
 *     "categoryNames": ["理工类"],
 *     "classRounds": [
 *       { "roundNum": 1, "periodStart": "2026-09-01 00:00:00", "periodEnd": "2026-09-13 23:59:59" }
 *     ]
 *   }]
 * }
 */
@Transactional
@PostMapping("/classes")
public Map<String, Object> syncClasses(@RequestBody Map<String, Object> body) {
    List<Map> dataList = extractList(body, "data");

    // ① 清空（顺序：先中间表，再轮次，最后主表）
    classCatRepo.deleteAll();
    roundRepo.deleteAll();
    classRepo.deleteAll();

    // ② 全量插入
    int inserted = 0;
    DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    for (Map<String, Object> item : dataList) {
        String name = (String) item.get("name");
        if (name == null || name.isBlank()) continue;

        // 插入 class 主表
        ClassInfo cls = new ClassInfo();
        cls.setName(name);
        cls.setQuota(item.get("quota") == null ? 0 : (Integer) item.get("quota"));
        cls.setDescription((String) item.get("description"));
        cls.setIsDeleted(0);
        cls.setEnrolled(0);
        cls.setSource("sync");
        ClassInfo saved = classRepo.save(cls);

        // 插入轮次（class_rounds）
        Object roundsObj = item.get("classRounds");
        if (roundsObj instanceof List) {
            for (Map<String, Object> r : (List<Map>) roundsObj) {
                ClassRound cr = new ClassRound();
                cr.setClassId(saved.getId());
                cr.setRoundNum(r.get("roundNum") == null ? 1 : (Integer) r.get("roundNum"));
                cr.setPeriodStart(parseDt(r.get("periodStart"), dtFmt, dFmt, true));
                cr.setPeriodEnd(parseDt(r.get("periodEnd"), dtFmt, dFmt, false));
                roundRepo.save(cr);
            }
        }

        // 插入类别关联（class_category）
        Object namesObj = item.get("categoryNames");
        if (namesObj instanceof List) {
            for (String catName : (List<String>) namesObj) {
                if (catName == null || catName.isBlank()) continue;
                final String n = catName.trim();
                categoryRepo.findByName(n).ifPresent(cat -> {
                    ClassCategory cc = new ClassCategory();
                    cc.setClassId(saved.getId());
                    cc.setCategoryId(cat.getId());
                    classCatRepo.save(cc);
                });
            }
        }

        inserted++;
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", dataList.size());
    result.put("inserted", inserted);
    return R.ok("同步成功", result);
}
```

- [ ] **步骤 2：补充 parseDt 工具方法（在 syncClasses 下面加 private 方法）**

```java
/**
 * 解析日期字符串，支持 ISO 格式（yyyy-MM-dd HH:mm:ss）和纯日期格式（yyyy-MM-dd）
 * isStart=true：纯日期补 00:00:00
 * isStart=false：纯日期补 23:59:59
 */
private java.time.LocalDateTime parseDt(Object val, DateTimeFormatter dtFmt,
                                         DateTimeFormatter dFmt, boolean isStart) {
    if (val == null) return null;
    String s = val.toString().trim();
    try {
        return java.time.LocalDateTime.parse(s, dtFmt);
    } catch (Exception e) {
        java.time.LocalDate d = java.time.LocalDate.parse(s, dFmt);
        return isStart ? d.atStartOfDay() : d.atTime(23, 59, 59);
    }
}
```

- [ ] **步骤 3：验证代码语法正确（目测）**

确认所有方法引用、变量名无误。

- [ ] **步骤 4：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/SyncController.java
git commit -m "refactor: syncClasses 改为先清再插策略，支持 ISO 日期格式"
```

---

## 任务 3：删除废弃辅助方法

**文件：** `enroll-server/src/main/java/com/enroll/server/controller/SyncController.java`

- [ ] **步骤 1：删除 computePeriod 方法（L144-174）**

找到 `computePeriod` 方法整个删掉。

- [ ] **步骤 2：删除 upsertClassRounds 方法（L179-203）**

找到 `upsertClassRounds` 方法整个删掉。

- [ ] **步骤 3：删除 upsertClassCategories 方法（L208-231）**

找到 `upsertClassCategories` 方法整个删掉。

- [ ] **步骤 4：删除旧的 parseDt 方法（如果有残留）**

确认只有一个 parseDt 方法（步骤 2 加的那个）。

- [ ] **步骤 5：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/SyncController.java
git commit -m "refactor: 删除 computePeriod/upsertClassRounds/upsertClassCategories 废弃方法"
```

---

## 任务 4：验证编译通过

**文件：** 无新增文件

- [ ] **步骤 1：编译项目**

```bash
cd enroll-server
./mvnw.cmd compile -DskipTests
```

预期：BUILD SUCCESS，无编译错误。

- [ ] **步骤 2：如果编译失败，根据错误修复**

常见错误：import 缺失、方法签名不匹配、类型转换错误。根据错误提示修复后重新编译。

---

## 任务 5：验证同步接口

> 前提：后端已启动（8081 端口）

- [ ] **步骤 1：验证 categories 同步**

```bash
curl -s -X POST "http://localhost:8081/api/admin/sync/categories" \
  -H "Content-Type: application/json" \
  -d '{"data":[{"name":"测试类别A"},{"name":"测试类别B"}]}'
```

预期：`{"code":200,"message":"同步成功","data":{"total":2,"inserted":2}}`

- [ ] **步骤 2：验证 classes 同步（依赖步骤1的类别）**

```bash
curl -s -X POST "http://localhost:8081/api/admin/sync/classes" \
  -H "Content-Type: application/json" \
  -d '{
    "data":[{
      "name":"测试班级",
      "quota":30,
      "categoryNames":["测试类别A"],
      "classRounds":[
        {"roundNum":1,"periodStart":"2026-09-01 00:00:00","periodEnd":"2026-09-13 23:59:59"},
        {"roundNum":2,"periodStart":"2026-09-15 08:00:00","periodEnd":"2026-09-16 23:59:59"}
      ]
    }]
  }'
```

预期：`{"code":200,"message":"同步成功","data":{"total":1,"inserted":1}}`

- [ ] **步骤 3：验证数据库数据**

```bash
mysql -uenroll -p***REMOVED*** enroll_db -e "
SELECT id, name, quota, source FROM ssc_classes;
SELECT * FROM ssc_class_rounds;
SELECT * FROM ssc_class_category;
SELECT * FROM ssc_categories;
"
```

预期：
- ssc_classes 有 "测试班级" 记录
- ssc_class_rounds 有 2 轮
- ssc_class_category 有类别关联
- ssc_categories 有 "测试类别A" 和 "测试类别B"

---

## 自检清单

- [ ] syncCategories 用 TRUNCATE 策略（deleteAll）
- [ ] syncClasses 清空顺序：class_category → class_rounds → classes
- [ ] 日期格式只用 ISO（yyyy-MM-dd HH:mm:ss）
- [ ] 废弃方法 computePeriod、upsertClassRounds、upsertClassCategories 已删除
- [ ] parseDt 只保留一个，兼容 ISO 和纯日期
- [ ] 编译通过
- [ ] categories 同步验证通过
- [ ] classes 同步验证通过
