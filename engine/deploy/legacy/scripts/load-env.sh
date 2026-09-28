#!/usr/bin/env bash
# 由仓库根目录下的 build.sh、start.sh、dev.sh source。
# 先加载发布版本与运行配置，再计算镜像默认值，保留 IMAGE_* 显式覆盖。
set -a
if [[ -f .env ]]; then
  source <(sed $'s/\r$//' .env)
fi
source ./imgversion.conf
set +a
