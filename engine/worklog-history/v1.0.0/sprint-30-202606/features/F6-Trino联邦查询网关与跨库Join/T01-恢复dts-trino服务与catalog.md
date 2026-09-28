# T01 恢复 dts-trino 服务与 catalog

## 状态

DONE

## 实施要点

- 在 dts-stack `docker-compose-app.yml` 恢复 `dts-trino`。
- 加入 `dts-core` 网络访问 PG,并加入外部 `docker_huahui` 网络访问旧 MySQL。
- `postgres.properties` 与 `mysql.properties` 只引用 `${ENV:...}` 环境变量,不写入明文密码。

## 验收

- `docker compose config --services` 包含 `dts-trino`。
- `services/dts-trino/init/catalog/postgres.properties` 存在。
- `services/dts-trino/init/catalog/mysql.properties` 存在。
