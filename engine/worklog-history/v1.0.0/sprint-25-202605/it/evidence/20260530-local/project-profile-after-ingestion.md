# Sprint-25 Project Profile After Ingestion

**Date**: 2026-05-30  
**Environment**: local DTS PostgreSQL `biadmin.public`

## `p_project_green` Profile

Status distribution:

```text
 status | count
--------+-------
      1 | 36293
     -1 |     2
```

Import status distribution:

```text
 import_status | count
---------------+-------
             2 | 36266
             1 |    29
```

Parent bucket distribution:

```text
 parent_bucket | count
---------------+-------
 child         | 20209
 -1            | 16086
```

Numeric and date summary:

```text
 rows  | rent_nulls | cost_nulls | rent_sum  |  cost_sum  | total_number_sum | good_number_sum |    min_pose_time    |    max_pose_time
-------+------------+------------+-----------+------------+------------------+-----------------+---------------------+---------------------
 36295 |       4268 |          0 | 623546.44 | 1879939.26 |           203845 |          100138 | 2021-04-01 00:00:00 | 2024-09-01 00:00:00
```

dbt DWD quality markers:

```text
 rows  | orphan_project_rows | orphan_position_rows | orphan_goods_price_rows
-------+---------------------+----------------------+-------------------------
 36295 |                 286 |                    0 |                    1449
```

## `p_position_adjustment*` Profile

ODS soft-key check:

```text
 rows  | missing_parent_adjustment | orphan_old_position | orphan_new_position
-------+---------------------------+---------------------+---------------------
 21009 |                         0 |                 264 |                 245
```

DWD status distribution:

```text
 adjustment_status_code | rows  | adjustment_number_sum | rent_amount_raw_sum | cost_amount_raw_sum
------------------------+-------+-----------------------+---------------------+---------------------
 PAS-FINISHED           | 20984 |                 45236 |           341860.93 |          1025021.36
 PAS-CANCELLED          |    25 |                    25 |              351.00 |             1370.80
```

Adjustment type distribution:

```text
 adjustment_type_raw | rows
---------------------+-------
                   1 | 20623
                   2 |   249
                   3 |   137
```

## ADS/DWS Summary

```text
 rows | projects | green_row_count | good_number_sum | rent_amount_raw_sum | cost_amount_raw_sum
------+----------+-----------------+-----------------+---------------------+---------------------
  239 |      137 |           36295 |       100138.00 |           623546.44 |          1879939.26
```

```text
 rows | green_row_count | placed_row_count | effective_good_number_sum | rent_amount_raw_sum | cost_amount_raw_sum
------+-----------------+------------------+---------------------------+---------------------+---------------------
  222 |           36009 |            36007 |                 203227.00 |           614245.94 |          1862841.26
```

Status ADS:

```text
 status_subject | status_code  | status_label | row_count
----------------+--------------+--------------+-----------
 project        | PRJ-ACTIVE   | 正常         |       149
 project        | PRJ-INACTIVE | 停用         |        73
 project_green  | PGS-PLACED   | 摆放中       |     36293
 project_green  | PGS-UNKNOWN  | 未知         |         2
```

## Remaining P0 Decisions

- `rent` / `cost` remain raw sums until business confirms whether to multiply by `total_number` or `good_number`.
- `parent_id=-1` top-level rows account for 16086 of 36295 rows; group-count semantics still need business confirmation.
- Project status filtering still needs business confirmation because active-only ADS totals differ from all-row DWD totals.
