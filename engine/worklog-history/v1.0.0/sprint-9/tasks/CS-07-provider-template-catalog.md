# CS-07: Provider 模板目录增强

**状态**: DONE
**依赖**: AE-03, CS-01

## 目标

增强 `ProviderTemplate` 目录，补充国际主流、中国主流、本地部署三类 LLM Provider，并返回推荐模板元数据，供前端统一渲染下拉和默认值。

## 技术设计

- 扩展 `ProviderTemplate` 元数据：
  - `region`
  - `category`
  - `recommended`
  - `sortOrder`
- 输出到 `/api/ai/config/providers/templates`
- 保持现有 `name/displayName/defaultBaseUrl/defaultModel` 等字段不变

## 完成标准

- [ ] 模板接口返回分组和推荐信息
- [ ] 覆盖国际主流、中国主流、本地部署主流 Provider
- [ ] 推荐模板可供前端在“新建 Provider”时默认使用
