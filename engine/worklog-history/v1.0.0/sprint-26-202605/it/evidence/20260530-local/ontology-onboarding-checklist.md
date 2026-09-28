# Sprint-26 F4/T01 本体化域接入 checklist 验证

**时间**: 2026-05-30
**范围**: `assets/ontology-domain-onboarding-checklist.md`。

## RED 证据

命令：

```bash
bash worklog/v1.0.0/sprint-26-202605/it/test_ontology_onboarding_checklist.sh
```

结果：失败，checklist 文件尚不存在。

关键失败：

```text
missing checklist file: .../assets/ontology-domain-onboarding-checklist.md
```

## GREEN 证据

命令：

```bash
bash worklog/v1.0.0/sprint-26-202605/it/test_ontology_onboarding_checklist.sh
```

结果：

```text
[static] ontology onboarding checklist covers pack/runtime/reconcile/action/project rehearsal
```

覆盖点：

- pack 四节：links / metrics / signals / actions。
- Java 扩展点：`OntologyService`、`AssetBackedPlannerPolicy`、`PACK_FILES`、`normalizeSemanticDomain`。
- adminweb 对账、adminapi 草稿端点、`saveDraft*` 安全边界。
- Golden Questions 要求。
- 项目域纸面演练：`p_project_green`、`xycyl_dim_project`、`xycyl_dws_project_green_monthly`。
- adminapi gateway base URL 与业务 Authorization 阻塞处理约定。
