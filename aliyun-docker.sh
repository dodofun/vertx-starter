#!/bin/bash

# docker 登录
# docker login --username=dodofun2020 registry.cn-beijing.aliyuncs.com
# 设立镜像
docker tag dodofun/vertx:latest registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
# 更新镜像
docker push registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
# 拉取最新镜像
# docker pull registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest