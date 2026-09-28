# Sprint-25 F0/T02 权限重试与数据画像证据

**时间**: 2026-05-30
**范围**: 项目域必需 ODS、old PRS 本地源库、远端只读 row-count 探测。

> 2026-05-30 后续权限恢复并完成 ingestion task `46` 后，本地 DTS ODS 阻塞已解除；本文件保留首轮失败上下文，并补充最新重试结果。P0 业务口径仍未由开发侧拍板。

## 本地 DTS ODS profile

命令：

```bash
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
```

首轮结果摘要：

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

结论：首轮权限重试后，远端网络端口可达，但当前执行环境仍无法完成远端 MySQL/SSH 只读 row-count。当时 Sprint-25 F0/T02 仍缺可访问的真实项目域业务数据。

## 后续权限恢复后重试

命令：

```bash
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
```

结果摘要：

```text
11 required ODS tables: FOUND
metadata columns: _dts_source_system=present, _dts_import_time=present

ods_ptr_mysql_b_goods                    row_count=6027
ods_ptr_mysql_b_goods_price              row_count=6517
ods_ptr_mysql_p_contract                 row_count=306
ods_ptr_mysql_p_customer                 row_count=180
ods_ptr_mysql_p_floor_layer              row_count=1679
ods_ptr_mysql_p_floor_number             row_count=362
ods_ptr_mysql_p_position                 row_count=17396
ods_ptr_mysql_p_position_adjustment      row_count=6016
ods_ptr_mysql_p_position_adjustment_item row_count=21009
ods_ptr_mysql_p_project                  row_count=242
ods_ptr_mysql_p_project_green            row_count=36295
```

远端直连复测仍未成功，未写入任何连接凭据：

```text
remote mysql: ERROR 2013 Lost connection at reading initial communication packet
remote ssh: Connection closed by remote host
```

当前可用数据面以 ingestion task `46` 入湖后的本地 DTS ODS 为准，详见 `project-ingestion-runtime.md` 与 `project-profile-after-ingestion.md`。

## 当前阻塞

- 项目域关键表已具备可画像数据；F0/T02 入数阻塞解除。
- `p_project_green` 快照粒度、实摆组数、金额口径、停用项目过滤等 P0 决策仍不能由开发侧猜测。
- F1/F2/F3 可标记 baseline 完成，但不能把项目域 ADS 晋升为最终生产业务口径。
