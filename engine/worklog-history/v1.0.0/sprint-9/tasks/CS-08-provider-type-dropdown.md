# CS-08: Webapp Provider Type 下拉与推荐模板联动

**状态**: DONE
**依赖**: CS-04, CS-05, CS-07

## 目标

把 Provider Type 改成分组下拉框，并让“新建 Provider”默认套用推荐模板；保留 `Custom` 入口供后续扩展。

## 技术设计

- `Provider Type` 改为下拉框，不再自由文本输入
- 选项分组：
  - 国际主流
  - 中国主流
  - 本地部署
  - 自定义
- 新建时默认选中推荐模板并自动填充标准字段
- 编辑时切换类型仅覆盖模板字段，不破坏“API Key 留空不修改”语义

## 完成标准

- [ ] Provider Type 使用下拉框
- [ ] 新建 Provider 默认加载推荐模板
- [ ] 选择不同 Provider Type 时表单自动填充标准模板值
- [ ] 选择 `Custom` 时允许管理员手工填写
