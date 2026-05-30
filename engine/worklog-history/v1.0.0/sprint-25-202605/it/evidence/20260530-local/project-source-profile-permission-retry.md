# Sprint-25 F0/T02 权限重试与数据画像阻塞证据

**时间**: 2026-05-30
**范围**: 项目域必需 ODS、old PRS 本地源库、远端只读 row-count 探测。

## 本地 DTS ODS profile

命令：

```bash
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
```

结果摘要：

```text
11 required ODS tables: FOUND
metadata columns: _dts_source_system=present, _dts_import_time=present

ods_ptr_mysql_p_project    row_count=242
ods_ptr_mysql_p_customer   row_count=180

ods_ptr_mysql_b_goods                       row_count=0
ods_ptr_mysql_b_goods_price                 row_count=0
ods_ptr_mysql_p_contract                    row_count=0
ods_ptr_mysql_p_floor_layer                 row_count=0
ods_ptr_mysql_p_floor_number                row_count=0
ods_ptr_mysql_p_position                    row_count=0
ods_ptr_mysql_p_position_adjustment         row_count=0
ods_ptr_mysql_p_position_adjustment_item    row_count=0
ods_ptr_mysql_p_project_green               row_count=0
```

`p_project` / `p_customer` 当前可画像：

```text
p_project.status: 1=167, 2=75
p_customer.status: 1=175, 2=5
```

## 本地 old PRS MySQL 源库

命令使用 `prs-old-mysql` 容器内客户端，只读查询 `rs_cloud_flower` information_schema。

结果：

```text
b_goods                         0
b_goods_price                   0
p_contract                      0
p_customer                      0
p_floor_layer                   0
p_floor_number                  0
p_position                      0
p_position_adjustment           0
p_position_adjustment_item      0
p_project                       0
p_project_green                 0
```

结论：本地 old PRS seed 库不能作为 Sprint-25 项目域入数来源。

## 远端只读探测

使用 `docs/账号.txt` 中的敏感账号配置进行只读探测，未把连接凭据写入证据。

结果：

```text
remote mysql tcp port: reachable
mysql handshake: ERROR 2013 Lost connection at reading initial communication packet
remote ssh tcp port: reachable
ssh auth/tunnel: connection closed by remote host
```

结论：权限重试后，远端网络端口可达，但当前执行环境仍无法完成远端 MySQL/SSH 只读 row-count。Sprint-25 F0/T02 仍缺可访问的真实项目域业务数据。

## 当前阻塞

- `p_project_green`、`p_position_adjustment*`、`p_position`、`p_contract`、`b_goods*` 等项目域关键表仍无可画像数据。
- `p_project_green` 快照粒度、实摆组数、金额口径、停用项目过滤等 P0 决策仍不能由开发侧猜测。
- F1/F2/F3 只能保留 import-ready dbt 包和验收框架，不能标记生产事实口径 DONE。
