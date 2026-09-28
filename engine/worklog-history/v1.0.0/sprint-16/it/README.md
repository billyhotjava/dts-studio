# Sprint-16 IT 验收说明

当前 `it/` 目录已落地三条可执行 smoke：

1. `test_fixed_report_fastpath.sh`
   - 登录本地 `3003`
   - 校验固定报表目录可返回数据
   - 校验固定报表运行态不再 404，并正确返回 `BACKING_REQUIRED`
2. `test_copilot_template_first.sh`
   - 验证 `财务报表` 优先返回固定报表候选
   - 验证 `采购汇总` 优先命中固定报表模板，而不是直接进入探索式 SQL
3. `test_multi_surface_fixed_report_reuse.sh`
   - 真实浏览器登录本地 `3003`
   - 校验 `Dashboards / Report Factory / Screens` 的固定报表快捷入口
   - 校验三处入口都进入各自的创建流程，并正确带入 `fixedReportTemplate`

## 运行方式

```bash
bash worklog/v1.0.0/sprint-16/it/test_fixed_report_fastpath.sh
bash worklog/v1.0.0/sprint-16/it/test_copilot_template_first.sh
bash worklog/v1.0.0/sprint-16/it/test_multi_surface_fixed_report_reuse.sh
```

可选环境变量：

- `BASE_URL`：默认 `http://127.0.0.1:3003`
- `ANALYTICS_USERNAME`：默认 `admin`
- `ANALYTICS_PASSWORD`：默认 `Devops123@`
- `DATASOURCE_ID`：默认 `7`

## 当前基线

详见 [performance-baseline.md](./performance-baseline.md)。
