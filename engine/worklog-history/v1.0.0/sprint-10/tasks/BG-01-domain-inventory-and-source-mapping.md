# BG-01: 业务域盘点与语义源映射

**状态**: READY
**依赖**: IN-03, NV-07

## 目标

盘点 `adminapi/adminweb` 中与项目履约、现场业务相关的控制器、API、页面和候选数据表，形成业务语义资产的来源清单。

## 技术设计

- 以 `project / flowerbiz / tasknew / pendulum` 为优先模块
- 建立映射：
  - 业务对象 -> 控制器/API -> 页面 -> 候选表
  - 业务词 -> 中文叫法 -> 英文/表字段名
- 输出后续语义包、join contract、指标口径的原始来源

## 参考范围

- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/flowerbiz/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/tasknew/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/pendulum/**`
- `adminweb/src/api/flower/**`
- `adminweb/src/views/flower/**`

## 完成标准

- [ ] 形成两个主题域的业务对象清单
- [ ] 形成业务对象到源码入口的映射表
- [ ] 标出高频业务词、关键时间字段、关键状态字段
- [ ] 为 BG-02~BG-06 提供可直接引用的来源清单
