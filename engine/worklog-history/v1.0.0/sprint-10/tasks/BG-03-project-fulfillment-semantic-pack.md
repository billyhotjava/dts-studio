# BG-03: 状态码与业务枚举词典

**优先级**: P0
**状态**: READY
**依赖**: BG-01

## 目标

建立一张结构化的业务枚举词典表，覆盖所有关键表的状态码、业务类型码、分类码，使 NL2SQL 上下文编译时可自动注入相关枚举映射，同时支持视图层的状态翻译。

## 技术设计

### 词典表结构

在 `copilot_analytics` 库中新建 `biz_enum_dictionary` 表：

```sql
CREATE TABLE biz_enum_dictionary (
    id            BIGSERIAL PRIMARY KEY,
    source_db     VARCHAR(64)   NOT NULL,  -- 来源数据库（rs_cloud_flower）
    table_name    VARCHAR(128)  NOT NULL,  -- 表名
    field_name    VARCHAR(128)  NOT NULL,  -- 字段名
    code          VARCHAR(32)   NOT NULL,  -- 编码值
    label         VARCHAR(64)   NOT NULL,  -- 中文显示名
    description   VARCHAR(256),            -- 补充说明
    sort_order    INT DEFAULT 0,           -- 排序
    is_active     BOOLEAN DEFAULT true,    -- 是否启用
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now(),
    UNIQUE(source_db, table_name, field_name, code)
);
```

### 初始数据（基于源码审查）

**p_project.status**
| code | label | description |
|------|-------|-------------|
| 1 | 正常 | 项目正常运营中 |
| 2 | 停用 | 项目已停用 |

**p_project.type**
| code | label |
|------|-------|
| 1 | 租摆 |
| 2 | 节日摆 |

**p_contract.status**
| code | label |
|------|-------|
| 1 | 草稿 |
| 2 | 执行中 |
| 3 | 已结束 |

**p_contract.settlement_type**
| code | label |
|------|-------|
| 1 | 按实摆结算 |
| 2 | 固定月租 |

**p_position.status**
| code | label |
|------|-------|
| 0 | 正常 |
| 1 | 停用 |

**p_position.type**
| code | label |
|------|-------|
| 1 | 室内 |
| 2 | 室外 |

**p_project_green.status**
| code | label |
|------|-------|
| 1 | 摆放中 |
| 2 | 换花中 |
| 3 | 加花中 |
| 4 | 减花中 |
| 5 | 调花中 |
| 6 | 坏账处理中 |
| 7 | 已结束 |

**t_flower_biz_info.biz_type**
| code | label | description |
|------|-------|-------------|
| 1 | 换花 | 替换现有绿植 |
| 2 | 加花 | 新增绿植 |
| 3 | 减花 | 退租移除绿植 |
| 4 | 调花 | 项目间转移绿植 |
| 5 | 售花 | 销售绿植 |
| 6 | 坏账 | 损坏核销 |
| 7 | 销售 | 销售（含组合） |
| 8 | 内购 | 内部采购 |
| 11 | 加盆架 | 新增花盆/花架 |
| 12 | 减盆架 | 移除花盆/花架 |

**t_flower_biz_info.status**
| code | label |
|------|-------|
| -1 | 作废 |
| 1 | 审核中 |
| 2 | 备货中 |
| 3 | 核算中 |
| 4 | 待结算 |
| 5 | 已完成 |
| 20 | 草稿 |
| 21 | 驳回 |

**t_flower_biz_info.bear_cost_type**
| code | label |
|------|-------|
| 1 | 养护人 |
| 2 | 领导 |
| 3 | 公司 |
| 4 | 客户 |

**i_pendulum_info.status**
| code | label |
|------|-------|
| 1 | 草稿 |
| 2 | 待审批 |
| 3 | 初摆中 |
| 4 | 已完成 |
| 5 | 审批驳回 |
| 6 | 已作废 |

**t_daily_task_info.status**
| code | label |
|------|-------|
| -1 | 已作废 |
| 1 | 待发起 |
| 2 | 进行中 |
| 10 | 已结束 |

**t_daily_task_info.task_type**
| code | label |
|------|-------|
| 1 | 销售类 |
| 2 | 内购类 |
| 3 | 实摆变更类 |
| 4 | 增值服务类 |
| 5 | 支持类 |
| 6 | 初摆类 |

**t_curing_record.record_type**
| code | label |
|------|-------|
| 1 | 养护记录 |
| 2 | 工作日报 |

**p_project.check_cycle**
| code | label |
|------|-------|
| 1 | 每月 |
| 2 | 双月 |
| 3 | 季度 |
| 6 | 半年 |
| 12 | 年度 |

### 易混淆业务词汇表

同步维护在 `analytics_synonym` 或词典补充字段中：

| 用户说法 | 实际含义 | 备注 |
|----------|---------|------|
| 报花 | 所有花卉业务的统称 | 不是某一种 biz_type |
| 减花 | 退租移除绿植 (biz_type=3) | ≠ 剪花（修剪养护） |
| 剪花 | 养护修剪 | 属于养护记录，不是报花业务 |
| 调花 | 项目间转移绿植 (biz_type=4) | ≠ 摆位调整（p_position_adjustment，同项目内移动） |
| 摆位调整 | 同项目内移动绿植位置 | 对应 p_position_adjustment 表 |
| 实摆 | 当前实际在位的绿植 | 对应 p_project_green |
| 初摆 | 新项目首次布置 | 对应 i_pendulum_info |
| 项目点 | 客户现场的物理地点 | 对应 p_project |
| 摆位 | 项目点内具体放花的位置 | 对应 p_position |

## 消费方式

1. **视图层消费**：视图 DDL 中 CASE WHEN 引用词典值做状态翻译
2. **NL2SQL 上下文注入**：编译 prompt 时自动附加目标视图相关的枚举说明
3. **同义词关联**：与 `analytics_synonym` 联动，支持用户自然语言到精确编码的映射

## 完成标准

- [ ] `biz_enum_dictionary` 表创建（Liquibase changeset）
- [ ] 初始数据 seed 脚本覆盖上述所有枚举
- [ ] 提供按 table_name + field_name 查询枚举的 API
- [ ] 易混淆词汇表入库到 `analytics_synonym`
- [ ] BG-02 视图层可引用词典做状态翻译
