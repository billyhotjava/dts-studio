# BG-11: Copilot 交互体验增强

**优先级**: P0
**状态**: READY
**依赖**: BG-04, BG-07

## 目标

消除 NL2SQL 在前端交互层的三个核心体验断裂：用户不知道问什么、SQL 结果要跳页才能看、模型没有反馈闭环。使 Copilot 聊天面板成为可自洽的业务问答入口。

## 问题背景

当前 `CopilotChat.tsx`（822 行）已具备多轮对话、SQL 提取、数据源选择和 Tool 追踪等基础能力，但存在三个影响落地的 P0 缺口：

1. **空白对话框**：用户打开 Copilot 面板看到空输入框，不知道能问什么、怎么问
2. **结果跳转断裂**：SQL 提取后只能点"创建可视化"跳到 `/questions/new`，离开对话上下文，无法回来修正
3. **无反馈回收**：用户无法标记"有用/无用"，团队无法知道哪些问题回答得好、哪些差

## 技术设计

### 一、推荐问句与快捷入口

#### 1.1 新会话欢迎卡片

在空会话（无消息）时显示欢迎卡片，替代空白输入框：

```tsx
// CopilotChat.tsx - 新增 WelcomeCard 组件
function WelcomeCard({ onQuestionClick }: { onQuestionClick: (q: string) => void }) {
    return (
        <div className="copilot-welcome">
            <div className="copilot-welcome__icon">🌿</div>
            <h3 className="copilot-welcome__title">你好，我是绿植业务助手</h3>
            <p className="copilot-welcome__desc">
                我可以帮你查询项目、报花、结算、任务、养护等业务数据。试试问我：
            </p>
            <div className="copilot-welcome__suggestions">
                {ROLE_SUGGESTIONS.map((group) => (
                    <div key={group.role} className="copilot-welcome__group">
                        <span className="copilot-welcome__role">{group.role}</span>
                        {group.questions.map((q) => (
                            <button
                                key={q}
                                className="copilot-welcome__question"
                                onClick={() => onQuestionClick(q)}
                            >
                                {q}
                            </button>
                        ))}
                    </div>
                ))}
            </div>
        </div>
    );
}
```

#### 1.2 推荐问句数据源

从 BG-04 的 `nl2sql_query_template` 表获取推荐问句：

```typescript
// 新增 API
listSuggestedQuestions: (limit = 12) =>
    fetchJson<SuggestedQuestion[]>("/api/ai/nl2sql/suggestions?limit=" + limit),

// 类型定义
interface SuggestedQuestion {
    templateCode: string;
    domain: string;        // project / flowerbiz / settlement / task / curing
    roleHint: string;      // manager / biz / ops / finance / curing
    question: string;      // 示例问句
    description?: string;  // 简短说明
}
```

#### 1.3 角色分组推荐

按用户角色（未来可从登录信息自动推断）分组展示：

```typescript
const ROLE_SUGGESTIONS = [
    {
        role: "项目管理",
        questions: [
            "当前在服项目一共多少个？",
            "哪些合同90天内到期？",
            "各项目在摆绿植数排行",
        ],
    },
    {
        role: "花卉业务",
        questions: [
            "本月各类报花业务数量分布",
            "加花次数最多的项目是哪个？",
            "有多少待审批的报花单？",
        ],
    },
    {
        role: "财务结算",
        questions: [
            "上月未结算的项目有哪些？",
            "各客户欠款排名",
            "本月已开票金额是多少？",
        ],
    },
    {
        role: "现场养护",
        questions: [
            "本月哪些摆位还没做过养护？",
            "养护人均负责多少摆位？",
            "进行中的初摆任务有几个？",
        ],
    },
];
```

#### 1.4 对话尾部追问建议

每次 AI 回复后，在消息底部展示 2-3 个追问建议（由后端根据当前会话上下文生成，或从模板库匹配同域问句）：

```tsx
// AI 消息底部
{msg.role === "assistant" && followUpSuggestions.length > 0 && (
    <div className="copilot-chat__followups">
        {followUpSuggestions.map((q) => (
            <button
                key={q}
                className="copilot-chat__followup-btn"
                onClick={() => handleSend(q)}
            >
                {q}
            </button>
        ))}
    </div>
)}
```

---

### 二、Chat 内 SQL 预览与内联执行

#### 2.1 替换当前的跳转模式

当前行为：
```
提取 SQL → 显示两个按钮（创建可视化 / 复制 SQL）→ 点击跳转到 /questions/new
```

目标行为：
```
提取 SQL → 显示 SQL 预览（语法高亮）→ 用户可编辑 → 点击"执行"→ 结果在 Chat 内展示
                                                    → 点击"创建可视化" → 跳转（保留）
```

#### 2.2 内联 SQL 预览组件

```tsx
// 新增 InlineSqlPreview 组件
function InlineSqlPreview({
    sql,
    databaseId,
    onExecute,
}: {
    sql: string;
    databaseId?: number;
    onExecute?: (result: CardQueryResponse) => void;
}) {
    const [editableSql, setEditableSql] = useState(sql);
    const [isEditing, setIsEditing] = useState(false);
    const [runState, setRunState] = useState<"idle" | "loading" | "loaded" | "error">("idle");
    const [result, setResult] = useState<CardQueryResponse | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleRun = async () => {
        if (!databaseId || !editableSql.trim()) return;
        setRunState("loading");
        try {
            const res = await analyticsApi.runDatasetQuery({
                database: databaseId,
                type: "native",
                native: { query: editableSql.trim() },
                context: "copilot-inline",
            });
            setResult(res);
            setRunState("loaded");
            onExecute?.(res);
        } catch (e) {
            setError(e instanceof Error ? e.message : String(e));
            setRunState("error");
        }
    };

    return (
        <div className="copilot-sql-preview">
            <div className="copilot-sql-preview__header">
                <span className="copilot-sql-preview__label">生成的 SQL</span>
                <div className="copilot-sql-preview__actions">
                    <button onClick={() => setIsEditing(!isEditing)}>
                        {isEditing ? "收起编辑" : "编辑"}
                    </button>
                    <button onClick={() => navigator.clipboard.writeText(editableSql)}>
                        复制
                    </button>
                </div>
            </div>

            {isEditing ? (
                <textarea
                    className="copilot-sql-preview__editor"
                    value={editableSql}
                    onChange={(e) => setEditableSql(e.target.value)}
                    rows={Math.min(editableSql.split("\n").length + 1, 12)}
                />
            ) : (
                <pre className="copilot-sql-preview__code">
                    <code>{editableSql}</code>
                </pre>
            )}

            <div className="copilot-sql-preview__toolbar">
                <button
                    className="copilot-sql-preview__run-btn"
                    onClick={handleRun}
                    disabled={runState === "loading"}
                >
                    {runState === "loading" ? "执行中..." : "▶ 执行查询"}
                </button>
                <button
                    className="copilot-sql-preview__viz-btn"
                    onClick={() => {
                        const params = new URLSearchParams({
                            sql: editableSql,
                            ...(databaseId ? { db: String(databaseId) } : {}),
                            autorun: "1",
                        });
                        window.location.href = `/questions/new?${params.toString()}`;
                    }}
                >
                    创建可视化
                </button>
            </div>

            {/* 内联结果展示 */}
            {runState === "loaded" && result?.data && (
                <div className="copilot-sql-preview__result">
                    <div className="copilot-sql-preview__result-meta">
                        {result.row_count != null && <span>{result.row_count} 行</span>}
                        {result.running_time != null && <span>{result.running_time}ms</span>}
                    </div>
                    <DataTable
                        cols={result.data.cols ?? []}
                        rows={result.data.rows ?? []}
                        maxRows={100}
                        pageSize={10}
                    />
                </div>
            )}

            {runState === "error" && (
                <div className="copilot-sql-preview__error">
                    <span>查询失败：{error}</span>
                    <button onClick={() => setIsEditing(true)}>编辑 SQL 修正</button>
                </div>
            )}
        </div>
    );
}
```

#### 2.3 替换 CopilotChat 中的 SQL 展示

在 `CopilotChat.tsx` 中替换原有的两个按钮：

```tsx
// 替换 lines 569-607
{extractedSql && (
    <InlineSqlPreview
        sql={extractedSql}
        databaseId={selectedDbId ?? undefined}
    />
)}
```

#### 2.4 内联结果的约束

- 内联结果表格最多显示 100 行（完整结果引导到可视化页）
- 内联 DataTable 使用紧凑模式（`pageSize=10`，无分页导航）
- 执行失败时显示错误 + "编辑 SQL 修正" 按钮
- 标记 `context: "copilot-inline"` 用于审计区分

---

### 三、反馈机制

#### 3.1 消息级反馈

每条 AI 回复底部增加反馈按钮：

```tsx
// AI 消息底部操作栏
{msg.role === "assistant" && (
    <div className="copilot-chat__msg-actions">
        {/* SQL 预览区域 */}
        {extractedSql && <InlineSqlPreview sql={extractedSql} databaseId={selectedDbId} />}

        {/* 反馈按钮 */}
        <div className="copilot-chat__feedback">
            <button
                className={`copilot-chat__feedback-btn ${feedback === "up" ? "active" : ""}`}
                onClick={() => handleFeedback(msg.id, "up")}
                title="有帮助"
            >
                👍
            </button>
            <button
                className={`copilot-chat__feedback-btn ${feedback === "down" ? "active" : ""}`}
                onClick={() => handleFeedback(msg.id, "down")}
                title="不准确"
            >
                👎
            </button>
        </div>
    </div>
)}
```

#### 3.2 负面反馈详情收集

点踩后弹出简短反馈表单：

```tsx
function FeedbackForm({ messageId, onSubmit, onCancel }: FeedbackFormProps) {
    const [reason, setReason] = useState<string>("");
    const [detail, setDetail] = useState<string>("");

    const REASONS = [
        { value: "wrong_sql", label: "SQL 不正确" },
        { value: "wrong_data", label: "数据不准确" },
        { value: "wrong_table", label: "查错了表" },
        { value: "misunderstood", label: "没理解我的意思" },
        { value: "too_slow", label: "响应太慢" },
        { value: "other", label: "其他" },
    ];

    return (
        <div className="copilot-feedback-form">
            <p>哪里不对？</p>
            <div className="copilot-feedback-form__reasons">
                {REASONS.map((r) => (
                    <button
                        key={r.value}
                        className={`copilot-feedback-form__reason ${reason === r.value ? "active" : ""}`}
                        onClick={() => setReason(r.value)}
                    >
                        {r.label}
                    </button>
                ))}
            </div>
            {reason && (
                <textarea
                    placeholder="补充说明（可选）"
                    value={detail}
                    onChange={(e) => setDetail(e.target.value)}
                    rows={2}
                />
            )}
            <div className="copilot-feedback-form__actions">
                <button onClick={onCancel}>取消</button>
                <button
                    onClick={() => onSubmit({ messageId, rating: "down", reason, detail })}
                    disabled={!reason}
                >
                    提交
                </button>
            </div>
        </div>
    );
}
```

#### 3.3 后端反馈 API

```typescript
// 新增 API
submitChatFeedback: (body: {
    sessionId: string;
    messageId: string;
    rating: "up" | "down";
    reason?: string;
    detail?: string;
    generatedSql?: string;
    correctedSql?: string;
}) => sendJson<void>("/api/ai/agent/chat/feedback", body),
```

#### 3.4 反馈数据存储

在 `copilot_ai` 库新建 `ai_chat_feedback` 表：

```sql
CREATE TABLE ai_chat_feedback (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(64)  NOT NULL,
    message_id      VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64),
    user_name       VARCHAR(128),
    rating          VARCHAR(8)   NOT NULL,  -- up / down
    reason          VARCHAR(32),            -- wrong_sql / wrong_data / wrong_table / misunderstood / too_slow / other
    detail          TEXT,
    generated_sql   TEXT,                   -- AI 生成的 SQL
    corrected_sql   TEXT,                   -- 用户修正后的 SQL（如有）
    routed_domain   VARCHAR(32),            -- 路由到的业务域
    target_view     VARCHAR(128),           -- 目标视图
    template_code   VARCHAR(64),            -- 命中的模板编码（如有）
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_feedback_rating ON ai_chat_feedback(rating);
CREATE INDEX idx_feedback_reason ON ai_chat_feedback(reason);
CREATE INDEX idx_feedback_created ON ai_chat_feedback(created_at);
```

#### 3.5 反馈数据消费

- **短期**：运营通过 SQL Workbench 查看反馈数据，人工分析高频失败模式
- **中期**：负面反馈中 `reason = 'wrong_sql'` 且有 `corrected_sql` 的记录，自动沉淀为 few-shot 样本
- **长期**：反馈数据驱动预制模板和同义词的迭代

---

## 影响范围

| 模块 | 变更 |
|------|------|
| `dts-copilot-webapp` | 新增 `WelcomeCard`, `InlineSqlPreview`, `FeedbackForm` 组件；修改 `CopilotChat.tsx` |
| `dts-copilot-ai` | 新增 `/api/ai/nl2sql/suggestions` 和 `/api/ai/agent/chat/feedback` 端点；新增 `ai_chat_feedback` 表 |
| `dts-copilot-ai` | Liquibase changeset：`ai_chat_feedback` |

## 完成标准

- [ ] 新会话显示欢迎卡片 + 4 组推荐问句
- [ ] 点击推荐问句自动填入输入框并发送
- [ ] AI 回复中的 SQL 以预览卡片展示（替代纯文本）
- [ ] SQL 预览支持编辑模式
- [ ] SQL 预览支持内联执行，结果在 Chat 内以表格展示（最多 100 行）
- [ ] 执行失败时显示错误信息 + "编辑修正"引导
- [ ] "创建可视化" 按钮保留，跳转到 CardEditor
- [ ] 每条 AI 回复底部有 👍👎 反馈按钮
- [ ] 点踩后弹出反馈表单（原因选择 + 补充说明）
- [ ] 反馈数据持久化到 `ai_chat_feedback` 表
- [ ] 反馈 API 记录 session/message/user/sql/domain 等上下文
