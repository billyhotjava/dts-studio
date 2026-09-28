# IN-03: 园林平台数据源注册

**状态**: READY
**依赖**: BA-02

## 目标

将园林平台的 MySQL 业务数据库注册为 copilot-analytics 的数据源，使 AI 和 BI 能查询园林业务数据。

## 技术设计

### 注册数据源

```json
{
  "name": "馨懿诚园林业务库",
  "dbType": "mysql",
  "jdbcUrl": "jdbc:mysql://garden-mysql:3306/flowers?useUnicode=true&characterEncoding=utf8",
  "username": "readonly_user",
  "password": "***"
}
```

### 安全考虑

- 使用**只读账号**连接园林数据库
- copilot-ai 的 SQL 沙箱确保只执行 SELECT 查询
- 建议创建 MySQL 只读用户：
  ```sql
  CREATE USER 'copilot_reader'@'%' IDENTIFIED BY 'xxx';
  GRANT SELECT ON flowers.* TO 'copilot_reader'@'%';
  ```

### 元数据同步

注册后 copilot-analytics 自动扫描表和列元数据，供 NL2SQL 和 BI 使用。

### 核心表（NL2SQL 上下文）

| 表名 | 说明 |
|------|------|
| p_project | 项目点 |
| p_contract | 合同 |
| p_position | 摆位 |
| u_personnel | 员工 |
| a_collection_record | 收款记录 |
| a_invoice_info | 发票 |
| flower_biz | 花卉业务操作 |
| store_house | 仓库 |
| purchase_info | 采购信息 |

## 影响文件

- 无代码修改，纯配置操作
- 可选：提供初始化脚本 `dts-copilot/scripts/register-garden-datasource.sh`

## 完成标准

- [ ] 园林 MySQL 数据源注册成功
- [ ] 表和列元数据扫描完成
- [ ] copilot-analytics 可查询园林业务表
- [ ] NL2SQL 可基于园林数据生成 SQL
