# 重复报名名单生成脚本 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 写一个 Python 脚本，导入用户从内网 phpMyAdmin 复制出的查重结果文本（脱敏版），自动解析并生成「重复报名名单」CSV。

**架构：** 纯 Python 标准库（无第三方依赖），脚本读取 `tmp/dup_query_result.txt`（已存档的原始粘贴文本），按注释标记切分 ②③④⑦ 四个数据块，正则解析每行字段，以身份证+手机号为 key 合并成名单，输出 UTF-8 BOM CSV（Excel 双击可开）+ 控制台摘要。

**技术栈：** Python 3.12+，仅标准库 `re / csv / collections / pathlib`

**背景事实（已核实）：**
- 输入数据来自内网库 `smart_academic_affairs.ssc_applications`，字段全脱敏（身份证前6后4、手机号前3后4）
- 明细列被 phpMyAdmin 截断（以 `...` 结尾），解析时取"|"分割能拿到的最多字段，不报错
- 名单核心字段：姓名、脱敏身份证、脱敏手机号、有效报名数、涉及班级、是否同班重复、记录id（可用部分）

---

## 文件结构

- 创建：`tmp/gen_dup_list.py` —— 主脚本（解析 + 合并 + 输出）
- 创建：`tmp/重复报名名单.csv` —— 输出产物（脚本运行生成）
- 输入：`tmp/dup_query_result.txt` —— 用户粘贴的原始数据（已存在，本轮已存档）
- 测试：`tmp/test_gen_dup_list.py` —— 用存档的真实数据做断言测试

---

### 任务 1：写解析器核心（块切分 + ②身份证块解析）

**文件：**
- 创建：`tmp/gen_dup_list.py`

- [ ] **步骤 1：编写脚本骨架与块切分函数**

```python
# -*- coding: utf-8 -*-
"""重复报名名单生成器：解析 phpMyAdmin 粘贴文本 -> 重复名单 CSV"""
import re, csv
from collections import OrderedDict
from pathlib import Path

INPUT = Path(__file__).parent / "dup_query_result.txt"
OUTPUT = Path(__file__).parent / "重复报名名单.csv"

BLOCK_PATTERN = re.compile(r"^# =+ (②|③|④|⑦) ", re.M)

def split_blocks(text: str):
    """按 '# ===== ② xxx =====' 注释切分数据块，返回 {编号: 该块文本}"""
    positions = [(m.start(), m.group(1)) for m in BLOCK_PATTERN.finditer(text)]
    blocks = {}
    for i, (pos, key) in enumerate(positions):
        end = positions[i + 1][0] if i + 1 < len(positions) else len(text)
        blocks[key] = text[pos:end]
    return blocks
```

- [ ] **步骤 2：写②块解析函数（同一身份证多条 1/3）**

```python
def parse_idcard_block(text: str):
    """② 块：每行 = 身份证_脱敏 | 有效报名数 | 明细(可能截断)
    返回 [{id_card, count, name, phone, class_id, detail}]"""
    rows = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("身份证"):
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        id_card, count, detail = parts[0], parts[1], "\t".join(parts[2:])
        # 明细格式: id=110054|状态=1|班=2|名=孟羿|电话=150****6260|时间=...
        m = re.search(r"id=(\d+)", detail)
        name = re.search(r"名=([^|]+)", detail)
        phone = re.search(r"电话=([^|]+)", detail)
        class_m = re.search(r"班=(\d+)", detail)
        rows.append({
            "id_card": id_card,
            "count": int(count),
            "name": name.group(1) if name else "",
            "phone": phone.group(1) if phone else "",
            "class_id": class_m.group(1) if class_m else "",
            "first_id": m.group(1) if m else "",
            "detail": detail,
        })
    return rows
```

- [ ] **步骤 3：快速冒烟测试（打印 ② 块解析结果行数）**

运行：`"***REMOVED******REMOVED***/binaries/python/versions/3.13.12/python.exe" -c "import sys; sys.path.insert(0,'tmp'); import gen_dup_list as g; t=open('tmp/dup_query_result.txt',encoding='utf-8').read(); b=g.split_blocks(t); r=g.parse_idcard_block(b['②']); print(len(r)); [print(x) for x in r[:3]]"`

预期：输出 `11` 行，前 3 行分别含 `130638********6538 / 2 / 孟羿`、`330103********1024 / 2 / 许淡胭`、`330103********1022 / 2 / 陈诺颖`

---

### 任务 2：③手机号块 + ④同班块 + ⑦计数块解析

**文件：**
- 修改：`tmp/gen_dup_list.py`

- [ ] **步骤 1：写③块解析函数**

```python
def parse_phone_block(text: str):
    """③ 块：每行 = 手机号_脱敏 | 有效报名数 | 明细(含 证件=脱敏身份证)"""
    rows = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("手机号"):
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        phone, count, detail = parts[0], parts[1], "\t".join(parts[2:])
        id_card = re.search(r"证件=([^|]+)", detail)
        name = re.search(r"名=([^|]+)", detail)
        class_m = re.search(r"班=(\d+)", detail)
        m = re.search(r"id=(\d+)", detail)
        rows.append({
            "phone": phone,
            "count": int(count),
            "id_card": id_card.group(1) if id_card else "",
            "name": name.group(1) if name else "",
            "class_id": class_m.group(1) if class_m else "",
            "first_id": m.group(1) if m else "",
            "detail": detail,
        })
    return rows
```

- [ ] **步骤 2：写④块解析函数（同班重复，最严重）**

```python
def parse_sameclass_block(text: str):
    """④ 块：每行 = 身份证_脱敏 | 班级 | 同班记录数 | 明细(含多条 id=)"""
    rows = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("身份证"):
            continue
        parts = line.split("\t")
        if len(parts) < 4:
            continue
        id_card, class_id, cnt, detail = parts[0], parts[1], parts[2], "\t".join(parts[3:])
        ids = re.findall(r"id=(\d+)", detail)
        name = re.search(r"名=([^|]+)", detail)
        rows.append({
            "id_card": id_card,
            "class_id": class_id,
            "same_class_count": int(cnt),
            "ids": ids,
            "name": name.group(1) if name else "",
            "detail": detail,
        })
    return rows
```

- [ ] **步骤 3：写⑦块解析函数（enrolled 计数核对，附在名单尾部）**

```python
def parse_enrolled_block(text: str):
    """⑦ 块：每行 = id | name | quota | enrolled | 实际有效报名"""
    rows = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("id"):
            continue
        parts = line.split("\t")
        if len(parts) < 5:
            continue
        rows.append({
            "class_id": parts[0], "name": parts[1],
            "quota": parts[2], "enrolled": parts[3], "actual": parts[4],
        })
    return rows
```

- [ ] **步骤 4：冒烟测试**

运行：同上命令，打印 `parse_phone_block` 行数（预期 11）、`parse_sameclass_block` 行数（预期 3）、`parse_enrolled_block` 行数（预期 7）

---

### 任务 3：合并生成名单 + 输出 CSV + 控制台摘要

**文件：**
- 修改：`tmp/gen_dup_list.py`

- [ ] **步骤 1：写合并函数（以身份证为 key，③补手机号，④标同班重复）**

```python
def merge_rows(id_rows, phone_rows, sameclass_rows):
    """以脱敏身份证为 key 合并：姓名/手机号取②③一致值，④标记同班重复"""
    persons = OrderedDict()
    for r in id_rows:
        persons[r["id_card"]] = {
            "id_card": r["id_card"], "name": r["name"], "phone": r["phone"],
            "count": r["count"], "class_id": r["class_id"],
            "first_id": r["first_id"], "same_class": False,
            "same_class_detail": "",
        }
    for r in phone_rows:  # 补手机号（③明细里没直接列电话，用②的；这里仅补证件->名单验证）
        if r["id_card"] in persons and persons[r["id_card"]]["phone"] == "":
            persons[r["id_card"]]["phone"] = r["phone"]
    for r in sameclass_rows:  # 标记同班重复
        if r["id_card"] in persons:
            persons[r["id_card"]]["same_class"] = True
            persons[r["id_card"]]["same_class_detail"] = (
                f"班{r['class_id']}×{r['same_class_count']}条(id={','.join(r['ids'])})"
            )
    return list(persons.values())
```

- [ ] **步骤 2：写输出函数（CSV UTF-8 BOM + 控制台摘要）**

```python
def write_output(persons, enrolled_rows):
    with open(OUTPUT, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["序号", "姓名", "脱敏身份证", "脱敏手机号", "有效报名数",
                    "涉及班级", "同班重复", "同班重复明细", "首条记录id", "明细(截断)"])
        for i, p in enumerate(persons, 1):
            w.writerow([i, p["name"], p["id_card"], p["phone"], p["count"],
                        p["class_id"], "是" if p["same_class"] else "否",
                        p["same_class_detail"], p["first_id"], p["detail"]])
        w.writerow([])
        w.writerow(["== 班级 enrolled 计数核对 =="])
        w.writerow(["班级id", "班级名", "quota", "enrolled(表)", "实际有效报名"])
        for r in enrolled_rows:
            w.writerow([r["class_id"], r["name"], r["quota"], r["enrolled"], r["actual"]])

def print_summary(persons):
    print(f"重复报名人数: {len(persons)}")
    for p in persons:
        tag = " ⚠️同班重复" if p["same_class"] else ""
        print(f"  {p['name']} {p['id_card']} ×{p['count']} 班{p['class_id']}{tag}")
```

- [ ] **步骤 3：main 入口**

```python
def main():
    text = INPUT.read_text(encoding="utf-8")
    blocks = split_blocks(text)
    id_rows = parse_idcard_block(blocks["②"])
    phone_rows = parse_phone_block(blocks["③"])
    sameclass_rows = parse_sameclass_block(blocks["④"])
    enrolled_rows = parse_enrolled_block(blocks["⑦"])
    persons = merge_rows(id_rows, phone_rows, sameclass_rows)
    write_output(persons, enrolled_rows)
    print_summary(persons)
    print(f"\n名单已输出: {OUTPUT}")

if __name__ == "__main__":
    main()
```

- [ ] **步骤 4：运行脚本**

运行：`"***REMOVED******REMOVED***/binaries/python/versions/3.13.12/python.exe" tmp/gen_dup_list.py`

预期：控制台输出 11 行名单，其中贾静萱/黄琳茜带 `⚠️同班重复`；文件 `tmp/重复报名名单.csv` 生成

---

### 任务 4：真实数据断言测试

**文件：**
- 创建：`tmp/test_gen_dup_list.py`

- [ ] **步骤 1：编写断言测试**

```python
# -*- coding: utf-8 -*-
"""用存档的真实数据验证解析正确性"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import gen_dup_list as g

TEXT = Path(__file__).parent.joinpath("dup_query_result.txt").read_text(encoding="utf-8")
blocks = g.split_blocks(TEXT)
id_rows = g.parse_idcard_block(blocks["②"])
phone_rows = g.parse_phone_block(blocks["③"])
sameclass_rows = g.parse_sameclass_block(blocks["④"])
enrolled_rows = g.parse_enrolled_block(blocks["⑦"])
persons = g.merge_rows(id_rows, phone_rows, sameclass_rows)

def check(name, cond):
    print(("PASS" if cond else "FAIL"), name)
    assert cond, name

check("②身份证块=11人", len(id_rows) == 11)
check("③手机号块=11人", len(phone_rows) == 11)
check("④同班块=3组", len(sameclass_rows) == 3)
check("⑦班级块=7行", len(enrolled_rows) == 7)
check("合并名单=11人", len(persons) == 11)
check("贾静萱×5", next(p["count"] for p in persons if p["name"] == "贾静萱") == 5)
check("贾静萱同班重复", next(p["same_class"] for p in persons if p["name"] == "贾静萱") is True)
check("黄琳茜同班重复", next(p["same_class"] for p in persons if p["name"] == "黄琳茜") is True)
check("孟羿电话150****6260", next(p["phone"] for p in persons if p["name"] == "孟羿") == "150****6260")
check("邵佳琪×3", next(p["count"] for p in persons if p["name"] == "邵佳琪") == 3)
print(f"\n全部通过 ✅ 共 {len(persons)} 人")
```

- [ ] **步骤 2：运行测试**

运行：`"***REMOVED******REMOVED***/binaries/python/versions/3.13.12/python.exe" tmp/test_gen_dup_list.py`

预期：全部 PASS，`全部通过 ✅ 共 11 人`

- [ ] **步骤 3：人工核对输出 CSV**

用 Read 读取 `tmp/重复报名名单.csv`，核对：11 行名单与原始数据一致；贾静萱、黄琳茜两行 `同班重复=是` 且明细含 `班4×2条` / `班6×3条`、`班6×2条`

---

## 验收标准（自检清单）

1. **规格覆盖**：脚本完整覆盖 ②③④⑦ 四块解析 + 合并 + CSV 输出 + 测试，用户要求的"导入查出的数据生成重复名单"全部落实
2. **无占位符**：所有函数均有完整实现代码和预期输出
3. **类型一致**：`id_rows/phone_rows/sameclass_rows/enrolled_rows/persons` 命名全脚本统一，字段 key 一致（id_card/name/phone/count/class_id/first_id/same_class/same_class_detail/detail）
4. **容错**：明细截断（`...` 结尾）不影响解析，regex 找不到字段时给空串不报错
5. **隐私**：输出文件全部使用脱敏字段，无完整身份证/手机号
