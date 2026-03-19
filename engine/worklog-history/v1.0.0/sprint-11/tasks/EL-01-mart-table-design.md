# EL-01: 主题层表结构设计

**优先级**: P0
**状态**: READY
**依赖**: BG-02

## 目标

设计两个主题层物化表的完整 DDL，包含字段定义、索引策略、分区方案和 watermark 机制。

## 技术设计

### 1. mart_project_fulfillment_daily

项目履约日维度宽表，每天每项目一行。

```sql
CREATE TABLE mart_project_fulfillment_daily (
    id                      BIGSERIAL PRIMARY KEY,

    -- 维度
    snapshot_date           DATE          NOT NULL,   -- 快照日期（粒度）
    project_id              BIGINT        NOT NULL,
    project_name            VARCHAR(256)  NOT NULL,
    project_code            VARCHAR(64),
    project_status_name     VARCHAR(16),              -- 正常/停用
    project_type_name       VARCHAR(16),              -- 租摆/节日摆
    customer_name           VARCHAR(256),
    contract_title          VARCHAR(256),
    contract_status_name    VARCHAR(16),
    settlement_type_name    VARCHAR(32),              -- 按实摆结算/固定月租
    manager_name            VARCHAR(128),
    biz_user_name           VARCHAR(128),
    curing_director_name    VARCHAR(128),

    -- 库存度量（当日快照）
    green_count             INT DEFAULT 0,            -- 在摆绿植数
    position_count          INT DEFAULT 0,            -- 摆位总数
    total_monthly_rent      DECIMAL(12,2) DEFAULT 0,  -- 当前月租金合计（参考值）

    -- 业务事件度量（当日增量）
    add_flower_count        INT DEFAULT 0,            -- 当日加花单数
    change_flower_count     INT DEFAULT 0,            -- 当日换花单数
    cut_flower_count        INT DEFAULT 0,            -- 当日减花单数
    transfer_flower_count   INT DEFAULT 0,            -- 当日调花单数
    add_flower_quantity     INT DEFAULT 0,            -- 当日加花总量
    change_flower_quantity  INT DEFAULT 0,            -- 当日换花总量
    cut_flower_quantity     INT DEFAULT 0,            -- 当日减花总量

    -- 养护度量（当日）
    curing_count            INT DEFAULT 0,            -- 当日养护次数
    curing_positions        INT DEFAULT 0,            -- 当日养护摆位数

    -- 任务度量（当日快照）
    pending_task_count      INT DEFAULT 0,            -- 进行中任务数
    completed_task_count    INT DEFAULT 0,            -- 当日完成任务数

    -- 结算度量（月级，当月数据冗余到每天方便趋势查询）
    settlement_month        VARCHAR(7),               -- YYYY-MM
    monthly_receivable      DECIMAL(12,2) DEFAULT 0,  -- 当月应收
    monthly_received        DECIMAL(12,2) DEFAULT 0,  -- 当月已收
    monthly_outstanding     DECIMAL(12,2) DEFAULT 0,  -- 当月未收

    -- 同步元数据
    sync_batch_id           VARCHAR(64),              -- 同步批次ID
    synced_at               TIMESTAMP     NOT NULL DEFAULT now(),

    UNIQUE(project_id, snapshot_date)
);

-- 索引
CREATE INDEX idx_mart_pf_date ON mart_project_fulfillment_daily(snapshot_date);
CREATE INDEX idx_mart_pf_project ON mart_project_fulfillment_daily(project_id);
CREATE INDEX idx_mart_pf_project_date ON mart_project_fulfillment_daily(project_name, snapshot_date);
CREATE INDEX idx_mart_pf_customer ON mart_project_fulfillment_daily(customer_name);
CREATE INDEX idx_mart_pf_month ON mart_project_fulfillment_daily(settlement_month);
```

### 2. fact_field_operation_event

现场业务事件事实表，每条报花业务单据一行。

```sql
CREATE TABLE fact_field_operation_event (
    id                      BIGSERIAL PRIMARY KEY,

    -- 事件标识
    biz_id                  BIGINT        NOT NULL,   -- 原始 t_flower_biz_info.id
    biz_code                VARCHAR(64),

    -- 事件维度
    event_date              DATE          NOT NULL,   -- 事件日期（从 apply_time 提取）
    event_month             VARCHAR(7)    NOT NULL,   -- YYYY-MM
    event_year              INT           NOT NULL,
    biz_type_name           VARCHAR(16)   NOT NULL,   -- 换花/加花/减花/调花/...（已翻译）
    biz_status_name         VARCHAR(16)   NOT NULL,   -- 已翻译
    is_urgent               VARCHAR(4),               -- 是/否

    -- 项目维度
    project_id              BIGINT,
    project_name            VARCHAR(256),
    customer_name           VARCHAR(256),
    manager_name            VARCHAR(128),

    -- 操作人维度
    apply_user_name         VARCHAR(128),
    curing_user_name        VARCHAR(128),
    bear_cost_type_name     VARCHAR(16),              -- 费用承担方（已翻译）

    -- 度量
    plant_number            INT DEFAULT 0,            -- 绿植数量
    total_rent              DECIMAL(12,2) DEFAULT 0,  -- 业务总租金
    total_cost              DECIMAL(12,2) DEFAULT 0,  -- 业务总成本

    -- 时间戳
    apply_time              TIMESTAMP,                -- 原始发起时间
    finish_time             TIMESTAMP,                -- 完成时间

    -- 同步元数据
    source_updated_at       TIMESTAMP,                -- 原始记录最后更新时间（watermark 字段）
    sync_batch_id           VARCHAR(64),
    synced_at               TIMESTAMP     NOT NULL DEFAULT now(),

    UNIQUE(biz_id)
);

-- 索引
CREATE INDEX idx_fact_foe_date ON fact_field_operation_event(event_date);
CREATE INDEX idx_fact_foe_month ON fact_field_operation_event(event_month);
CREATE INDEX idx_fact_foe_type ON fact_field_operation_event(biz_type_name);
CREATE INDEX idx_fact_foe_project ON fact_field_operation_event(project_name);
CREATE INDEX idx_fact_foe_project_month ON fact_field_operation_event(project_name, event_month);
CREATE INDEX idx_fact_foe_status ON fact_field_operation_event(biz_status_name);
CREATE INDEX idx_fact_foe_watermark ON fact_field_operation_event(source_updated_at);
```

### 3. Watermark 同步状态表

```sql
CREATE TABLE elt_sync_watermark (
    id                  BIGSERIAL PRIMARY KEY,
    target_table        VARCHAR(128) NOT NULL UNIQUE,  -- 目标表名
    last_watermark      TIMESTAMP,                     -- 上次同步的最大 updated_at
    last_sync_time      TIMESTAMP,                     -- 上次同步执行时间
    last_sync_rows      INT DEFAULT 0,                 -- 上次同步行数
    last_sync_duration  INT DEFAULT 0,                 -- 上次同步耗时(ms)
    sync_status         VARCHAR(16) DEFAULT 'IDLE',    -- IDLE/RUNNING/FAILED
    error_message       TEXT,
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now()
);

INSERT INTO elt_sync_watermark (target_table) VALUES
    ('mart_project_fulfillment_daily'),
    ('fact_field_operation_event');
```

## 数据来源映射

### mart_project_fulfillment_daily

| 字段 | 来源 | 说明 |
|------|------|------|
| green_count | `SELECT count(*) FROM p_project_green WHERE project_id=? AND status=1` | 每日快照 |
| add_flower_count | `SELECT count(*) FROM t_flower_biz_info WHERE project_id=? AND biz_type=2 AND DATE(apply_time)=?` | 当日增量 |
| monthly_receivable | `SELECT total_rent FROM a_month_accounting WHERE project_id=? AND month=?` | 月级冗余 |
| curing_count | `SELECT count(*) FROM t_curing_record WHERE project_id=? AND DATE(curing_time)=?` | 当日增量 |

### fact_field_operation_event

| 字段 | 来源 |
|------|------|
| 全部 | `t_flower_biz_info` LEFT JOIN `p_project` + 状态码翻译（复用 BG-03 枚举词典） |
| watermark | `t_flower_biz_info.update_time` |

## 完成标准

- [ ] 三张表 DDL 确认（Liquibase changeset）
- [ ] 索引策略合理，覆盖常用查询维度
- [ ] watermark 表初始化
- [ ] 数据来源映射文档完成
